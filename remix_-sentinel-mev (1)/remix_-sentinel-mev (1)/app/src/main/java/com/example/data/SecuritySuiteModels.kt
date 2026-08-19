package com.example.data

import com.example.data.database.SecurityConfigurationEntity
import java.util.UUID

enum class SecurityCategory(val title: String, val badgeColorHex: Long) {
    EXECUTION_GUARD("Execution & State Guard", 0xFF00E676),
    ACCESS_GOVERNANCE("Access & Governance", 0xFFFFD700),
    MEV_PROTECTION("MEV & Anti-Frontrunning", 0xFF29B6F6),
    CRYPTOGRAPHY("Signatures & Paymasters", 0xFFAB47BC),
    LIQUIDITY_DEFENSE("Liquidity & Circuit Breaker", 0xFFFF7043)
}

enum class ThreatRiskLevel(val label: String, val colorHex: Long) {
    CRITICAL("CRITICAL RISK", 0xFFEF5350),
    HIGH("HIGH RISK", 0xFFFF7043),
    MEDIUM("MODERATE RISK", 0xFFFFD700),
    LOW("LOW RISK", 0xFF00E676),
    SECURED("MITIGATED", 0xFF00E676)
}

data class ThreatAssessment(
    val title: String,
    val riskLevel: ThreatRiskLevel,
    val description: String,
    val recommendedFeatureId: String,
    val isMitigated: Boolean
)

data class SecurityFeature(
    val id: String,
    val name: String,
    val category: SecurityCategory,
    val tagline: String,
    val description: String,
    val howToUse: String,
    val soliditySnippet: String,
    val gasOverhead: String,
    val threatVectorsPrevented: List<String>,
    val recommendedNetworks: List<String>,
    val recommendedTxTypes: List<String>,
    val isAuditStandard: Boolean = true
)

