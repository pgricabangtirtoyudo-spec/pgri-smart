package id.or.pgricabangtirtoyudo.smart

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as itemsGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

fun bukaTautan(c: android.content.Context, url: String) {
    if (!url.startsWith("https://") && !url.startsWith("tel:") && !url.startsWith("mailto:")) return
    try { c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
}

/** Tautan Drive (/d/ID) diubah ke thumbnail beresolusi tinggi, sama seperti di web. */
fun fotoBn(u: String, lebar: Int = 1200): String {
    val m = Regex("/d/([\\w-]+)").find(u) ?: Regex("[?&]id=([\\w-]+)").find(u)
    return if (m != null && u.contains("google.com")) "https://drive.google.com/thumbnail?id=${m.groupValues[1]}&sz=w$lebar" else u
}

@Composable
fun TabInfo(info: JSONObject?, token: String = "", profil: JSONObject = JSONObject(), awal: Int = 0) {
    if (info == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Informasi belum termuat.", color = Abu) }; return }
    val bagian = listOf("Statistik", "Berita", "Pengumuman", "Agenda", "Dokumen", "Galeri", "Pengurus", "Polling", "Kontak")
    var pilih by remember(awal) { mutableIntStateOf(awal) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            bagian.forEachIndexed { i, n -> FilterChip(pilih == i, { pilih = i }, { Text(n) }) }
        }
        when (pilih) {
            0 -> Statistik(info.optJSONObject("statistik") ?: JSONObject())
            1 -> Berita(info.optJSONArray("berita"))
            2 -> Pengumuman(info.optJSONArray("pengumuman"))
            3 -> Agenda(info.optJSONArray("agenda"))
            4 -> Dokumen(info.optJSONArray("dokumen"))
            5 -> GaleriLayar(info.optJSONArray("galeri"))
            6 -> Pengurus(info.optJSONArray("pengurus"))
            7 -> PollingLayar(token)
            else -> Kontak(info.optJSONObject("situs") ?: JSONObject(), profil)
        }
    }
}

@Composable
fun BarData(judul: String, a: JSONArray?, total: Int) {
    val d = a.daftar()
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Kartu)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(judul, fontWeight = FontWeight.Bold, color = Teks)
            d.forEach { x ->
                val n = x.optInt("jumlah")
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(x.optString("nama"), fontSize = 13.sp); Text("$n", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)).background(TintAbu)) {
                        Box(Modifier.fillMaxWidth(if (total > 0) (n.toFloat() / total).coerceIn(0.02f, 1f) else 0f).fillMaxHeight().background(Navy))
                    }
                }
            }
        }
    }
}

@Composable
fun Statistik(s: JSONObject) {
    val total = s.optInt("total")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Navy)) {
            Column(Modifier.padding(16.dp)) {
                Text("Total Anggota", color = Color(0xFFCFD8EC), fontSize = 12.sp)
                Text("$total", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
        }
        BarData("Jenis Kelamin", s.optJSONArray("jk"), total)
        BarData("Status Kepegawaian", s.optJSONArray("status"), total)
        BarData("Jenjang Mengajar", s.optJSONArray("jenjang"), total)
        BarData("Per Ranting", s.optJSONArray("ranting"), total)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun Berita(a: JSONArray?) {
    val list = a.daftar()
    var buka by remember { mutableStateOf<JSONObject?>(null) }
    if (list.isEmpty()) { Text("Belum ada berita.", Modifier.padding(16.dp), color = Color.Gray); return }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(list) { b ->
            Card(Modifier.fillMaxWidth().clickable { buka = b }, colors = CardDefaults.cardColors(containerColor = Kartu)) {
                Column {
                    val f = b.optString("foto")
                    if (f.startsWith("https://")) AsyncImage(fotoBn(f, 1000), null, Modifier.fillMaxWidth().height(150.dp), contentScale = ContentScale.Crop)
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(b.optString("judul"), fontWeight = FontWeight.Bold, color = Teks)
                        Text(b.optString("tanggal"), fontSize = 11.sp, color = Color.Gray)
                        Text(b.optString("ringkasan"), fontSize = 13.sp, maxLines = 3)
                    }
                }
            }
        }
    }
    buka?.let { b ->
        AlertDialog(onDismissRequest = { buka = null }, confirmButton = { TextButton({ buka = null }) { Text("Tutup") } },
            title = { Text(b.optString("judul")) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                val f = b.optString("foto")
                if (f.startsWith("https://")) AsyncImage(fotoBn(f, 1600), null, Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.height(8.dp))
                Text(b.optString("isi").ifEmpty { b.optString("ringkasan") }, fontSize = 14.sp)
            } })
    }
}

