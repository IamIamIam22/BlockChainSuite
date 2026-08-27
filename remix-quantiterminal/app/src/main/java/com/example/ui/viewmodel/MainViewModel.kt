package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseCloudService
import com.example.data.gemini.GeminiAiService
import com.example.data.local.AppDatabase
import com.example.data.local.CommandHistoryEntity
import com.example.data.local.InstalledPackageEntity
import com.example.data.model.ChatMessage
import com.example.data.model.CloudSyncStatus
import com.example.data.model.LineType
import com.example.data.model.SessionStateSnapshot
import com.example.data.model.SoftwareItem
import com.example.data.model.TerminalLine
import com.example.data.model.UserProfile
import com.example.data.model.VirtualFile
import com.example.data.model.VirtualMachine
import com.example.data.repository.SoftwareMarketRepository
import com.example.data.repository.VirtualMachineRepository
import com.example.engine.RemoteBridgeEngine
import com.example.engine.RemoteBridgeState
import com.example.engine.SessionManager
import com.example.engine.TerminalEngine
import com.example.engine.VmFileSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val currentTab: NavigationTab = NavigationTab.TERMINAL,
    val activeVm: VirtualMachine? = null,
    val vms: List<VirtualMachine> = emptyList(),
    val terminalLines: List<TerminalLine> = emptyList(),
    val currentCommandInput: String = "",
    val isExecuting: Boolean = false,
    val historyIndex: Int = -1,
    val softwareList: List<SoftwareItem> = emptyList(),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val installedPackages: List<InstalledPackageEntity> = emptyList(),
    val currentFiles: List<VirtualFile> = emptyList(),
    val currentDirectory: String = "/home/clouduser",
    val activeEditingFile: VirtualFile? = null,
    val activeEditorContent: String = "",
    val isEditorModified: Boolean = false,
    val isCreatingVmDialogVisible: Boolean = false,
    val activeListeningPorts: List<Int> = listOf(3000, 8080, 9090),
    val messageToast: String? = null,
    val bridgeState: RemoteBridgeState = RemoteBridgeState(),
    val windowsTestInput: String = "python3 main.py",
    val windowsTestOutput: String = "",
    val isWindowsExecuting: Boolean = false,

    // Firebase Auth & Cloud Sync
    val userProfile: UserProfile? = null,
    val isAuthenticating: Boolean = false,
    val sessionState: SessionStateSnapshot = SessionStateSnapshot("vm-1", "/home/clouduser", emptyMap()),

    // Gemini Chatbot
    val chatMessages: List<ChatMessage> = emptyList(),
    val chatInput: String = "",
    val isChatLoading: Boolean = false,
    val selectedGeminiModel: String = "gemini-3.5-flash",
    val isAuthDialogVisible: Boolean = false
)

enum class NavigationTab {
    TERMINAL,
    COPILOT,
    SESSION_CLOUD,
    WINDOWS_BRIDGE,
    MARKETPLACE,
    VM_MANAGER,
    CODE_WORKSPACE,
    PORTS_PREVIEW
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val vmRepo = VirtualMachineRepository(db.virtualMachineDao())
    val fileSystem = VmFileSystem(db.virtualFileDao())
    val terminalEngine = TerminalEngine(fileSystem, db.installedPackageDao())
    val remoteBridgeEngine = RemoteBridgeEngine(terminalEngine, viewModelScope)
    val firebaseCloudService = FirebaseCloudService(application)
    val sessionManager = SessionManager(db.terminalSessionDao(), firebaseCloudService, viewModelScope)
    val geminiAiService = GeminiAiService(db.geminiChatDao())

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val commandHistoryList = mutableListOf<String>()

