package com.example.engine

import com.example.data.local.VirtualFileDao
import com.example.data.local.VirtualFileEntity
import com.example.data.model.VirtualFile

class VmFileSystem(private val fileDao: VirtualFileDao) {

    private val filesMap = mutableMapOf<String, VirtualFile>()
    private var currentDirectory = "/home/clouduser"

    fun getCurrentDirectory(): String = currentDirectory

    fun setCurrentDirectory(path: String): Boolean {
        val resolved = resolvePath(path)
        if (filesMap[resolved]?.isDirectory == true || resolved == "/" || resolved == "/home" || resolved == "/home/clouduser") {
            currentDirectory = resolved
            return true
        }
        return false
    }

    suspend fun initializeDefaultFiles(vmId: String, distroName: String) {
        filesMap.clear()

        // Root & system directories
        addFileInternal(VirtualFile("/", "", isDirectory = true))
        addFileInternal(VirtualFile("/home", "home", isDirectory = true))
        addFileInternal(VirtualFile("/home/clouduser", "clouduser", isDirectory = true))
        addFileInternal(VirtualFile("/home/clouduser/projects", "projects", isDirectory = true))
        addFileInternal(VirtualFile("/etc", "etc", isDirectory = true))
        addFileInternal(VirtualFile("/var", "var", isDirectory = true))
        addFileInternal(VirtualFile("/var/log", "log", isDirectory = true))
        addFileInternal(VirtualFile("/tmp", "tmp", isDirectory = true))

        // Starter files
        val osRelease = """
            NAME="$distroName"
            VERSION="24.04 LTS (Noble Numbat)"
            ID=ubuntu
            ID_LIKE=debian
            PRETTY_NAME="$distroName Cloud Node"
            VERSION_ID="24.04"
            HOME_URL="https://ubuntu.com/"
            SUPPORT_URL="https://help.ubuntu.com/"
            BUG_REPORT_URL="https://bugs.launchpad.net/ubuntu/"
        """.trimIndent()
        addFileInternal(VirtualFile("/etc/os-release", "os-release", false, osRelease, osRelease.length.toLong(), language = "shell"))
        addFileInternal(VirtualFile("/etc/hostname", "hostname", false, "cloudterm-node-01", 18, language = "plaintext"))

        // Sample Python Project
        val pyCode = """
            # CloudTerm Python 3.12 AI Microservice
            import sys
            import time

            def calculate_fibonacci(n):
                a, b = 0, 1
                result = []
                for _ in range(n):
                    result.append(a)
                    a, b = b, a + b
                return result

            print("⚡ Initializing Cloud AI Service on Python", sys.version.split()[0])
            print("Computing Fibonacci sequence (10 items)...")
            fib = calculate_fibonacci(10)
            print("Result:", fib)
            print("✅ Service ready on http://0.0.0.0:8000")
        """.trimIndent()
        addFileInternal(VirtualFile("/home/clouduser/projects/main.py", "main.py", false, pyCode, pyCode.length.toLong(), language = "python"))

        // Sample Node.js Project
        val jsCode = """
            // CloudTerm Node.js v22 Microservice
            const http = require('http');

            const PORT = 3000;
            console.log('🚀 Starting Node.js Web Server in Cloud VM...');
            console.log(`📡 Listening on http://localhost:${'$'}{PORT}`);
            console.log('📦 Packages: express, dotenv, cors loaded.');
        """.trimIndent()
        addFileInternal(VirtualFile("/home/clouduser/projects/server.js", "server.js", false, jsCode, jsCode.length.toLong(), language = "javascript"))

        // Sample Go Project
        val goCode = """
            package main

            import (
                "fmt"
                "runtime"
                "time"
            )

            func main() {
                fmt.Printf("⚡ Go 1.23 Cloud Worker running on %s/%s\n", runtime.GOOS, runtime.GOARCH)
                fmt.Printf("CPU Cores: %d\n", runtime.NumCPU())
                fmt.Printf("Timestamp: %s\n", time.Now().Format(time.RFC3339))
                fmt.Println("🚀 Goroutines started successfully.")
            }
        """.trimIndent()
        addFileInternal(VirtualFile("/home/clouduser/projects/main.go", "main.go", false, goCode, goCode.length.toLong(), language = "go"))

        // Sample Ruby Project
        val rbCode = """
            # Ruby 3.3 Cloud Service
            puts "💎 Ruby 3.3.4 Environment Active"
            puts "Host VM: CloudTerm Node (Ubuntu 24.04)"
            
            gems = ["rails", "sinatra", "puma", "sidekiq"]
            puts "Installed Gems: " + gems.join(", ")
            puts "Server listening for background jobs..."
        """.trimIndent()
        addFileInternal(VirtualFile("/home/clouduser/projects/app.rb", "app.rb", false, rbCode, rbCode.length.toLong(), language = "ruby"))

        // Sample Readme
        val readme = """
            # Welcome to CloudTerm VM Workspace
            
            This is a full cloud Linux environment equipped with modern developer runtimes:
            - Node.js & npm (v22.4)
            - Python & pip (v3.12)
            - Ruby & gem (v3.3)
            - Go / Golang (v1.23)
            - Rust & cargo (v1.80)
            - Docker & Container tools
            
            Try commands:
              neofetch
              npm install express
              python main.py
              go run main.go
              ruby app.rb
              curl wttr.in/Paris
              htop
        """.trimIndent()
        addFileInternal(VirtualFile("/home/clouduser/README.md", "README.md", false, readme, readme.length.toLong(), language = "markdown"))

        // Persist to Room
        filesMap.values.forEach { vf ->
            fileDao.insertFile(
                VirtualFileEntity(
                    path = vf.path,
                    vmId = vmId,
                    name = vf.name,
                    isDirectory = vf.isDirectory,
                    content = vf.content,
                    permissions = vf.permissions,
                    modifiedAt = vf.modifiedAt,
                    language = vf.language
                )
            )
        }
    }

