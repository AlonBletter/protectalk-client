package com.protectalk.protectalk.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        PushManager.onNewFcmToken(token)
        // TODO: Optionally enqueue background upload (WorkManager) if signed in
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // TODO: Parse message.data / message.notification
        // TODO: Show a notification or route in-app (later phases)
    }
}