@Composable
fun Pengumuman(a: JSONArray?) {
    val list = a.daftar()
    if (list.isEmpty()) { Text("Belum ada pengumuman.", Modifier.padding(16.dp), color = Color.Gray); return }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(list) { b ->
            val penting = b.optString("penting").lowercase().let { it == "ya" || it == "true" || it == "1" }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (penting) TintMerah else Kartu)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text((if (penting) "❗ " else "") + b.optString("judul"), fontWeight = FontWeight.Bold, color = if (penting) Merah else Teks)
                    Text(b.optString("tanggal"), fontSize = 11.sp, color = Color.Gray)
                    Text(b.optString("isi"), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun Agenda(a: JSONArray?) {
    val c = LocalContext.current
    val list = a.daftar()
    var segar by remember { mutableIntStateOf(0) }
    var catatan by remember { mutableStateOf("") }
    if (list.isEmpty()) { Text("Belum ada agenda.", Modifier.padding(16.dp), color = Abu); return }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (catatan.isNotEmpty()) item { Text(catatan, color = Hijau, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        items(list) { b ->
            val id = Pengingat.idAgenda(b.optString("judul"), b.optString("tanggal"))
            val ms = Pengingat.waktuAcara(b.optString("tanggal"), b.optString("waktu"))
            val masaDepan = ms != null && ms > System.currentTimeMillis()
            val on = segar >= 0 && Pengingat.aktif(c, id)
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Kartu), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(b.optString("judul"), fontWeight = FontWeight.Bold, color = Teks)
                    Text(listOf(b.optString("tanggal"), b.optString("waktu")).filter { it.isNotEmpty() }.joinToString(" • "), fontSize = 12.sp)
                    if (b.optString("lokasi").isNotEmpty()) Text("📍 " + b.optString("lokasi"), fontSize = 12.sp, color = Abu)
                    if (b.optString("keterangan").isNotEmpty()) Text(b.optString("keterangan"), fontSize = 12.sp, color = Abu)
                    if (masaDepan) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        FilterChip(on, {
                            if (on) { Pengingat.batal(c, id); catatan = "Pengingat dimatikan." }
                            else catatan = if (Pengingat.pasang(c, id, b.optString("judul"), b.optString("lokasi"), ms!!)) "Pengingat dipasang: sehari dan sejam sebelum acara." else "Acara terlalu dekat untuk dipasangi pengingat."
                            segar++
                        }, label = { Text(if (on) "Pengingat aktif" else "Ingatkan saya", fontSize = 12.sp) },
                            leadingIcon = { Icon(if (on) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone, null, Modifier.size(16.dp)) })
                        AssistChip({ Pengingat.keKalender(c, b.optString("judul"), b.optString("lokasi"), ms!!) }, label = { Text("Kalender", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Filled.CalendarMonth, null, Modifier.size(16.dp)) })
                    }
                }
            }
        }
    }
}

