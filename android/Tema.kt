package id.or.pgricabangtirtoyudo.smart

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AColor
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream

/** Pengaturan tampilan: "sistem" (ikut HP), "terang", atau "gelap". */
object Pal {
    var mode by mutableStateOf("sistem")
    var sistemGelap by mutableStateOf(false)
    val gelap: Boolean get() = mode == "gelap" || (mode == "sistem" && sistemGelap)
}

val Navy = Color(0xFF0D2F68)
val NavyTerang = Color(0xFF1A4FA3)
val Merah = Color(0xFFD3202F)
val Hijau = Color(0xFF1B9A4F)
val HijauGelap = Color(0xFF0F7A3D)
val Latar: Color get() = if (Pal.gelap) Color(0xFF0D1320) else Color(0xFFF2F5FA)
val Kartu: Color get() = if (Pal.gelap) Color(0xFF18202F) else Color.White
val Teks: Color get() = if (Pal.gelap) Color(0xFFD7E3FF) else Navy
val Abu: Color get() = if (Pal.gelap) Color(0xFF9BA7C2) else Color(0xFF6B7690)
val Garis: Color get() = if (Pal.gelap) Color(0xFF2A3550) else Color(0xFFE1E7F2)
val TintAbu: Color get() = if (Pal.gelap) Color(0xFF263149) else Color(0xFFE3E8F3)
val TintLatar: Color get() = if (Pal.gelap) Color(0xFF111A2B) else Color(0xFFF2F5FA)
val TintHijau: Color get() = if (Pal.gelap) Color(0xFF123524) else Color(0xFFE6F6EC)
val TintMerah: Color get() = if (Pal.gelap) Color(0xFF3A1A1F) else Color(0xFFFFEDEE)
val TintBiru: Color get() = if (Pal.gelap) Color(0xFF1B2A47) else Color(0xFFE3ECFB)
val TintKuning: Color get() = if (Pal.gelap) Color(0xFF3A3017) else Color(0xFFFFF7E0)
val TeksKuning: Color get() = if (Pal.gelap) Color(0xFFF2D58A) else Color(0xFF6B4E00)
val BULAN = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

@Composable
fun TemaPgri(isi: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (Pal.gelap) darkColorScheme(
            primary = Color(0xFF9DB8F0), onPrimary = Color(0xFF0A1B3D), secondary = Color(0xFFFF8A94), background = Latar, surface = Kartu,
            surfaceVariant = Color(0xFF263149), onSurface = Color(0xFFE3EAFB), onSurfaceVariant = Color(0xFFB4BED6), onBackground = Color(0xFFE3EAFB)
        ) else lightColorScheme(
            primary = Navy, onPrimary = Color.White, secondary = Merah, background = Latar, surface = Color.White,
            surfaceVariant = Color(0xFFE9EEF8), primaryContainer = Color(0xFFDCE6FA), onPrimaryContainer = Navy
        )
    ) { isi() }
}

/** Logo PGRI dalam lingkaran putih. */
@Composable
fun LogoPgri(ukuran: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(ukuran).background(Color.White, CircleShape).padding(ukuran / 14)) {
        Image(painterResource(R.drawable.logo_pgri), "Logo PGRI", Modifier.size(ukuran))
    }
}

fun buatQr(teks: String, ukuran: Int = 512): Bitmap {
    val m = QRCodeWriter().encode(teks, BarcodeFormat.QR_CODE, ukuran, ukuran)
    val b = Bitmap.createBitmap(ukuran, ukuran, Bitmap.Config.ARGB_8888)
    for (x in 0 until ukuran) for (y in 0 until ukuran) b.setPixel(x, y, if (m[x, y]) AColor.BLACK else AColor.WHITE)
    return b
}

fun fotoDariData(d: String): Bitmap? = try {
    if (d.startsWith("data:")) { val b = Base64.decode(d.substringAfter(","), Base64.DEFAULT); BitmapFactory.decodeByteArray(b, 0, b.size) } else null
} catch (e: Exception) { null }

fun inisial(nama: String): String {
    val p = nama.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (p.isEmpty()) return "?"
    return (p[0].take(1) + (if (p.size > 1) p[1].take(1) else "")).uppercase()
}

fun rupiah(n: Int): String = "Rp " + "%,d".format(n).replace(',', '.')

/** Potong tengah jadi persegi lalu perkecil. */
fun potongPersegi(b: Bitmap, sisi: Int): Bitmap {
    val m = minOf(b.width, b.height)
    val sq = Bitmap.createBitmap(b, (b.width - m) / 2, (b.height - m) / 2, m, m)
    return Bitmap.createScaledBitmap(sq, sisi, sisi, true)
}

/** Perkecil sisi terpanjang menjadi maks. */
fun perkecil(b: Bitmap, maks: Int): Bitmap {
    val s = minOf(1f, maks.toFloat() / maxOf(b.width, b.height))
    if (s >= 1f) return b
    return Bitmap.createScaledBitmap(b, maxOf(1, (b.width * s).toInt()), maxOf(1, (b.height * s).toInt()), true)
}

/** JPEG base64 (tanpa awalan) yang panjangnya tidak melebihi batas; null bila tidak muat. */
fun jpegBase64(b: Bitmap, batas: Int): String? {
    var q = 85
    while (q >= 30) {
        val o = ByteArrayOutputStream()
        b.compress(Bitmap.CompressFormat.JPEG, q, o)
        val s = Base64.encodeToString(o.toByteArray(), Base64.NO_WRAP)
        if (s.length <= batas) return s
        q -= 10
    }
    return null
}

fun bacaBitmap(c: Context, uri: Uri): Bitmap? = try {
    val opt = BitmapFactory.Options().apply { inSampleSize = 2 }
    c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opt) }
} catch (e: Exception) { null }

/** Isi QR absen: tautan ...?k=KODE atau kode polos. */
fun ambilKode(t: String): String {
    val s = t.trim()
    if (s.isEmpty()) return ""
    Regex("[?&#]k=([^&#\\s]+)").find(s)?.let { return java.net.URLDecoder.decode(it.groupValues[1], "UTF-8") }
    if (Regex("^[A-Za-z0-9_-]{3,40}$").matches(s)) return s
    return ""
}
