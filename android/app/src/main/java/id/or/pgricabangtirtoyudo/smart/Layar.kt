package id.or.pgricabangtirtoyudo.smart

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun Aplikasi(bukaWeb: (String) -> Unit) {
    val c = LocalContext.current
    var token by remember { mutableStateOf(Sesi.token(c)) }
    var profil by remember { mutableStateOf<JSONObject?>(null) }
    if (token.isEmpty()) {
        LayarMasuk { t, p -> Sesi.simpan(c, t, p.optString("npa")); profil = p; token = t }
    } else {
        Utama(token, profil, bukaWeb) { Sesi.hapusToken(c); token = ""; profil = null }
    }
}

// ───────────────────────── MASUK ─────────────────────────
@Composable
fun LayarMasuk(sukses: (String, JSONObject) -> Unit) {
    val scope = rememberCoroutineScope()
    var npa by remember { mutableStateOf("") }
    var sandi by remember { mutableStateOf("") }
    var lihat by remember { mutableStateOf(false) }
    var muat by remember { mutableStateOf(false) }
    var galat by remember { mutableStateOf("") }
    fun masuk() {
        if (muat || npa.isBlank() || sandi.isEmpty()) return
        galat = ""; muat = true
        scope.launch {
            try {
                val r = Api.panggil("loginAnggota", npa.trim(), sandi).obj()
                sukses(r.optString("token"), r.optJSONObject("profil") ?: JSONObject())
            } catch (e: Exception) { galat = e.message ?: "Gagal masuk." }
            muat = false
        }
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A2552), Navy, NavyTerang)))) {
        Box(Modifier.align(Alignment.TopEnd).offset(80.dp, (-60).dp).size(260.dp).background(Color.White.copy(alpha = 0.05f), CircleShape))
        Box(Modifier.align(Alignment.BottomStart).offset((-90).dp, 70.dp).size(300.dp).background(Color.White.copy(alpha = 0.05f), CircleShape))
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoPgri(104.dp, Modifier.shadow(12.dp, CircleShape))
            Spacer(Modifier.height(16.dp))
            Text("PGRI SMART", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            Text("PGRI Cabang Tirtoyudo", color = Color(0xFFCFD8EC), fontSize = 14.sp)
            Spacer(Modifier.height(26.dp))
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(10.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Masuk Anggota", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text("Gunakan NPA dan sandi akun anggota Anda.", fontSize = 13.sp, color = Abu)
                    OutlinedTextField(npa, { npa = it.filter { ch -> ch.isDigit() } }, label = { Text("NPA") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp), leadingIcon = { Icon(Icons.Filled.Badge, null, tint = Navy) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(sandi, { sandi = it }, label = { Text("Sandi") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp), leadingIcon = { Icon(Icons.Filled.Lock, null, tint = Navy) },
                        trailingIcon = { IconButton({ lihat = !lihat }) { Icon(if (lihat) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Lihat sandi") } },
                        visualTransformation = if (lihat) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                    if (galat.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().background(Color(0xFFFFEDEE), RoundedCornerShape(10.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ErrorOutline, null, tint = Merah, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                            Text(galat, color = Merah, fontSize = 13.sp)
                        }
                    }
                    Button(
                        onClick = { masuk() }, enabled = !muat && npa.isNotBlank() && sandi.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy)
                    ) {
                        if (muat) { CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)) }
                        Text(if (muat) "Memeriksa…" else "Masuk", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Sandi awal: 123456. Segera ganti setelah masuk.", fontSize = 12.sp, color = Abu, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Persatuan Guru Republik Indonesia", color = Color(0xFF9FB1D6), fontSize = 12.sp)
        }
    }
}

// ───────────────────────── UTAMA ─────────────────────────
fun hitungIuran(b: JSONObject): IntArray {
    val bulan = b.optJSONObject("bulan") ?: JSONObject()
    val sek = b.optJSONObject("sekarang") ?: JSONObject()
    val y = sek.optInt("y"); val m = sek.optInt("m"); val tahun = b.optInt("tahun")
    var lunas = 0; var belum = 0; var jumlah = 0
    for (i in 1..12) {
        val wajib = tahun < y || (tahun == y && i <= m)
        if (bulan.has("$i")) { lunas++; jumlah += bulan.optInt("$i") } else if (wajib) belum++
    }
    return intArrayOf(lunas, belum, jumlah)
}

@Composable
fun Utama(token: String, profil0: JSONObject?, bukaWeb: (String) -> Unit, keluar: () -> Unit) {
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var sub by remember { mutableStateOf<String?>(null) }
    var beranda by remember { mutableStateOf<JSONObject?>(null) }
    var hadir by remember { mutableStateOf<JSONObject?>(null) }
    var pesan by remember { mutableStateOf<JSONObject?>(null) }
    var info by remember { mutableStateOf<JSONObject?>(null) }
    var foto by remember { mutableStateOf("") }
    var galat by remember { mutableStateOf("") }
    var muat by remember { mutableStateOf(true) }
    var tahun by remember { mutableIntStateOf(0) }

    fun sesiHabis(e: Exception): Boolean {
        val m = (e.message ?: "").lowercase()
        return m.contains("sesi") || m.contains("masuk kembali") || m.contains("masuk lagi")
    }

    fun muatUlang() {
        scope.launch {
            muat = true; galat = ""
            try {
                val b = Api.panggil("getBerandaSaya", token, if (tahun == 0) null else tahun).obj()
                beranda = b
                if (tahun == 0) tahun = b.optJSONObject("sekarang")?.optInt("y") ?: 0
                hadir = Api.panggil("riwayatHadirSaya", token, if (tahun == 0) null else tahun).obj()
                pesan = Api.panggil("pesanSaya", token).obj()
                try { info = Api.panggil("infoPublikApp").obj() } catch (_: Exception) {}
                try { foto = Api.panggil("getFotoSaya", token).obj().optString("foto") } catch (_: Exception) {}
                try {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { t ->
                        scope.launch { try { Api.panggil("daftarPerangkat", token, t) } catch (_: Exception) {} }
                    }
                } catch (_: Exception) {}
            } catch (e: Exception) {
                if (sesiHabis(e)) keluar() else galat = e.message ?: "Gagal memuat."
            }
            muat = false
        }
    }
    LaunchedEffect(tahun) { muatUlang() }
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val ob = androidx.lifecycle.LifecycleEventObserver { _, ev -> if (ev == androidx.lifecycle.Lifecycle.Event.ON_RESUME && beranda != null) muatUlang() }
        owner.lifecycle.addObserver(ob); onDispose { owner.lifecycle.removeObserver(ob) }
    }
    BackHandler(enabled = sub != null || tab != 0) { if (sub != null) sub = null else tab = 0 }

    val profil = beranda?.optJSONObject("profil") ?: profil0 ?: JSONObject()
    val baru = pesan?.optInt("baru") ?: 0

    fun keluarAkun() {
        scope.launch {
            try { com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { t -> scope.launch { try { Api.panggil("hapusPerangkat", token, t) } catch (_: Exception) {} } } } catch (_: Exception) {}
            try { Api.panggil("logoutAnggota", token) } catch (_: Exception) {}
            keluar()
        }
    }

    Scaffold(
        containerColor = Latar,
        topBar = {
            Surface(color = Navy, shadowElevation = 4.dp) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    LogoPgri(38.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("PGRI SMART", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, letterSpacing = 0.5.sp)
                        Text("Cabang Tirtoyudo", color = Color(0xFFB9C7E6), fontSize = 11.sp)
                    }
                    IconButton({ muatUlang() }) { Icon(Icons.Filled.Refresh, "Muat ulang", tint = Color.White) }
                    IconButton({ sub = "pesan" }) {
                        BadgedBox({ if (baru > 0) Badge(containerColor = Merah) { Text("$baru") } }) { Icon(Icons.Filled.Notifications, "Pesan", tint = Color.White) }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                val item = listOf(
                    Triple("Beranda", Icons.Filled.Home, 0), Triple("Info", Icons.Filled.Info, 1), Triple("Absen", Icons.Filled.QrCodeScanner, 2),
                    Triple("Aduan", Icons.Filled.Campaign, 3), Triple("Akun", Icons.Filled.Person, 4)
                )
                item.forEach { (n, ik, i) ->
                    NavigationBarItem(tab == i && sub == null, { tab = i; sub = null }, { Icon(ik, n) }, label = { Text(n, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Navy, selectedTextColor = Navy, indicatorColor = Color(0xFFDCE6FA)))
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                beranda == null && muat -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Navy); Spacer(Modifier.height(10.dp)); Text("Memuat data…", color = Abu)
                }
                beranda == null -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CloudOff, null, tint = Abu, modifier = Modifier.size(48.dp)); Spacer(Modifier.height(8.dp))
                    Text(galat.ifEmpty { "Gagal memuat." }, color = Merah, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp)); Button({ muatUlang() }, colors = ButtonDefaults.buttonColors(containerColor = Navy)) { Text("Coba lagi") }
                }
                sub == "iuran" -> LayarIuran(beranda!!, tahun, { tahun = it }) { sub = null }
                sub == "hadir" -> LayarHadir(hadir, tahun, { tahun = it }) { sub = null }
                sub == "pesan" -> LayarPesan(pesan, token, { sub = null }) { scope.launch { try { pesan = Api.panggil("pesanSaya", token).obj() } catch (_: Exception) {} } }
                else -> when (tab) {
                    0 -> Beranda(profil, foto, beranda!!, info, baru, { foto = it }, token, { tab = it }, { sub = it })
                    1 -> TabInfo(info, token)
                    2 -> TabAbsen(token) { muatUlang() }
                    3 -> TabAduan(beranda!!, token) { muatUlang() }
                    else -> TabAkun(profil, foto, token, { foto = it }, bukaWeb) { keluarAkun() }
                }
            }
            if (muat && beranda != null) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter), color = Merah, trackColor = Color.Transparent)
        }
    }
}