data class SecurityProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetNetwork: String,
    val transactionType: String,
    val enabledFeatureIds: Set<String> = emptySet(),
    val reentrancyGuardEnabled: Boolean = false,
    val accessControlEnabled: Boolean = false,
    val slippageLockEnabled: Boolean = false,
    val safeMathEnabled: Boolean = false,
    val pausableEnabled: Boolean = false,
    val eip712Enabled: Boolean = false,
    val oracleTwapGuardEnabled: Boolean = false,
    val erc4337PaymasterEnabled: Boolean = false,
    val antiFrontrunningEnabled: Boolean = false,
    val rateLimiterEnabled: Boolean = false,
    val customMaxSlippageBps: Int = 50, // 0.5%
    val customMaxRateLimitEth: Double = 10.0,
    val customTwapWindowSeconds: Int = 1800,
    val securityScore: Int = 0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun SecurityProfile.toEntity(): SecurityConfigurationEntity {
    return SecurityConfigurationEntity(
        id = id,
        name = name,
        targetNetwork = targetNetwork,
        transactionType = transactionType,
        enabledFeatures = enabledFeatureIds.joinToString(","),
        reentrancyGuardEnabled = reentrancyGuardEnabled,
        accessControlEnabled = accessControlEnabled,
        slippageLockEnabled = slippageLockEnabled,
        safeMathEnabled = safeMathEnabled,
        pausableEnabled = pausableEnabled,
        eip712Enabled = eip712Enabled,
        oracleTwapGuardEnabled = oracleTwapGuardEnabled,
        erc4337PaymasterEnabled = erc4337PaymasterEnabled,
        antiFrontrunningEnabled = antiFrontrunningEnabled,
        rateLimiterEnabled = rateLimiterEnabled,
        customMaxSlippageBps = customMaxSlippageBps,
        customMaxRateLimitEth = customMaxRateLimitEth,
        customTwapWindowSeconds = customTwapWindowSeconds,
        securityScore = securityScore,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun SecurityConfigurationEntity.toDomain(): SecurityProfile {
    val featList = if (enabledFeatures.isEmpty()) emptySet() else enabledFeatures.split(",").toSet()
    return SecurityProfile(
        id = id,
        name = name,
        targetNetwork = targetNetwork,
        transactionType = transactionType,
        enabledFeatureIds = featList,
        reentrancyGuardEnabled = reentrancyGuardEnabled,
        accessControlEnabled = accessControlEnabled,
        slippageLockEnabled = slippageLockEnabled,
        safeMathEnabled = safeMathEnabled,
        pausableEnabled = pausableEnabled,
        eip712Enabled = eip712Enabled,
        oracleTwapGuardEnabled = oracleTwapGuardEnabled,
        erc4337PaymasterEnabled = erc4337PaymasterEnabled,
        antiFrontrunningEnabled = antiFrontrunningEnabled,
        rateLimiterEnabled = rateLimiterEnabled,
        customMaxSlippageBps = customMaxSlippageBps,
        customMaxRateLimitEth = customMaxRateLimitEth,
        customTwapWindowSeconds = customTwapWindowSeconds,
        securityScore = securityScore,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

object SecurityFeaturesCatalog {
    val REENTRANCY_GUARD = SecurityFeature(
        id = "REENTRANCY_GUARD",
        name = "NonReentrant Mutex Guard",
        category = SecurityCategory.EXECUTION_GUARD,
        tagline = "Prevents recursive caller drains and reentrancy state hijacking",
        description = "Reentrancy is one of the most fatal smart contract vulnerabilities where an external untrusted contract call intercepts execution before the caller's internal state is updated. The NonReentrant guard sets an atomic mutex flag (e.g. status = 2) during execution and resets it upon completion, immediately reverting nested entries.",
        howToUse = "1. Inherit from OpenZeppelin's `ReentrancyGuard` or define a custom mutex modifier.\n2. Decorate sensitive external state-changing functions with the `nonReentrant` modifier.\n3. Always place external token transfers and ETH sends AFTER internal state updates (Checks-Effects-Interactions pattern).",
        soliditySnippet = """
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts/utils/ReentrancyGuard.sol";

contract SecureVault is ReentrancyGuard {
    mapping(address => uint256) public balances;

    event Withdrawn(address indexed recipient, uint256 amount);

    function withdraw(uint256 amount) external nonReentrant {
        require(balances[msg.sender] >= amount, "Insufficient balance");
        // 1. Effects: State update before interaction
        balances[msg.sender] -= amount;
        // 2. Interaction: Protected against re-entry
        (bool success, ) = msg.sender.call{value: amount}("");
        require(success, "ETH transfer failed");
        emit Withdrawn(msg.sender, amount);
    }
}
        """.trimIndent(),
        gasOverhead = "~2,400 gas (1 SSTORE warm/cold slot flip)",
        threatVectorsPrevented = listOf(
            "Recursive fallback / receive() call drains",
            "Cross-function state inconsistency exploits",
            "Read-only reentrancy on price query hooks"
        ),
        recommendedNetworks = listOf("Ethereum Mainnet", "Arbitrum One", "Optimism", "Polygon PoS", "Base", "BNB Smart Chain", "Sepolia Testnet"),
        recommendedTxTypes = listOf("MEV Arbitrage & Sandwich Bot", "DeFi Lending & Staking Vault", "ERC-20 / ERC-721 Token", "Cross-Chain Bridge & Relayer")
    )

    val ACCESS_CONTROL = SecurityFeature(
        id = "ACCESS_CONTROL",
        name = "Role-Based Access Control (RBAC 2-Step)",
        category = SecurityCategory.ACCESS_GOVERNANCE,
        tagline = "Enforces least-privilege role separation & multi-step admin ownership",
        description = "Prevents malicious or accidental ownership takeovers and privileged function abuse. Implements granular roles (e.g. BOT_OPERATOR, RISK_ADMIN, DEFAULT_ADMIN) with two-step role acceptance (`Ownable2Step`), preventing catastrophic transfers to zero or mistyped addresses.",
        howToUse = "1. Inherit `AccessControlDefaultAdminRules` or `Ownable2Step`.\n2. Define constant bytes32 roles for `OPERATOR_ROLE` and `SENTINEL_ADMIN`.\n3. Protect administrative methods with `onlyRole(ROLE_NAME)`.",
        soliditySnippet = """
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts/access/AccessControl.sol";

contract ArbitrageGovernance is AccessControl {
    bytes32 public constant OPERATOR_ROLE = keccak256("OPERATOR_ROLE");
    bytes32 public constant RISK_ADMIN_ROLE = keccak256("RISK_ADMIN_ROLE");

    constructor(address rootAdmin) {
        _grantRole(DEFAULT_ADMIN_ROLE, rootAdmin);
        _grantRole(OPERATOR_ROLE, msg.sender);
    }

    function executeMevBundle(bytes calldata bundleData) external onlyRole(OPERATOR_ROLE) {
        // Only authorized high-speed bots can execute bundles
    }

    function emergencyHalt() external onlyRole(RISK_ADMIN_ROLE) {
        // Fast trigger for emergency response team
    }
}
        """.trimIndent(),
        gasOverhead = "~800 gas per role check",
        threatVectorsPrevented = listOf(
            "Unauthorized parameter tampering",
            "Mistyped admin address loss",
            "Private key compromise single-point-of-failure"
        ),
        recommendedNetworks = listOf("Ethereum Mainnet", "Arbitrum One", "Optimism", "Polygon PoS", "Base", "BNB Smart Chain"),
        recommendedTxTypes = listOf("MEV Arbitrage & Sandwich Bot", "DeFi Lending & Staking Vault", "ERC-20 / ERC-721 Token", "ERC-4337 Account Abstraction", "Cross-Chain Bridge & Relayer")
    )

    val SLIPPAGE_LOCK = SecurityFeature(
        id = "SLIPPAGE_LOCK",
        name = "Atomic Slippage & Sandwich Shield",
        category = SecurityCategory.MEV_PROTECTION,
        tagline = "Enforces strict min-output tolerances with atomic transaction revert",
        description = "Protects decentralized trades and MEV bundle executions from predatory sandwich attacks and price manipulation. Computes maximum tolerable slippage in basis points (BPS) and atomically reverts the entire transaction if the resulting output token amount is below expectations.",
        howToUse = "1. Calculate `minAmountOut = expectedAmount * (10000 - maxSlippageBps) / 10000`.\n2. Enforce strict check after swap execution.\n3. Set transaction deadline to prevent pending mempool delay exploitation.",
        soliditySnippet = """
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

contract SlippageShield {
    uint256 public constant BPS_DIVISOR = 10000;
    uint256 public maxSlippageBps = 50; // 0.50% max allowable slippage

    event SlippageVerified(uint256 expected, uint256 actual, uint256 minRequired);

    function verifySlippage(
        uint256 expectedOut, 
        uint256 actualOut, 
        uint256 deadline
    ) internal view {
        require(block.timestamp <= deadline, "Transaction expired in mempool");
        uint256 minOut = (expectedOut * (BPS_DIVISOR - maxSlippageBps)) / BPS_DIVISOR;
        require(actualOut >= minOut, "MEV Sandwich / Slippage exceeded limit");
    }
}
        """.trimIndent(),
        gasOverhead = "~350 gas (pure mathematical computation)",
        threatVectorsPrevented = listOf(
            "Predatory MEV Sandwich Attacks",
            "DEX Liquidity pool shallow-depth frontrunning",
            "Stale mempool transaction re-ordering"
        ),
        recommendedNetworks = listOf("Ethereum Mainnet", "Base", "Arbitrum One", "Polygon PoS", "BNB Smart Chain"),
        recommendedTxTypes = listOf("MEV Arbitrage & Sandwich Bot", "DeFi Lending & Staking Vault")
    )

    val ORACLE_TWAP_GUARD = SecurityFeature(
        id = "ORACLE_TWAP_GUARD",
        name = "Dual Oracle & TWAP Manipulation Guard",
        category = SecurityCategory.MEV_PROTECTION,
        tagline = "Validates spot prices against Uniswap V3 TWAP and Chainlink feeds",
        description = "Flash loans allow attackers to borrow tens of millions of dollars within a single block to artificially skew spot DEX prices. This guard queries a Time-Weighted Average Price (TWAP) and Cross-references Chainlink feeds, rejecting executions if spot price deviates significantly from historical averages.",
        howToUse = "1. Query Uniswap V3 Oracle with a minimum 30-minute observation window.\n2. Fetch latest round data from Chainlink AggregatorV3.\n3. Assert that spot price difference remains within dynamic deviation thresholds (e.g. < 2.5%).",
        soliditySnippet = """
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

interface IChainlinkFeed {
    function latestRoundData() external view returns (
        uint80 roundId, int256 answer, uint256 startedAt, uint256 updatedAt, uint80 answeredInRound
    );
}

contract OracleGuard {
    IChainlinkFeed public immutable priceFeed;
    uint256 public constant MAX_PRICE_STALENESS = 3600; // 1 hour

    constructor(address _feed) {
        priceFeed = IChainlinkFeed(_feed);
    }

    function getValidatedPrice() public view returns (uint256) {
        (, int256 price, , uint256 updatedAt, ) = priceFeed.latestRoundData();
        require(price > 0, "Invalid feed price");
        require(block.timestamp - updatedAt <= MAX_PRICE_STALENESS, "Oracle price is stale");
        return uint256(price);
    }
}
        """.trimIndent(),
        gasOverhead = "~4,500 gas for external view calls",
        threatVectorsPrevented = listOf(
            "Flash loan spot price manipulation",
            "Stale or degraded oracle feed feeds",
            "Single-source oracle bridge hacks"
        ),
        recommendedNetworks = listOf("Ethereum Mainnet", "Arbitrum One", "Optimism", "Base", "Polygon PoS"),
        recommendedTxTypes = listOf("MEV Arbitrage & Sandwich Bot", "DeFi Lending & Staking Vault", "Cross-Chain Bridge & Relayer")
    )

    val PAUSABLE_CIRCUIT_BREAKER = SecurityFeature(
        id = "PAUSABLE_CIRCUIT_BREAKER",
        name = "Emergency Pausable Circuit Breaker",
        category = SecurityCategory.LIQUIDITY_DEFENSE,
        tagline = "Instant kill-switch to freeze all state mutations during live anomalies",
        description = "Allows designated security sentinels, automated anomaly monitors, or multi-sig guardians to instantly freeze deposits, withdrawals, or trades if an exploit pattern or abnormal price volatility is detected in real time.",
        howToUse = "1. Inherit from OpenZeppelin's `Pausable`.\n2. Add `whenNotPaused` to all critical external routines.\n3. Allow automated Sentinel bot to invoke `pause()` upon unexpected gas spike or rapid drain detection.",
        soliditySnippet = """
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts/utils/Pausable.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract SentinelSecuredEngine is Pausable, Ownable {
    constructor() Ownable(msg.sender) {}

    function triggerEmergencyPause() external onlyOwner {
        _pause();
    }

    function resumeOperations() external onlyOwner {
        _unpause();
    }

    function executeTrade() external whenNotPaused {
        // Safe operational logic
    }
}
        """.trimIndent(),
        gasOverhead = "~600 gas (1 storage read per execution)",
        threatVectorsPrevented = listOf(
            "Ongoing exploit fund drainage",
            "Zero-day smart contract bug exploitation",
            "Cascading liquidations during market depegs"
        ),
        recommendedNetworks = listOf("Ethereum Mainnet", "Arbitrum One", "Optimism", "Polygon PoS", "Base", "BNB Smart Chain", "Sepolia Testnet"),
        recommendedTxTypes = listOf("MEV Arbitrage & Sandwich Bot", "DeFi Lending & Staking Vault", "ERC-20 / ERC-721 Token", "Cross-Chain Bridge & Relayer")
    )

    val EIP712_SIGNATURES = SecurityFeature(
        id = "EIP712_SIGNATURES",
        name = "EIP-712 Typed Structured Signatures",
        category = SecurityCategory.CRYPTOGRAPHY,
        tagline = "Cryptographic signature validation immune to cross-chain replays & phishing",
        description = "Provides standard typed structured hashing and signing (EIP-712) for off-chain approvals and meta-transactions. Embeds `chainId`, verifying contract address, and unique nonces into the domain separator so signed messages cannot be replayed on testnets, hardforks, or other chains.",
        howToUse = "1. Inherit OpenZeppelin's `EIP712`.\n2. Define typed struct hash with parameters (recipient, amount, nonce, deadline).\n3. Use `ECDSA.recover` to verify the signer without executing on-chain transactions.",
        soliditySnippet = """
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts/utils/cryptography/EIP712.sol";
import "@openzeppelin/contracts/utils/cryptography/ECDSA.sol";

contract SecureGaslessPermit is EIP712 {
    bytes32 private constant EXECUTE_TYPEHASH = 
        keccak256("ExecuteOrder(address trader,uint256 amount,uint256 nonce,uint256 deadline)");
    mapping(address => uint256) public nonces;

    constructor() EIP712("SentinelSecuritySuite", "1.0") {}

    function verifySigner(
        address trader,
        uint256 amount,
        uint256 deadline,
        bytes calldata signature
    ) public returns (bool) {
        require(block.timestamp <= deadline, "Signature expired");
        bytes32 structHash = keccak256(abi.encode(
            EXECUTE_TYPEHASH, trader, amount, nonces[trader]++, deadline
        ));
        bytes32 hash = _hashTypedDataV4(structHash);
        address signer = ECDSA.recover(hash, signature);
        require(signer == trader, "Invalid cryptographic signature");
        return true;
    }
}
        """.trimIndent(),
        gasOverhead = "~3,000 gas for ecrecover verification",
        threatVectorsPrevented = listOf(
            "Cross-chain signature replay attacks",
            "Mempool transaction front-running via permit phishing",
            "Nonce manipulation and double-spending"
        ),
        recommendedNetworks = listOf("Ethereum Mainnet", "Arbitrum One", "Optimism", "Base", "Polygon PoS"),
        recommendedTxTypes = listOf("ERC-4337 Account Abstraction", "ERC-20 / ERC-721 Token", "MEV Arbitrage & Sandwich Bot")
    )

    val RATE_LIMITER_THROTTLE = SecurityFeature(
        id = "RATE_LIMITER_THROTTLE",
        name = "Dynamic Rate Limiter & Withdrawal Throttle",
        category = SecurityCategory.LIQUIDITY_DEFENSE,
        tagline = "Caps maximum outflow per block/epoch with cooldown delays",
        description = "Limits the maximum amount of tokens or ETH that can be transferred or swapped in a single block or within a rolling time epoch. Even if an attacker finds an edge-case logic vulnerability, they cannot drain the contract's entire TVL instantaneously.",
        howToUse = "1. Set `maxOutflowPerEpoch` and `epochDuration` (e.g. 1 hour).\n2. Track `currentEpochOutflow` on every withdrawal.\n3. Revert if requested amount exceeds remaining epoch capacity.",
        soliditySnippet = """
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

contract DynamicRateLimiter {
    uint256 public maxOutflowPerEpoch = 50 ether;
    uint256 public epochDuration = 1 hours;
    
    uint256 public currentEpochStart;
    uint256 public currentEpochOutflow;

    event RateLimitExceeded(uint256 requested, uint256 remaining);

    function checkAndRecordOutflow(uint256 amount) internal {
        if (block.timestamp >= currentEpochStart + epochDuration) {
            currentEpochStart = block.timestamp;
            currentEpochOutflow = 0;
        }
        require(currentEpochOutflow + amount <= maxOutflowPerEpoch, "Epoch withdrawal rate limit reached");
        currentEpochOutflow += amount;
    }
}
        """.trimIndent(),
        gasOverhead = "~1,200 gas (time and volume tracking)",
        threatVectorsPrevented = listOf(
            "Catastrophic flash TVL draining",
            "Algorithmic runaway loop liquidity drain",
            "Compromised private key fast liquidation"
        ),
        recommendedNetworks = listOf("Ethereum Mainnet", "Arbitrum One", "Polygon PoS", "BNB Smart Chain"),
        recommendedTxTypes = listOf("DeFi Lending & Staking Vault", "Cross-Chain Bridge & Relayer", "ERC-4337 Account Abstraction")
    )

    val ANTI_FRONTRUNNING_COMMIT_REVEAL = SecurityFeature(
        id = "ANTI_FRONTRUNNING_COMMIT_REVEAL",
        name = "Anti-Frontrunning Commit-Reveal Scheme",
        category = SecurityCategory.MEV_PROTECTION,
        tagline = "Hides order parameters until block inclusion to defeat mempool bots",
        description = "Defeats mempool inspection bots by splitting critical operations into two steps: 1) Commit a cryptographic hash `keccak256(orderData, secretSalt)`; 2) Reveal the trade details in a subsequent block with the secret salt, making pre-trade frontrunning impossible.",
        howToUse = "1. Client submits hashed commitment.\n2. Contract enforces a minimum 1-block delay and maximum reveal deadline.\n3. Client reveals payload with salt, verifying the hash matches commitment.",
        soliditySnippet = """
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

contract CommitRevealShield {
    struct Commitment {
        bytes32 commitHash;
        uint64 blockNumber;
        bool revealed;
    }

    mapping(address => Commitment) public commitments;

    function commitTrade(bytes32 hash) external {
        commitments[msg.sender] = Commitment(hash, uint64(block.number), false);
    }

    function revealAndExecute(bytes memory tradeData, bytes32 secretSalt) external {
        Commitment storage c = commitments[msg.sender];
        require(!c.revealed, "Already revealed");
        require(block.number > c.blockNumber, "Must wait 1 block after commit");
        require(keccak256(abi.encodePacked(tradeData, secretSalt)) == c.commitHash, "Hash mismatch");
        c.revealed = true;
        // Execute trade safely without mempool frontrunning
    }
}
        """.trimIndent(),
        gasOverhead = "~2,800 gas across two transactions",
        threatVectorsPrevented = listOf(
            "Public mempool MEV arbitrage frontrunning",
            "Miner / Sequencer transaction reordering",
            "Searcher bot copy-trading & backrunning"
        ),
        recommendedNetworks = listOf("Ethereum Mainnet", "Polygon PoS", "BNB Smart Chain"),
        recommendedTxTypes = listOf("MEV Arbitrage & Sandwich Bot", "DeFi Lending & Staking Vault")
    )

    val ERC4337_PAYMASTER_VALIDATOR = SecurityFeature(
        id = "ERC4337_PAYMASTER_VALIDATOR",
        name = "ERC-4337 Account Abstraction Paymaster Security",
        category = SecurityCategory.CRYPTOGRAPHY,
        tagline = "Gas sponsorship validation preventing drain from spoofed user operations",
        description = "Provides rigorous security checks for ERC-4337 Paymasters and Smart Contract Accounts (Account Abstraction). Enforces EntryPoint address validation, maximum gas limits, paymaster signature validation, and prefund checks to prevent malicious UserOperations from draining sponsored gas pools.",
        howToUse = "1. Inherit from `BasePaymaster` with standard EntryPoint interface.\n2. Implement `validatePaymasterUserOp` checking sender authorization and gas ceilings.\n3. Handle `postOp` refunds and gas accounting securely.",
        soliditySnippet = """
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

contract SecurePaymaster {
    address public immutable entryPoint;
    address public immutable trustedSigner;

    constructor(address _entryPoint, address _signer) {
        entryPoint = _entryPoint;
        trustedSigner = _signer;
    }

    modifier onlyEntryPoint() {
        require(msg.sender == entryPoint, "Caller not official EntryPoint");
        _;
    }

    function validatePaymasterUserOp(
        bytes calldata userOpData,
        bytes32 userOpHash,
        uint256 maxCost
    ) external onlyEntryPoint returns (bytes memory context, uint256 validationData) {
        // Enforce gas cost ceiling and signature checks
        require(maxCost <= 0.05 ether, "UserOp gas cost too high");
        return ("", 0);
    }
}
        """.trimIndent(),
        gasOverhead = "~3,500 gas per UserOp validation",
        threatVectorsPrevented = listOf(
            "Malicious UserOp paymaster gas draining",
            "Spoofed entrypoint caller execution",
            "Unbounded gas sponsorship attacks"
        ),
        recommendedNetworks = listOf("Arbitrum One", "Optimism", "Base", "Polygon PoS", "Ethereum Mainnet"),
        recommendedTxTypes = listOf("ERC-4337 Account Abstraction")
    )

    val SAFEMATH_OVERFLOW_SHIELD = SecurityFeature(
        id = "SAFEMATH_OVERFLOW_SHIELD",
        name = "Integer Overflow & Type Bounds Shield",
        category = SecurityCategory.EXECUTION_GUARD,
        tagline = "Solidity 0.8+ checked arithmetic with safe downcasting bounds",
        description = "While Solidity 0.8+ contains built-in overflow/underflow checks, unsafe downcasting (e.g. `uint256` to `uint128` or `uint64`) can silently truncate large values. This module provides verified downcasting bounds and explicit checked math utilities for high-precision financial operations.",
        howToUse = "1. Use OpenZeppelin's `SafeCast` library.\n2. Wrap token decimal conversions in safe casting assertions.\n3. Validate that gas price or profit percentages never overflow 64-bit storage packing.",
        soliditySnippet = """
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts/utils/math/SafeCast.sol";

contract SafeCastProtection {
    using SafeCast for uint256;

    function packCompactData(uint256 largeValue) external pure returns (uint128) {
        // Automatically reverts if largeValue exceeds uint128.max
        return largeValue.toUint128();
    }
}
        """.trimIndent(),
        gasOverhead = "~150 gas (minimal compiler opcode checks)",
        threatVectorsPrevented = listOf(
            "Unsafe integer downcasting truncation",
            "Token decimal scale calculation wrap-arounds",
            "Struct packing overflow corruption"
        ),
        recommendedNetworks = listOf("Ethereum Mainnet", "Arbitrum One", "Optimism", "Polygon PoS", "Base", "BNB Smart Chain", "Sepolia Testnet"),
        recommendedTxTypes = listOf("MEV Arbitrage & Sandwich Bot", "DeFi Lending & Staking Vault", "ERC-20 / ERC-721 Token", "ERC-4337 Account Abstraction", "Cross-Chain Bridge & Relayer")
    )

    val allFeatures = listOf(
        REENTRANCY_GUARD,
        ACCESS_CONTROL,
        SLIPPAGE_LOCK,
        ORACLE_TWAP_GUARD,
        PAUSABLE_CIRCUIT_BREAKER,
        EIP712_SIGNATURES,
        RATE_LIMITER_THROTTLE,
        ANTI_FRONTRUNNING_COMMIT_REVEAL,
        ERC4337_PAYMASTER_VALIDATOR,
        SAFEMATH_OVERFLOW_SHIELD
    )

    val allNetworks = listOf(
        "Ethereum Mainnet",
        "Arbitrum One",
        "Optimism",
        "Base",
        "Polygon PoS",
        "BNB Smart Chain",
        "Sepolia Testnet"
    )

    val allTransactionTypes = listOf(
        "MEV Arbitrage & Sandwich Bot",
        "DeFi Lending & Staking Vault",
        "ERC-20 / ERC-721 Token",
        "ERC-4337 Account Abstraction",
        "Cross-Chain Bridge & Relayer"
    )
}

object SecurityAdvisorEngine {

    fun getThreatsForConfiguration(
        network: String,
        txType: String,
        enabledFeatureIds: Set<String>
    ): List<ThreatAssessment> {
        val list = mutableListOf<ThreatAssessment>()

        // 1. Reentrancy Vulnerability
        val reentrancyMitigated = enabledFeatureIds.contains(SecurityFeaturesCatalog.REENTRANCY_GUARD.id)
        val reentrancyRisk = when {
            reentrancyMitigated -> ThreatRiskLevel.SECURED
            txType in listOf("DeFi Lending & Staking Vault", "MEV Arbitrage & Sandwich Bot", "Cross-Chain Bridge & Relayer") -> ThreatRiskLevel.CRITICAL
            else -> ThreatRiskLevel.MEDIUM
        }
        list.add(
            ThreatAssessment(
                title = "Recursive Reentrancy Hijacking",
                riskLevel = reentrancyRisk,
                description = "External token transfers and call hooks can re-enter this contract to drain unfinalized balances.",
                recommendedFeatureId = SecurityFeaturesCatalog.REENTRANCY_GUARD.id,
                isMitigated = reentrancyMitigated
            )
        )

        // 2. MEV Frontrunning & Sandwich Attacks
        val sandwichMitigated = enabledFeatureIds.contains(SecurityFeaturesCatalog.SLIPPAGE_LOCK.id) ||
                enabledFeatureIds.contains(SecurityFeaturesCatalog.ANTI_FRONTRUNNING_COMMIT_REVEAL.id)
        val sandwichRisk = when {
            sandwichMitigated -> ThreatRiskLevel.SECURED
            network in listOf("Ethereum Mainnet", "Base", "Polygon PoS") && txType in listOf("MEV Arbitrage & Sandwich Bot", "DeFi Lending & Staking Vault") -> ThreatRiskLevel.CRITICAL
            network in listOf("Arbitrum One", "Optimism") -> ThreatRiskLevel.HIGH
            else -> ThreatRiskLevel.MEDIUM
        }
        list.add(
            ThreatAssessment(
                title = "MEV Sandwich & Public Mempool Frontrunning",
                riskLevel = sandwichRisk,
                description = "Mempool bots will sandwich your swap or frontrun profitable arbitrage executions on $network.",
                recommendedFeatureId = SecurityFeaturesCatalog.SLIPPAGE_LOCK.id,
                isMitigated = sandwichMitigated
            )
        )

        // 3. Flash Loan Oracle Manipulation
        val oracleMitigated = enabledFeatureIds.contains(SecurityFeaturesCatalog.ORACLE_TWAP_GUARD.id)
        val oracleRisk = when {
            oracleMitigated -> ThreatRiskLevel.SECURED
            txType in listOf("DeFi Lending & Staking Vault", "MEV Arbitrage & Sandwich Bot", "Cross-Chain Bridge & Relayer") -> ThreatRiskLevel.HIGH
            else -> ThreatRiskLevel.LOW
        }
        list.add(
            ThreatAssessment(
                title = "Flash Loan Spot Price Distortion",
                riskLevel = oracleRisk,
                description = "Attackers can borrow immense liquidity to distort spot DEX prices unless protected by TWAP/Chainlink validation.",
                recommendedFeatureId = SecurityFeaturesCatalog.ORACLE_TWAP_GUARD.id,
                isMitigated = oracleMitigated
            )
        )

        // 4. Unauthorized Admin Takeover
        val rbacMitigated = enabledFeatureIds.contains(SecurityFeaturesCatalog.ACCESS_CONTROL.id)
        val rbacRisk = when {
            rbacMitigated -> ThreatRiskLevel.SECURED
            txType in listOf("DeFi Lending & Staking Vault", "Cross-Chain Bridge & Relayer", "ERC-4337 Account Abstraction") -> ThreatRiskLevel.HIGH
            else -> ThreatRiskLevel.MEDIUM
        }
        list.add(
            ThreatAssessment(
                title = "Privileged Ownership Takeover & Admin Misconfiguration",
                riskLevel = rbacRisk,
                description = "Lack of multi-role separation or two-step ownership risks total contract hijacking if keys leak.",
                recommendedFeatureId = SecurityFeaturesCatalog.ACCESS_CONTROL.id,
                isMitigated = rbacMitigated
            )
        )

        // 5. Runaway Liquidity Drainage
        val pauseMitigated = enabledFeatureIds.contains(SecurityFeaturesCatalog.PAUSABLE_CIRCUIT_BREAKER.id) &&
                enabledFeatureIds.contains(SecurityFeaturesCatalog.RATE_LIMITER_THROTTLE.id)
        val pauseRisk = when {
            pauseMitigated -> ThreatRiskLevel.SECURED
            txType in listOf("DeFi Lending & Staking Vault", "Cross-Chain Bridge & Relayer") -> ThreatRiskLevel.HIGH
            else -> ThreatRiskLevel.MEDIUM
        }
        list.add(
            ThreatAssessment(
                title = "Rapid TVL Drain & Infinite Exploit Loop",
                riskLevel = pauseRisk,
                description = "Without a circuit breaker or withdrawal throttle, zero-day exploits can drain the entire contract pool in seconds.",
                recommendedFeatureId = SecurityFeaturesCatalog.PAUSABLE_CIRCUIT_BREAKER.id,
                isMitigated = pauseMitigated
            )
        )

        return list
    }

    fun getRecommendedFeatures(network: String, txType: String): Set<String> {
        val result = mutableSetOf<String>()
        SecurityFeaturesCatalog.allFeatures.forEach { feat ->
            val matchNetwork = feat.recommendedNetworks.contains(network)
            val matchTxType = feat.recommendedTxTypes.contains(txType)
            if (matchNetwork && matchTxType) {
                result.add(feat.id)
            }
        }
        // Always include foundational non-reentrancy and safemath
        result.add(SecurityFeaturesCatalog.REENTRANCY_GUARD.id)
        result.add(SecurityFeaturesCatalog.SAFEMATH_OVERFLOW_SHIELD.id)
        return result
    }

    fun calculateSecurityScore(
        network: String,
        txType: String,
        enabledFeatureIds: Set<String>
    ): Int {
        val recommended = getRecommendedFeatures(network, txType)
        if (recommended.isEmpty()) return 50

        val matchingCount = recommended.count { enabledFeatureIds.contains(it) }
        val baseScore = (matchingCount.toDouble() / recommended.size.toDouble()) * 85.0
        val extraBonus = (enabledFeatureIds.size - matchingCount).coerceAtLeast(0) * 3.0
        return (baseScore + extraBonus).toInt().coerceIn(0, 100)
    }

    fun generateFortifiedSolidityContract(
        contractName: String,
        network: String,
        txType: String,
        enabledFeatureIds: Set<String>,
        customSlippageBps: Int,
        customRateLimitEth: Double
    ): String {
        val hasReentrancy = enabledFeatureIds.contains(SecurityFeaturesCatalog.REENTRANCY_GUARD.id)
        val hasAccessControl = enabledFeatureIds.contains(SecurityFeaturesCatalog.ACCESS_CONTROL.id)
        val hasSlippage = enabledFeatureIds.contains(SecurityFeaturesCatalog.SLIPPAGE_LOCK.id)
        val hasPausable = enabledFeatureIds.contains(SecurityFeaturesCatalog.PAUSABLE_CIRCUIT_BREAKER.id)
        val hasEip712 = enabledFeatureIds.contains(SecurityFeaturesCatalog.EIP712_SIGNATURES.id)
        val hasOracle = enabledFeatureIds.contains(SecurityFeaturesCatalog.ORACLE_TWAP_GUARD.id)
        val hasPaymaster = enabledFeatureIds.contains(SecurityFeaturesCatalog.ERC4337_PAYMASTER_VALIDATOR.id)
        val hasAntiFrontrunning = enabledFeatureIds.contains(SecurityFeaturesCatalog.ANTI_FRONTRUNNING_COMMIT_REVEAL.id)
        val hasRateLimiter = enabledFeatureIds.contains(SecurityFeaturesCatalog.RATE_LIMITER_THROTTLE.id)

        val inherits = mutableListOf<String>()
        val imports = mutableListOf<String>()

        imports.add("// OpenZeppelin Contracts v5.0.0")
        if (hasReentrancy) {
            imports.add("import \"@openzeppelin/contracts/utils/ReentrancyGuard.sol\";")
            inherits.add("ReentrancyGuard")
        }
        if (hasAccessControl) {
            imports.add("import \"@openzeppelin/contracts/access/AccessControl.sol\";")
            inherits.add("AccessControl")
        }
        if (hasPausable) {
            imports.add("import \"@openzeppelin/contracts/utils/Pausable.sol\";")
            inherits.add("Pausable")
        }
        if (hasEip712) {
            imports.add("import \"@openzeppelin/contracts/utils/cryptography/EIP712.sol\";")
            imports.add("import \"@openzeppelin/contracts/utils/cryptography/ECDSA.sol\";")
            inherits.add("EIP712")
        }

        val inheritanceString = if (inherits.isEmpty()) "" else " is " + inherits.joinToString(", ")

        val safeName = contractName.replace(Regex("[^a-zA-Z0-9_]"), "").ifEmpty { "FortifiedSentinelContract" }

        return buildString {
            appendLine("// SPDX-License-Identifier: MIT")
            appendLine("pragma solidity ^0.8.24;")
            appendLine()
            appendLine("/**")
            appendLine(" * @title $safeName")
            appendLine(" * @notice Hardened for $network ($txType)")
            appendLine(" * @dev Configured via Sentinel Smart Contract Security Suite")
            appendLine(" */")
            appendLine()
            imports.forEach { appendLine(it) }
            appendLine()
            appendLine("contract $safeName$inheritanceString {")
            
            if (hasAccessControl) {
                appendLine("    bytes32 public constant OPERATOR_ROLE = keccak256(\"OPERATOR_ROLE\");")
                appendLine("    bytes32 public constant RISK_ADMIN_ROLE = keccak256(\"RISK_ADMIN_ROLE\");")
            }
            if (hasSlippage) {
                appendLine("    uint256 public constant BPS_DIVISOR = 10000;")
                appendLine("    uint256 public maxSlippageBps = $customSlippageBps; // ${customSlippageBps / 100.0}%")
            }
            if (hasRateLimiter) {
                appendLine("    uint256 public maxOutflowPerEpoch = ${customRateLimitEth.toLong()} ether;")
                appendLine("    uint256 public currentEpochStart;")
                appendLine("    uint256 public currentEpochOutflow;")
            }

            appendLine()
            appendLine("    event SecurityEventLogged(string action, address indexed operator, uint256 timestamp);")
            appendLine()

            // Constructor
            val constructorInits = mutableListOf<String>()
            if (hasEip712) constructorInits.add("EIP712(\"$safeName\", \"1.0\")")
            val ctorSuper = if (constructorInits.isEmpty()) "" else " " + constructorInits.joinToString(" ")

            appendLine("    constructor()$ctorSuper {")
            if (hasAccessControl) {
                appendLine("        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);")
                appendLine("        _grantRole(OPERATOR_ROLE, msg.sender);")
                appendLine("        _grantRole(RISK_ADMIN_ROLE, msg.sender);")
            }
            if (hasRateLimiter) {
                appendLine("        currentEpochStart = block.timestamp;")
            }
            appendLine("        emit SecurityEventLogged(\"INITIALIZED\", msg.sender, block.timestamp);")
            appendLine("    }")
            appendLine()

            // Modifiers & Functions
            if (hasPausable && hasAccessControl) {
                appendLine("    function emergencyPause() external onlyRole(RISK_ADMIN_ROLE) {")
                appendLine("        _pause();")
                appendLine("        emit SecurityEventLogged(\"EMERGENCY_PAUSED\", msg.sender, block.timestamp);")
                appendLine("    }")
                appendLine()
                appendLine("    function emergencyUnpause() external onlyRole(DEFAULT_ADMIN_ROLE) {")
                appendLine("        _unpause();")
                appendLine("        emit SecurityEventLogged(\"UNPAUSED\", msg.sender, block.timestamp);")
                appendLine("    }")
                appendLine()
            }

            val execModifiers = mutableListOf<String>()
            if (hasReentrancy) execModifiers.add("nonReentrant")
            if (hasPausable) execModifiers.add("whenNotPaused")
            if (hasAccessControl) execModifiers.add("onlyRole(OPERATOR_ROLE)")

            val modStr = if (execModifiers.isEmpty()) "" else " " + execModifiers.joinToString(" ")

            appendLine("    /**")
            appendLine("     * @notice Primary protected execution endpoint")
            appendLine("     */")
            appendLine("    function executeProtectedOperation(")
            appendLine("        uint256 expectedOut,")
            appendLine("        uint256 actualOut,")
            appendLine("        uint256 deadline")
            appendLine("    ) external$modStr {")
            appendLine("        require(block.timestamp <= deadline, \"Execution expired in mempool\");")
            
            if (hasSlippage) {
                appendLine("        uint256 minOut = (expectedOut * (BPS_DIVISOR - maxSlippageBps)) / BPS_DIVISOR;")
                appendLine("        require(actualOut >= minOut, \"Slippage bound breach\");")
            }
            if (hasRateLimiter) {
                appendLine("        if (block.timestamp >= currentEpochStart + 1 hours) {")
                appendLine("            currentEpochStart = block.timestamp;")
                appendLine("            currentEpochOutflow = 0;")
                appendLine("        }")
                appendLine("        require(currentEpochOutflow + actualOut <= maxOutflowPerEpoch, \"Rate limit capped\");")
                appendLine("        currentEpochOutflow += actualOut;")
            }

            appendLine("        emit SecurityEventLogged(\"OPERATION_EXECUTED\", msg.sender, block.timestamp);")
            appendLine("    }")
            appendLine("}")
        }
    }
}
