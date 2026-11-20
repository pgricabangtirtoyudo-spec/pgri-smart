package id.or.pgricabangtirtoyudo.smart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val URL_WEB_ADMIN = "https://smart.pgricabangtirtoyudo.or.id/"

private val LABEL_MENU = mapOf("dash" to "Dashboard", "keuangan" to "Kas & Keuangan", "iuran" to "Iuran", "pesan" to "Pesan", "anggota" to "Data Anggota", "lembaga" to "Lembaga",
    "pengumuman" to "Pengumuman", "berita" to "Berita", "agenda" to "Agenda", "galeri" to "Galeri", "absen" to "Absensi", "aduan" to "Aduan", "surat" to "Surat",
    "dokumen" to "Dokumen", "notulen" to "Notulen", "agp" to "AGP", "rka" to "RKA", "pengurus" to "Pengurus")

private val NAMA_BULAN = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

/** Panel admin native: Ringkasan, Bukti Iuran, Aduan, Aktivitas, Perangkat. Hak akses ditentukan server. */
@Composable
fun PanelAdmin(info: JSONObject, bukaWeb: (String) -> Unit, keluar: () -> Unit) {
    val token = info.optString("token")
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf("ringkas") }
    var ringkas by remember { mutableStateOf<JSONObject?>(null) }
    var galat by remember { mutableStateOf("") }
    var muat by remember { mutableStateOf(true) }
    var tutup by remember { mutableStateOf(false) }

    fun keluarAman() {
        if (tutup) return
        tutup = true
        scope.launch { try { Api.panggil("logoutAdmin", token) } catch (e: Exception) { } ; keluar() }
    }
    fun salahSesi(m: String): Boolean {
        if (m.contains("Sesi", true) && (m.contains("berakhir", true) || m.contains("tidak valid", true) || m.contains("login", true))) {
            tutup = true; keluar(); return true
        }
        return false
    }
    fun muatRingkas() {
        muat = true; galat = ""
        scope.launch {
            try { ringkas = Api.panggil("adminRingkas", token).obj() }
            catch (e: Exception) { val m = e.message ?: "Gagal memuat."; if (!salahSesi(m)) galat = m }
            muat = false
        }
    }
    LaunchedEffect(Unit) { muatRingkas() }

    val hak = ringkas?.optJSONObject("hak")
    val role = info.optString("role")
    val bolehBukti = hak?.optBoolean("buktiIuran") == true
    val bolehAduan = hak?.optBoolean("aduan") == true
    val bolehTanggapi = hak?.optBoolean("aduanTanggapi") == true
    val bolehHadir = hak?.optBoolean("kehadiranRanting") == true
    val ranting = hak?.optString("ranting").orEmpty()

    Column(Modifier.fillMaxSize().background(Latar)) {
        Surface(color = Navy) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                LogoPgri(36.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Panel Admin", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Text(info.optString("nama").ifBlank { "Admin" } + (if (role == "ranting") " · Ranting " + ranting.ifBlank { info.optString("ranting") } else if (info.optString("peran").isNotBlank()) " · " + info.optString("peran") else if (role == "super") " · Admin Cabang" else ""),
                        color = Color(0xFFCBD5E8), fontSize = 12.sp, maxLines = 1)
                }
                TextButton(onClick = { keluarAman() }) { Icon(Icons.Filled.Logout, null, tint = Color.White); Spacer(Modifier.width(4.dp)); Text("Keluar", color = Color.White) }
            }
        }
        val tabs = buildList {
            add("ringkas" to "Ringkasan")
            if (bolehBukti) add("bukti" to "Bukti Iuran")
            if (bolehAduan) add("aduan" to "Aduan")
            if (bolehHadir) add("hadir" to "Kehadiran")
        }
        Row(Modifier.fillMaxWidth().background(Kartu).hScroll().padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tabs.forEach { (k, n) ->
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if (tab == k) Navy else TintAbu).clickable { tab = k }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text(n, color = if (tab == k) Color.White else Teks, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                "bukti" -> AdminBukti(token, ::salahSesi, { muatRingkas() })
                "aduan" -> AdminAduan(token, bolehTanggapi, ::salahSesi, { muatRingkas() })
                "hadir" -> AdminHadir(token, ::salahSesi)
                else -> AdminRingkasan(ringkas, role, ranting, info.optJSONArray("menu"), muat, galat, { muatRingkas() }, { tab = it }, { u -> bukaWeb(u + "#tk=" + token) })
            }
        }
    }
}

