package com.example.engine

import com.example.data.local.InstalledPackageDao
import com.example.data.local.InstalledPackageEntity
import com.example.data.model.LineType
import com.example.data.model.TerminalLine
import com.example.data.model.VirtualMachine
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TerminalEngine(
    private val fileSystem: VmFileSystem,
    private val packageDao: InstalledPackageDao
) {
    var onSessionStateChanged: ((currentDirectory: String, envVars: Map<String, String>) -> Unit)? = null

    private val envVars = mutableMapOf(
        "USER" to "clouduser",
        "HOME" to "/home/clouduser",
        "SHELL" to "/bin/bash",
        "TERM" to "xterm-256color",
        "PATH" to "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/root/.cargo/bin:/root/.nvm/versions/node/v22/bin:/root/go/bin",
        "LANG" to "en_US.UTF-8",
        "NODE_ENV" to "development",
        "PYTHONUNBUFFERED" to "1",
        "RUBY_VERSION" to "3.3.4",
        "GOPATH" to "/home/clouduser/go",
        "TARGET_BYPASS_MODE" to "enabled",
        "CLOUD_SESSION_PERSIST" to "active"
    )

    fun getEnvVars(): Map<String, String> = envVars.toMap()

    fun setEnvVars(newEnvs: Map<String, String>) {
        if (newEnvs.isNotEmpty()) {
            envVars.putAll(newEnvs)
        }
    }

    fun getCurrentDirectory(): String = fileSystem.getCurrentDirectory()

    fun setCurrentDirectory(dir: String) {
        fileSystem.setCurrentDirectory(dir)
    }

    private val installedTools = mutableSetOf(
        "apt", "dpkg", "bash", "sh", "coreutils", "curl", "wget", "git", "grep",
        "python3", "pip3", "node", "npm", "npx", "go", "ruby", "gem", "cargo", "rustc", "docker", "htop", "neofetch"
    )

    fun getPrompt(vm: VirtualMachine): String {
        val user = envVars["USER"] ?: "clouduser"
        val host = vm.name.lowercase().replace(" ", "-").take(15)
        var cwd = fileSystem.getCurrentDirectory()
        if (cwd == "/home/clouduser") cwd = "~"
        else if (cwd.startsWith("/home/clouduser/")) cwd = "~" + cwd.removePrefix("/home/clouduser")
        return "$user@$host:$cwd$ "
    }

    suspend fun execute(rawCommand: String, vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val trimmed = rawCommand.trim()
        if (trimmed.isEmpty()) return

        // Check for pipes (simple piping support e.g. "cat README.md | grep npm" or "history | grep node")
        if (trimmed.contains(" | ")) {
            handlePipedCommand(trimmed, vm, onOutput)
            return
        }

        // Check for redirection > or >>
        if (trimmed.contains(" > ") || trimmed.contains(" >> ")) {
            handleRedirectCommand(trimmed, vm, onOutput)
            return
        }

        // Check for direct environment variable assignment (e.g. PORT=8080 or DEBUG=1)
        if (trimmed.contains("=") && !trimmed.contains(" ") && !trimmed.startsWith("./")) {
            handleExport(listOf(trimmed), onOutput)
            return
        }

        val expandedCommand = expandVariables(trimmed)
        val parts = parseCommandArgs(expandedCommand)
        val cmd = parts.firstOrNull() ?: return
        val args = parts.drop(1)

        when (cmd.lowercase()) {
            "help" -> showHelp(onOutput)
            "man" -> showManPage(args.firstOrNull(), onOutput)
            "clear" -> {
                // Handled in ViewModel or UI
            }
            "pwd" -> onOutput(TerminalLine(text = fileSystem.getCurrentDirectory()))
            "cd" -> handleCd(args.firstOrNull() ?: "~", onOutput)
            "ls" -> handleLs(args, onOutput)
            "cat" -> handleCat(args, onOutput)
            "echo" -> handleEcho(args, onOutput)
            "mkdir" -> handleMkdir(args, vm, onOutput)
            "touch" -> handleTouch(args, vm, onOutput)
            "rm" -> handleRm(args, vm, onOutput)
            "grep" -> handleGrep(args, onOutput)
            "find" -> handleFind(args, onOutput)
            "export" -> handleExport(args, onOutput)
            "unset" -> handleUnset(args, onOutput)
            "set" -> handleSet(onOutput)
            "env", "printenv" -> handleEnv(args, onOutput)
            "session" -> handleSessionCommand(args, vm, onOutput)
            "whoami" -> onOutput(TerminalLine(text = envVars["USER"] ?: "clouduser"))
            "hostname" -> onOutput(TerminalLine(text = vm.name.lowercase().replace(" ", "-")))
            "uptime" -> handleUptime(vm, onOutput)
            "uname" -> handleUname(args, vm, onOutput)
            "date" -> {
                val sdf = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US)
                onOutput(TerminalLine(text = sdf.format(Date())))
            }
            "free" -> handleFree(vm, args, onOutput)
            "df" -> handleDf(vm, onOutput)
            "neofetch", "fastfetch" -> handleNeofetch(vm, onOutput)
            "htop", "top" -> handleTop(vm, onOutput)
            "ps" -> handlePs(onOutput)
            "which" -> handleWhich(args, onOutput)
            "curl" -> handleCurl(args, onOutput)
            "wget" -> handleWget(args, vm, onOutput)
            "ping" -> handlePing(args, onOutput)
            "ifconfig", "ip" -> handleIp(vm, onOutput)
            "lscpu" -> handleLscpu(vm, onOutput)

            // Package Managers & Runtimes
            "apt", "apt-get" -> handleApt(args, vm, onOutput)
            "npm" -> handleNpm(args, vm, onOutput)
            "npx" -> handleNpx(args, vm, onOutput)
            "pip", "pip3" -> handlePip(args, vm, onOutput)
            "python", "python3" -> handlePython(args, onOutput)
            "node" -> handleNode(args, onOutput)
            "go" -> handleGo(args, vm, onOutput)
            "ruby" -> handleRuby(args, onOutput)
            "gem" -> handleGem(args, vm, onOutput)
            "cargo" -> handleCargo(args, vm, onOutput)
            "rustc" -> handleRustc(args, onOutput)
            "docker" -> handleDocker(args, onOutput)
            "git" -> handleGit(args, onOutput)
            "ollama" -> handleOllama(args, onOutput)
            "bun" -> handleBun(args, onOutput)
            "deno" -> handleDeno(args, onOutput)

            // Windows Terminal & Remote Bridge / Targeting Bypass commands
            "bypass" -> handleBypassCommand(args, onOutput)
            "bridge", "remote" -> handleBridgeCommand(args, vm, onOutput)
            "powershell", "win", "win-connect" -> handlePowerShellCommand(vm, onOutput)
            "win-exec" -> handleWinExecCommand(args, vm, onOutput)

            else -> {
                // Check if user is executing a local script e.g. ./server.js or ./script.sh
                if (cmd.startsWith("./")) {
                    handleLocalScript(cmd.removePrefix("./"), onOutput)
                } else {
                    onOutput(
                        TerminalLine(
                            text = "bash: $cmd: command not found. Try 'apt install $cmd' or 'help'.",
                            type = LineType.ERROR
                        )
                    )
                }
            }
        }
    }

    private fun showHelp(onOutput: (TerminalLine) -> Unit) {
        val help = """
            ╔══════════════════════════════════════════════════════════════════╗
            ║               CloudTerm VM - Interactive Cloud Terminal          ║
            ╚══════════════════════════════════════════════════════════════════╝
            
            ⚡ Package Managers & Software Tools:
              apt / apt-get install <pkg>  : Install system packages (nginx, redis, htop...)
              npm install <pkg> / npm run  : Manage JavaScript/Node.js dependencies
              pip install <pkg> / pip list : Python package manager
              gem install <gem>            : Ruby gem manager
              go get / go run <file.go>    : Golang package & compiler
              cargo add / cargo run        : Rust package manager
              docker run / docker ps       : Virtual container management
              ollama run <model>           : Run local AI LLM models (Llama, Gemma)
              
            🚀 Runtime Execution:
              python3 <file.py>            : Execute Python 3 scripts
              node <file.js>               : Execute Node.js scripts
              ruby <file.rb>               : Execute Ruby scripts
              go run <file.go>             : Compile & run Go source
              
            🌐 Cloud & Network Utilities:
              curl <url>                   : Real HTTP GET/POST with live response
              wget <url>                   : Download web assets to virtual storage
              ping <host>                  : Real ICMP network latency check
              
            📊 System & Monitoring:
              neofetch                     : Display VM OS, Kernel, Hardware specs
              htop / top                   : Live CPU, memory & process table
              free -h / df -h / uptime     : System memory & disk usage
              ps aux / kill <pid>          : Active process management
              
            📁 Virtual Filesystem:
              ls -la, cd <dir>, pwd        : Directory navigation
              cat <file>, echo, grep, find : File inspection and manipulation
              touch <file>, mkdir, rm -rf  : Create / delete files & directories
        """.trimIndent()
        onOutput(TerminalLine(text = help, type = LineType.SYSTEM_INFO))
    }

    private fun showManPage(cmd: String?, onOutput: (TerminalLine) -> Unit) {
        if (cmd == null) {
            onOutput(TerminalLine(text = "What manual page do you want? Try 'man apt', 'man npm', 'man python'.", type = LineType.WARNING))
            return
        }
        onOutput(TerminalLine(text = "MANUAL PAGE FOR '$cmd(1)':\nDetailed developer manual page for $cmd in CloudTerm Linux container.", type = LineType.SYSTEM_INFO))
    }

    private fun handleCd(path: String, onOutput: (TerminalLine) -> Unit) {
        val targetPath = when {
            path == "~" -> "/home/clouduser"
            path.startsWith("~/") -> "/home/clouduser" + path.removePrefix("~")
            else -> path
        }
        val success = fileSystem.setCurrentDirectory(targetPath)
        if (!success) {
            onOutput(TerminalLine(text = "bash: cd: $path: No such file or directory", type = LineType.ERROR))
        } else {
            onSessionStateChanged?.invoke(fileSystem.getCurrentDirectory(), envVars)
        }
    }

    private fun handleLs(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        val showAll = args.contains("-a") || args.contains("-la") || args.contains("-al") || args.contains("-l")
        val targetPath = args.firstOrNull { !it.startsWith("-") } ?: fileSystem.getCurrentDirectory()
        val files = fileSystem.listFiles(targetPath)

        if (files.isEmpty()) {
            if (showAll) {
                onOutput(TerminalLine(text = "total 0\ndrwxr-xr-x 2 clouduser clouduser 4096 . \ndrwxr-xr-x 4 clouduser clouduser 4096 .."))
            }
            return
        }

        if (showAll) {
            val lines = mutableListOf<String>()
            lines.add("total ${files.size * 4}")
            lines.add("drwxr-xr-x 4 clouduser clouduser 4096 .")
            lines.add("drwxr-xr-x 4 clouduser clouduser 4096 ..")
            files.forEach { f ->
                val typeChar = if (f.isDirectory) "d" else "-"
                val perm = f.permissions.take(9).padEnd(9, '-')
                val size = if (f.isDirectory) "4096" else f.sizeBytes.toString().padStart(6)
                val colorPrefix = if (f.isDirectory) "\uD83D\uDCC1 " else "\uD83D\uDCC4 "
                lines.add("$typeChar$perm 1 clouduser clouduser $size $colorPrefix${f.name}")
            }
            onOutput(TerminalLine(text = lines.joinToString("\n"), type = LineType.CODE))
        } else {
            val output = files.joinToString("   ") { (if (it.isDirectory) "\uD83D\uDCC1 " else "\uD83D\uDCC4 ") + it.name }
            onOutput(TerminalLine(text = output, type = LineType.CODE))
        }
    }

    private fun handleCat(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        if (args.isEmpty()) {
            onOutput(TerminalLine(text = "cat: missing file operand", type = LineType.ERROR))
            return
        }
        val file = fileSystem.getFile(args.first())
        if (file == null) {
            onOutput(TerminalLine(text = "cat: ${args.first()}: No such file or directory", type = LineType.ERROR))
        } else if (file.isDirectory) {
            onOutput(TerminalLine(text = "cat: ${args.first()}: Is a directory", type = LineType.ERROR))
        } else {
            onOutput(TerminalLine(text = file.content, type = LineType.CODE))
        }
    }

    private fun handleEcho(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        val text = args.joinToString(" ").removeSurrounding("\"").removeSurrounding("'")
        onOutput(TerminalLine(text = text))
    }

    private suspend fun handleMkdir(args: List<String>, vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        if (args.isEmpty()) {
            onOutput(TerminalLine(text = "mkdir: missing operand", type = LineType.ERROR))
            return
        }
        val dir = args.first { !it.startsWith("-") }
        val success = fileSystem.makeDirectory(vm.id, dir)
        if (!success) {
            onOutput(TerminalLine(text = "mkdir: cannot create directory '$dir': File exists", type = LineType.ERROR))
        }
    }

    private suspend fun handleTouch(args: List<String>, vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        if (args.isEmpty()) {
            onOutput(TerminalLine(text = "touch: missing file operand", type = LineType.ERROR))
            return
        }
        args.forEach { fileName ->
            val lang = when {
                fileName.endsWith(".py") -> "python"
                fileName.endsWith(".js") || fileName.endsWith(".ts") -> "javascript"
                fileName.endsWith(".go") -> "go"
                fileName.endsWith(".rb") -> "ruby"
                fileName.endsWith(".rs") -> "rust"
                fileName.endsWith(".json") -> "json"
                fileName.endsWith(".md") -> "markdown"
                fileName.endsWith(".sh") -> "shell"
                else -> "plaintext"
            }
            fileSystem.saveFile(vm.id, fileName, "", lang)
        }
    }

    private suspend fun handleRm(args: List<String>, vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val target = args.firstOrNull { !it.startsWith("-") }
        if (target == null) {
            onOutput(TerminalLine(text = "rm: missing operand", type = LineType.ERROR))
            return
        }
        val success = fileSystem.deletePath(vm.id, target)
        if (!success) {
            onOutput(TerminalLine(text = "rm: cannot remove '$target': No such file or directory", type = LineType.ERROR))
        }
    }

    private fun handleGrep(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        if (args.size < 2) {
            onOutput(TerminalLine(text = "Usage: grep <pattern> <file>", type = LineType.WARNING))
            return
        }
        val query = args[0]
        val file = fileSystem.getFile(args[1])
        if (file == null) {
            onOutput(TerminalLine(text = "grep: ${args[1]}: No such file or directory", type = LineType.ERROR))
            return
        }
        val matches = file.content.lines().filter { it.contains(query, ignoreCase = true) }
        if (matches.isEmpty()) {
            onOutput(TerminalLine(text = "(no matches found)"))
        } else {
            onOutput(TerminalLine(text = matches.joinToString("\n"), type = LineType.ACCENT))
        }
    }

    private fun handleFind(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        val query = args.lastOrNull { !it.startsWith("-") } ?: ""
        val all = fileSystem.getAllFiles()
        val matched = all.filter { it.path.contains(query) }
        onOutput(TerminalLine(text = matched.joinToString("\n") { it.path }, type = LineType.CODE))
    }

    private fun handleExport(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        if (args.isEmpty()) {
            handleEnv(emptyList(), onOutput)
            return
        }
        val assignment = args.joinToString(" ")
        if (assignment.contains("=")) {
            val (key, value) = assignment.split("=", limit = 2)
            val cleanedKey = key.trim().removePrefix("export ").trim()
            val cleanedVal = value.trim().removeSurrounding("\"").removeSurrounding("'")
            envVars[cleanedKey] = cleanedVal
            onSessionStateChanged?.invoke(fileSystem.getCurrentDirectory(), envVars)
            onOutput(TerminalLine(text = "export $cleanedKey=\"$cleanedVal\" (auto-saved to cloud session)", type = LineType.SUCCESS))
        } else {
            val key = args.first().trim().removePrefix("export ").trim()
            val value = envVars[key]
            if (value != null) {
                onOutput(TerminalLine(text = "declare -x $key=\"$value\"", type = LineType.CODE))
            } else {
                onOutput(TerminalLine(text = "bash: export: `$key`: not a valid identifier or unset", type = LineType.WARNING))
            }
        }
    }

    private fun handleUnset(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        if (args.isEmpty()) {
            onOutput(TerminalLine(text = "unset: not enough arguments", type = LineType.WARNING))
            return
        }
        args.forEach { varName ->
            val key = varName.trim()
            if (envVars.containsKey(key)) {
                envVars.remove(key)
                onSessionStateChanged?.invoke(fileSystem.getCurrentDirectory(), envVars)
                onOutput(TerminalLine(text = "Unset variable '$key' (session updated).", type = LineType.SUCCESS))
            }
        }
    }

    private fun handleSet(onOutput: (TerminalLine) -> Unit) {
        val lines = envVars.map { "${it.key}=${it.value}" } + listOf(
            "BASH=/bin/bash",
            "BASH_VERSINFO=([0]=\"5\" [1]=\"2\" [2]=\"21\")",
            "EUID=1000",
            "HOSTTYPE=x86_64",
            "OSTYPE=linux-gnu",
            "SHLVL=1",
            "UID=1000"
        )
        onOutput(TerminalLine(text = lines.joinToString("\n"), type = LineType.CODE))
    }

    private fun handleEnv(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        val specificVar = args.firstOrNull { !it.startsWith("-") }
        if (specificVar != null) {
            val value = envVars[specificVar]
            if (value != null) {
                onOutput(TerminalLine(text = value))
            } else {
                onOutput(TerminalLine(text = "env: '$specificVar': No such environment variable", type = LineType.WARNING))
            }
        } else {
            val lines = envVars.toSortedMap().map { "${it.key}=${it.value}" }.joinToString("\n")
            onOutput(TerminalLine(text = lines, type = LineType.CODE))
        }
    }

    private fun handleSessionCommand(args: List<String>, vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val sub = args.firstOrNull()?.lowercase() ?: "status"
        when (sub) {
            "status" -> {
                val cwd = fileSystem.getCurrentDirectory()
                val envCount = envVars.size
                val statusText = """
                ╔══════════════════════════════════════════════════════════════════╗
                ║                CloudTerm VM Session State & Persistence          ║
                ╚══════════════════════════════════════════════════════════════════╝
                📍 Active Node:         ${vm.name} (${vm.id})
                📂 Working Directory:   $cwd
                🔑 Environment Vars:    $envCount variables active (persisted)
                🛡️ Targeting Bypass:    ENABLED (Commands run in cloud sandbox)
                💾 Local Persistence:   Room SQLite Database (Auto-saved on change)
                ☁️ Cloud Persistence:   Firebase Firestore (Realtime sync)
                
                Commands:
                  • session env         : View all loaded environment variables
                  • session save        : Force immediate session snapshot
                  • export KEY=VALUE    : Set and auto-save new environment variable
                  • unset KEY           : Remove environment variable
                  • cd <path>           : Change working directory (resumes after restart)
                """.trimIndent()
                onOutput(TerminalLine(text = statusText, type = LineType.SYSTEM_INFO))
            }
            "env" -> {
                val lines = envVars.toSortedMap().map { "  🔑 \u001B[36m${it.key}\u001B[0m = \u001B[32m\"${it.value}\"\u001B[0m" }
                onOutput(TerminalLine(text = "Active Session Environment Variables (${envVars.size}):\n${lines.joinToString("\n")}", type = LineType.CODE))
            }
            "save" -> {
                onSessionStateChanged?.invoke(fileSystem.getCurrentDirectory(), envVars)
                onOutput(TerminalLine(text = "✅ Session snapshot successfully saved to Local Room DB & Cloud Firestore.", type = LineType.SUCCESS))
            }
            "restore" -> {
                onSessionStateChanged?.invoke(fileSystem.getCurrentDirectory(), envVars)
                onOutput(TerminalLine(text = "🔄 Session state verified and restored.", type = LineType.SUCCESS))
            }
            else -> {
                onOutput(TerminalLine(text = "Usage: session [status | env | save | restore]", type = LineType.WARNING))
            }
        }
    }

    private fun expandVariables(command: String): String {
        var result = command
        envVars.forEach { (k, v) ->
            result = result.replace("$$k", v).replace("\${$k}", v)
        }
        return result
    }

    private fun handleUptime(vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val hours = vm.uptimeSeconds / 3600
        val mins = (vm.uptimeSeconds % 3600) / 60
        onOutput(TerminalLine(text = " 14:15:00 up $hours hrs $mins min, 1 user, load average: 0.18, 0.22, 0.15"))
    }

    private fun handleUname(args: List<String>, vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        if (args.contains("-a")) {
            onOutput(TerminalLine(text = "Linux ${vm.name.lowercase().replace(" ", "-")} 6.8.0-40-generic #41-Ubuntu SMP PREEMPT_DYNAMIC Wed Jul 31 16:00:00 UTC 2026 x86_64 x86_64 x86_64 GNU/Linux"))
        } else {
            onOutput(TerminalLine(text = "Linux"))
        }
    }

    private fun handleFree(vm: VirtualMachine, args: List<String>, onOutput: (TerminalLine) -> Unit) {
        val totalMb = vm.ramMb
        val usedMb = vm.ramUsageMb
        val freeMb = totalMb - usedMb
        val buffMb = 480
        val availMb = freeMb + buffMb

        val text = """
                       total        used        free      shared  buff/cache   available
        Mem:        ${totalMb}Mi      ${usedMb}Mi      ${freeMb}Mi        18Mi       ${buffMb}Mi      ${availMb}Mi
        Swap:       4096Mi         0Mi      4096Mi
        """.trimIndent()
        onOutput(TerminalLine(text = text, type = LineType.CODE))
    }

    private fun handleDf(vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val total = vm.diskGb
        val used = (total * 0.18).toInt()
        val avail = total - used
        val text = """
        Filesystem     1K-blocks      Used Available Use% Mounted on
        /dev/root       ${total * 1024 * 1024}  ${used * 1024 * 1024}  ${avail * 1024 * 1024}  18% /
        tmpfs             4194304         0   4194304   0% /dev/shm
        /dev/nvme0n1p1     524288     65536    458752  13% /boot/efi
        """.trimIndent()
        onOutput(TerminalLine(text = text, type = LineType.CODE))
    }

    private fun handleNeofetch(vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val art = """
         _   _ _                 _ _____                     
        | | | | |               | |_   _|                    
        | | | | |__  _   _ _ __ | |_| | ___ _ __ _ __ ___   
        | | | | '_ \| | | | '_ \| __| |/ _ \ '__| '_ ` _ \  
        | |_| | |_) | |_| | | | | |_| |  __/ |  | | | | | | 
         \___/|_.__/ \__,_|_| |_|\__\_/\___|_|  |_| |_| |_| 
        
        clouduser@${vm.name.lowercase().replace(" ", "-")}
        ------------------------------------------
        OS: ${vm.distro} x86_64
        Host: CloudTerm Virtual Machine (KVM/QEMU)
        Kernel: 6.8.0-40-generic
        Uptime: ${vm.uptimeSeconds / 3600} hours, ${(vm.uptimeSeconds % 3600) / 60} mins
        Packages: ${vm.installedPackageCount} (dpkg), 12 (npm), 8 (pip), 4 (gem), 6 (go)
        Shell: bash 5.2.21
        Terminal: xterm-256color
        CPU: AMD EPYC 9R14 (${vm.cpuCores} vCPUs @ 3.400GHz)
        Memory: ${vm.ramUsageMb}MiB / ${vm.ramMb}MiB (${(vm.ramUsageMb.toFloat() / vm.ramMb * 100).toInt()}%)
        Disk: 18.2G / ${vm.diskGb}G (NVMe SSD)
        Region: ${vm.region}
        Public IP: ${vm.publicIp}
        Private IP: ${vm.ipAddress}
        """.trimIndent()
        onOutput(TerminalLine(text = art, type = LineType.ACCENT))
    }

    private fun handleTop(vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val text = """
        top - 14:18:22 up 1:04, 1 user, load average: 0.12, 0.18, 0.14
        Tasks: 114 total, 1 running, 113 sleeping, 0 stopped, 0 zombie
        %Cpu(s): ${vm.cpuUsagePercent} us, 1.2 sy, 0.0 ni, 85.8 id, 0.2 wa, 0.0 hi, 0.4 si
        MiB Mem : ${vm.ramMb}.0 total, ${vm.ramMb - vm.ramUsageMb}.0 free, ${vm.ramUsageMb}.0 used, 480.0 buff/cache
        
          PID USER      PR  NI    VIRT    RES    SHR S  %CPU  %MEM     TIME+ COMMAND
         1240 clouduse  20   0  982144 142320  42100 S   6.2   1.8   0:14.22 node server.js
         1389 clouduse  20   0  624892 210450  58400 S   4.8   2.6   0:08.50 python3 main.py
         1450 clouduse  20   0  412030  98200  32100 S   2.1   1.2   0:03.11 go-worker
          890 root      20   0  182040  24500  18200 S   0.8   0.3   0:01.80 docker-daemon
          950 postgres  20   0  380120  48200  38900 S   0.5   0.6   0:00.95 postgres
          980 redis     20   0   89200  18200  12400 S   0.2   0.2   0:00.44 redis-server
            1 root      20   0  168420  12400   8900 S   0.0   0.1   0:02.10 systemd
        """.trimIndent()
        onOutput(TerminalLine(text = text, type = LineType.CODE))
    }

    private fun handlePs(onOutput: (TerminalLine) -> Unit) {
        val text = """
        PID TTY          TIME CMD
       1010 pts/0    00:00:00 bash
       1240 pts/0    00:00:14 node server.js
       1389 pts/0    00:00:08 python3 main.py
       1502 pts/0    00:00:00 ps
        """.trimIndent()
        onOutput(TerminalLine(text = text, type = LineType.CODE))
    }

    private fun handleWhich(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        if (args.isEmpty()) return
        val target = args.first()
        val path = when (target) {
            "python", "python3" -> "/usr/bin/python3"
            "node" -> "/usr/local/bin/node"
            "npm" -> "/usr/local/bin/npm"
            "go" -> "/usr/local/go/bin/go"
            "ruby" -> "/usr/bin/ruby"
            "gem" -> "/usr/bin/gem"
            "cargo" -> "/root/.cargo/bin/cargo"
            "docker" -> "/usr/bin/docker"
            "curl" -> "/usr/bin/curl"
            "git" -> "/usr/bin/git"
            else -> "/usr/bin/$target"
        }
        onOutput(TerminalLine(text = path))
    }

    private suspend fun handleCurl(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        val url = args.firstOrNull { !it.startsWith("-") }
        if (url == null) {
            onOutput(TerminalLine(text = "curl: try 'curl --help' or 'curl <URL>'", type = LineType.WARNING))
            return
        }
        onOutput(TerminalLine(text = "📡 Fetching $url from Cloud VM network...", type = LineType.SYSTEM_INFO))
        val response = NetworkExecutor.executeCurl(url)
        onOutput(TerminalLine(text = response, type = LineType.CODE))
    }

    private suspend fun handleWget(args: List<String>, vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val url = args.firstOrNull { !it.startsWith("-") }
        if (url == null) {
            onOutput(TerminalLine(text = "wget: missing URL", type = LineType.ERROR))
            return
        }
        onOutput(TerminalLine(text = "--2026-08-21 14:20:00--  $url\nResolving host...\nConnecting to cloud network... connected.\nHTTP request sent, awaiting response... 200 OK", type = LineType.SYSTEM_INFO))
        val content = NetworkExecutor.executeCurl(url)
        val fileName = url.split("/").lastOrNull()?.takeIf { it.isNotEmpty() } ?: "downloaded_file.txt"
        fileSystem.saveFile(vm.id, fileName, content)
        onOutput(TerminalLine(text = "Saving to: '$fileName'\n'$fileName' saved [${content.length}/${content.length}]", type = LineType.SUCCESS))
    }

    private suspend fun handlePing(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        val host = args.firstOrNull { !it.startsWith("-") } ?: "8.8.8.8"
        onOutput(TerminalLine(text = "Initiating cloud network ping to $host...", type = LineType.SYSTEM_INFO))
        val result = NetworkExecutor.executePing(host)
        onOutput(TerminalLine(text = result, type = LineType.CODE))
    }

    private fun handleIp(vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val text = """
        1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN group default
            link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
            inet 127.0.0.1/8 scope host lo
        2: eth0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 9001 qdisc fq_codel state UP group default
            link/ether 06:2e:58:39:a1:04 brd ff:ff:ff:ff:ff:ff
            inet ${vm.ipAddress}/20 metric 100 brd 10.240.15.255 scope global eth0
            inet6 fe80::42e:58ff:fe39:a104/64 scope link
        3: docker0: <NO-CARRIER,BROADCAST,MULTICAST,UP> mtu 1500 qdisc noqueue state DOWN group default
            inet 172.17.0.1/16 brd 172.17.255.255 scope global docker0
        """.trimIndent()
        onOutput(TerminalLine(text = text, type = LineType.CODE))
    }

    private fun handleLscpu(vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val text = """
        Architecture:            x86_64
          CPU op-mode(s):        32-bit, 64-bit
          Address sizes:         48 bits physical, 48 bits virtual
          Byte Order:            Little Endian
        CPU(s):                  ${vm.cpuCores}
          On-line CPU(s) list:   0-${vm.cpuCores - 1}
        Vendor ID:               AuthenticAMD
          Model name:            AMD EPYC 9R14 32-Core Processor
          CPU family:            25
          Model:                 17
          Thread(s) per core:    2
          Core(s) per socket:    ${vm.cpuCores / 2}
          Socket(s):             1
          BogoMIPS:              6799.88
        Hypervisor vendor:       KVM
        Virtualization type:     full
        """.trimIndent()
        onOutput(TerminalLine(text = text, type = LineType.CODE))
    }

    // --- Package Managers ---

    private suspend fun handleApt(args: List<String>, vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val subCmd = args.firstOrNull() ?: "help"
        when (subCmd) {
            "update" -> {
                onOutput(TerminalLine(text = "Hit:1 http://archive.ubuntu.com/ubuntu noble InRelease\nGet:2 http://security.ubuntu.com/ubuntu noble-security InRelease [126 kB]\nGet:3 http://archive.ubuntu.com/ubuntu noble-updates InRelease [126 kB]\nFetched 252 kB in 1s (252 kB/s)\nReading package lists... Done\nBuilding dependency tree... Done\nAll packages are up to date.", type = LineType.SUCCESS))
            }
            "install" -> {
                val pkgs = args.drop(1).filter { !it.startsWith("-") }
                if (pkgs.isEmpty()) {
                    onOutput(TerminalLine(text = "apt: missing package name", type = LineType.ERROR))
                    return
                }
                pkgs.forEach { pkg ->
                    onOutput(TerminalLine(text = "Reading package lists... Done\nBuilding dependency tree... Done\nThe following NEW packages will be installed: $pkg\nNeed to get 14.8 MB of archives.\nUnpacking $pkg ...\nSetting up $pkg ...\nProcessing triggers for man-db (2.12.0)...", type = LineType.SYSTEM_INFO))
                    delay(300)
                    packageDao.insertPackage(
                        InstalledPackageEntity(
                            id = "apt_${pkg}_${System.currentTimeMillis()}",
                            vmId = vm.id,
                            name = pkg,
                            toolType = "APT",
                            version = "latest",
                            commandRun = "apt install $pkg"
                        )
                    )
                    installedTools.add(pkg)
                    onOutput(TerminalLine(text = "✅ Successfully installed $pkg in VM environment.", type = LineType.SUCCESS))
                }
            }
            "list" -> {
                onOutput(TerminalLine(text = "Listing installed system packages...\n${installedTools.joinToString("\n") { "$it/noble,now 1.0.0-cloud amd64 [installed]" }}", type = LineType.CODE))
            }
            else -> onOutput(TerminalLine(text = "Usage: apt update | apt install <package> | apt list --installed", type = LineType.WARNING))
        }
    }

    private suspend fun handleNpm(args: List<String>, vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val sub = args.firstOrNull() ?: "help"
        when (sub) {
            "-v", "--version" -> onOutput(TerminalLine(text = "10.8.2"))
            "init" -> {
                val pkgJson = """
                {
                  "name": "cloud-node-app",
                  "version": "1.0.0",
                  "description": "CloudTerm VM Application",
                  "main": "server.js",
                  "scripts": {
                    "start": "node server.js",
                    "dev": "nodemon server.js",
                    "test": "echo \"Error: no test specified\" && exit 1"
                  },
                  "dependencies": {}
                }
                """.trimIndent()
                fileSystem.saveFile(vm.id, "package.json", pkgJson, "json")
                onOutput(TerminalLine(text = "Wrote to /home/clouduser/package.json:\n$pkgJson", type = LineType.CODE))
                onOutput(TerminalLine(text = "✅ package.json initialized successfully.", type = LineType.SUCCESS))
            }
            "install", "i", "add" -> {
                val pkgs = args.drop(1).filter { !it.startsWith("-") }
                if (pkgs.isEmpty()) {
                    onOutput(TerminalLine(text = "npm: audited 142 packages in 890ms\nfound 0 vulnerabilities", type = LineType.SUCCESS))
                    return
                }
                pkgs.forEach { pkg ->
                    onOutput(TerminalLine(text = "npm info adding $pkg to package.json dependencies...", type = LineType.SYSTEM_INFO))
                    delay(300)
                    packageDao.insertPackage(
                        InstalledPackageEntity(
                            id = "npm_${pkg}_${System.currentTimeMillis()}",
                            vmId = vm.id,
                            name = pkg,
                            toolType = "NPM",
                            version = "latest",
                            commandRun = "npm install $pkg"
                        )
                    )
                    onOutput(TerminalLine(text = "+ $pkg@latest\nadded 1 package, and audited 184 packages in 1.2s\nfound 0 vulnerabilities\n✅ Package '$pkg' ready in node_modules.", type = LineType.SUCCESS))
                }
            }
            "list", "ls" -> {
                onOutput(TerminalLine(text = "cloud-node-app@1.0.0 /home/clouduser\n├── express@4.19.2\n├── cors@2.8.5\n├── dotenv@16.4.5\n├── axios@1.7.4\n└── typescript@5.5.4", type = LineType.CODE))
            }
            "run" -> {
                val script = args.getOrNull(1) ?: "start"
                onOutput(TerminalLine(text = "> cloud-node-app@1.0.0 $script\n> node server.js", type = LineType.SYSTEM_INFO))
                handleNode(listOf("server.js"), onOutput)
            }
            else -> onOutput(TerminalLine(text = "npm <command>\nUsage: npm install <pkg> | npm run <script> | npm init | npm list", type = LineType.WARNING))
        }
    }

    private fun handleNpx(args: List<String>, vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val tool = args.firstOrNull() ?: "help"
        onOutput(TerminalLine(text = "Need to install the following packages:\n  $tool\nOk to proceed? (y) y\nRunning $tool in CloudTerm VM...", type = LineType.SYSTEM_INFO))
        onOutput(TerminalLine(text = "✨ Done! Execution finished successfully.", type = LineType.SUCCESS))
    }

    private suspend fun handlePip(args: List<String>, vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val sub = args.firstOrNull() ?: "help"
        when (sub) {
            "-V", "--version" -> onOutput(TerminalLine(text = "pip 24.1.2 from /usr/local/lib/python3.12/site-packages/pip (python 3.12)"))
            "install" -> {
                val pkgs = args.drop(1).filter { !it.startsWith("-") }
                if (pkgs.isEmpty()) {
                    onOutput(TerminalLine(text = "ERROR: You must give at least one requirement to install.", type = LineType.ERROR))
                    return
                }
                pkgs.forEach { pkg ->
                    onOutput(TerminalLine(text = "Collecting $pkg\n  Downloading $pkg-latest-py3-none-any.whl (2.4 MB)\nInstalling collected packages: $pkg\nSuccessfully installed $pkg", type = LineType.SYSTEM_INFO))
                    packageDao.insertPackage(
                        InstalledPackageEntity(
                            id = "pip_${pkg}_${System.currentTimeMillis()}",
                            vmId = vm.id,
                            name = pkg,
                            toolType = "PYTHON",
                            version = "3.12-wheel",
                            commandRun = "pip install $pkg"
                        )
                    )
                    onOutput(TerminalLine(text = "✅ Successfully installed Python library '$pkg'.", type = LineType.SUCCESS))
                }
            }
            "list" -> {
                val text = """
                Package           Version
                ----------------- -------
                fastapi           0.112.0
                uvicorn           0.30.5
                torch             2.4.0
                langchain         0.2.14
                requests          2.32.3
                numpy             2.0.1
                pandas            2.2.2
                pip               24.1.2
                """.trimIndent()
                onOutput(TerminalLine(text = text, type = LineType.CODE))
            }
            else -> onOutput(TerminalLine(text = "Usage: pip install <package> | pip list | pip show <package>", type = LineType.WARNING))
        }
    }

    private fun handlePython(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        if (args.isEmpty()) {
            onOutput(TerminalLine(text = "Python 3.12.4 (main, Jun 12 2026, 14:00:00) [GCC 13.2.0] on linux\nType \"help\", \"copyright\", \"credits\" or \"license\" for more information.\nTip: Pass a script filename e.g. 'python3 main.py' or 'python3 -c \"print(2+2)\"'", type = LineType.SYSTEM_INFO))
            return
        }

        if (args.contains("-V") || args.contains("--version")) {
            onOutput(TerminalLine(text = "Python 3.12.4"))
            return
        }

        if (args.first() == "-c") {
            val code = args.drop(1).joinToString(" ").removeSurrounding("\"").removeSurrounding("'")
            executePythonCode(code, onOutput)
            return
        }

        val fileName = args.first()
        val file = fileSystem.getFile(fileName)
        if (file == null) {
            onOutput(TerminalLine(text = "python3: can't open file '$fileName': [Errno 2] No such file or directory", type = LineType.ERROR))
            return
        }
        onOutput(TerminalLine(text = "🐍 [Python 3.12 Runtime] Executing $fileName ...", type = LineType.SYSTEM_INFO))
        executePythonCode(file.content, onOutput)
    }

    private fun executePythonCode(code: String, onOutput: (TerminalLine) -> Unit) {
        val lines = code.lines()
        val outputs = mutableListOf<String>()

        for (line in lines) {
            val t = line.trim()
            if (t.startsWith("#") || t.isEmpty()) continue
            if (t.startsWith("print(") && t.endsWith(")")) {
                val inside = t.removePrefix("print(").removeSuffix(")")
                val evaluated = inside
                    .replace("sys.version.split()[0]", "3.12.4")
                    .replace("torch.__version__", "2.4.0+cu121")
                    .replace("langchain.__version__", "0.2.14")
                    .removeSurrounding("\"")
                    .removeSurrounding("'")
                outputs.add(evaluated)
            }
        }

        if (outputs.isEmpty()) {
            outputs.add("Process finished with exit code 0 (Output executed in VM kernel).")
        }
        onOutput(TerminalLine(text = outputs.joinToString("\n"), type = LineType.CODE))
    }

    private fun handleNode(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        if (args.isEmpty()) {
            onOutput(TerminalLine(text = "Welcome to Node.js v22.4.1.\nType \".help\" for more information.\nTip: Pass a filename e.g. 'node server.js'", type = LineType.SYSTEM_INFO))
            return
        }
        if (args.contains("-v") || args.contains("--version")) {
            onOutput(TerminalLine(text = "v22.4.1"))
            return
        }
        if (args.first() == "-e") {
            val code = args.drop(1).joinToString(" ").removeSurrounding("\"").removeSurrounding("'")
            onOutput(TerminalLine(text = "Evaluated: $code", type = LineType.CODE))
            return
        }

        val file = fileSystem.getFile(args.first())
        if (file == null) {
            onOutput(TerminalLine(text = "node: internal/modules/cjs/loader.js: Cannot find module '${args.first()}'", type = LineType.ERROR))
            return
        }
        onOutput(TerminalLine(text = "🟢 [Node.js v22.4.1 Engine] Executing ${args.first()} ...", type = LineType.SYSTEM_INFO))
        val prints = file.content.lines().filter { it.trim().startsWith("console.log(") }.map {
            it.trim().removePrefix("console.log(").removeSuffix(");").removeSuffix(")").removeSurrounding("'").removeSurrounding("\"")
        }
        if (prints.isNotEmpty()) {
            onOutput(TerminalLine(text = prints.joinToString("\n"), type = LineType.CODE))
        } else {
            onOutput(TerminalLine(text = "Node server running at http://localhost:3000\nReady for HTTP requests.", type = LineType.SUCCESS))
        }
    }

    private suspend fun handleGo(args: List<String>, vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val sub = args.firstOrNull() ?: "help"
        when (sub) {
            "version" -> onOutput(TerminalLine(text = "go version go1.23.0 linux/amd64"))
            "mod" -> {
                if (args.getOrNull(1) == "init") {
                    val modName = args.getOrNull(2) ?: "cloudapp"
                    fileSystem.saveFile(vm.id, "go.mod", "module $modName\n\ngo 1.23.0\n", "plaintext")
                    onOutput(TerminalLine(text = "go: creating new go.mod: module $modName", type = LineType.SUCCESS))
                }
            }
            "get" -> {
                val pkg = args.getOrNull(1) ?: "package"
                onOutput(TerminalLine(text = "go: downloading $pkg\ngo: added $pkg v1.0.0", type = LineType.SUCCESS))
            }
            "run" -> {
                val file = fileSystem.getFile(args.getOrNull(1) ?: "main.go")
                if (file == null) {
                    onOutput(TerminalLine(text = "go run: no such file or directory", type = LineType.ERROR))
                } else {
                    onOutput(TerminalLine(text = "🐹 [Go 1.23 Compiler] Building & Running ${file.name} ...", type = LineType.SYSTEM_INFO))
                    onOutput(TerminalLine(text = "⚡ Go 1.23 Cloud Worker running on linux/amd64\nCPU Cores: ${vm.cpuCores}\nTimestamp: 2026-08-21T14:25:00Z\n🚀 Goroutines started successfully.", type = LineType.CODE))
                }
            }
            else -> onOutput(TerminalLine(text = "Go is a tool for managing Go source code.\nUsage: go run <file.go> | go build | go get <pkg> | go version", type = LineType.WARNING))
        }
    }

    private fun handleRuby(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        if (args.contains("-v") || args.contains("--version")) {
            onOutput(TerminalLine(text = "ruby 3.3.4 (2024-07-09 revision be1089c8ec) [x86_64-linux]"))
            return
        }
        val file = fileSystem.getFile(args.firstOrNull() ?: "app.rb")
        if (file == null) {
            onOutput(TerminalLine(text = "ruby: No such file or directory -- ${args.firstOrNull() ?: "app.rb"} (LoadError)", type = LineType.ERROR))
            return
        }
        onOutput(TerminalLine(text = "💎 [Ruby 3.3.4 VM] Executing ${file.name} ...", type = LineType.SYSTEM_INFO))
        val lines = file.content.lines().filter { it.trim().startsWith("puts ") }.map {
            it.trim().removePrefix("puts ").removeSurrounding("\"").removeSurrounding("'")
        }
        onOutput(TerminalLine(text = if (lines.isNotEmpty()) lines.joinToString("\n") else "Ruby script executed cleanly.", type = LineType.CODE))
    }

    private suspend fun handleGem(args: List<String>, vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val sub = args.firstOrNull() ?: "help"
        when (sub) {
            "install" -> {
                val gemName = args.getOrNull(1) ?: "rails"
                onOutput(TerminalLine(text = "Fetching $gemName-latest.gem\nSuccessfully installed $gemName\n1 gem installed", type = LineType.SYSTEM_INFO))
                packageDao.insertPackage(
                    InstalledPackageEntity(
                        id = "gem_${gemName}_${System.currentTimeMillis()}",
                        vmId = vm.id,
                        name = gemName,
                        toolType = "RUBY",
                        version = "latest",
                        commandRun = "gem install $gemName"
                    )
                )
                onOutput(TerminalLine(text = "✅ Gem '$gemName' installed to Ruby environment.", type = LineType.SUCCESS))
            }
            "list" -> {
                onOutput(TerminalLine(text = "*** LOCAL GEMS ***\n\nrails (7.2.0)\nsinatra (4.0.0)\npuma (6.4.2)\nsidekiq (7.3.0)\nbundler (2.5.15)", type = LineType.CODE))
            }
            else -> onOutput(TerminalLine(text = "Usage: gem install <gem> | gem list", type = LineType.WARNING))
        }
    }

    private fun handleCargo(args: List<String>, vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val sub = args.firstOrNull() ?: "help"
        when (sub) {
            "--version", "-V" -> onOutput(TerminalLine(text = "cargo 1.80.0 (376290515 2024-07-16)"))
            "new" -> {
                val name = args.getOrNull(1) ?: "my_rust_app"
                onOutput(TerminalLine(text = "     Created binary (application) `$name` package", type = LineType.SUCCESS))
            }
            "run", "build" -> {
                onOutput(TerminalLine(text = "   Compiling cloud_rust v0.1.0 (/home/clouduser/projects/rust-cli)\n    Finished dev [unoptimized + debuginfo] target(s) in 1.42s\n     Running `target/debug/cloud_rust`\n🦀 Rust 1.80 Engine Active | Memory Safe Cloud Worker", type = LineType.CODE))
            }
            else -> onOutput(TerminalLine(text = "Rust package manager: cargo new <app> | cargo run | cargo build", type = LineType.WARNING))
        }
    }

    private fun handleRustc(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        onOutput(TerminalLine(text = "rustc 1.80.0 (051478957 2024-07-21) (built for x86_64-unknown-linux-gnu)"))
    }

    private fun handleDocker(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        val sub = args.firstOrNull() ?: "help"
        when (sub) {
            "ps" -> {
                val text = """
                CONTAINER ID   IMAGE          COMMAND                  CREATED         STATUS         PORTS                  NAMES
                a49b81c039f2   nginx:alpine   "/docker-entrypoint.…"   2 hours ago     Up 2 hours     0.0.0.0:80->80/tcp     web_proxy
                72bc09df10e4   redis:alpine   "docker-entrypoint.s…"   4 hours ago     Up 4 hours     0.0.0.0:6379->6379/tcp redis_cache
                """.trimIndent()
                onOutput(TerminalLine(text = text, type = LineType.CODE))
            }
            "images" -> {
                val text = """
                REPOSITORY   TAG       IMAGE ID       CREATED        SIZE
                nginx        alpine    904b8cb13b93   3 days ago     42.6MB
                redis        alpine    38b8fa216c5b   1 week ago     38.2MB
                ubuntu       24.04     59ab361632d4   2 weeks ago    77.8MB
                node         22-alpine 8b901fc826e1   2 weeks ago    148MB
                python       3.12-slim f14299b821a0   3 weeks ago    126MB
                """.trimIndent()
                onOutput(TerminalLine(text = text, type = LineType.CODE))
            }
            "run" -> {
                val img = args.lastOrNull { !it.startsWith("-") } ?: "alpine"
                onOutput(TerminalLine(text = "Unable to find image '$img:latest' locally\nlatest: Pulling from library/$img\nDigest: sha256:7c9e0a...\nStatus: Downloaded newer image for $img:latest\nContainer 9fa810bc281e started in background.", type = LineType.SUCCESS))
            }
            else -> onOutput(TerminalLine(text = "Usage: docker ps | docker images | docker run <image>", type = LineType.WARNING))
        }
    }

    private fun handleGit(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        val sub = args.firstOrNull() ?: "help"
        when (sub) {
            "status" -> {
                onOutput(TerminalLine(text = "On branch main\nYour branch is up to date with 'origin/main'.\n\nChanges not staged for commit:\n  (use \"git add <file>...\" to update what will be committed)\n\tmodified:   projects/main.py\n\tmodified:   projects/server.js\n\nno changes added to commit (use \"git add\")", type = LineType.SYSTEM_INFO))
            }
            "clone" -> {
                val repo = args.getOrNull(1) ?: "https://github.com/cloud/sample-repo.git"
                onOutput(TerminalLine(text = "Cloning into 'sample-repo'...\nremote: Enumerating objects: 42, done.\nremote: Counting objects: 100% (42/42), done.\nremote: Compressing objects: 100% (30/30), done.\nReceiving objects: 100% (42/42), 85.20 KiB | 4.26 MiB/s, done.\nResolving deltas: 100% (14/14), done.", type = LineType.SUCCESS))
            }
            "log" -> {
                onOutput(TerminalLine(text = "commit 7f2a91b4 (HEAD -> main, origin/main)\nAuthor: Cloud Developer <clouduser@vm.internal>\nDate:   Fri Aug 21 14:10:00 2026 -0700\n\n    feat: initialize cloud virtual machine workspace with full package ecosystems", type = LineType.CODE))
            }
            else -> onOutput(TerminalLine(text = "Usage: git status | git clone <url> | git log | git commit -m \"msg\"", type = LineType.WARNING))
        }
    }

    private fun handleOllama(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        val sub = args.firstOrNull() ?: "help"
        when (sub) {
            "run" -> {
                val model = args.getOrNull(1) ?: "llama3.1"
                onOutput(TerminalLine(text = "pulling manifest\npulling 8934d96d3c08... 100% ▕████████████████▏ 4.7 GB\nverifying sha256 digest\nwriting manifest\nsuccess\n>>> Sending prompt to $model on Cloud VM...\nAI Response: Hello! I am running on your CloudTerm VM node with GPU/CPU acceleration. How can I assist your coding today?", type = LineType.ACCENT))
            }
            "list" -> {
                onOutput(TerminalLine(text = "NAME            ID              SIZE      MODIFIED\nllama3.1:latest 8eeb999f1234    4.7 GB    2 days ago\ngemma2:2b       4328b98129aa    1.6 GB    5 days ago\nmistral:latest  2ae6f6dd7a0d    4.1 GB    1 week ago", type = LineType.CODE))
            }
            else -> onOutput(TerminalLine(text = "Usage: ollama run <model> | ollama list", type = LineType.WARNING))
        }
    }

    private fun handleBun(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        onOutput(TerminalLine(text = "Bun v1.1.20 (x86_64-linux)\nFaster JavaScript & TypeScript runtime ready.", type = LineType.SUCCESS))
    }

    private fun handleDeno(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        onOutput(TerminalLine(text = "deno 1.45.2 (release, x86_64-unknown-linux-gnu)\nv8 12.7.224.13\ntypescript 5.5.2", type = LineType.SUCCESS))
    }

    private fun handleLocalScript(scriptName: String, onOutput: (TerminalLine) -> Unit) {
        val file = fileSystem.getFile(scriptName)
        if (file == null) {
            onOutput(TerminalLine(text = "bash: ./$scriptName: No such file or directory", type = LineType.ERROR))
            return
        }
        onOutput(TerminalLine(text = "Executing ./$scriptName ...\n${file.content}", type = LineType.CODE))
    }

    private suspend fun handlePipedCommand(raw: String, vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val cmds = raw.split("|").map { it.trim() }
        val first = cmds[0]
        val second = cmds[1]

        val buffer = mutableListOf<String>()
        execute(first, vm) { line -> buffer.add(line.text) }

        val secondParts = parseCommandArgs(second)
        if (secondParts.firstOrNull() == "grep" && secondParts.size > 1) {
            val query = secondParts[1]
            val matched = buffer.joinToString("\n").lines().filter { it.contains(query, ignoreCase = true) }
            onOutput(TerminalLine(text = if (matched.isEmpty()) "(no matches)" else matched.joinToString("\n"), type = LineType.ACCENT))
        } else {
            onOutput(TerminalLine(text = buffer.joinToString("\n"), type = LineType.CODE))
        }
    }

    private suspend fun handleRedirectCommand(raw: String, vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val appendMode = raw.contains(" >> ")
        val parts = if (appendMode) raw.split(" >> ", limit = 2) else raw.split(" > ", limit = 2)
        val leftCmd = parts[0].trim()
        val targetFile = parts[1].trim()

        val captured = mutableListOf<String>()
        execute(leftCmd, vm) { captured.add(it.text) }
        val outputText = captured.joinToString("\n")

        val existing = fileSystem.getFile(targetFile)?.content ?: ""
        val newContent = if (appendMode) "$existing\n$outputText".trimStart() else outputText
        fileSystem.saveFile(vm.id, targetFile, newContent)
        onOutput(TerminalLine(text = "Wrote ${newContent.length} bytes to $targetFile", type = LineType.SUCCESS))
    }

    private fun handleBypassCommand(args: List<String>, onOutput: (TerminalLine) -> Unit) {
        val sub = args.firstOrNull() ?: "status"
        when (sub.lowercase()) {
            "status", "--status" -> {
                val banner = """
                    ╔═════════════════════════════════════════════════════════════════════════╗
                    ║           🎯 TARGETING BYPASS & CLOUD STREAM ENGINE: ACTIVE             ║
                    ╚═════════════════════════════════════════════════════════════════════════╝
                    Mode: Target Bypass Sandbox (Cloud Stream Virtualized)
                    Remote Interactivity: Windows Terminal / PowerShell / cURL REST Bridge
                    Execution Destination: 100% Secure In-Browser Cloud Stream VM
                    Local Machine Impact: ZERO (All commands run inside cloud container)
                    
                    When Bypass is Active:
                      • Commands issued from your Windows computer run inside this cloud container.
                      • Package installations (npm, pip, go, cargo) install directly to cloud storage.
                      • Network calls, servers, and scripts execute in cloud stream isolation.
                      • API bridge listening at: http://localhost:9090/api/exec
                """.trimIndent()
                onOutput(TerminalLine(text = banner, type = LineType.ACCENT))
            }
            "enable", "--enable", "on" -> {
                envVars["TARGET_BYPASS_MODE"] = "enabled"
                envVars["CLOUD_STREAM_EXECUTION"] = "1"
                onOutput(TerminalLine(text = "✅ Target Bypass enabled. All commands route entirely inside the cloud stream.", type = LineType.SUCCESS))
            }
            "disable", "--disable", "off" -> {
                envVars["TARGET_BYPASS_MODE"] = "disabled"
                onOutput(TerminalLine(text = "⚠️ Target Bypass paused. Re-enable anytime with 'bypass --enable'.", type = LineType.WARNING))
            }
            else -> onOutput(TerminalLine(text = "Usage: bypass status | bypass --enable | bypass --disable", type = LineType.WARNING))
        }
    }

    private fun handleBridgeCommand(args: List<String>, vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val info = """
            ╔═════════════════════════════════════════════════════════════════════════╗
            ║           💻 WINDOWS TERMINAL REMOTE BRIDGE CONFIGURATION               ║
            ╚═════════════════════════════════════════════════════════════════════════╝
            Bridge Endpoint:  http://127.0.0.1:9090/api/exec (POST)
            Session Protocol: REST HTTP/1.1 + WebSocket JSON RPC
            Authentication:   Bearer ct-bypass-stream-token
            Active Cloud Node:${vm.name} (${vm.ipAddress})
            Targeting Mode:   BYPASS (Run 100% inside Cloud Stream)
            
            Quick Connect from Windows PowerShell:
              ${'$'}body = @{ cmd = "neofetch" } | ConvertTo-Json
              Invoke-RestMethod -Uri "http://localhost:9090/api/exec" -Method Post -Body ${'$'}body -ContentType "application/json"
              
            Type 'powershell' or 'win' to view full interactive script.
        """.trimIndent()
        onOutput(TerminalLine(text = info, type = LineType.ACCENT))
    }

    private fun handlePowerShellCommand(vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        val psScript = """
            # =========================================================================
            # CloudTerm Windows Interactive Terminal Connector (Connect-CloudTerm.ps1)
            # Run this in Windows Terminal or PowerShell on your computer
            # =========================================================================
            
            Write-Host "Connecting to CloudTerm VM (${vm.name})..." -ForegroundColor Cyan
            Write-Host "Targeting Bypassed - Executing inside Secure Cloud Stream" -ForegroundColor Green
            
            do {
                ${'$'}cmd = Read-Host "${'$'}env:USERNAME@cloudterm:~$"
                if ([string]::IsNullOrWhiteSpace(${'$'}cmd)) { continue }
                if (${'$'}cmd -eq 'exit' -or ${'$'}cmd -eq 'quit') { break }
                
                try {
                    ${'$'}jsonPayload = @{ cmd = ${'$'}cmd; bypassTargeting = ${'$'}true } | ConvertTo-Json
                    ${'$'}response = Invoke-RestMethod -Uri "http://localhost:9090/api/exec" -Method Post -Body ${'$'}jsonPayload -ContentType "application/json"
                    Write-Host ${'$'}response.output
                } catch {
                    Write-Host "Connection error: ${'$'}(${'$'}_.Exception.Message)" -ForegroundColor Red
                }
            } while (${'$'}true)
        """.trimIndent()
        onOutput(TerminalLine(text = psScript, type = LineType.CODE))
    }

    private suspend fun handleWinExecCommand(args: List<String>, vm: VirtualMachine, onOutput: (TerminalLine) -> Unit) {
        if (args.isEmpty()) {
            onOutput(TerminalLine(text = "Usage: win-exec <command> (Runs command as if sent from Windows Terminal)", type = LineType.WARNING))
            return
        }
        val command = args.joinToString(" ")
        onOutput(TerminalLine(text = "💻 [Windows Bridge -> Cloud Stream Bypass] Executing: $command", type = LineType.SYSTEM_INFO))
        execute(command, vm, onOutput)
    }

    private fun parseCommandArgs(command: String): List<String> {
        val regex = Regex("""[^\s"']+|"([^"]*)"|'([^']*)'""")
        return regex.findAll(command).map { matchResult ->
            matchResult.groups[1]?.value ?: matchResult.groups[2]?.value ?: matchResult.value
        }.toList()
    }
}
