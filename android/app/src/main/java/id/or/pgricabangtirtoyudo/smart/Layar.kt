package id.or.pgricabangtirtoyudo.smart

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AColor
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import org.json.JSONObject

val Navy = Color(0xFF0D2F68)
val Merah = Color(0xFFD3202F)
val Hijau = Color(0xFF1FA557)
val Latar = Color(0xFFF3F5FA)
val BULAN = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

@Composable
fun TemaPgri(isi: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Navy, secondary = Merah, background = Latar, surface = Color.White)) { isi() }
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

fun rapi(s: String): String = s.trim()

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

@Composable
fun LayarMasuk(sukses: (String, JSONObject) -> Unit) {
    val scope = rememberCoroutineScope()
    var npa by remember { mutableStateOf("") }
    var sandi by remember { mutableStateOf("") }
    var muat by remember { mutableStateOf(false) }
    var galat by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Navy, Color(0xFF16408A)))).padding(24.dp).statusBarsPadding(),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("PGRI SMART", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Cabang Tirtoyudo", color = Color(0xFFCFD8EC), fontSize = 14.sp)
        Spacer(Modifier.height(28.dp))
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Masuk Anggota", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy)
                OutlinedTextField(npa, { npa = it }, label = { Text("NPA") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(sandi, { sandi = it }, label = { Text("Sandi") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                if (galat.isNotEmpty()) Text(galat, color = Merah, fontSize = 13.sp)
                Button(
                    onClick = {
                        galat = ""; muat = true
                        scope.launch {
                            try {
                                val r = Api.panggil("loginAnggota", npa.trim(), sandi).obj()
                                sukses(r.optString("token"), r.optJSONObject("profil") ?: JSONObject())
                            } catch (e: Exception) { galat = e.message ?: "Gagal masuk." }
                            muat = false
                        }
                    },
                    enabled = !muat && npa.isNotBlank() && sandi.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)
                ) { Text(if (muat) "Memeriksa…" else "Masuk") }
                Text("Sandi awal: 123456 (segera ganti setelah masuk).", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Utama(token: String, profil0: JSONObject?, bukaWeb: (String) -> Unit, keluar: () -> Unit) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var beranda by remember { mutableStateOf<JSONObject?>(null) }
    var hadir by remember { mutableStateOf<JSONObject?>(null) }
    var pesan by remember { mutableStateOf<JSONObject?>(null) }
    var info by remember { mutableStateOf<JSONObject?>(null) }
    var foto by remember { mutableStateOf<Bitmap?>(null) }
    var galat by remember { mutableStateOf("") }
    var muat by remember { mutableStateOf(true) }
    var tahun by remember { mutableIntStateOf(0) }

    fun sesiHabis(e: Exception): Boolean {
        val m = (e.message ?: "").lowercase()
        return m.contains("sesi") || m.contains("masuk lagi") || m.contains("kedaluwarsa")
    }

    fun muatUlang() {
        scope.launch {
            muat = true; galat = ""
            try {
                val b = Api.panggil("getBerandaSaya", token, if (tahun == 0) null else tahun).obj()
                beranda = b; if (tahun == 0) tahun = b.optJSONObject("sekarang")?.optInt("y") ?: 0
                hadir = Api.panggil("riwayatHadirSaya", token, tahun).obj()
                pesan = Api.panggil("pesanSaya", token).obj()
                try { info = Api.panggil("infoPublikApp").obj() } catch (_: Exception) {}
                try { foto = fotoDariData(Api.panggil("getFotoSaya", token).obj().optString("foto")) } catch (_: Exception) {}
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

    val profil = beranda?.optJSONObject("profil") ?: profil0 ?: JSONObject()
    val baru = pesan?.optInt("baru") ?: 0

    Scaffold(
        containerColor = Latar,
        topBar = {
            TopAppBar(
                title = { Text("PGRI SMART", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, titleContentColor = Color.White, actionIconContentColor = Color.White),
                actions = {
                    IconButton({ muatUlang() }) { Icon(Icons.Filled.Refresh, "Muat ulang") }
                    IconButton({
                        scope.launch {
                            try { com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { t -> scope.launch { try { Api.panggil("hapusPerangkat", token, t) } catch (_: Exception) {} } } } catch (_: Exception) {}
                            try { Api.panggil("logoutAnggota", token) } catch (_: Exception) {}
                            keluar()
                        }
                    }) { Icon(Icons.Filled.Logout, "Keluar") }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Filled.Badge, null) }, label = { Text("Kartu") })
                NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Filled.Payments, null) }, label = { Text("Iuran") })
                NavigationBarItem(tab == 2, { tab = 2 }, { Icon(Icons.Filled.EventAvailable, null) }, label = { Text("Hadir") })
                NavigationBarItem(tab == 3, { tab = 3 }, { Icon(Icons.Filled.Info, null) }, label = { Text("Info") })
                NavigationBarItem(tab == 4, { tab = 4 }, {
                    BadgedBox({ if (baru > 0) Badge { Text("$baru") } }) { Icon(Icons.Filled.Mail, null) }
                }, label = { Text("Pesan") })
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                beranda == null && muat -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                beranda == null -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(galat.ifEmpty { "Gagal memuat." }, color = Merah)
                    Spacer(Modifier.height(12.dp)); Button({ muatUlang() }) { Text("Coba lagi") }
                }
                else -> when (tab) {
                    0 -> TabKartu(profil, foto, bukaWeb)
                    1 -> TabIuran(beranda!!, tahun) { tahun = it }
                    2 -> TabHadir(hadir, tahun) { tahun = it }
                    3 -> TabInfo(info)
                    else -> TabPesan(pesan, token) { scope.launch { try { pesan = Api.panggil("pesanSaya", token).obj() } catch (_: Exception) {} } }
                }
            }
            if (muat && beranda != null) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
}

@Composable
fun TabKartu(p: JSONObject, foto: Bitmap?, bukaWeb: (String) -> Unit) {
    val npa = p.optString("npa")
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(6.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Column {
                Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Navy, Color(0xFF1B4DA0)))).padding(16.dp)) {
                    Column(Modifier.padding(end = 70.dp)) {
                        Text("KARTU ANGGOTA", color = Color(0xFFFFD6DA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("PGRI Cabang Tirtoyudo", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    if (npa.isNotEmpty()) Image(buatQr(Konfig.URL_VERIFIKASI + npa).asImageBitmap(), "QR", Modifier.size(56.dp).align(Alignment.TopEnd).clip(RoundedCornerShape(6.dp)).background(Color.White))
                }
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(84.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE3E8F3)), contentAlignment = Alignment.Center) {
                        if (foto != null) Image(foto.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Icon(Icons.Filled.Person, null, tint = Color.Gray, modifier = Modifier.size(44.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(rapi(p.optString("nama")), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy)
                        Text("NPA: " + p.optString("npa").ifEmpty { "-" }, fontSize = 13.sp)
                        Text(p.optString("status"), fontSize = 12.sp, color = Color.DarkGray)
                        Text(p.optString("tempat"), fontSize = 12.sp, color = Color.DarkGray)
                        Text("Ranting " + p.optString("ranting"), fontSize = 12.sp, color = Color.DarkGray)
                    }
                }
                Box(Modifier.fillMaxWidth().background(Merah).padding(6.dp), contentAlignment = Alignment.Center) {
                    Text("Persatuan Guru Republik Indonesia", color = Color.White, fontSize = 11.sp)
                }
            }
        }
        Text("Ketuk tombol di bawah untuk fitur yang belum tersedia di aplikasi ini (absen QR, aduan, agenda, ubah sandi/foto).", fontSize = 12.sp, color = Color.Gray)
        OutlinedButton({ bukaWeb("https://smart.pgricabangtirtoyudo.or.id/") }, Modifier.fillMaxWidth()) { Text("Buka versi lengkap") }
    }
}

@Composable
fun SelectorTahun(tahun: Int, ubah: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        IconButton({ ubah(tahun - 1) }) { Icon(Icons.Filled.ChevronLeft, "Sebelumnya") }
        Text("$tahun", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy)
        IconButton({ ubah(tahun + 1) }) { Icon(Icons.Filled.ChevronRight, "Berikutnya") }
    }
}

@Composable
fun TabIuran(b: JSONObject, tahun: Int, ubah: (Int) -> Unit) {
    val bulan = b.optJSONObject("bulan") ?: JSONObject()
    val sek = b.optJSONObject("sekarang") ?: JSONObject()
    val y = sek.optInt("y"); val m = sek.optInt("m")
    val tarif = b.optInt("tarif")
    var lunas = 0; var belum = 0
    for (i in 1..12) {
        val wajib = tahun < y || (tahun == y && i <= m)
        if (bulan.has("$i")) lunas++ else if (wajib) belum++
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SelectorTahun(tahun, ubah)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Ringkas("Lunas", "$lunas bln", Hijau, Modifier.weight(1f))
            Ringkas("Belum", "$belum bln", Merah, Modifier.weight(1f))
            Ringkas("Tarif", "Rp " + "%,d".format(tarif).replace(',', '.'), Navy, Modifier.weight(1f))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items((1..12).toList()) { i ->
                val ada = bulan.has("$i")
                val wajib = tahun < y || (tahun == y && i <= m)
                val (teks, warna) = when { ada -> "Lunas" to Hijau; wajib -> "Belum" to Merah; else -> "—" to Color.Gray }
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(BULAN[i - 1] + " $tahun")
                        Text(teks, color = warna, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun Ringkas(judul: String, nilai: String, warna: Color, mod: Modifier) {
    Card(mod, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(judul, fontSize = 11.sp, color = Color.Gray)
            Text(nilai, fontWeight = FontWeight.Bold, color = warna, fontSize = 14.sp)
        }
    }
}

@Composable
fun TabHadir(h: JSONObject?, tahun: Int, ubah: (Int) -> Unit) {
    val list = h?.optJSONArray("list").daftar().filter { it.optBoolean("hadir") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectorTahun(tahun, ubah)
        Text("Kehadiran: ${h?.optInt("hadir") ?: 0} dari ${h?.optInt("total") ?: 0} kegiatan (${h?.optInt("persen") ?: 0}%)", fontWeight = FontWeight.Bold, color = Navy)
        if (list.isEmpty()) Text("Belum ada kegiatan yang diikuti pada tahun ini.", color = Color.Gray)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(list) { k ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(Hijau))
                        Column {
                            Text(k.optString("judul"), fontWeight = FontWeight.SemiBold)
                            Text(k.optString("tanggal") + (k.optString("lokasi").takeIf { it.isNotEmpty() }?.let { " • $it" } ?: ""), fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabPesan(p: JSONObject?, token: String, segarkan: () -> Unit) {
    val scope = rememberCoroutineScope()
    val list = p?.optJSONArray("list").daftar()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (list.isEmpty()) Text("Belum ada pesan.", color = Color.Gray)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list) { m ->
                val belum = m.optString("dibaca").isEmpty()
                Card(colors = CardDefaults.cardColors(containerColor = if (belum) Color(0xFFEAF1FF) else Color.White),
                    onClick = { if (belum) scope.launch { try { Api.panggil("tandaiPesanDibaca", token, m.optString("id")) } catch (_: Exception) {}; segarkan() } }) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(m.optString("judul"), fontWeight = FontWeight.Bold, color = Navy)
                        Text(m.optString("isi"), fontSize = 14.sp)
                        Text(m.optString("pengirim") + " • " + m.optString("tanggal"), fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
