package id.or.pgricabangtirtoyudo.smart

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONObject

val KATEGORI_ADUAN = listOf("Keanggotaan", "Kesejahteraan", "Advokasi / Perlindungan Hukum", "Administrasi", "Saran & Masukan", "Lainnya")

fun warnaStatus(s: String): Pair<Color, Color> = when (s) {
    "Diproses" -> TeksKuning to TintKuning
    "Selesai" -> HijauGelap to TintHijau
    "Ditolak" -> Merah to TintMerah
    else -> Teks to TintBiru
}

@Composable
fun TabAduan(b: JSONObject, token: String, wa: String, profil: JSONObject, segarkan: () -> Unit) {
    val c = androidx.compose.ui.platform.LocalContext.current
    val daftar = b.optJSONArray("aduan").daftar()
    var form by remember { mutableStateOf(false) }
    var terbuka by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Layanan Aduan", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Teks)
        Text("Sampaikan aduan, saran, atau pertanyaan kepada pengurus cabang. Hanya Anda dan pengurus yang dapat melihatnya.", fontSize = 13.sp, color = Abu)
        Button({ form = true }, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Merah)) {
            Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp)); Text("Buat Aduan Baru", fontWeight = FontWeight.Bold)
        }
        if (wa.isNotEmpty()) OutlinedButton({ bukaTautan(c, waLink(wa, profil)) }, Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Filled.Chat, null, tint = Hijau); Spacer(Modifier.width(8.dp)); Text("Tanya cepat lewat WhatsApp pengurus")
        }
        if (info.isNotEmpty()) Text(info, color = Hijau, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (daftar.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Inbox, null, tint = Abu, modifier = Modifier.size(48.dp))
                Text("Belum ada aduan.", color = Abu)
            }
        }
        daftar.forEach { a ->
            val kode = a.optString("kode"); val buka = terbuka == kode
            val (fg, bg) = warnaStatus(a.optString("status"))
            Card(Modifier.fillMaxWidth().clickable { terbuka = if (buka) null else kode }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Kartu), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(a.optString("judul").ifEmpty { a.optString("kategori") }, fontWeight = FontWeight.Bold, color = Teks)
                            Text(a.optString("kategori") + " • " + a.optString("tanggal"), fontSize = 11.sp, color = Abu)
                        }
                        Text(a.optString("status"), color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    if (buka) {
                        Text("Aduan Anda", fontSize = 11.sp, color = Teks, fontWeight = FontWeight.Bold)
                        Text(a.optString("isi"), fontSize = 13.sp, modifier = Modifier.fillMaxWidth().background(TintLatar, RoundedCornerShape(10.dp)).padding(10.dp))
                        val tg = a.optString("tanggapan")
                        if (tg.isNotEmpty()) {
                            Text("Tanggapan pengurus", fontSize = 11.sp, color = HijauGelap, fontWeight = FontWeight.Bold)
                            Text(tg, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().background(TintHijau, RoundedCornerShape(10.dp)).padding(10.dp))
                        } else Text("Belum ada tanggapan dari pengurus.", fontSize = 12.sp, color = Abu)
                        val rw = a.optJSONArray("riwayat").daftar()
                        rw.forEach { x ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.padding(top = 5.dp).size(8.dp).background(Navy, CircleShape))
                                Column {
                                    Text(x.optString("s"), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Teks)
                                    Text(x.optString("w").take(16).replace('T', ' ') + (x.optString("o").takeIf { it.isNotEmpty() }?.let { " • $it" } ?: ""), fontSize = 10.5.sp, color = Abu)
                                    if (x.optString("p").isNotEmpty()) Text(x.optString("p"), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    if (form) FormAduan(token, { form = false }) { form = false; info = "Aduan terkirim. Terima kasih."; segarkan() }
}

@Composable
fun FormAduan(token: String, tutup: () -> Unit, sukses: () -> Unit) {
    val scope = rememberCoroutineScope()
    var kategori by remember { mutableStateOf(KATEGORI_ADUAN[0]) }
    var judul by remember { mutableStateOf("") }
    var isi by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var galat by remember { mutableStateOf("") }
    var sibuk by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { if (!sibuk) tutup() },
        title = { Text("Aduan Baru") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton({ menu = true }, Modifier.fillMaxWidth()) { Text(kategori, Modifier.weight(1f), maxLines = 1); Icon(Icons.Filled.ArrowDropDown, null) }
                    DropdownMenu(menu, { menu = false }) {
                        KATEGORI_ADUAN.forEach { k -> DropdownMenuItem({ Text(k) }, { kategori = k; menu = false }) }
                    }
                }
                OutlinedTextField(judul, { judul = it.take(200) }, label = { Text("Judul") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(isi, { isi = it.take(3000) }, label = { Text("Isi aduan (min. 10 huruf)") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                if (galat.isNotEmpty()) Text(galat, color = Merah, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(enabled = !sibuk, onClick = {
                if (judul.isBlank()) { galat = "Judul wajib diisi."; return@TextButton }
                if (isi.trim().length < 10) { galat = "Isi aduan terlalu singkat."; return@TextButton }
                sibuk = true; galat = ""
                scope.launch {
                    try { Api.panggil("kirimAduanSaya", token, kategori, judul.trim(), isi.trim()); sukses() }
                    catch (e: Exception) { galat = e.message ?: "Gagal mengirim."; sibuk = false }
                }
            }) { Text(if (sibuk) "Mengirim…" else "Kirim") }
        },
        dismissButton = { TextButton({ tutup() }, enabled = !sibuk) { Text("Batal") } })
}
