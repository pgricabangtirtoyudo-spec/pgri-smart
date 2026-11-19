package id.or.pgricabangtirtoyudo.smart

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch

/** Absen: pindai QR kegiatan, atau pilih kegiatan hari ini tanpa QR. Keduanya diakhiri foto selfie. */
@Composable
fun TabAbsen(token: String, selesai: () -> Unit) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    var kode by remember { mutableStateOf("") }
    var judul by remember { mutableStateOf("") }
    var selfie by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf("") }
    var galat by remember { mutableStateOf(false) }
    var sibuk by remember { mutableStateOf(false) }
    var hasil by remember { mutableStateOf<String?>(null) }
    var daftar by remember { mutableStateOf<List<org.json.JSONObject>?>(null) }
    var namaAkun by remember { mutableStateOf("") }
    var memuat by remember { mutableStateOf(false) }

    fun info(t: String, bad: Boolean = false) { status = t; galat = bad }
    fun ulang() { kode = ""; judul = ""; selfie = null; status = ""; hasil = null; daftar = null }

    val kamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) { selfie = bmp; info("Foto siap. Tekan Kirim Kehadiran.") } else info("Pengambilan foto dibatalkan.", true)
    }
    val izinKamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) kamera.launch(null) else info("Izin kamera dibutuhkan untuk foto selfie absen.", true)
    }
    fun ambilSelfie() {
        if (ContextCompat.checkSelfPermission(c, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) kamera.launch(null)
        else izinKamera.launch(Manifest.permission.CAMERA)
    }
    fun pindai() {
        info("Membuka pemindai…")
        val opsi = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        GmsBarcodeScanning.getClient(c, opsi).startScan()
            .addOnSuccessListener { b ->
                val k = ambilKode(b.rawValue ?: "")
                if (k.isEmpty()) info("QR ini bukan kode absensi PGRI SMART.", true)
                else { kode = k; judul = ""; selfie = null; info("Kode kegiatan terbaca. Ambil foto selfie untuk menyelesaikan absen.") }
            }
            .addOnCanceledListener { info("") }
            .addOnFailureListener { e -> info("Pemindai tidak dapat dibuka: " + (e.message ?: ""), true) }
    }
    fun muatDaftar() {
        memuat = true; info("")
        scope.launch {
            try {
                val r = Api.panggil("getKegiatanAbsenSaya", token).obj()
                namaAkun = r.optString("nama"); daftar = r.optJSONArray("list").daftar()
            } catch (e: Exception) { info(e.message ?: "Gagal memuat kegiatan.", true); daftar = emptyList() }
            memuat = false
        }
    }
    fun kirim() {
        val s = selfie ?: return
        val b64 = jpegBase64(perkecil(s, 480), 150000)
        if (b64 == null) { info("Foto terlalu besar. Ambil ulang.", true); return }
        sibuk = true; info("Mencatat kehadiran…")
        scope.launch {
            try {
                val r = Api.panggil("absenAnggotaFoto", token, kode, b64).obj()
                hasil = (if (r.optBoolean("sudah")) "Kehadiran Anda sudah tercatat." else "Kehadiran tercatat. Terima kasih!") +
                    (r.optString("kegiatan").takeIf { it.isNotEmpty() }?.let { "\n$it" } ?: "")
                info(""); selesai()
            } catch (e: Exception) { info(e.message ?: "Gagal mencatat kehadiran.", true) }
            sibuk = false
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Absen Kegiatan", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Teks)
        Text("Kehadiran dicatat atas nama akun Anda, tanpa mengetik NPA.", fontSize = 13.sp, color = Abu)

        if (hasil != null) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = TintHijau), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Hijau, modifier = Modifier.size(64.dp))
                    Text(hasil!!, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = HijauGelap, fontSize = 16.sp)
                    Button({ ulang() }, colors = ButtonDefaults.buttonColors(containerColor = Hijau)) { Text("Selesai") }
                }
            }
            return@Column
        }

        if (kode.isEmpty()) {
            TombolBesar("Pindai QR Absen", "Arahkan kamera ke kode QR kegiatan", Icons.Filled.QrCodeScanner, Brush.linearGradient(listOf(Navy, NavyTerang))) { daftar = null; pindai() }
            TombolBesar("Absen Tanpa QR", "Pilih kegiatan hari ini, nama terisi otomatis", Icons.Filled.Edit, Brush.linearGradient(listOf(HijauGelap, Color(0xFF1FA557)))) { muatDaftar() }
            if (memuat) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Hijau)
            daftar?.let { d ->
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Kartu), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(14.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Absen dicatat atas nama ${namaAkun.ifEmpty { "akun Anda" }}.", fontSize = 12.sp, color = Abu)
                        if (d.isEmpty()) Text("Tidak ada kegiatan dengan absensi terbuka hari ini. Bila sedang mengikuti kegiatan, minta panitia membuka absensi atau gunakan Pindai QR.", fontSize = 13.sp, color = TeksKuning,
                            modifier = Modifier.background(TintKuning, RoundedCornerShape(10.dp)).padding(10.dp))
                        d.forEach { k ->
                            val sudah = k.optBoolean("sudah")
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Garis, RoundedCornerShape(12.dp)).clickable(enabled = !sudah) {
                                kode = k.optString("kode"); judul = k.optString("judul"); selfie = null; info("Ambil foto selfie untuk menyelesaikan absen.")
                            }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(if (sudah) Icons.Filled.CheckCircle else Icons.Filled.EventAvailable, null, tint = if (sudah) Hijau else Navy)
                                Column(Modifier.weight(1f)) {
                                    Text(k.optString("judul"), fontWeight = FontWeight.SemiBold, color = if (sudah) Abu else Teks)
                                    Text(if (sudah) "Sudah tercatat" else k.optString("lokasi").ifEmpty { "Absensi dibuka" }, fontSize = 12.sp, color = Abu)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Kartu), elevation = CardDefaults.cardElevation(3.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (judul.isNotEmpty()) judul else "Kode kegiatan terbaca", fontWeight = FontWeight.Bold, color = Teks, fontSize = 16.sp, textAlign = TextAlign.Center)
                    Box(Modifier.size(170.dp).clip(CircleShape).background(TintAbu).border(3.dp, Navy, CircleShape), contentAlignment = Alignment.Center) {
                        val s = selfie
                        if (s != null) Image(s.asImageBitmap(), "Selfie", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Icon(Icons.Filled.PhotoCamera, null, tint = Abu, modifier = Modifier.size(56.dp))
                    }
                    OutlinedButton({ ambilSelfie() }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), enabled = !sibuk) {
                        Icon(Icons.Filled.PhotoCamera, null); Spacer(Modifier.width(8.dp)); Text(if (selfie == null) "Ambil Foto Selfie" else "Ganti Foto")
                    }
                    Button({ kirim() }, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp), enabled = selfie != null && !sibuk, colors = ButtonDefaults.buttonColors(containerColor = Hijau)) {
                        if (sibuk) { CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
                        Text("Kirim Kehadiran", fontWeight = FontWeight.Bold)
                    }
                    TextButton({ ulang() }, enabled = !sibuk) { Text("Batal", color = Abu) }
                }
            }
        }
        if (status.isNotEmpty()) Text(status, color = if (galat) Merah else Teks, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
fun TombolBesar(judul: String, ket: String, ikon: androidx.compose.ui.graphics.vector.ImageVector, latar: Brush, klik: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(latar).clickable(onClick = klik).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.size(54.dp).background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Icon(ikon, null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(judul, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            Text(ket, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = Color.White)
    }
}
