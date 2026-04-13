package com.mohamedabdelazeim.zekr.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.appcompat.app.AppCompatActivity
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
        // التحقق من المكالمات العادية والواتساب/زوم
        if (isCallActive() || isAudioBusy() || isInCommunication()) {
            scheduleNext(this)
            stopSelf()
            return START_NOT_STICKY
        }

        val index = ZekrPrefs.nextZekrIndex(this)
        val zekr = ZekrData.zekrList[index]

        val notif = buildNotification(zekr.name, zekr.text)
        startForeground(NOTIF_ID, notif)

        if (zekr.audioRes != null) {            mediaPlayer?.release()
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

    // التحقق من المكالمات العادية
    private fun isCallActive(): Boolean {
        return try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.callState != TelephonyManager.CALL_STATE_IDLE
        } catch (e: Exception) {
            false
        }
    }

    // التحقق من مكالمات الواتساب/زوم/فايبر
    private fun isInCommunication(): Boolean {
        return try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode == AudioManager.MODE_IN_CALL ||             audioManager.mode == AudioManager.MODE_IN_COMMUNICATION
        } catch (e: Exception) {
            false
        }
    }

    // التحقق من وجود صوت شغال (موسيقى، فيديو، إلخ)
    private fun isAudioBusy(): Boolean {
        return try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.isMusicActive
        } catch (e: Exception) {
            false
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val bmp = BitmapFactory.decodeResource(resources, R.drawable.notification_father)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(bmp)
            .setContentTitle("🤲 $title")
            .setContentText(text.take(80))
            .setStyle(NotificationCompat.BigPictureStyle()
                .bigPicture(bmp)
                .bigLargeIcon(null as android.graphics.Bitmap?)
                .setSummaryText(text.take(100))
            )
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .build()
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "ذكر", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "إشعارات الأذكار اليومية"
            enableVibration(true)
        }

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(ch)
    }

    private fun acquireWakeLock() {        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ZekrApp::WakeLockTag"
        )
        wakeLock?.acquire(10*60*1000L) // 10 دقائق
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
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
