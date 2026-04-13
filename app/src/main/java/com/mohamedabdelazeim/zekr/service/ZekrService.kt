package com.mohamedabdelazeim.zekr.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import com.mohamedabdelazeim.zekr.MainActivity
import com.mohamedabdelazeim.zekr.R
import com.mohamedabdelazeim.zekr.data.ZekrData
import com.mohamedabdelazeim.zekr.data.ZekrPrefs

class ZekrService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "zekr_channel"
        const val NOTIF_ID = 2001
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        
        if (isCallActive() || isAudioBusy() || isInCommunication()) {
            scheduleNext(this)
            stopSelf()
            return START_NOT_STICKY
        }

        val mode = ZekrPrefs.getPlaybackMode(this)
        
        val index = if (mode == 1) {
            ZekrPrefs.getRepeatIndex(this)
        } else {            ZekrPrefs.nextZekrIndex(this)
        }

        val safeIndex = index.coerceIn(0, ZekrData.zekrList.size - 1)
        val zekr = ZekrData.zekrList[safeIndex]

        val notif = buildNotification(zekr.name, zekr.text)
        startForeground(NOTIF_ID, notif)

        if (zekr.audioRes != null) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, zekr.audioRes)
            mediaPlayer?.setOnCompletionListener {
                it.release()
                scheduleNext(this)
                stopSelf()
            }
            mediaPlayer?.start()
        } else {
            android.os.Handler(mainLooper).postDelayed({
                scheduleNext(this)
                stopSelf()
            }, 5000)
        }

        return START_NOT_STICKY
    }

    private fun scheduleNext(context: Context) {
        if (!ZekrPrefs.isEnabled(context)) return
        
        val interval = ZekrPrefs.getIntervalInMinutes(context).toLong()
        val intent = Intent(context, ZekrService::class.java)
        
        val pending = PendingIntent.getService(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + interval * 60 * 1000,
            pending
        )
    }

    private fun isCallActive(): Boolean {
        return try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager            tm.callState != TelephonyManager.CALL_STATE_IDLE
        } catch (e: Exception) { false }
    }

    private fun isInCommunication(): Boolean {
        return try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode == AudioManager.MODE_IN_CALL || 
            audioManager.mode == AudioManager.MODE_IN_COMMUNICATION
        } catch (e: Exception) { false }
    }

    private fun isAudioBusy(): Boolean {
        return try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.isMusicActive
        } catch (e: Exception) { false }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val bmp = try {
            BitmapFactory.decodeResource(resources, R.drawable.notification_father)
        } catch (e: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🤲 $title")
            .setContentText(text.take(80))
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))

        if (bmp != null) {
            builder.setLargeIcon(bmp)
                .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bmp))
        }

        return builder.build()
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "ذكر", NotificationManager.IMPORTANCE_HIGH).apply {            description = "إشعارات الأذكار اليومية"
            enableVibration(true)
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(ch)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ZekrApp::WakeLockTag"
        )
        wakeLock?.acquire(10*60*1000L)
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