// ───────────────────────── BERANDA ─────────────────────────
@Composable
fun Beranda(profil: JSONObject, foto: String, b: JSONObject, info: JSONObject?, pesanBaru: Int, setFoto: (String) -> Unit, token: String, setTab: (Int) -> Unit, setSub: (String) -> Unit) {
    val nama = profil.optString("nama")
    val r = hitungIuran(b)
    var qrBesar by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box {
            Box(Modifier.fillMaxWidth().height(130.dp).background(Brush.verticalGradient(listOf(Navy, Color(0xFF184894)))))
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Selamat datang,", color = Color(0xFFCFD8EC), fontSize = 13.sp)
                Text(nama.split(" ").firstOrNull()?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(14.dp))
                KartuAnggota(profil, foto, { setTab(4) }, { qrBesar = true })
            }
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Spacer(Modifier.height(2.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Aksi("Absen", Icons.Filled.QrCodeScanner, Hijau) { setTab(2) }
                Aksi("Iuran", Icons.Filled.Payments, Navy) { setSub("iuran") }
                Aksi("Kehadiran", Icons.Filled.EventAvailable, Navy) { setSub("hadir") }
                Aksi("Aduan", Icons.Filled.Campaign, Merah) { setTab(3) }
                Aksi("Pesan", Icons.Filled.Mail, Navy, pesanBaru) { setSub("pesan") }
            }
            // ringkasan iuran
            val wajib = r[0] + r[1]
            Card(Modifier.fillMaxWidth().clickable { setSub("iuran") }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Status Iuran ${b.optInt("tahun")}", fontWeight = FontWeight.Bold, color = Navy)
                        Text(if (r[1] == 0) "Lunas sampai bulan ini" else "${r[1]} bulan belum tercatat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (r[1] == 0) Hijau else Merah)
                    }
                    LinearProgressIndicator(progress = { if (wajib > 0) r[0].toFloat() / wajib else 0f }, Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = Hijau, trackColor = Color(0xFFE3E8F3))
                    Text("${r[0]} bulan lunas • ${rupiah(b.optInt("tarif"))} per bulan", fontSize = 12.sp, color = Abu)
                }
            }
            // berita terbaru
            val berita = info?.optJSONArray("berita").daftar().take(2)
            if (berita.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Berita Terbaru", fontWeight = FontWeight.Bold, color = Navy, fontSize = 16.sp)
                    TextButton({ setTab(1) }) { Text("Lihat semua", color = Merah, fontSize = 13.sp) }
                }
                berita.forEach { n ->
                    Card(Modifier.fillMaxWidth().clickable { setTab(1) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val f = n.optString("foto")
                            Box(Modifier.size(78.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE3E8F3))) {
                                if (f.startsWith("https://")) AsyncImage(fotoBn(f, 500), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(n.optString("judul"), fontWeight = FontWeight.Bold, color = Navy, fontSize = 14.sp, maxLines = 2)
                                Text(n.optString("tanggal"), fontSize = 11.sp, color = Abu)
                                Text(n.optString("ringkasan"), fontSize = 12.sp, maxLines = 2, color = Color(0xFF33405C))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
    if (qrBesar) {
        val npa = profil.optString("npa")
        AlertDialog(onDismissRequest = { qrBesar = false }, confirmButton = { TextButton({ qrBesar = false }) { Text("Tutup") } },
            title = { Text("Kode QR Anggota") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Image(buatQr(Konfig.URL_VERIFIKASI + npa, 700).asImageBitmap(), "QR", Modifier.size(260.dp))
                    Text(profil.optString("nama"), fontWeight = FontWeight.Bold, color = Navy)
                    Text("NPA $npa", fontSize = 13.sp, color = Abu)
                }
            })
    }
}

@Composable
fun Aksi(judul: String, ikon: androidx.compose.ui.graphics.vector.ImageVector, warna: Color, badge: Int = 0, klik: () -> Unit) {
    Column(Modifier.width(62.dp).clickable(onClick = klik), horizontalAlignment = Alignment.CenterHorizontally) {
        BadgedBox({ if (badge > 0) Badge(containerColor = Merah) { Text("$badge") } }) {
            Box(Modifier.size(52.dp).shadow(3.dp, RoundedCornerShape(16.dp)).background(Color.White, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Icon(ikon, judul, tint = warna, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(judul, fontSize = 11.sp, color = Navy, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** Kartu anggota: sama seperti di web (gradien, logo, QR kanan atas, foto kotak, pita bawah). */
@Composable
fun KartuAnggota(p: JSONObject, foto: String, ubahFoto: () -> Unit, besarkanQr: () -> Unit) {
    val npa = p.optString("npa")
    val bentuk = RoundedCornerShape(18.dp)
    Box(
        Modifier.fillMaxWidth().shadow(12.dp, bentuk).clip(bentuk)
            .background(Brush.linearGradient(listOf(Navy, NavyTerang, Color(0xFF763769))))
    ) {
        Box(Modifier.align(Alignment.TopEnd).offset(50.dp, (-50).dp).size(170.dp).background(Color.White.copy(alpha = 0.07f), CircleShape))
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)) {
            Row(Modifier.padding(end = 76.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LogoPgri(42.dp)
                Column {
                    Text("KARTU ANGGOTA", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
                    Text("PGRI CABANG TIRTOYUDO", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                FotoKotak(foto, p.optString("nama"), 84, ubahFoto)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(p.optString("nama"), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 20.sp)
                    Text("NPA $npa", color = Color.White.copy(alpha = 0.95f), fontSize = 13.sp, letterSpacing = 0.6.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(p.optString("ranting"), color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp)
                    Text(p.optString("tempat").ifEmpty { "-" }, color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, lineHeight = 15.sp)
                    if (p.optString("status").isNotEmpty()) Text(p.optString("status"), color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Persatuan Guru Republik Indonesia", color = Color.White.copy(alpha = 0.75f), fontSize = 10.5.sp)
        }
        if (npa.isNotEmpty()) {
            Box(Modifier.align(Alignment.TopEnd).padding(14.dp).background(Color.White, RoundedCornerShape(8.dp)).padding(4.dp).clickable { besarkanQr() }) {
                Image(buatQr(Konfig.URL_VERIFIKASI + npa).asImageBitmap(), "QR anggota", Modifier.size(56.dp))
            }
        }
    }
}

@Composable
fun FotoKotak(foto: String, nama: String, ukuran: Int, klik: (() -> Unit)?) {
    val bmp = remember(foto) { fotoDariData(foto) }
    Box(Modifier.size(ukuran.dp)) {
        Box(
            Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(Color(0xFF2A63B8)).border(BorderStroke(2.dp, Color.White.copy(alpha = 0.9f)), RoundedCornerShape(10.dp))
                .then(if (klik != null) Modifier.clickable { klik() } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            when {
                bmp != null -> Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                foto.startsWith("https://") -> AsyncImage(fotoBn(foto, 400), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else -> Text(inisial(nama), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = (ukuran / 3.2).sp)
            }
        }
        if (klik != null) Box(Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).size(24.dp).background(Color(0xFFE5384A), CircleShape).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(13.dp))
        }
    }
}

// ───────────────────────── SUB-LAYAR ─────────────────────────
@Composable
fun HeaderSub(judul: String, kembali: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(kembali) { Icon(Icons.Filled.ArrowBack, "Kembali", tint = Navy) }
        Text(judul, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy)
    }
}

@Composable
fun SelectorTahun(tahun: Int, ubah: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        IconButton({ ubah(tahun - 1) }) { Icon(Icons.Filled.ChevronLeft, "Sebelumnya", tint = Navy) }
        Text("$tahun", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Navy, modifier = Modifier.padding(horizontal = 12.dp))
        IconButton({ ubah(tahun + 1) }) { Icon(Icons.Filled.ChevronRight, "Berikutnya", tint = Navy) }
    }
}

@Composable
fun KotakAngka(judul: String, nilai: String, warna: Color, mod: Modifier) {
    Card(mod, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(judul, fontSize = 11.sp, color = Abu)
            Text(nilai, fontWeight = FontWeight.ExtraBold, color = warna, fontSize = 15.sp)
        }
    }
}

@Composable
fun LayarIuran(b: JSONObject, tahun: Int, ubah: (Int) -> Unit, kembali: () -> Unit) {
    val bulan = b.optJSONObject("bulan") ?: JSONObject()
    val sek = b.optJSONObject("sekarang") ?: JSONObject()
    val y = sek.optInt("y"); val m = sek.optInt("m")
    val r = hitungIuran(b)
    Column(Modifier.fillMaxSize()) {
        HeaderSub("Status Iuran", kembali)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SelectorTahun(tahun, ubah)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KotakAngka("Lunas", "${r[0]} bulan", Hijau, Modifier.weight(1f))
                KotakAngka("Belum", "${r[1]} bulan", Merah, Modifier.weight(1f))
                KotakAngka("Iuran/bulan", rupiah(b.optInt("tarif")), Navy, Modifier.weight(1.2f))
            }
            (1..12).chunked(3).forEach { baris ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    baris.forEach { i ->
                        val ada = bulan.has("$i"); val wajib = tahun < y || (tahun == y && i <= m)
                        val (teks, fg, bg) = when {
                            ada -> Triple("Lunas", Hijau, Color(0xFFE6F6EC))
                            wajib -> Triple("Belum", Merah, Color(0xFFFFEDEE))
                            else -> Triple("—", Abu, Color(0xFFEDF0F6))
                        }
                        Column(Modifier.weight(1f).background(bg, RoundedCornerShape(14.dp)).padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(BULAN[i - 1], fontWeight = FontWeight.Bold, color = Navy)
                            Text(teks, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
                        }
                    }
                }
            }
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Total tercatat: ${rupiah(r[2])}", fontWeight = FontWeight.Bold, color = Navy)
                    if (r[1] > 0) Text("Belum tercatat sampai bulan ini: ${r[1]} bulan (${rupiah(r[1] * b.optInt("tarif"))})", color = Merah, fontSize = 13.sp)
                    Text("Status mengikuti catatan bendahara. Bila ada yang tidak sesuai, hubungi pengurus ranting.", fontSize = 12.sp, color = Abu)
                }
            }
        }
    }
}

@Composable
fun LayarHadir(h: JSONObject?, tahun: Int, ubah: (Int) -> Unit, kembali: () -> Unit) {
    val list = h?.optJSONArray("list").daftar().filter { it.optBoolean("hadir") }
    val total = h?.optInt("total") ?: 0; val n = h?.optInt("hadir") ?: 0
    Column(Modifier.fillMaxSize()) {
        HeaderSub("Riwayat Kehadiran", kembali)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SelectorTahun(tahun, ubah)
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Navy)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("Kehadiran $tahun", color = Color(0xFFCFD8EC), fontSize = 12.sp); Text("$n dari $total kegiatan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                    Text("${h?.optInt("persen") ?: 0}%", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                }
            }
            if (list.isEmpty()) Text("Belum ada kegiatan yang diikuti pada tahun ini.", color = Abu)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list) { k ->
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Hijau)
                            Column {
                                Text(k.optString("judul"), fontWeight = FontWeight.SemiBold, color = Navy)
                                Text(k.optString("tanggal") + (k.optString("lokasi").takeIf { it.isNotEmpty() }?.let { " • $it" } ?: ""), fontSize = 12.sp, color = Abu)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LayarPesan(p: JSONObject?, token: String, kembali: () -> Unit, segarkan: () -> Unit) {
    val scope = rememberCoroutineScope()
    val list = p?.optJSONArray("list").daftar()
    Column(Modifier.fillMaxSize()) {
        HeaderSub("Pesan", kembali)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (list.isEmpty()) Text("Belum ada pesan.", color = Abu)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list) { m ->
                    val belum = m.optString("dibaca").isEmpty()
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = if (belum) Color(0xFFEAF1FF) else Color.White),
                        onClick = { if (belum) scope.launch { try { Api.panggil("tandaiPesanDibaca", token, m.optString("id")) } catch (_: Exception) {}; segarkan() } }) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text((if (belum) "● " else "") + m.optString("judul"), fontWeight = FontWeight.Bold, color = Navy)
                            Text(m.optString("isi"), fontSize = 14.sp)
                            Text(m.optString("pengirim") + " • " + m.optString("tanggal"), fontSize = 11.sp, color = Abu)
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── AKUN ─────────────────────────
@Composable
fun TabAkun(profil: JSONObject, foto: String, token: String, setFoto: (String) -> Unit, bukaWeb: (String) -> Unit, keluar: () -> Unit) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    var pesan by remember { mutableStateOf("") }
    var dialogSandi by remember { mutableStateOf(false) }
    var sibuk by remember { mutableStateOf(false) }
    val pilihFoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bmp = bacaBitmap(c, uri)
        if (bmp == null) { pesan = "Foto tidak dapat dibaca."; return@rememberLauncherForActivityResult }
        val kotak = potongPersegi(bmp, 256)
        val b64 = jpegBase64(kotak, 40000)
        if (b64 == null) { pesan = "Foto terlalu besar, coba foto lain."; return@rememberLauncherForActivityResult }
        sibuk = true; pesan = "Menyimpan foto…"
        scope.launch {
            try { val r = Api.panggil("simpanFotoSaya", token, "data:image/jpeg;base64,$b64").obj(); setFoto(r.optString("foto")); pesan = "✓ Foto profil tersimpan." }
            catch (e: Exception) { pesan = e.message ?: "Gagal menyimpan foto." }
            sibuk = false
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                FotoKotak(foto, profil.optString("nama"), 72, { pilihFoto.launch("image/*") })
                Column {
                    Text(profil.optString("nama"), fontWeight = FontWeight.ExtraBold, color = Navy, fontSize = 16.sp)
                    Text("NPA ${profil.optString("npa")}", fontSize = 13.sp, color = Abu)
                    Text(profil.optString("tempat"), fontSize = 12.sp, color = Abu)
                }
            }
        }
        if (pesan.isNotEmpty()) Text(pesan, fontSize = 13.sp, color = if (pesan.startsWith("✓") || pesan.startsWith("Menyimpan")) Hijau else Merah)
        BarisAkun(Icons.Filled.PhotoCamera, "Ganti foto profil", "Foto dipotong persegi otomatis") { if (!sibuk) pilihFoto.launch("image/*") }
        if (foto.isNotEmpty()) BarisAkun(Icons.Filled.Delete, "Hapus foto profil", "Kembali ke inisial nama") {
            scope.launch { try { Api.panggil("hapusFotoSaya", token); setFoto(""); pesan = "✓ Foto profil dihapus." } catch (e: Exception) { pesan = e.message ?: "Gagal." } }
        }
        BarisAkun(Icons.Filled.Lock, "Ganti sandi", "Disarankan mengganti sandi awal") { dialogSandi = true }
        BarisAkun(Icons.Filled.Language, "Buka versi lengkap", "Situs PGRI SMART di peramban") { bukaWeb("https://smart.pgricabangtirtoyudo.or.id/") }
        BarisAkun(Icons.Filled.Logout, "Keluar", "Akhiri sesi di perangkat ini", Merah) { keluar() }
        Text("PGRI SMART Tirtoyudo", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 11.sp, color = Abu)
    }
    if (dialogSandi) DialogSandi(token, { dialogSandi = false }) { pesan = "✓ Sandi berhasil diganti." }
}

@Composable
fun BarisAkun(ikon: androidx.compose.ui.graphics.vector.ImageVector, judul: String, ket: String, warna: Color = Navy, klik: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = klik), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(warna.copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(ikon, null, tint = warna) }
            Column(Modifier.weight(1f)) { Text(judul, fontWeight = FontWeight.SemiBold, color = warna); Text(ket, fontSize = 12.sp, color = Abu) }
            Icon(Icons.Filled.ChevronRight, null, tint = Abu)
        }
    }
}

@Composable
fun DialogSandi(token: String, tutup: () -> Unit, selesai: () -> Unit) {
    val scope = rememberCoroutineScope()
    var lama by remember { mutableStateOf("") }; var baru by remember { mutableStateOf("") }; var ulang by remember { mutableStateOf("") }
    var galat by remember { mutableStateOf("") }; var sibuk by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = tutup,
        title = { Text("Ganti Sandi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(lama, { lama = it }, label = { Text("Sandi lama") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(baru, { baru = it }, label = { Text("Sandi baru (min. 6 karakter)") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(ulang, { ulang = it }, label = { Text("Ulangi sandi baru") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                if (galat.isNotEmpty()) Text(galat, color = Merah, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(enabled = !sibuk, onClick = {
                if (lama.isEmpty() || baru.isEmpty()) { galat = "Lengkapi semua isian."; return@TextButton }
                if (baru != ulang) { galat = "Ulangi sandi baru dengan sama persis."; return@TextButton }
                sibuk = true; galat = ""
                scope.launch {
                    try { Api.panggil("ubahSandiAnggota", token, lama, baru); selesai(); tutup() } catch (e: Exception) { galat = e.message ?: "Gagal."; sibuk = false }
                }
            }) { Text(if (sibuk) "Menyimpan…" else "Simpan") }
        },
        dismissButton = { TextButton(tutup) { Text("Batal") } })
}
