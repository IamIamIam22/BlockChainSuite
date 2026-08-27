package com.example.data.repository

import com.example.data.model.SoftwareItem
import com.example.data.model.ToolType

object SoftwareMarketRepository {

    val catalog: List<SoftwareItem> = listOf(
        // Modern AI & LLM Tools (2025/2026 trending)
        SoftwareItem(
            id = "ollama",
            name = "Ollama",
            toolType = ToolType.AI_LLM,
            version = "0.3.14",
            description = "Run open-source large language models (Llama 3.1, Mistral, DeepSeek, Gemma 2) locally in VM with clean REST API.",
            category = "AI / LLM",
            commandInstall = "curl -fsSL https://ollama.com/install.sh | sh",
            launchCommand = "ollama run llama3",
            stars = "92.4k",
            releaseDate = "v0.3.14 (2026)",
            sampleCode = "# Run local LLM model\nollama run gemma2:2b\ncurl http://localhost:11434/api/generate -d '{\"model\": \"gemma2\", \"prompt\": \"Hello AI\"}'"
        ),
        SoftwareItem(
            id = "pytorch",
            name = "PyTorch 2.4",
            toolType = ToolType.PYTHON,
            version = "2.4.0",
            description = "Premier tensor computation and deep neural network platform with GPU/MPS & CPU acceleration.",
            category = "Machine Learning",
            commandInstall = "pip install torch torchvision torchaudio",
            launchCommand = "python -c 'import torch; print(torch.__version__)'",
            stars = "81.0k",
            releaseDate = "2026 Latest",
            sampleCode = "import torch\nx = torch.rand(5, 3)\nprint('PyTorch Tensor:\n', x)\nprint('Device:', 'cuda' if torch.cuda.is_available() else 'cpu')"
        ),
        SoftwareItem(
            id = "langchain",
            name = "LangChain",
            toolType = ToolType.PYTHON,
            version = "0.2.14",
            description = "Building context-aware reasoning applications powered by LLMs, agents, chains, and vector memory.",
            category = "AI / LLM",
            commandInstall = "pip install langchain langchain-community langchain-core",
            launchCommand = "python -c 'import langchain; print(langchain.__version__)'",
            stars = "89.2k",
            releaseDate = "2026",
            sampleCode = "from langchain_core.prompts import PromptTemplate\nprompt = PromptTemplate.from_template('Explain {topic} in one line.')\nprint(prompt.format(topic='Cloud Virtual Machines'))"
        ),
        SoftwareItem(
            id = "uv-python",
            name = "uv (Fast Python Manager)",
            toolType = ToolType.PYTHON,
            version = "0.3.2",
            description = "An extremely fast Python package and project manager, written in Rust. 10-100x faster than pip.",
            category = "Package Manager",
            commandInstall = "curl -LsSf https://astral.sh/uv/install.sh | sh",
            launchCommand = "uv pip list",
            stars = "35.8k",
            releaseDate = "2026 Modern",
            sampleCode = "uv venv .venv\nuv pip install fastapi uvicorn httpx\nuv run python app.py"
        ),

        // Node.js / NPM Ecosystem
        SoftwareItem(
            id = "express",
            name = "Express.js",
            toolType = ToolType.NPM,
            version = "4.19.2",
            description = "Fast, unopinionated, minimalist web framework for Node.js REST API servers and microservices.",
            category = "Web Framework",
            commandInstall = "npm install express",
            launchCommand = "node server.js",
            stars = "63.5k",
            releaseDate = "Active",
            sampleCode = "const express = require('express');\nconst app = express();\napp.get('/', (req, res) => res.json({ status: 'running', cloud: 'CloudTerm VM' }));\napp.listen(3000, () => console.log('Listening on port 3000'));"
        ),
        SoftwareItem(
            id = "nextjs",
            name = "Next.js",
            toolType = ToolType.NPM,
            version = "14.2.5",
            description = "The React framework for the Web. Full-stack hybrid static & server rendering with App Router.",
            category = "Frontend / Fullstack",
            commandInstall = "npx create-next-app@latest my-app",
            launchCommand = "npm run dev",
            stars = "124.0k",
            releaseDate = "Latest",
            sampleCode = "// app/page.tsx\nexport default function Page() {\n  return <h1>Welcome to CloudTerm VM Next.js</h1>;\n}"
        ),
        SoftwareItem(
            id = "bun",
            name = "Bun Runtime",
            toolType = ToolType.SYSTEM,
            version = "1.1.20",
            description = "Incredibly fast JavaScript & TypeScript all-in-one toolkit: bundler, test runner, and Node.js-compatible package manager.",
            category = "Runtime",
            commandInstall = "curl -fsSL https://bun.sh/install | bash",
            launchCommand = "bun --version",
            stars = "73.2k",
            releaseDate = "2026",
            sampleCode = "Bun.serve({\n  fetch(req) {\n    return new Response('Hello from Bun on Cloud VM!');\n  },\n  port: 3000,\n});"
        ),
        SoftwareItem(
            id = "typescript",
            name = "TypeScript",
            toolType = ToolType.NPM,
            version = "5.5.4",
            description = "Typed superset of JavaScript that compiles to plain JavaScript for reliable enterprise codebases.",
            category = "Languages",
            commandInstall = "npm install -g typescript ts-node",
            launchCommand = "tsc --version",
            stars = "99.1k",
            releaseDate = "Stable",
            sampleCode = "interface ServerNode {\n  id: string;\n  status: 'active' | 'idle';\n}\nconst node: ServerNode = { id: 'vm-01', status: 'active' };\nconsole.log(node);"
        ),

        // Python Ecosystem
        SoftwareItem(
            id = "fastapi",
            name = "FastAPI",
            toolType = ToolType.PYTHON,
            version = "0.112.0",
            description = "Modern, high-performance web framework for building APIs with Python 3.8+ based on standard Python type hints.",
            category = "Web Framework",
            commandInstall = "pip install fastapi uvicorn",
            launchCommand = "uvicorn main:app --reload",
            stars = "74.8k",
            releaseDate = "Latest",
            sampleCode = "from fastapi import FastAPI\napp = FastAPI()\n\n@app.get('/')\ndef read_root():\n    return {'cloud_vm': 'Ubuntu 24.04', 'engine': 'FastAPI'}"
        ),
        SoftwareItem(
            id = "django",
            name = "Django",
            toolType = ToolType.PYTHON,
            version = "5.1.0",
            description = "The web framework for perfectionists with deadlines. Batteries included with ORM, admin dashboard, auth.",
            category = "Web Framework",
            commandInstall = "pip install django",
            launchCommand = "django-admin startproject mysite",
            stars = "78.4k",
            releaseDate = "Stable",
            sampleCode = "# Django setup in VM\ndjango-admin startproject dev_server\ncd dev_server\npython manage.py runserver 8000"
        ),

        // Ruby Ecosystem
        SoftwareItem(
            id = "rails",
            name = "Ruby on Rails",
            toolType = ToolType.RUBY,
            version = "7.2.0",
            description = "Full-stack web-application framework that includes everything needed to create database-backed web applications.",
            category = "Web Framework",
            commandInstall = "gem install rails",
            launchCommand = "rails new my_api --api",
            stars = "55.3k",
            releaseDate = "v7.2 (2026)",
            sampleCode = "# config/routes.rb\nRails.application.routes.draw do\n  root to: proc { [200, {}, ['Ruby on Rails Cloud VM is live!']] }\nend"
        ),
        SoftwareItem(
            id = "sinatra",
            name = "Sinatra (Ruby)",
            toolType = ToolType.RUBY,
            version = "4.0.0",
            description = "Classy web development framework in Ruby for creating web applications in microservices architecture with minimal effort.",
            category = "Web Framework",
            commandInstall = "gem install sinatra",
            launchCommand = "ruby app.rb",
            stars = "12.1k",
            releaseDate = "Active",
            sampleCode = "require 'sinatra'\n\nget '/' do\n  'Hello from Sinatra inside CloudTerm!'\nend"
        ),

        // Go Ecosystem
        SoftwareItem(
            id = "fiber-go",
            name = "Fiber (Go)",
            toolType = ToolType.GO,
            version = "2.52.5",
            description = "An Express-inspired web framework built on top of Fasthttp, the fastest HTTP engine for Go with zero memory allocation.",
            category = "Web Framework",
            commandInstall = "go get -u github.com/gofiber/fiber/v2",
            launchCommand = "go run main.go",
            stars = "33.9k",
            releaseDate = "2026",
            sampleCode = "package main\nimport \"github.com/gofiber/fiber/v2\"\n\nfunc main() {\n    app := fiber.New()\n    app.Get(\"/\", func(c *fiber.Ctx) error {\n        return c.SendString(\"Fiber Go running on Cloud VM\")\n    })\n    app.Listen(\":3000\")\n}"
        ),
        SoftwareItem(
            id = "gin-gonic",
            name = "Gin (Go)",
            toolType = ToolType.GO,
            version = "1.10.0",
            description = "HTTP web framework written in Go (Golang). Features a Martini-like API with top tier performance (40x faster).",
            category = "Web Framework",
            commandInstall = "go get -u github.com/gin-gonic/gin",
            launchCommand = "go run server.go",
            stars = "77.5k",
            releaseDate = "Stable",
            sampleCode = "package main\nimport \"github.com/gin-gonic/gin\"\n\nfunc main() {\n    r := gin.Default()\n    r.GET(\"/ping\", func(c *gin.Context) {\n        c.JSON(200, gin.H{\"message\": \"pong\"})\n    })\n    r.Run(\":8080\")\n}"
        ),

        // Rust Ecosystem
        SoftwareItem(
            id = "axum",
            name = "Axum (Rust)",
            toolType = ToolType.RUST,
            version = "0.7.5",
            description = "Ergonomic and modular web framework built with Tokio, Tower, and Hyper for blazingly fast memory-safe microservices.",
            category = "Web Framework",
            commandInstall = "cargo add axum tokio --features tokio/full",
            launchCommand = "cargo run",
            stars = "18.6k",
            releaseDate = "2026",
            sampleCode = "use axum::{routing::get, Router};\n\n#[tokio::main]\nasync fn main() {\n    let app = Router::new().route(\"/\", get(|| async { \"Axum Rust Cloud Engine\" }));\n    let listener = tokio::net::TcpListener::bind(\"0.0.0.0:3000\").await.unwrap();\n    axum::serve(listener, app).await.unwrap();\n}"
        ),

        // DevOps, Containers & Cloud Tools
        SoftwareItem(
            id = "docker-ce",
            name = "Docker Engine & Compose",
            toolType = ToolType.DOCKER,
            version = "27.1.1",
            description = "Industry standard containerization engine for packaging and running distributed applications anywhere.",
            category = "DevOps / Containers",
            commandInstall = "apt-get install -y docker.io docker-compose",
            launchCommand = "docker ps",
            stars = "68.0k",
            releaseDate = "2026",
            sampleCode = "docker run -d -p 80:80 --name webserver nginx:alpine\ndocker ps\ndocker logs webserver"
        ),
        SoftwareItem(
            id = "redis-server",
            name = "Redis In-Memory Store",
            toolType = ToolType.DEV_OPS,
            version = "7.2.5",
            description = "Ultra-fast in-memory data structure store used as a database, cache, message broker, and streaming engine.",
            category = "Databases",
            commandInstall = "apt-get install -y redis-server",
            launchCommand = "redis-cli ping",
            stars = "64.1k",
            releaseDate = "Stable",
            sampleCode = "# Start Redis service\nredis-server --daemonize yes\nredis-cli set cloud \"CloudTerm-VM\"\nredis-cli get cloud"
        ),
        SoftwareItem(
            id = "postgresql",
            name = "PostgreSQL 16",
            toolType = ToolType.DEV_OPS,
            version = "16.4",
            description = "The World's Most Advanced Open Source Relational Database with JSONB, vector search, and transactional integrity.",
            category = "Databases",
            commandInstall = "apt-get install -y postgresql postgresql-contrib",
            launchCommand = "psql -U postgres -c 'SELECT version();'",
            stars = "14.2k",
            releaseDate = "2026",
            sampleCode = "sudo -u postgres psql -c \"CREATE DATABASE devdb;\"\nsudo -u postgres psql -d devdb -c \"SELECT 'PostgreSQL on CloudTerm' AS status;\""
        ),
        SoftwareItem(
            id = "neovim",
            name = "Neovim (Lua-Powered)",
            toolType = ToolType.SYSTEM,
            version = "0.10.1",
            description = "Vim-fork focused on extensibility and usability with Lua scripting, Treesitter syntax tree, and LSP client.",
            category = "CLI / Editors",
            commandInstall = "apt-get install -y neovim",
            launchCommand = "nvim main.py",
            stars = "81.6k",
            releaseDate = "Latest",
            sampleCode = "# Open file in Neovim terminal editor\nnvim script.py"
        ),
        SoftwareItem(
            id = "htop",
            name = "htop System Monitor",
            toolType = ToolType.SYSTEM,
            version = "3.3.0",
            description = "Interactive process viewer, text-mode application (for Unix systems) and visual process manager.",
            category = "System / Telemetry",
            commandInstall = "apt-get install -y htop",
            launchCommand = "htop",
            stars = "6.5k",
            releaseDate = "Stable",
            sampleCode = "# Launch interactive process table\nhtop"
        ),
        SoftwareItem(
            id = "neofetch",
            name = "Neofetch / Fastfetch",
            toolType = ToolType.SYSTEM,
            version = "7.1.0",
            description = "CLI system information tool written in bash 3.2+ that displays information about your OS, hardware, and kernel with ASCII logos.",
            category = "System / Telemetry",
            commandInstall = "apt-get install -y neofetch",
            launchCommand = "neofetch",
            stars = "22.3k",
            releaseDate = "Active",
            sampleCode = "# Display full cloud virtual machine hardware & OS specs\nneofetch"
        )
    )
}
