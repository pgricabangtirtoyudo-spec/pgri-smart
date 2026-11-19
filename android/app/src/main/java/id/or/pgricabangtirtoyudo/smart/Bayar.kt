package id.or.pgricabangtirtoyudo.smart

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** Bayar iuran: lihat rekening/QRIS, pilih bulan, unggah foto bukti. Pengurus mengonfirmasi, lalu iuran tercatat lunas. */
@Composable
fun LayarBayar(token: String, b: JSONObject, tahun: Int, kembali: () -> Unit, segarkan: () -> Unit) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    val clip = LocalClipboardManager.current
    var info by remember { mutableStateOf<JSONObject?>(null) }
    var galat by remember { mutableStateOf("") }
    var pilih by remember { mutableStateOf(setOf<Int>()) }
    var bukti by remember { mutableStateOf<Bitmap?>(null) }
    var sibuk by remember { mutableStateOf(false) }
    var pesan by remember { mutableStateOf("") }
    var ok by remember { mutableStateOf(false) }

    fun muat() { scope.launch { try { info = Api.panggil("infoBayarSaya", token).obj() } catch (e: Exception) { galat = e.message ?: "Gagal memuat." } } }
    LaunchedEffect(Unit) { muat() }
    val pilihFoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { val bm = bacaBitmap(c, uri); if (bm != null) { bukti = perkecil(bm, 1280); pesan = "" } else { pesan = "Foto tidak dapat dibaca."; ok = false } }
    }

    val bulan = b.optJSONObject("bulan") ?: JSONObject()
    val sek = b.optJSONObject("sekarang") ?: JSONObject()
    val y = sek.optInt("y"); val m = sek.optInt("m")
    val riwayat = info?.optJSONArray("riwayat").daftar()
    val menunggu = HashSet<Int>()
    riwayat.filter { it.optString("status") == "Menunggu" && it.optInt("tahun") == tahun }.forEach { r -> val a = r.optJSONArray("bulan"); if (a != null) for (i in 0 until a.length()) menunggu.add(a.optInt(i)) }
    val tarif = info?.optInt("tarif") ?: b.optInt("tarif")

    Column(Modifier.fillMaxSize()) {
        HeaderSub("Bayar Iuran", kembali)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (galat.isNotEmpty()) Text(galat, color = Merah)
            info?.let { i ->
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Kartu), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tujuan pembayaran", fontWeight = FontWeight.Bold, color = Teks)
                        if (i.optString("bank").isNotEmpty()) {
                            Text(i.optString("bank"), fontSize = 14.sp)
                            TextButton({ clip.setText(AnnotatedString(i.optString("bank"))) }) { Icon(Icons.Filled.ContentCopy, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Salin") }
                        }
                        val q = i.optString("qris")
                        if (q.startsWith("https://")) AsyncImage(fotoBn(q, 900), "QRIS", Modifier.fillMaxWidth().heightIn(max = 320.dp), contentScale = ContentScale.Fit)
                        if (i.optString("info").isNotEmpty()) Text(i.optString("info"), fontSize = 12.sp, color = Abu)
                        if (i.optString("bank").isEmpty() && !q.startsWith("https://")) Text("Pengurus belum mengisi rekening/QRIS. Hubungi pengurus cabang.", fontSize = 13.sp, color = Abu)
                    }
                }
            }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Kartu), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. Pilih bulan yang dibayar ($tahun)", fontWeight = FontWeight.Bold, color = Teks)
                    (1..12).chunked(4).forEach { baris ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            baris.forEach { i ->
                                val lunas = bulan.has("$i"); val tunggu = menunggu.contains(i)
                                val bisa = !lunas && !tunggu
                                FilterChip(pilih.contains(i), { if (bisa) pilih = if (pilih.contains(i)) pilih - i else pilih + i }, enabled = bisa,
                                    label = { Text(BULAN[i - 1] + if (lunas) " ✓" else if (tunggu) " …" else "", fontSize = 12.sp) }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Text("✓ sudah lunas   … menunggu konfirmasi", fontSize = 11.sp, color = Abu)
                    Text("Total: ${rupiah(tarif * pilih.size)}  (${pilih.size} bulan × ${rupiah(tarif)})", fontWeight = FontWeight.SemiBold, color = Teks)
                }
            }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Kartu), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("2. Unggah foto bukti bayar", fontWeight = FontWeight.Bold, color = Teks)
                    bukti?.let { Image(it.asImageBitmap(), "Bukti", Modifier.fillMaxWidth().heightIn(max = 260.dp), contentScale = ContentScale.Fit) }
                    OutlinedButton({ pilihFoto.launch("image/*") }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Filled.Image, null); Spacer(Modifier.width(8.dp)); Text(if (bukti == null) "Pilih Foto / Tangkapan Layar" else "Ganti Foto")
                    }
                    Button(onClick = {
                        val bm = bukti ?: return@Button
                        val b64 = jpegBase64(bm, 1400000)
                        if (b64 == null) { pesan = "Foto terlalu besar."; ok = false; return@Button }
                        sibuk = true; pesan = "Mengirim…"; ok = true
                        scope.launch {
                            try {
                                val arr = JSONArray(); pilih.sorted().forEach { arr.put(it) }
                                Api.panggil("kirimBuktiIuran", token, tahun, arr, b64)
                                pesan = "Bukti terkirim. Menunggu konfirmasi pengurus."; ok = true; pilih = emptySet(); bukti = null; muat(); segarkan()
                            } catch (e: Exception) { pesan = e.message ?: "Gagal mengirim."; ok = false }
                            sibuk = false
                        }
                    }, enabled = pilih.isNotEmpty() && bukti != null && !sibuk, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Hijau)) {
                        Text("Kirim Bukti Bayar", fontWeight = FontWeight.Bold)
                    }
                    if (pesan.isNotEmpty()) Text(pesan, fontSize = 13.sp, color = if (ok) Hijau else Merah)
                }
            }
            if (riwayat.isNotEmpty()) {
                Text("Riwayat kiriman", fontWeight = FontWeight.Bold, color = Teks)
                riwayat.forEach { r ->
                    val st = r.optString("status")
                    val (fg, bg) = when (st) { "Disetujui" -> HijauGelap to TintHijau; "Ditolak" -> Merah to TintMerah; else -> Teks to TintBiru }
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Kartu)) {
                        Column(Modifier.padding(12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                val bl = r.optJSONArray("bulan"); val nama = (0 until (bl?.length() ?: 0)).joinToString(", ") { BULAN[(bl!!.optInt(it) - 1).coerceIn(0, 11)] }
                                Text("${r.optInt("tahun")} • $nama", fontWeight = FontWeight.SemiBold, color = Teks)
                                Text(st, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp))
                            }
                            Text(rupiah(r.optInt("jumlah")) + " • " + r.optString("waktu"), fontSize = 11.sp, color = Abu)
                            if (r.optString("catatan").isNotEmpty()) Text("Catatan: " + r.optString("catatan"), fontSize = 12.sp, color = Merah)
                        }
                    }
                }
            }
        }
    }
}