@Composable
private fun Modifier.hScroll(): Modifier = this.horizontalScroll(rememberScrollState())

@Composable
private fun KotakAngka(judul: String, nilai: String, warna: Color, modifier: Modifier = Modifier, klik: (() -> Unit)? = null) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Kartu).let { if (klik != null) it.clickable { klik() } else it }.padding(16.dp)) {
        Text(nilai, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = warna)
        Text(judul, fontSize = 12.sp, color = Abu)
    }
}

@Composable
private fun AdminRingkasan(r: JSONObject?, role: String, ranting: String, menu: org.json.JSONArray?, muat: Boolean, galat: String, ulang: () -> Unit, ke: (String) -> Unit, bukaWeb: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (muat && r == null) { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Navy) } }
        if (galat.isNotEmpty()) {
            Text(galat, color = Merah, fontSize = 13.sp)
            OutlinedButton(onClick = ulang) { Text("Coba lagi") }
        }
        if (r != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KotakAngka(if (role == "ranting") "Anggota ranting" else "Total anggota", r.optInt("anggota").toString(), Navy, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (r.has("buktiMenunggu")) KotakAngka("Bukti iuran menunggu", r.optInt("buktiMenunggu").toString(), TeksKuning, Modifier.weight(1f), { ke("bukti") })
                if (r.has("aduanBaru")) KotakAngka("Aduan baru", r.optInt("aduanBaru").toString(), Merah, Modifier.weight(1f), { ke("aduan") })
            }
        }
        val web = (0 until (menu?.length() ?: 0)).mapNotNull { LABEL_MENU[menu!!.optString(it)] }
        if (web.isNotEmpty()) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kartu).padding(14.dp)) {
            Text("Menu peran Anda di panel web", fontWeight = FontWeight.Bold, color = Teks, fontSize = 14.sp)
            Text(web.joinToString(" · "), color = Abu, fontSize = 13.sp)
        }
        OutlinedButton(onClick = { bukaWeb(URL_WEB_ADMIN) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Filled.OpenInBrowser, null); Spacer(Modifier.width(8.dp)); Text("Buka panel web lengkap")
        }
        Text("Fitur lanjutan sesuai peran Anda tersedia di panel web.", color = Abu, fontSize = 12.sp)
    }
}

@Composable
private fun Memuat(muat: Boolean, galat: String, kosong: Boolean, pesanKosong: String, ulang: () -> Unit, isi: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        if (muat) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Navy)
        if (galat.isNotEmpty()) Column(Modifier.padding(16.dp)) { Text(galat, color = Merah, fontSize = 13.sp); OutlinedButton(onClick = ulang) { Text("Coba lagi") } }
        else if (!muat && kosong) Text(pesanKosong, color = Abu, modifier = Modifier.padding(24.dp))
        else isi()
    }
}

