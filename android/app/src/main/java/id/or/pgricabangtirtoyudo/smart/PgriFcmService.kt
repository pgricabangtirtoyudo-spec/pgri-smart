package id.or.pgricabangtirtoyudo.smart

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PgriFcmService : FirebaseMessagingService() {

    /** Kode HP baru/berubah: simpan; situs akan mendaftarkannya lagi saat anggota membuka aplikasi. */
    override fun onNewToken(token: String) {
        getSharedPreferences("pgri", MODE_PRIVATE).edit().putString("fcm", token).apply()
    }

    override fun onMessageReceived(m: RemoteMessage) {
        val judul = m.notification?.title ?: m.data["judul"] ?: "PGRI SMART"
        val isi = m.notification?.body ?: m.data["isi"] ?: ""
        Notif.tampil(this, judul, isi, m.data["url"] ?: "")
    }
}