    init {
        // Wire up remote bridge engine active VM callback
        remoteBridgeEngine.getActiveVm = { _uiState.value.activeVm }

        // Wire up remote command execution logger into the terminal
        remoteBridgeEngine.onRemoteCommandExecuted = { cmd, output, source, bypass ->
            val vm = _uiState.value.activeVm
            val prompt = if (vm != null) terminalEngine.getPrompt(vm) else "clouduser@cloudterm:~$ "
            val badge = if (bypass) " [🎯 BYPASS: Cloud Stream]" else ""
            val inputLine = TerminalLine(
                text = "💻 $source$badge: $cmd",
                type = LineType.INPUT
            )
            val outputLines = output.lines().map { lineStr ->
                TerminalLine(text = lineStr, type = LineType.OUTPUT)
            }
            _uiState.value = _uiState.value.copy(
                terminalLines = _uiState.value.terminalLines + listOf(inputLine) + outputLines
            )
            refreshFiles()
            // Auto save session after remote commands
            vm?.let {
                sessionManager.notifyStateChanged(
                    vmId = it.id,
                    currentDirectory = fileSystem.getCurrentDirectory(),
                    envVars = terminalEngine.getEnvVars(),
                    isBypass = remoteBridgeEngine.bridgeState.value.isBypassTargetingActive,
                    lastCommand = cmd
                )
            }
        }

        // Wire up terminal engine session change callback
        terminalEngine.onSessionStateChanged = { dir, envs ->
            _uiState.value.activeVm?.let { vm ->
                sessionManager.notifyStateChanged(
                    vmId = vm.id,
                    currentDirectory = dir,
                    envVars = envs,
                    isBypass = remoteBridgeEngine.bridgeState.value.isBypassTargetingActive
                )
                _uiState.value = _uiState.value.copy(
                    currentDirectory = dir
                )
            }
        }

        // Observe Session State
        viewModelScope.launch {
            sessionManager.currentSessionState.collect { snapshot ->
                _uiState.value = _uiState.value.copy(sessionState = snapshot)
            }
        }

        // Observe Firebase Auth
        viewModelScope.launch {
            firebaseCloudService.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(userProfile = user)
                if (user != null) {
                    _uiState.value.activeVm?.let { vm ->
                        sessionManager.forceCloudSync(
                            vmId = vm.id,
                            currentDirectory = fileSystem.getCurrentDirectory(),
                            envVars = terminalEngine.getEnvVars(),
                            isBypass = remoteBridgeEngine.bridgeState.value.isBypassTargetingActive
                        )
                    }
                }
            }
        }

        // Observe Gemini Chat Messages
        viewModelScope.launch {
            geminiAiService.persistedMessages.collect { msgs ->
                _uiState.value = _uiState.value.copy(chatMessages = msgs)
            }
        }

        // Observe Windows Bridge State
        viewModelScope.launch {
            remoteBridgeEngine.bridgeState.collect { bState ->
                _uiState.value = _uiState.value.copy(bridgeState = bState)
            }
        }

        // Initialize Virtual Machines
        viewModelScope.launch {
            vmRepo.initializeDefaultVmsIfEmpty()
        }

        viewModelScope.launch {
            vmRepo.allVms.collect { vmList ->
                val active = _uiState.value.activeVm?.let { current ->
                    vmList.find { it.id == current.id }
                } ?: vmList.firstOrNull()

                _uiState.value = _uiState.value.copy(
                    vms = vmList,
                    activeVm = active
                )

                if (active != null) {
                    if (fileSystem.getAllFiles().isEmpty()) {
                        fileSystem.initializeDefaultFiles(active.id, active.distro)
                        refreshFiles()
                        showInitialWelcomeBanner(active)
                    }

                    // Restore session (Working Directory & Environment Variables) for this VM
                    sessionManager.restoreSessionForVm(active.id) { restoredDir, restoredEnvs, isBypass ->
                        terminalEngine.setCurrentDirectory(restoredDir)
                        terminalEngine.setEnvVars(restoredEnvs)
                        remoteBridgeEngine.setBypassTargeting(isBypass)
                        refreshFiles()
                    }
                }
            }
        }

