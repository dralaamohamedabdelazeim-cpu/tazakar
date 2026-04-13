package com.mohamedabdelazeim.zekr.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mohamedabdelazeim.zekr.service.ZekrService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, ZekrService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