@Composable
private fun AdminBukti(token: String, sesiHabis: (String) -> Boolean, berubah: () -> Unit) {
    val scope = rememberCoroutineScope()
    var daftar by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var muat by remember { mutableStateOf(true) }
    var galat by remember { mutableStateOf("") }
    var pilih by remember { mutableStateOf<JSONObject?>(null) }
    var filter by remember { mutableStateOf("Menunggu") }
    fun muatUlang() {
        muat = true; galat = ""
        scope.launch {
            try { daftar = Api.panggil("adminBukti", token).let { it as? org.json.JSONArray }.daftar() }
            catch (e: Exception) { val m = e.message ?: "Gagal."; if (!sesiHabis(m)) galat = m }
            muat = false
        }
    }
    LaunchedEffect(Unit) { muatUlang() }
    val tampil = daftar.filter { filter == "Semua" || it.optString("status") == filter }
    Memuat(muat, galat, tampil.isEmpty(), "Tidak ada bukti dengan status $filter.", { muatUlang() }) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Menunggu", "Disetujui", "Ditolak", "Semua").forEach { f ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f) })
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tampil.forEach { b ->
                    val st = b.optString("status")
                    val (fg, bg) = when (st) { "Disetujui" -> HijauGelap to TintHijau; "Ditolak" -> Merah to TintMerah; else -> TeksKuning to TintKuning }
                    val bln = b.optJSONArray("bulan")?.let { a -> (0 until a.length()).map { NAMA_BULAN.getOrElse(a.optInt(it) - 1) { "?" } } }?.joinToString(", ") ?: ""
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kartu).clickable { pilih = b }.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(b.optString("nama"), fontWeight = FontWeight.Bold, color = Teks, modifier = Modifier.weight(1f))
                            Text(st, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                        Text("NPA ${b.optString("npa")} · ${b.optInt("tahun")} · $bln", color = Abu, fontSize = 12.sp)
                        Text(rupiah(b.optInt("jumlah")), color = Teks, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    pilih?.let { b ->
        var catatan by remember(b) { mutableStateOf("") }
        var proses by remember(b) { mutableStateOf(false) }
        var pesan by remember(b) { mutableStateOf("") }
        val menunggu = b.optString("status") == "Menunggu"
        fun putus(s: String) {
            if (proses) return
            proses = true; pesan = ""
            scope.launch {
                try { Api.panggil("adminPutus", token, b.optString("id"), s, catatan.trim()); pilih = null; muatUlang(); berubah() }
                catch (e: Exception) { val m = e.message ?: "Gagal."; if (!sesiHabis(m)) pesan = m }
                proses = false
            }
        }
        AlertDialog(onDismissRequest = { if (!proses) pilih = null }, containerColor = Kartu,
            title = { Text(b.optString("nama"), color = Teks) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("NPA ${b.optString("npa")} · ${rupiah(b.optInt("jumlah"))}", color = Abu, fontSize = 13.sp)
                    val u = b.optString("bukti")
                    if (u.startsWith("https://")) AsyncImage(fotoBn(u, 1000), null, Modifier.fillMaxWidth().heightIn(max = 360.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Fit)
                    if (b.optString("catatan").isNotBlank()) Text("Catatan: " + b.optString("catatan"), color = Abu, fontSize = 12.sp)
                    if (menunggu) OutlinedTextField(catatan, { catatan = it }, label = { Text("Catatan (wajib jika menolak)") }, modifier = Modifier.fillMaxWidth())
                    if (pesan.isNotEmpty()) Text(pesan, color = Merah, fontSize = 12.sp)
                }
            },
            confirmButton = {
                if (menunggu) Button(onClick = { putus("Disetujui") }, enabled = !proses, colors = ButtonDefaults.buttonColors(containerColor = Hijau)) { Text("Setujui") }
                else TextButton(onClick = { pilih = null }) { Text("Tutup") }
            },
            dismissButton = {
                if (menunggu) Row {
                    TextButton(onClick = { putus("Ditolak") }, enabled = !proses && catatan.isNotBlank()) { Text("Tolak", color = Merah) }
                    TextButton(onClick = { pilih = null }, enabled = !proses) { Text("Batal") }
                }
            })
    }
}

@Composable
private fun AdminAduan(token: String, bolehTanggapi: Boolean, sesiHabis: (String) -> Boolean, berubah: () -> Unit) {
    val scope = rememberCoroutineScope()
    var daftar by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var muat by remember { mutableStateOf(true) }
    var galat by remember { mutableStateOf("") }
    var pilih by remember { mutableStateOf<JSONObject?>(null) }
    fun muatUlang() {
        muat = true; galat = ""
        scope.launch {
            try { daftar = Api.panggil("adminAduan", token).let { it as? org.json.JSONArray }.daftar() }
            catch (e: Exception) { val m = e.message ?: "Gagal."; if (!sesiHabis(m)) galat = m }
            muat = false
        }
    }
    LaunchedEffect(Unit) { muatUlang() }
    Memuat(muat, galat, daftar.isEmpty(), "Belum ada aduan.", { muatUlang() }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            daftar.forEach { a ->
                val (fg, bg) = warnaStatus(a.optString("status"))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kartu).clickable { pilih = a }.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(a.optString("judul"), fontWeight = FontWeight.Bold, color = Teks, modifier = Modifier.weight(1f), maxLines = 2)
                        Text(a.optString("status"), color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                    Text("${a.optString("nama")} · ${a.optString("ranting")} · ${a.optString("tanggal")}", color = Abu, fontSize = 12.sp)
                    Text(a.optString("isi"), color = Teks, fontSize = 13.sp, maxLines = 2)
                }
            }
        }
    }
    pilih?.let { a ->
        var status by remember(a) { mutableStateOf(a.optString("status").ifBlank { "Baru" }) }
        var tanggapan by remember(a) { mutableStateOf(a.optString("tanggapan")) }
        var proses by remember(a) { mutableStateOf(false) }
        var pesan by remember(a) { mutableStateOf("") }
        AlertDialog(onDismissRequest = { if (!proses) pilih = null }, containerColor = Kartu,
            title = { Text(a.optString("judul"), color = Teks) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${a.optString("nama")} (NPA ${a.optString("npa")}) · ${a.optString("kategori")}", color = Abu, fontSize = 12.sp)
                    Text(a.optString("isi"), color = Teks, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.hScroll()) {
                        if (bolehTanggapi) listOf("Baru", "Diproses", "Selesai", "Ditolak").forEach { s ->
                            FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s) })
                        }
                    }
                    if (bolehTanggapi) OutlinedTextField(tanggapan, { tanggapan = it }, label = { Text("Tanggapan untuk anggota") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    else Text(if (tanggapan.isBlank()) "Peran Anda hanya dapat melihat aduan." else "Tanggapan: $tanggapan", color = Abu, fontSize = 13.sp)
                    if (pesan.isNotEmpty()) Text(pesan, color = Merah, fontSize = 12.sp)
                }
            },
            confirmButton = {
                if (!bolehTanggapi) TextButton(onClick = { pilih = null }) { Text("Tutup") } else
                Button(enabled = !proses, colors = ButtonDefaults.buttonColors(containerColor = Navy), onClick = {
                    proses = true; pesan = ""
                    scope.launch {
                        try { Api.panggil("adminTanggapi", token, a.optString("id"), status, tanggapan.trim()); pilih = null; muatUlang(); berubah() }
                        catch (e: Exception) { val m = e.message ?: "Gagal."; if (!sesiHabis(m)) pesan = m }
                        proses = false
                    }
                }) { Text(if (proses) "Menyimpan…" else "Simpan") }
            },
            dismissButton = { TextButton(onClick = { pilih = null }, enabled = !proses) { Text("Batal") } })
    }
}

