package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R

object SentinelNotificationManager {
    private const val CHANNEL_ID = "sentinel_alerts_channel"
    private const val CHANNEL_NAME = "Sentinel MEV & Faucet Alerts"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for automated faucet refills and executed MEV opportunities"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showFaucetRefillNotification(
        context: Context,
        faucetName: String,
        amountRefilled: String,
        newBalance: String
    ) {
        createNotificationChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🚰 Faucet Balance Refilled")
            .setContentText("Automated refill triggered for $faucetName! Refilled $amountRefilled (New Balance: $newBalance)")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Automated refill triggered for $faucetName!\nRefilled: $amountRefilled\nNew Vault Balance: $newBalance"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((System.currentTimeMillis() % 10000).toInt() + 1000, builder.build())
    }

    fun showMevExecutionNotification(
        context: Context,
        asset: String,
        profitUsd: Double,
        txHash: String,
        chain: String,
        blockNumber: Long
    ) {
        createNotificationChannel(context)
        val shortHash = if (txHash.length > 12) "${txHash.take(8)}...${txHash.takeLast(6)}" else txHash
        val profitText = String.format("$%.2f", profitUsd)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚡ MEV Opportunity Executed")
            .setContentText("Successfully executed $asset flash arbitrage on $chain! Profit: +$profitText (Block #$blockNumber)")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "⚡ MEV Bundle Confirmed on $chain!\nAsset: $asset | Block: #$blockNumber\nNet Profit: +$profitText\nTx Hash: $shortHash"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((System.currentTimeMillis() % 10000).toInt() + 2000, builder.build())
    }

    fun showFundTransferNotification(
        context: Context,
        coin: String,
        network: String,
        amount: Double,
        destinationAddress: String,
        txHash: String,
        gasFeeEth: Double,
        gasPriceGwei: Double
    ) {
        createNotificationChannel(context)
        val shortHash = if (txHash.length > 12) "${txHash.take(8)}...${txHash.takeLast(6)}" else txHash
        val shortDest = if (destinationAddress.length > 12) "${destinationAddress.take(6)}...${destinationAddress.takeLast(4)}" else destinationAddress
        val amountStr = String.format("%.4f %s", amount, coin)
        val gasStr = if (gasFeeEth > 0) String.format("%.6f ETH (%.1f Gwei)", gasFeeEth, gasPriceGwei) else "Sponsored by Paymaster (0.00 ETH)"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("💸 Fund Transfer Dispatched")
            .setContentText("Dispatched $amountStr to $shortDest on $network! Gas: $gasStr")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "💸 Mainnet Fund Transfer Confirmed!\n" +
                    "Asset: $amountStr on $network\n" +
                    "Recipient: $destinationAddress\n" +
                    "Gas Fee: $gasStr\n" +
                    "Tx Hash: $shortHash"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((System.currentTimeMillis() % 10000).toInt() + 3000, builder.build())
    }
}