        // Initialize software catalog
        _uiState.value = _uiState.value.copy(softwareList = SoftwareMarketRepository.catalog)
    }

    private fun showInitialWelcomeBanner(vm: VirtualMachine) {
        val welcome = listOf(
            TerminalLine(text = "═════════════════════════════════════════════════════════════════════", type = LineType.ACCENT),
            TerminalLine(text = " 🚀 CloudTerm Virtual Machine Engine v2.5.0 (x86_64-cloud)", type = LineType.ACCENT),
            TerminalLine(text = " Connected to: ${vm.name} [${vm.region}] - Node IP: ${vm.ipAddress}", type = LineType.SYSTEM_INFO),
            TerminalLine(text = " Pre-installed Runtimes: Node.js 22, Python 3.12, Go 1.23, Ruby 3.3, Rust 1.80, Docker", type = LineType.SUCCESS),
            TerminalLine(text = " ✨ Auto-Resume Active: Working directory & environment variables automatically saved.", type = LineType.SUCCESS),
            TerminalLine(text = " 🤖 Gemini AI Copilot ready. Type 'help' or explore the AI Copilot tab.", type = LineType.SYSTEM_INFO),
            TerminalLine(text = "═════════════════════════════════════════════════════════════════════", type = LineType.ACCENT)
        )
        _uiState.value = _uiState.value.copy(terminalLines = welcome)
    }

    fun setTab(tab: NavigationTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
        if (tab == NavigationTab.CODE_WORKSPACE) {
            refreshFiles()
        }
    }

    fun setActiveVm(vm: VirtualMachine) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                activeVm = vm,
                terminalLines = _uiState.value.terminalLines + listOf(
                    TerminalLine(text = "🔄 Switched active VM connection to [${vm.name}] (${vm.ipAddress})", type = LineType.SYSTEM_INFO)
                )
            )
            fileSystem.initializeDefaultFiles(vm.id, vm.distro)
            
            // Restore VM session
            sessionManager.restoreSessionForVm(vm.id) { restoredDir, restoredEnvs, isBypass ->
                terminalEngine.setCurrentDirectory(restoredDir)
                terminalEngine.setEnvVars(restoredEnvs)
                remoteBridgeEngine.setBypassTargeting(isBypass)
                refreshFiles()
            }
        }
    }

    fun updateCommandInput(input: String) {
        _uiState.value = _uiState.value.copy(currentCommandInput = input)
    }

    fun executeCommand(commandToRun: String? = null) {
        val cmd = commandToRun ?: _uiState.value.currentCommandInput
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return

        val vm = _uiState.value.activeVm ?: return
        if (!vm.isRunning) {
            _uiState.value = _uiState.value.copy(
                terminalLines = _uiState.value.terminalLines + listOf(
                    TerminalLine(text = "bash: Host is powered off. Please start VM '${vm.name}' first.", type = LineType.ERROR)
                ),
                currentCommandInput = ""
            )
            return
        }

        val prompt = terminalEngine.getPrompt(vm)
        val inputLine = TerminalLine(text = "$prompt$trimmed", type = LineType.INPUT)

        // Clear command if requested
        if (trimmed.equals("clear", ignoreCase = true) || trimmed.equals("cls", ignoreCase = true)) {
            _uiState.value = _uiState.value.copy(
                terminalLines = emptyList(),
                currentCommandInput = "",
                historyIndex = -1
            )
            return
        }

        // Add to history
        commandHistoryList.add(trimmed)
        viewModelScope.launch {
            db.commandHistoryDao().insertHistory(
                CommandHistoryEntity(vmId = vm.id, command = trimmed)
            )
        }

        _uiState.value = _uiState.value.copy(
            terminalLines = _uiState.value.terminalLines + inputLine,
            currentCommandInput = "",
            isExecuting = true,
            historyIndex = -1
        )

        viewModelScope.launch {
            terminalEngine.execute(trimmed, vm) { outputLine ->
                _uiState.value = _uiState.value.copy(
                    terminalLines = _uiState.value.terminalLines + outputLine
                )
            }
            _uiState.value = _uiState.value.copy(isExecuting = false)
            refreshFiles()

            // Save session state automatically after command
            sessionManager.notifyStateChanged(
                vmId = vm.id,
                currentDirectory = fileSystem.getCurrentDirectory(),
                envVars = terminalEngine.getEnvVars(),
                isBypass = remoteBridgeEngine.bridgeState.value.isBypassTargetingActive,
                lastCommand = trimmed
            )
        }
    }

    fun historyUp() {
        if (commandHistoryList.isEmpty()) return
        val currentIdx = _uiState.value.historyIndex
        val newIdx = if (currentIdx == -1) {
            commandHistoryList.size - 1
        } else {
            (currentIdx - 1).coerceAtLeast(0)
        }
        _uiState.value = _uiState.value.copy(
            historyIndex = newIdx,
            currentCommandInput = commandHistoryList[newIdx]
        )
    }

    fun historyDown() {
        if (commandHistoryList.isEmpty()) return
        val currentIdx = _uiState.value.historyIndex
        if (currentIdx == -1) return
        val newIdx = currentIdx + 1
        if (newIdx >= commandHistoryList.size) {
            _uiState.value = _uiState.value.copy(historyIndex = -1, currentCommandInput = "")
        } else {
            _uiState.value = _uiState.value.copy(
                historyIndex = newIdx,
                currentCommandInput = commandHistoryList[newIdx]
            )
        }
    }

    fun searchSoftware(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun setSelectedCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun installSoftware(item: SoftwareItem) {
        val vm = _uiState.value.activeVm ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                terminalLines = _uiState.value.terminalLines + listOf(
                    TerminalLine(text = "📦 Installing '${item.name}' via Marketplace...", type = LineType.SYSTEM_INFO)
                )
            )

            terminalEngine.execute(item.commandInstall, vm) { line ->
                _uiState.value = _uiState.value.copy(
                    terminalLines = _uiState.value.terminalLines + line
                )
            }

            db.installedPackageDao().insertPackage(
                InstalledPackageEntity(
                    id = "${vm.id}_${item.id}",
                    vmId = vm.id,
                    name = item.name,
                    toolType = item.toolType.name,
                    version = item.version,
                    commandRun = item.commandInstall
                )
            )

            _uiState.value = _uiState.value.copy(
                messageToast = "Successfully installed ${item.name}!"
            )
            refreshFiles()
        }
    }

    fun toggleVmPower(vm: VirtualMachine) {
        val updated = vm.copy(isRunning = !vm.isRunning)
        viewModelScope.launch {
            vmRepo.toggleVmPower(vm)
            _uiState.value = _uiState.value.copy(
                messageToast = if (updated.isRunning) "Started VM '${vm.name}'" else "Shut down VM '${vm.name}'"
            )
        }
    }

    fun createVirtualMachine(name: String, distro: String, region: String, cpu: Int, ram: Int, disk: Int) {
        viewModelScope.launch {
            val newVm = vmRepo.createVm(
                name = name,
                distro = distro,
                region = region,
                cpuCores = cpu,
                ramMb = ram,
                diskGb = disk
            )
            _uiState.value = _uiState.value.copy(
                isCreatingVmDialogVisible = false,
                messageToast = "Virtual Machine '$name' provisioned successfully!"
            )
            setActiveVm(newVm)
        }
    }

    fun setCreateVmDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isCreatingVmDialogVisible = visible)
    }

    fun deleteVirtualMachine(vm: VirtualMachine) {
        viewModelScope.launch {
            vmRepo.deleteVm(vm.id)
            val remaining = _uiState.value.vms.filter { it.id != vm.id }
            _uiState.value = _uiState.value.copy(
                vms = remaining,
                activeVm = remaining.firstOrNull()
            )
        }
    }

    fun refreshFiles() {
        val files = fileSystem.listFiles(_uiState.value.currentDirectory)
        _uiState.value = _uiState.value.copy(
            currentFiles = files,
            currentDirectory = fileSystem.getCurrentDirectory()
        )
    }

    fun openFileInEditor(file: VirtualFile) {
        if (file.isDirectory) {
            fileSystem.setCurrentDirectory(file.path)
            refreshFiles()
            _uiState.value.activeVm?.let { vm ->
                sessionManager.notifyStateChanged(
                    vmId = vm.id,
                    currentDirectory = fileSystem.getCurrentDirectory(),
                    envVars = terminalEngine.getEnvVars(),
                    isBypass = remoteBridgeEngine.bridgeState.value.isBypassTargetingActive
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(
                activeEditingFile = file,
                activeEditorContent = file.content,
                isEditorModified = false,
                currentTab = NavigationTab.CODE_WORKSPACE
            )
        }
    }

    fun updateEditorContent(content: String) {
        _uiState.value = _uiState.value.copy(
            activeEditorContent = content,
            isEditorModified = true
        )
    }

    fun saveActiveFile() {
        val activeFile = _uiState.value.activeEditingFile ?: return
        val vm = _uiState.value.activeVm ?: return
        viewModelScope.launch {
            fileSystem.saveFile(
                vmId = vm.id,
                path = activeFile.path,
                content = _uiState.value.activeEditorContent,
                language = activeFile.language
            )
            _uiState.value = _uiState.value.copy(
                isEditorModified = false,
                activeEditingFile = activeFile.copy(content = _uiState.value.activeEditorContent),
                messageToast = "File '${activeFile.name}' saved."
            )
            refreshFiles()
        }
    }

    fun runActiveFileInTerminal() {
        val activeFile = _uiState.value.activeEditingFile ?: return
        saveActiveFile()
        setTab(NavigationTab.TERMINAL)

        val runCmd = when (activeFile.language.lowercase()) {
            "python" -> "python3 ${activeFile.path}"
            "javascript", "js" -> "node ${activeFile.path}"
            "go" -> "go run ${activeFile.path}"
            "ruby", "rb" -> "ruby ${activeFile.path}"
            "rust", "rs" -> "cargo run"
            "shell", "sh" -> "bash ${activeFile.path}"
            else -> "cat ${activeFile.path}"
        }
        executeCommand(runCmd)
    }

    fun createNewFile(fileName: String) {
        val vm = _uiState.value.activeVm ?: return
        val path = "${fileSystem.getCurrentDirectory()}/$fileName"
        val lang = when {
            fileName.endsWith(".py") -> "python"
            fileName.endsWith(".js") -> "javascript"
            fileName.endsWith(".go") -> "go"
            fileName.endsWith(".rb") -> "ruby"
            fileName.endsWith(".rs") -> "rust"
            fileName.endsWith(".json") -> "json"
            else -> "plaintext"
        }
        viewModelScope.launch {
            fileSystem.saveFile(vm.id, path, "", lang)
            refreshFiles()
            val newFile = fileSystem.getFile(path)
            if (newFile != null) {
                openFileInEditor(newFile)
            }
        }
    }

    // --- Gemini AI Copilot Methods ---

    fun updateChatInput(input: String) {
        _uiState.value = _uiState.value.copy(chatInput = input)
    }

    fun setGeminiModel(model: String) {
        _uiState.value = _uiState.value.copy(selectedGeminiModel = model)
    }

    fun sendChatMessage(promptText: String? = null) {
        val text = promptText ?: _uiState.value.chatInput
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val activeVm = _uiState.value.activeVm
        val vmContext = if (activeVm != null) {
            "VM: ${activeVm.name}, Distro: ${activeVm.distro}, CWD: ${fileSystem.getCurrentDirectory()}, EnvVars: ${terminalEngine.getEnvVars().keys.joinToString(", ")}"
        } else {
            "CWD: ${fileSystem.getCurrentDirectory()}"
        }

        _uiState.value = _uiState.value.copy(
            chatInput = "",
            isChatLoading = true
        )

        viewModelScope.launch {
            geminiAiService.sendMessage(
                userPrompt = trimmed,
                modelName = _uiState.value.selectedGeminiModel,
                activeVmContext = vmContext
            )
            _uiState.value = _uiState.value.copy(isChatLoading = false)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            geminiAiService.clearHistory()
        }
    }

    fun runCodeSnippetInTerminal(snippet: String) {
        setTab(NavigationTab.TERMINAL)
        executeCommand(snippet.trim())
    }

    // --- Firebase Auth & Session State Methods ---

    fun setAuthDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isAuthDialogVisible = visible)
    }

    fun signInWithGoogle(context: Context) {
        _uiState.value = _uiState.value.copy(isAuthenticating = true)
        viewModelScope.launch {
            val result = firebaseCloudService.signInWithGoogleCredential(context)
            _uiState.value = _uiState.value.copy(isAuthenticating = false)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    messageToast = "Signed in as ${result.getOrNull()?.displayName ?: "User"}",
                    isAuthDialogVisible = false
                )
                triggerManualCloudSync()
            } else {
                _uiState.value = _uiState.value.copy(
                    messageToast = "Sign-In notice: ${result.exceptionOrNull()?.message ?: "Using local developer profile"}"
                )
            }
        }
    }

    fun signInDevProfile(email: String = "developer@cloudterm.internal", name: String = "Cloud Developer") {
        firebaseCloudService.signInDevProfile(email, name)
        _uiState.value = _uiState.value.copy(
            isAuthDialogVisible = false,
            messageToast = "Signed in as $name"
        )
        triggerManualCloudSync()
    }

    fun signOut() {
        viewModelScope.launch {
            firebaseCloudService.signOut()
            _uiState.value = _uiState.value.copy(messageToast = "Signed out from Cloud Sync.")
        }
    }

    fun triggerManualCloudSync() {
        val vm = _uiState.value.activeVm ?: return
        viewModelScope.launch {
            val success = sessionManager.forceCloudSync(
                vmId = vm.id,
                currentDirectory = fileSystem.getCurrentDirectory(),
                envVars = terminalEngine.getEnvVars(),
                isBypass = remoteBridgeEngine.bridgeState.value.isBypassTargetingActive
            )
            _uiState.value = _uiState.value.copy(
                messageToast = if (success) "Session state backed up to Firebase Firestore!" else "Saved to Local Room DB (Sign in for Cloud Sync)"
            )
        }
    }

    fun setCustomEnvironmentVariable(key: String, value: String) {
        val cleanedKey = key.trim()
        val cleanedVal = value.trim()
        if (cleanedKey.isNotEmpty()) {
            val map = terminalEngine.getEnvVars().toMutableMap()
            map[cleanedKey] = cleanedVal
            terminalEngine.setEnvVars(map)
            _uiState.value.activeVm?.let { vm ->
                sessionManager.notifyStateChanged(
                    vmId = vm.id,
                    currentDirectory = fileSystem.getCurrentDirectory(),
                    envVars = map,
                    isBypass = remoteBridgeEngine.bridgeState.value.isBypassTargetingActive
                )
            }
            _uiState.value = _uiState.value.copy(messageToast = "Set $cleanedKey=$cleanedVal")
        }
    }

    fun removeCustomEnvironmentVariable(key: String) {
        val map = terminalEngine.getEnvVars().toMutableMap()
        if (map.containsKey(key)) {
            map.remove(key)
            terminalEngine.setEnvVars(map)
            _uiState.value.activeVm?.let { vm ->
                sessionManager.notifyStateChanged(
                    vmId = vm.id,
                    currentDirectory = fileSystem.getCurrentDirectory(),
                    envVars = map,
                    isBypass = remoteBridgeEngine.bridgeState.value.isBypassTargetingActive
                )
            }
            _uiState.value = _uiState.value.copy(messageToast = "Removed $key")
        }
    }

    // --- Windows Bridge & Bypass Methods ---

    fun toggleBypassTargeting() {
        remoteBridgeEngine.toggleBypassTargeting()
        val newState = remoteBridgeEngine.bridgeState.value.isBypassTargetingActive
        _uiState.value.activeVm?.let { vm ->
            sessionManager.notifyStateChanged(
                vmId = vm.id,
                currentDirectory = fileSystem.getCurrentDirectory(),
                envVars = terminalEngine.getEnvVars(),
                isBypass = newState
            )
        }
        _uiState.value = _uiState.value.copy(
            messageToast = if (newState) "🎯 Bypass Targeting ENABLED: Cloud Stream Execution" else "⚠️ Bypass Targeting Paused"
        )
    }

    fun setBypassTargeting(enabled: Boolean) {
        remoteBridgeEngine.setBypassTargeting(enabled)
    }

    fun setBridgePort(port: Int) {
        remoteBridgeEngine.setPort(port)
        _uiState.value = _uiState.value.copy(
            messageToast = "Remote Bridge port changed to $port"
        )
    }

    fun setBridgeToken(token: String) {
        remoteBridgeEngine.setSessionToken(token)
    }

    fun toggleBridgeServer() {
        if (remoteBridgeEngine.bridgeState.value.isRunning) {
            remoteBridgeEngine.stopServer()
            _uiState.value = _uiState.value.copy(messageToast = "Windows Remote Bridge daemon stopped.")
        } else {
            remoteBridgeEngine.startServer()
            _uiState.value = _uiState.value.copy(messageToast = "Windows Remote Bridge daemon listening on port ${_uiState.value.bridgeState.port}.")
        }
    }

    fun updateWindowsTestInput(input: String) {
        _uiState.value = _uiState.value.copy(windowsTestInput = input)
    }

    fun executeWindowsTestCommand() {
        val cmd = _uiState.value.windowsTestInput.trim()
        if (cmd.isEmpty()) return

        _uiState.value = _uiState.value.copy(isWindowsExecuting = true)
        viewModelScope.launch {
            val (output, latency) = remoteBridgeEngine.executeDirectFromWindows(
                command = cmd,
                source = "Windows PowerShell 7.4",
                bypassTargeting = _uiState.value.bridgeState.isBypassTargetingActive
            )
            _uiState.value = _uiState.value.copy(
                windowsTestOutput = output,
                isWindowsExecuting = false,
                messageToast = "Executed via Windows Bridge in ${latency}ms (Bypass: ${_uiState.value.bridgeState.isBypassTargetingActive})"
            )
        }
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(messageToast = null)
    }
}
