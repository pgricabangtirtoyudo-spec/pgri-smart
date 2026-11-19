package id.or.pgricabangtirtoyudo.smart

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object Notif {
    const val KANAL = "pgri_info"

    fun buatKanal(c: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val m = c.getSystemService(NotificationManager::class.java)
            if (m.getNotificationChannel(KANAL) == null) {
                val k = NotificationChannel(KANAL, "Pengingat dan informasi PGRI", NotificationManager.IMPORTANCE_HIGH)
                k.description = "Agenda pengurus, tanggapan aduan, dan informasi cabang"
                m.createNotificationChannel(k)
            }
        }
    }

    /** Dipakai saat aplikasi sedang terbuka (notifikasi dari FCM tidak otomatis tampil di kondisi ini). */
    fun tampil(c: Context, judul: String, isi: String, url: String) {
        buatKanal(c)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val i = Intent(c, NativeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (url.isNotEmpty()) putExtra("url", url)
        }
        val id = (System.currentTimeMillis() % 100000).toInt()
        val pi = PendingIntent.getActivity(c, id, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(c, KANAL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(judul)
            .setContentText(isi)
            .setStyle(NotificationCompat.BigTextStyle().bigText(isi))
            .setColor(ContextCompat.getColor(c, R.color.navy))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(c).notify(id, n)
    }
}