@Composable
private fun AdminHadir(token: String, sesiHabis: (String) -> Boolean) {
    val scope = rememberCoroutineScope()
    var d by remember { mutableStateOf<JSONObject?>(null) }
    var muat by remember { mutableStateOf(true) }
    var galat by remember { mutableStateOf("") }
    var pilih by remember { mutableStateOf<JSONObject?>(null) }
    var rinci by remember { mutableStateOf<JSONObject?>(null) }
    fun muatUlang() {
        muat = true; galat = ""
        scope.launch {
            try { d = Api.panggil("adminKegiatan", token).obj() }
            catch (e: Exception) { val m = e.message ?: "Gagal."; if (!sesiHabis(m)) galat = m }
            muat = false
        }
    }
    LaunchedEffect(Unit) { muatUlang() }
    LaunchedEffect(pilih) {
        rinci = null
        val k = pilih ?: return@LaunchedEffect
        try { rinci = Api.panggil("adminAbsensi", token, k.optString("id")).obj() } catch (e: Exception) { sesiHabis(e.message ?: "") }
    }
    val daftar = d?.optJSONArray("list").daftar()
    Memuat(muat, galat, daftar.isEmpty(), "Belum ada kegiatan.", { muatUlang() }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Anggota ranting: ${d?.optInt("total") ?: 0}", color = Abu, fontSize = 13.sp)
            daftar.forEach { k ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kartu).clickable { pilih = k }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(k.optString("judul"), fontWeight = FontWeight.Bold, color = Teks)
                        Text("${k.optString("tanggal")} · ${k.optString("lokasi")}", color = Abu, fontSize = 12.sp)
                    }
                    Text("${k.optInt("n")} hadir", color = HijauGelap, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
    pilih?.let { k ->
        AlertDialog(onDismissRequest = { pilih = null }, containerColor = Kartu, title = { Text(k.optString("judul"), color = Teks) },
            text = {
                val r = rinci
                if (r == null) CircularProgressIndicator(color = Navy) else Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val h = r.optJSONArray("hadir").daftar(); val b = r.optJSONArray("belum").daftar()
                    Text("Hadir (${h.size})", fontWeight = FontWeight.Bold, color = HijauGelap)
                    h.forEach { Text("• " + it.optString("nama"), color = Teks, fontSize = 13.sp) }
                    Text("Belum hadir (${b.size})", fontWeight = FontWeight.Bold, color = Merah, modifier = Modifier.padding(top = 8.dp))
                    b.forEach { Text("• " + it.optString("nama"), color = Teks, fontSize = 13.sp) }
                }
            }, confirmButton = { TextButton(onClick = { pilih = null }) { Text("Tutup") } })
    }
}