    fun resolvePath(rawPath: String): String {
        var clean = rawPath.trim()
        if (clean == "~" || clean == "~/.") return "/home/clouduser"
        if (clean.startsWith("~/")) {
            clean = "/home/clouduser/" + clean.removePrefix("~/")
        }
        if (!clean.startsWith("/")) {
            clean = if (currentDirectory == "/") "/$clean" else "$currentDirectory/$clean"
        }

        // Normalize . and ..
        val parts = clean.split("/").filter { it.isNotEmpty() }
        val stack = mutableListOf<String>()
        for (part in parts) {
            if (part == ".") continue
            if (part == "..") {
                if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
            } else {
                stack.add(part)
            }
        }
        return "/" + stack.joinToString("/")
    }

    fun listFiles(dirPath: String = currentDirectory): List<VirtualFile> {
        val target = resolvePath(dirPath)
        val prefix = if (target == "/") "/" else "$target/"

        return filesMap.values.filter { file ->
            if (file.path == target) return@filter false
            if (!file.path.startsWith(prefix)) return@filter false
            val remaining = file.path.removePrefix(prefix)
            !remaining.contains("/") // direct children only
        }.sortedWith(compareByDescending<VirtualFile> { it.isDirectory }.thenBy { it.name })
    }

    fun getAllFiles(): List<VirtualFile> {
        return filesMap.values.toList()
    }

    fun getFile(path: String): VirtualFile? {
        val resolved = resolvePath(path)
        return filesMap[resolved]
    }

    suspend fun saveFile(vmId: String, path: String, content: String, language: String = "plaintext") {
        val resolved = resolvePath(path)
        val fileName = resolved.split("/").last()
        val vf = VirtualFile(
            path = resolved,
            name = fileName,
            isDirectory = false,
            content = content,
            sizeBytes = content.length.toLong(),
            language = language
        )
        filesMap[resolved] = vf
        fileDao.insertFile(
            VirtualFileEntity(
                path = resolved,
                vmId = vmId,
                name = fileName,
                isDirectory = false,
                content = content,
                permissions = "-rw-r--r--",
                modifiedAt = System.currentTimeMillis(),
                language = language
            )
        )
    }

    suspend fun makeDirectory(vmId: String, path: String): Boolean {
        val resolved = resolvePath(path)
        if (filesMap.containsKey(resolved)) return false
        val dirName = resolved.split("/").last()
        val vf = VirtualFile(
            path = resolved,
            name = dirName,
            isDirectory = true,
            permissions = "drwxr-xr-x"
        )
        filesMap[resolved] = vf
        fileDao.insertFile(
            VirtualFileEntity(
                path = resolved,
                vmId = vmId,
                name = dirName,
                isDirectory = true,
                content = "",
                permissions = "drwxr-xr-x",
                modifiedAt = System.currentTimeMillis(),
                language = "folder"
            )
        )
        return true
    }

    suspend fun deletePath(vmId: String, path: String): Boolean {
        val resolved = resolvePath(path)
        if (!filesMap.containsKey(resolved)) return false
        val isDir = filesMap[resolved]?.isDirectory == true
        filesMap.remove(resolved)
        fileDao.deleteFile(vmId, resolved)

        if (isDir) {
            val prefix = "$resolved/"
            val toRemove = filesMap.keys.filter { it.startsWith(prefix) }
            toRemove.forEach {
                filesMap.remove(it)
            }
            fileDao.deleteDirectoryRecursive(vmId, resolved)
        }
        return true
    }

    private fun addFileInternal(file: VirtualFile) {
        filesMap[file.path] = file
    }
}