@Composable
fun Dokumen(a: JSONArray?) {
    val c = LocalContext.current
    val list = a.daftar()
    if (list.isEmpty()) { Text("Belum ada dokumen.", Modifier.padding(16.dp), color = Color.Gray); return }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(list) { b ->
            Card(Modifier.fillMaxWidth().clickable { bukaTautan(c, b.optString("url")) }, colors = CardDefaults.cardColors(containerColor = Kartu)) {
                Column(Modifier.padding(14.dp)) {
                    Text("📄 " + b.optString("judul"), fontWeight = FontWeight.SemiBold, color = Teks)
                    Text(listOf(b.optString("kategori"), b.optString("tanggal")).filter { it.isNotEmpty() }.joinToString(" • "), fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun Pengurus(a: JSONArray?) {
    val list = a.daftar()
    if (list.isEmpty()) { Text("Data pengurus belum tersedia.", Modifier.padding(16.dp), color = Color.Gray); return }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(list) { b ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Kartu)) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val f = b.optString("foto")
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(TintAbu)) {
                        if (f.startsWith("https://")) AsyncImage(fotoBn(f, 400), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                    Column {
                        Text(b.optString("nama"), fontWeight = FontWeight.Bold, color = Teks)
                        Text(b.optString("jabatan"), fontSize = 12.sp, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

fun namaAkun(u: String): String {
    if (!u.startsWith("http")) return u
    val p = u.substringBefore("?").trimEnd('/').substringAfterLast('/')
    return if (p.isEmpty()) u else if (p.startsWith("@")) p else "@$p"
}

@Composable
fun Kontak(s: JSONObject, profil: JSONObject = JSONObject()) {
    val c = LocalContext.current
    @Composable fun baris(ikon: androidx.compose.ui.graphics.vector.ImageVector, judul: String, nilai: String, url: String, warna: Color = Teks) {
        if (nilai.isEmpty()) return
        Card(Modifier.fillMaxWidth().clickable(enabled = url.isNotEmpty()) { bukaTautan(c, url) }, shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Kartu), elevation = CardDefaults.cardElevation(1.dp)) {
            Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(warna.copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(ikon, null, tint = warna)
                }
                Column(Modifier.weight(1f)) {
                    Text(judul, fontSize = 11.sp, color = Abu)
                    Text(nilai, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Teks)
                }
                if (url.isNotEmpty()) Icon(Icons.Filled.ChevronRight, null, tint = Abu)
            }
        }
    }
    val wa = s.optString("wa")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        baris(Icons.Filled.LocationOn, "Alamat", s.optString("alamat"), if (s.optString("peta").startsWith("https://")) s.optString("peta") else "", Merah)
        baris(Icons.Filled.Schedule, "Jam layanan", s.optString("jam"), "")
        baris(Icons.Filled.Phone, "Telepon", s.optString("telp"), if (s.optString("telp").isNotEmpty()) "tel:" + s.optString("telp").replace(" ", "") else "")
        baris(Icons.Filled.Chat, "WhatsApp (chat dengan pengurus)", wa, waLink(wa, profil), Hijau)
        baris(Icons.Filled.Email, "Email", s.optString("email"), if (s.optString("email").isNotEmpty()) "mailto:" + s.optString("email") else "")
        baris(Icons.Filled.PhotoCamera, "Instagram", namaAkun(s.optString("ig")), s.optString("ig"), Color(0xFFC13584))
        baris(Icons.Filled.PlayCircle, "YouTube", namaAkun(s.optString("yt")), s.optString("yt"), Merah)
        baris(Icons.Filled.MusicNote, "TikTok", namaAkun(s.optString("tt")), s.optString("tt"), Color(0xFF111111))
        if (listOf("alamat", "telp", "wa", "email", "ig", "yt", "tt").all { s.optString(it).isEmpty() })
            Text("Kontak belum diisi oleh admin.", color = Abu)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun Galeri(a: JSONArray?) {
    val semua = a.daftar().filter { it.optString("foto").startsWith("https://") }
    val album = semua.groupBy { it.optString("album") + "|" + it.optString("tanggal") }.values.toList()
    var terbuka by remember { mutableStateOf<List<JSONObject>?>(null) }
    var besar by remember { mutableStateOf<String?>(null) }
    if (album.isEmpty()) { Text("Belum ada foto galeri.", Modifier.padding(16.dp), color = Color.Gray); return }
    LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsGrid(album) { fs ->
            Card(Modifier.fillMaxWidth().clickable { terbuka = fs }, colors = CardDefaults.cardColors(containerColor = Kartu)) {
                Column {
                    AsyncImage(fotoBn(fs[0].optString("foto"), 600), null, Modifier.fillMaxWidth().height(110.dp), contentScale = ContentScale.Crop)
                    Column(Modifier.padding(8.dp)) {
                        Text(fs[0].optString("album"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Teks, maxLines = 2)
                        Text(fs[0].optString("tanggal") + " • ${fs.size} foto", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
    terbuka?.let { fs ->
        AlertDialog(onDismissRequest = { terbuka = null }, confirmButton = { TextButton({ terbuka = null }) { Text("Tutup") } },
            title = { Text(fs[0].optString("album")) },
            text = {
                Column {
                    if (fs[0].optString("ket").isNotEmpty()) Text(fs[0].optString("ket"), fontSize = 12.sp, color = Color.DarkGray)
                    LazyVerticalGrid(GridCells.Fixed(3), Modifier.heightIn(max = 380.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemsGrid(fs) { f ->
                            AsyncImage(fotoBn(f.optString("foto"), 500), null, Modifier.aspectRatio(1f).clip(RoundedCornerShape(6.dp)).clickable { besar = fotoBn(f.optString("foto"), 1800) }, contentScale = ContentScale.Crop)
                        }
                    }
                }
            })
    }
    besar?.let { u ->
        AlertDialog(onDismissRequest = { besar = null }, confirmButton = { TextButton({ besar = null }) { Text("Tutup") } },
            text = { AsyncImage(u, null, Modifier.fillMaxWidth(), contentScale = ContentScale.Fit) })
    }
}

/** Memakai data galeri dari infoPublikApp; bila server lama belum memuatnya, panggil getGaleri langsung. */
@Composable
fun GaleriLayar(awal: JSONArray?) {
    var data by remember { mutableStateOf(awal) }
    var galat by remember { mutableStateOf("") }
    var muat by remember { mutableStateOf(awal == null) }
    LaunchedEffect(Unit) {
        if (awal == null) {
            try {
                val r = Api.panggil("getGaleri") as? JSONArray
                val h = JSONArray()
                r.daftar().forEach { g ->
                    h.put(JSONObject().put("album", g.optString("ALBUM")).put("tanggal", g.optString("TANGGAL")).put("ket", g.optString("KETERANGAN")).put("foto", g.optString("FOTO_URL")))
                }
                data = h
            } catch (e: Exception) { galat = e.message ?: "Gagal memuat galeri." }
            muat = false
        }
    }
    when {
        muat -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Teks) }
        galat.isNotEmpty() -> Text(galat, Modifier.padding(16.dp), color = Merah)
        else -> Galeri(data)
    }
}

fun waLink(wa: String, profil: JSONObject): String {
    if (wa.isEmpty()) return ""
    val pesan = "Assalamu'alaikum, saya ${profil.optString("nama")} (NPA ${profil.optString("npa")}), ${profil.optString("ranting")}. Saya ingin bertanya: "
    return "https://wa.me/$wa?text=" + java.net.URLEncoder.encode(pesan, "UTF-8")
}

@Composable
fun PollingLayar(token: String) {
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<JSONArray?>(null) }
    var galat by remember { mutableStateOf("") }
    fun muat() { scope.launch { try { data = Api.panggil("pollingSaya", token) as? JSONArray ?: JSONArray(); galat = "" } catch (e: Exception) { galat = e.message ?: "Gagal memuat polling." } } }
    LaunchedEffect(Unit) { muat() }
    val list = data.daftar()
    when {
        galat.isNotEmpty() -> Text(galat, Modifier.padding(16.dp), color = Merah)
        data == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Navy) }
        list.isEmpty() -> Text("Belum ada polling.", Modifier.padding(16.dp), color = Abu)
        else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list) { p -> KartuPolling(p, token) { muat() } }
        }
    }
}

@Composable
fun KartuPolling(p: JSONObject, token: String, segarkan: () -> Unit) {
    val scope = rememberCoroutineScope()
    val tipe = p.optString("tipe"); val ganda = tipe == "Pilihan Ganda"; val isian = tipe == "Isian Singkat"
    val aktif = p.optBoolean("aktif"); val sudah = p.optBoolean("sudah")
    val opsi = p.optJSONArray("opsi"); val hasil = p.optJSONArray("hasil")
    var pilih by remember { mutableStateOf(setOf<Int>()) }
    var teks by remember { mutableStateOf("") }
    var pesan by remember { mutableStateOf("") }
    var sibuk by remember { mutableStateOf(false) }
    val jml = (0 until (opsi?.length() ?: 0)).map { hasil?.optInt(it) ?: 0 }
    val total = jml.sum()
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Kartu), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(p.optString("judul"), fontWeight = FontWeight.Bold, color = Teks, modifier = Modifier.weight(1f))
                Text(if (aktif) "Dibuka" else "Ditutup", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (aktif) HijauGelap else Abu,
                    modifier = Modifier.background(if (aktif) TintHijau else TintAbu, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp))
            }
            if (p.optString("deskripsi").isNotEmpty()) Text(p.optString("deskripsi"), fontSize = 13.sp)
            if (p.optString("tutup").isNotEmpty()) Text("Ditutup: " + p.optString("tutup"), fontSize = 11.sp, color = Abu)
            val tampilHasil = !isian && (sudah || !aktif) && opsi != null
            if (tampilHasil) {
                for (i in 0 until opsi!!.length()) {
                    val n = jml.getOrElse(i) { 0 }
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(opsi.optString(i), fontSize = 13.sp); Text("$n", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)).background(TintAbu)) {
                            Box(Modifier.fillMaxWidth(if (total > 0) (n.toFloat() / total).coerceIn(0.02f, 1f) else 0f).fillMaxHeight().background(Hijau))
                        }
                    }
                }
                if (sudah) Text("✓ Suara Anda sudah tercatat.", fontSize = 12.sp, color = Hijau, fontWeight = FontWeight.SemiBold)
            } else if (aktif) {
                if (isian) OutlinedTextField(teks, { teks = it.take(500) }, label = { Text("Jawaban Anda") }, modifier = Modifier.fillMaxWidth())
                else if (opsi != null) for (i in 0 until opsi.length()) {
                    Row(Modifier.fillMaxWidth().clickable { pilih = if (ganda) (if (pilih.contains(i)) pilih - i else pilih + i) else setOf(i) }, verticalAlignment = Alignment.CenterVertically) {
                        if (ganda) Checkbox(pilih.contains(i), { pilih = if (it) pilih + i else pilih - i }) else RadioButton(pilih.contains(i), { pilih = setOf(i) })
                        Text(opsi.optString(i), fontSize = 14.sp)
                    }
                }
                Button(onClick = {
                    sibuk = true; pesan = ""
                    scope.launch {
                        try {
                            val arg: Any = if (isian) teks else if (ganda) JSONArray().also { a -> pilih.sorted().forEach { a.put(it) } } else pilih.first()
                            Api.panggil("suaraSaya", token, p.optString("id"), arg)
                            segarkan()
                        } catch (e: Exception) { pesan = e.message ?: "Gagal mengirim suara."; sibuk = false }
                    }
                }, enabled = !sibuk && (if (isian) teks.isNotBlank() else pilih.isNotEmpty()), colors = ButtonDefaults.buttonColors(containerColor = Navy), shape = RoundedCornerShape(12.dp)) {
                    Text(if (sibuk) "Mengirim…" else "Kirim Suara")
                }
                if (pesan.isNotEmpty()) Text(pesan, color = Merah, fontSize = 12.sp)
            } else if (isian) Text("Jawaban isian tidak ditampilkan.", fontSize = 12.sp, color = Abu)
        }
    }
}
