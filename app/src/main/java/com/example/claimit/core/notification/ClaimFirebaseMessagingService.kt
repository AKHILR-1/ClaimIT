package com.example.claimit.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.claimit.MainActivity
import com.example.claimit.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ClaimFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "ClaimFCMService"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "Refreshed FCM token received: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        try {
            val authUser = FirebaseAuth.getInstance().currentUser ?: return
            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "device_default"
            val rtdb = try {
                FirebaseDatabase.getInstance("https://claimit-322b5-default-rtdb.asia-southeast1.firebasedatabase.app")
            } catch (_: Exception) {
                FirebaseDatabase.getInstance()
            }

            val tokenMap = mapOf(
                "token" to token,
                "userId" to authUser.uid,
                "organizationId" to "lpu_main_campus",
                "updatedAt" to ServerValue.TIMESTAMP
            )

            rtdb.reference.child("fcmTokens").child(authUser.uid).child(deviceId).setValue(tokenMap)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save FCM token on token refresh: ${e.message}")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "ClaimIT Notification"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: remoteMessage.data["body"]
            ?: "New update on ClaimIT"

        val itemId = remoteMessage.data["itemId"] ?: ""
        val claimId = remoteMessage.data["claimId"] ?: ""
        val type = remoteMessage.data["type"] ?: ""

        showNotification(title, body, itemId, claimId, type)
    }

    private fun showNotification(
        title: String,
        body: String,
        itemId: String,
        claimId: String,
        type: String
    ) {
        val channelId = "claimit_notifications_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "ClaimIT Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Lost & Found items and ownership claims"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_ITEM_ID", itemId)
            putExtra("EXTRA_CLAIM_ID", claimId)
            putExtra("EXTRA_NOTIFICATION_TYPE", type)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.claimit_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
