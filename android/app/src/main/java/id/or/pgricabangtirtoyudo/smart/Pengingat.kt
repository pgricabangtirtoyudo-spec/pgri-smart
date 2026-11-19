package id.or.pgricabangtirtoyudo.smart

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import java.util.Calendar
import java.util.TimeZone

/** Pengingat agenda: notifikasi sehari dan sejam sebelum acara, dibuat di HP (tanpa server). */
object Pengingat {
    private fun p(c: Context) = c.getSharedPreferences("pgri_ingatkan", Context.MODE_PRIVATE)

    fun idAgenda(judul: String, tanggal: String) = "$judul|$tanggal"

    /** Waktu acara (ms) dari tanggal yyyy-MM-dd atau dd/MM/yyyy dan jam "08.00" / "08:00 WIB"; tanpa jam dianggap 07.00. */
    fun waktuAcara(tanggal: String, waktu: String): Long? {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
        val a = Regex("(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})").find(tanggal)
        val b = Regex("(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{4})").find(tanggal)
        val (y, m, d) = when {
            a != null -> Triple(a.groupValues[1].toInt(), a.groupValues[2].toInt(), a.groupValues[3].toInt())
            b != null -> Triple(b.groupValues[3].toInt(), b.groupValues[2].toInt(), b.groupValues[1].toInt())
            else -> return null
        }
        val j = Regex("(\\d{1,2})[.:](\\d{2})").find(waktu)
        val jam = j?.groupValues?.get(1)?.toInt() ?: 7
        val mnt = j?.groupValues?.get(2)?.toInt() ?: 0
        cal.set(y, m - 1, d, jam.coerceIn(0, 23), mnt.coerceIn(0, 59), 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun aktif(c: Context, id: String) = p(c).contains(id)

    private fun pi(c: Context, id: String, jenis: Int, judul: String, isi: String): PendingIntent {
        val i = Intent(c, ReceiverPengingat::class.java).putExtra("judul", judul).putExtra("isi", isi)
        return PendingIntent.getBroadcast(c, ("$id#$jenis").hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun jadwalkan(c: Context, id: String, judul: String, lokasi: String, ms: Long) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val skrg = System.currentTimeMillis()
        val tempat = if (lokasi.isNotEmpty()) " di $lokasi" else ""
        val h1 = ms - 24L * 3600 * 1000; val j1 = ms - 3600L * 1000
        try {
            if (h1 > skrg) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, h1, pi(c, id, 1, "Besok: $judul", "Agenda PGRI$tempat."))
            if (j1 > skrg) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, j1, pi(c, id, 2, "Sejam lagi: $judul", "Agenda PGRI$tempat."))
        } catch (_: Exception) {}
    }

    /** true bila pengingat terpasang. */
    fun pasang(c: Context, id: String, judul: String, lokasi: String, ms: Long): Boolean {
        if (ms - 3600L * 1000 <= System.currentTimeMillis()) return false
        p(c).edit().putString(id, "$judul\u0001$lokasi\u0001$ms").apply()
        jadwalkan(c, id, judul, lokasi, ms)
        return true
    }

    fun batal(c: Context, id: String) {
        val v = p(c).getString(id, null)
        p(c).edit().remove(id).apply()
        if (v == null) return
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val f = v.split('\u0001')
        for (j in 1..2) try { am.cancel(pi(c, id, j, f.getOrElse(0) { "" }, "")) } catch (_: Exception) {}
    }

    /** Setelah HP dinyalakan ulang. */
    fun pulihkan(c: Context) {
        val skrg = System.currentTimeMillis()
        p(c).all.forEach { (id, v) ->
            val f = (v as? String)?.split('\u0001') ?: return@forEach
            val ms = f.getOrNull(2)?.toLongOrNull() ?: return@forEach
            if (ms < skrg) p(c).edit().remove(id).apply() else jadwalkan(c, id, f[0], f.getOrElse(1) { "" }, ms)
        }
    }

    fun keKalender(c: Context, judul: String, lokasi: String, ms: Long) {
        try {
            c.startActivity(Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, judul).putExtra(CalendarContract.Events.EVENT_LOCATION, lokasi)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, ms).putExtra(CalendarContract.EXTRA_EVENT_END_TIME, ms + 2L * 3600 * 1000)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {}
    }
}

class ReceiverPengingat : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Notif.tampil(c, i.getStringExtra("judul") ?: "Agenda PGRI", i.getStringExtra("isi") ?: "", "agenda")
    }
}

class ReceiverBoot : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) { if (i.action == Intent.ACTION_BOOT_COMPLETED) Pengingat.pulihkan(c) }
}
