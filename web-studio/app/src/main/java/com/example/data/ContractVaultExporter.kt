package com.example.data

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ContractVaultExporter {

    fun generateWalletSecurityConfigJson(
        smartWallets: List<SmartWallet>,
        config: SentinelConfig
    ): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val walletEntries = smartWallets.mapIndexed { idx, wallet ->
            val label = if (wallet.address.equals(config.walletAddress, ignoreCase = true)) "Trust Wallet (Linked Profit Wallet)" else "Smart Account #${idx + 1}"
            """
            {
              "wallet_id": "wallet_${idx + 1}",
              "name": "$label",
              "chain": "Ethereum Mainnet",
              "address": "${wallet.address}",
              "public_key": "${wallet.publicKey}",
              "private_key": "${wallet.privateKey}",
              "mnemonic_phrase": "${wallet.mnemonic}",
              "derivation_path": "m/44'/60'/0'/0/0",
              "security_type": "BIP-39 Hardware-Compatible Keypair",
              "eth_balance": ${wallet.ethBalance},
              "btc_balance": ${wallet.btcBalance}
            }
            """.trimIndent()
        }.joinToString(",\n    ")

        return """
        {
          "version": "2.1.0",
          "generated_timestamp": "$dateStr",
          "application": "Sentinel MEV Bot & Smart Contract Engine",
          "security_policy": {
            "reentrancy_guard_enforced": true,
            "access_control_owner": "${config.walletAddress}",
            "gas_policy_enabled": ${config.gasPolicyEnabled},
            "daily_contract_limit": ${config.dailyLimitContracts},
            "target_profit_pct": ${config.targetProfitPct},
            "default_rpc_url": "${config.rpcUrl}"
          },
          "rpc_network_endpoints": [
            { "network": "Ethereum Mainnet", "rpc": "https://eth-mainnet.g.alchemy.com/v2/demo", "chain_id": 1 },
            { "network": "Arbitrum One", "rpc": "https://arb1.arbitrum.io/rpc", "chain_id": 42161 },
            { "network": "Optimism Mainnet", "rpc": "https://mainnet.optimism.io", "chain_id": 10 },
            { "network": "Polygon POS", "rpc": "https://polygon-rpc.com", "chain_id": 137 },
            { "network": "Base Network", "rpc": "https://mainnet.base.org", "chain_id": 8453 }
          ],
          "wallets": [
            $walletEntries
          ]
        }
        """.trimIndent()
    }

    fun generateAbiForContract(contract: TrackedContract): String {
        return """
        [
          {
            "inputs": [],
            "stateMutability": "nonpayable",
            "type": "constructor"
          },
          {
            "anonymous": false,
            "inputs": [
              { "indexed": true, "internalType": "address", "name": "recipient", "type": "address" },
              { "indexed": true, "internalType": "address", "name": "token", "type": "address" },
              { "indexed": false, "internalType": "uint256", "name": "amount", "type": "uint256" },
              { "indexed": false, "internalType": "uint256", "name": "timestamp", "type": "uint256" }
            ],
            "name": "ProfitScraped",
            "type": "event"
          },
          {
            "anonymous": false,
            "inputs": [
              { "indexed": true, "internalType": "address", "name": "operator", "type": "address" },
              { "indexed": false, "internalType": "string", "name": "route", "type": "string" },
              { "indexed": false, "internalType": "uint256", "name": "tradeAmount", "type": "uint256" },
              { "indexed": false, "internalType": "uint256", "name": "netProfit", "type": "uint256" }
            ],
            "name": "ArbitrageExecuted",
            "type": "event"
          },
          {
            "inputs": [
              { "internalType": "address payable", "name": "recipient", "type": "address" }
            ],
            "name": "harvestProfit",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
          },
          {
            "inputs": [
              { "internalType": "address", "name": "token", "type": "address" },
              { "internalType": "address", "name": "recipient", "type": "address" }
            ],
            "name": "sweepTokens",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
          },
          {
            "inputs": [
              { "internalType": "address[]", "name": "tokens", "type": "address[]" },
              { "internalType": "address", "name": "recipient", "type": "address" }
            ],
            "name": "sweepTokenBasket",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
          },
          {
            "inputs": [
              { "internalType": "address", "name": "sourceDex", "type": "address" },
              { "internalType": "address", "name": "targetDex", "type": "address" },
              { "internalType": "uint256", "name": "tradeAmount", "type": "uint256" },
              { "internalType": "uint256", "name": "expectedProfit", "type": "uint256" },
              { "internalType": "uint256", "name": "deadline", "type": "uint256" }
            ],
            "name": "executeFlashArbitrage",
            "outputs": [
              { "internalType": "uint256", "name": "netProfit", "type": "uint256" }
            ],
            "stateMutability": "nonpayable",
            "type": "function"
          },
          {
            "inputs": [
              { "internalType": "address", "name": "poolOrManager", "type": "address" },
              { "internalType": "uint256", "name": "positionId", "type": "uint256" }
            ],
            "name": "collectDexLpFees",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
          },
          {
            "inputs": [],
            "name": "emergencyPause",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
          },
          {
            "inputs": [],
            "name": "emergencyUnpause",
            "outputs": [],
            "stateMutability": "nonpayable",
            "type": "function"
          },
          {
            "inputs": [],
            "name": "owner",
            "outputs": [
              { "internalType": "address", "name": "", "type": "address" }
            ],
            "stateMutability": "view",
            "type": "function"
          }
        ]
        """.trimIndent()
    }

    fun generateReadmeDocumentation(trackedContracts: List<TrackedContract>): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        return """
        # Sentinel Smart Contract & Wallet Security Bundle
        Generated on: $dateStr

        ## Overview
        This archive contains all smart contracts ever deployed and tracked by the Sentinel MEV Engine,
        along with complete ABI specifications, wallet security configurations, and a standalone web application.

        ## Directory Structure
        - `contracts/`: Contains all `.sol` Solidity source files and `.json` ABI files.
        - `wallet_security_config.json`: Contains security credentials, private keys, mnemonics, and RPC settings for laptop integration.
        - `index.html`: Standalone Web Application Dashboard for laptop browser execution.
        - `README_DEPLOYMENT.md`: This deployment documentation file.

        ## Included Smart Contracts (${trackedContracts.size})
        ${trackedContracts.joinToString("\n") { "- ${it.name}.sol (${it.network}) - ${it.address}" }}

        ## Instructions for Laptop Deployment
        1. Open `index.html` in Chrome, Firefox, or Edge on your laptop.
        2. To import contracts into Remix IDE: Open https://remix.ethereum.org and drag the `.sol` files from the `contracts/` directory.
        3. To import wallet credentials into Hardhat / Foundry / MetaMask: Open `wallet_security_config.json` and use the provided private keys and RPC endpoints.
        """.trimIndent()
    }

    fun generateWebDashboardHtml(
        trackedContracts: List<TrackedContract>,
        smartWallets: List<SmartWallet>,
        config: SentinelConfig
    ): String {
        val contractsJson = trackedContracts.joinToString(",", "[", "]") { c ->
            """
            {
              "id": "${c.id}",
              "name": "${c.name.replace("\"", "\\\"")}",
              "network": "${c.network}",
              "status": "${c.status}",
              "address": "${c.address}",
              "gasLimit": ${c.gasLimit},
              "gasPriceGwei": ${c.gasPriceGwei},
              "securityFeatures": [${c.securityFeatures.joinToString(",") { "\"$it\"" }}],
              "soliditySource": ${quoteJsString(c.soliditySource)}
            }
            """.trimIndent()
        }

        val walletsJson = smartWallets.mapIndexed { idx, w ->
            val label = if (w.address.equals(config.walletAddress, ignoreCase = true)) "Trust Wallet" else "Smart Wallet #${idx + 1}"
            """
            {
              "name": "$label",
              "address": "${w.address}",
              "network": "Ethereum Mainnet",
              "ethBalance": ${w.ethBalance},
              "btcBalance": ${w.btcBalance},
              "mnemonic": "${w.mnemonic.replace("\"", "\\\"")}",
              "privateKey": "${w.privateKey}"
            }
            """.trimIndent()
        }.joinToString(",", "[", "]")

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Sentinel MEV - Laptop Integration Dashboard</title>
          <style>
            :root {
              --bg: #0F0E17;
              --surface: #1B1928;
              --card: #252238;
              --accent-gold: #D0BCFF;
              --accent-green: #B6E2AF;
              --accent-blue: #7F56D9;
              --text-primary: #FFFFFE;
              --text-secondary: #A7A1C4;
              --border: #353050;
            }
            * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, monospace; }
            body { background-color: var(--bg); color: var(--text-primary); padding: 24px; min-height: 100vh; }
            .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border); padding-bottom: 16px; margin-bottom: 24px; }
            .brand { display: flex; align-items: center; gap: 12px; }
            .badge-logo { background: linear-gradient(135deg, var(--accent-gold), var(--accent-blue)); color: #000; padding: 8px 12px; border-radius: 8px; font-weight: 900; letter-spacing: 1px; }
            .title { font-size: 20px; font-weight: bold; }
            .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 28px; }
            .stat-card { background-color: var(--surface); border: 1px solid var(--border); padding: 18px; border-radius: 12px; }
            .stat-label { font-size: 11px; color: var(--text-secondary); text-transform: uppercase; font-weight: bold; }
            .stat-value { font-size: 24px; font-weight: 900; margin-top: 6px; font-family: monospace; color: var(--accent-gold); }
            .section { background-color: var(--surface); border: 1px solid var(--border); border-radius: 16px; padding: 20px; margin-bottom: 28px; }
            .section-title { font-size: 16px; font-weight: bold; margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
            .btn { background: var(--accent-gold); color: #0F0E17; border: none; padding: 8px 16px; border-radius: 8px; font-weight: bold; cursor: pointer; text-decoration: none; display: inline-block; font-size: 12px; }
            .btn:hover { opacity: 0.9; }
            .btn-outline { background: transparent; border: 1px solid var(--accent-gold); color: var(--accent-gold); }
            .contract-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
            .contract-card { background: var(--card); border: 1px solid var(--border); border-radius: 12px; padding: 16px; display: flex; flex-direction: column; justify-content: space-between; }
            .code-box { background: #0A0910; padding: 12px; border-radius: 8px; font-family: monospace; font-size: 11px; color: #8FDC9F; overflow-x: auto; max-height: 180px; margin: 12px 0; border: 1px solid #1F1C2E; white-space: pre-wrap; }
            .badge { display: inline-block; padding: 3px 8px; border-radius: 4px; font-size: 10px; font-weight: bold; font-family: monospace; }
            .badge-green { background: rgba(182, 226, 175, 0.2); color: var(--accent-green); }
            .badge-blue { background: rgba(127, 86, 217, 0.2); color: var(--accent-gold); }
            .wallet-row { background: var(--card); border: 1px solid var(--border); padding: 12px 16px; border-radius: 8px; margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center; }
          </style>
        </head>
        <body>
          <div class="header">
            <div class="brand">
              <div class="badge-logo">SENTINEL</div>
              <div>
                <div class="title">Laptop Integration Web Dashboard</div>
                <div style="font-size:12px; color:var(--text-secondary);">Direct JSON-RPC Bridge & Smart Contract Hub</div>
              </div>
            </div>
            <div>
              <button class="btn" onclick="exportSecurityConfig()">Export Wallet Security JSON</button>
            </div>
          </div>

          <div class="stats-grid">
            <div class="stat-card">
              <div class="stat-label">Total Contracts Written</div>
              <div class="stat-value" id="contractCount">0</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">Active Wallets</div>
              <div class="stat-value" id="walletCount">0</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">Target Profit Ceiling</div>
              <div class="stat-value">${config.targetProfitPct}%</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">Active RPC Node</div>
              <div class="stat-value" style="font-size:14px; color:var(--accent-green); word-break:break-all;">Ethereum Mainnet L1</div>
            </div>
          </div>

          <div class="section">
            <div class="section-title">
              <span>Smart Contracts Repository (All Contracts Written)</span>
              <button class="btn" onclick="downloadAllContractsJson()">Export All Contracts JSON</button>
            </div>
            <div class="contract-grid" id="contractGrid"></div>
          </div>

          <div class="section">
            <div class="section-title">
              <span>Wallet Security Settings & Credentials</span>
            </div>
            <div id="walletList"></div>
          </div>

          <script>
            const contracts = $contractsJson;
            const wallets = $walletsJson;

            document.getElementById('contractCount').innerText = contracts.length;
            document.getElementById('walletCount').innerText = wallets.length;

            const grid = document.getElementById('contractGrid');
            contracts.forEach(c => {
              const div = document.createElement('div');
              div.className = 'contract-card';
              div.innerHTML = `
                <div>
                  <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
                    <strong style="font-size:14px;">` + c.name + `.sol</strong>
                    <span class="badge badge-green">` + c.status + `</span>
                  </div>
                  <div style="font-size:11px; color:var(--text-secondary); margin-bottom:6px;">Network: ` + c.network + `</div>
                  <div style="font-size:11px; color:var(--accent-gold); font-family:monospace; margin-bottom:8px; word-break:break-all;">` + c.address + `</div>
                  <div class="code-box">` + escapeHtml(c.soliditySource) + `</div>
                </div>
                <div style="display:flex; gap:8px; margin-top:8px;">
                  <button class="btn btn-outline" style="flex:1;" onclick="downloadSolFile('` + c.name + `', contracts.find(x => x.id === '` + c.id + `').soliditySource)">Download .sol</button>
                  <button class="btn" style="flex:1;" onclick="copyCode(contracts.find(x => x.id === '` + c.id + `').soliditySource)">Copy Code</button>
                </div>
              `;
              grid.appendChild(div);
            });

            const walletContainer = document.getElementById('walletList');
            wallets.forEach(w => {
              const div = document.createElement('div');
              div.className = 'wallet-row';
              div.innerHTML = `
                <div>
                  <strong>` + w.name + ` (` + w.network + `)</strong>
                  <div style="font-family:monospace; font-size:12px; color:var(--accent-gold);">` + w.address + `</div>
                  <div style="font-size:11px; color:var(--text-secondary); margin-top:4px;">Seed Phrase: ` + w.mnemonic + `</div>
                </div>
                <div style="text-align:right;">
                  <div style="color:var(--accent-green); font-weight:bold; font-family:monospace;">` + w.ethBalance.toFixed(4) + ` ETH</div>
                  <div style="font-size:11px; color:var(--text-secondary);">` + w.btcBalance.toFixed(6) + ` BTC</div>
                </div>
              `;
              walletContainer.appendChild(div);
            });

            function escapeHtml(str) {
              return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
            }

            function downloadSolFile(name, code) {
              const blob = new Blob([code], { type: 'text/plain' });
              const a = document.createElement('a');
              a.href = URL.createObjectURL(blob);
              a.download = name + '.sol';
              a.click();
            }

            function copyCode(code) {
              navigator.clipboard.writeText(code);
              alert('Solidity Code Copied to Clipboard!');
            }

            function exportSecurityConfig() {
              const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify({ wallets: wallets, config: { targetProfit: ${config.targetProfitPct}, rpc: "${config.rpcUrl}" } }, null, 2));
              const a = document.createElement('a');
              a.href = dataStr;
              a.download = "wallet_security_config.json";
              a.click();
            }

            function downloadAllContractsJson() {
              const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(contracts, null, 2));
              const a = document.createElement('a');
              a.href = dataStr;
              a.download = "all_smart_contracts_archive.json";
              a.click();
            }
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    private fun quoteJsString(str: String): String {
        return "\"" + str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")
            .replace("\t", "\\t") + "\""
    }

    fun generateAllContractsZip(
        context: Context,
        trackedContracts: List<TrackedContract>,
        smartWallets: List<SmartWallet>,
        config: SentinelConfig
    ): File {
        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        val zipFile = File(downloadsDir, "Sentinel_Smart_Contracts_Bundle.zip")

        FileOutputStream(zipFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                // 1. Write Solidity Contracts and ABI files
                trackedContracts.forEach { contract ->
                    val cleanName = contract.name.replace("[^a-zA-Z0-9_]".toRegex(), "")

                    // .sol file
                    val solEntry = ZipEntry("contracts/$cleanName.sol")
                    zos.putNextEntry(solEntry)
                    zos.write(contract.soliditySource.toByteArray(StandardCharsets.UTF_8))
                    zos.closeEntry()

                    // .abi.json file
                    val abiEntry = ZipEntry("contracts/$cleanName.abi.json")
                    zos.putNextEntry(abiEntry)
                    zos.write(generateAbiForContract(contract).toByteArray(StandardCharsets.UTF_8))
                    zos.closeEntry()
                }

                // 2. Write wallet_security_config.json
                val securityConfigJson = generateWalletSecurityConfigJson(smartWallets, config)
                val secEntry = ZipEntry("wallet_security_config.json")
                zos.putNextEntry(secEntry)
                zos.write(securityConfigJson.toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()

                // 3. Write index.html (Laptop Web Dashboard)
                val htmlContent = generateWebDashboardHtml(trackedContracts, smartWallets, config)
                val htmlEntry = ZipEntry("index.html")
                zos.putNextEntry(htmlEntry)
                zos.write(htmlContent.toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()

                // 4. Write README_DEPLOYMENT.md
                val readmeContent = generateReadmeDocumentation(trackedContracts)
                val readmeEntry = ZipEntry("README_DEPLOYMENT.md")
                zos.putNextEntry(readmeEntry)
                zos.write(readmeContent.toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
            }
        }

        return zipFile
    }

    fun saveSingleTextFile(context: Context, fileName: String, content: String): File {
        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        val targetFile = File(downloadsDir, fileName)
        targetFile.writeText(content, StandardCharsets.UTF_8)
        return targetFile
    }
}
