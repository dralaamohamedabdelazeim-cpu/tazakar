package com.mohamedabdelazeim.zekr.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
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

    // 1. عند إنشاء الخدمة: بنجهز القناة والـ WakeLock
    override fun onCreate() {
        super.onCreate()
        createChannel()
        acquireWakeLock()
    }

    // 2. عند بدء الخدمة: المنطق الرئيسي
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        
        // أ. التحقق من المكالمات والصوت (عشان المقاطعة)
        if (isCallActive() || isAudioBusy() || isInCommunication()) {
            scheduleNext(this)
            stopSelf()
            return START_NOT_STICKY
        }

        // ب. تحديد الذكر بناءً على اختيار المستخدم (الجديد)
        val mode = ZekrPrefs.getPlaybackMode(this) // 0 = ترتيب، 1 = تكرار
        
        val index = if (mode == 1) {
            // لو تكرار: هجيب الذكر اللي المستخدم اختاره من الإعدادات
            ZekrPrefs.getRepeatIndex(this)
        } else {            // لو ترتيب: هجيب الذكر اللي جاي في الدور
            ZekrPrefs.nextZekrIndex(this)
        }

        // حماية عشان الرقم مايزيدش عن حجم القائمة
        val safeIndex = index.coerceIn(0, ZekrData.zekrList.size - 1)
        val zekr = ZekrData.zekrList[safeIndex]

        // ج. تشغيل الإشعار الأمامي
        val notif = buildNotification(zekr.name, zekr.text)
        startForeground(NOTIF_ID, notif)

        // د. تشغيل الصوت أو الانتظار
        if (zekr.audioRes != null) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, zekr.audioRes)
            mediaPlayer?.setOnCompletionListener {
                it.release()
                scheduleNext(this) // جدولة التكرار الجاي بعد ما الصوت يخلص
                stopSelf()
            }
            mediaPlayer?.start()
        } else {
            // لو مفيش صوت، انتظر 5 ثواني ثم جدول التالي
            android.os.Handler(mainLooper).postDelayed({
                scheduleNext(this)
                stopSelf()
            }, 5000)
        }

        return START_NOT_STICKY
    }

    // 3. جدولة التكرار القادم (AlarmManager)
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
            pending        )
    }

    // 4. دوال التحقق (مكالمات، واتساب، موسيقى)
    private fun isCallActive(): Boolean {
        return try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.callState != TelephonyManager.CALL_STATE_IDLE
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

    // 5. بناء الإشعار
    private fun buildNotification(title: String, text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        // محاولة جلب صورة الإشعار، لو ملقتهاش استخدم أيقونة التطبيق
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

        if (bmp != null) {            builder.setLargeIcon(bmp)
                .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bmp))
        }

        return builder.build()
    }

    // 6. إنشاء قناة الإشعارات
    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "ذكر", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "إشعارات الأذكار اليومية"
            enableVibration(true)
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(ch)
    }

    // 7. إدارة WakeLock (عشان الموبايل مايوقفش الخدمة)
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ZekrApp::WakeLockTag"
        )
        wakeLock?.acquire(10*60*1000L) // 10 دقائق (يتجدد تلقائياً مع كل تشغيل للخدمة)
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    // 8. تنظيف الموارد عند الإغلاق
    override fun onDestroy() {
        mediaPlayer?.release()
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
