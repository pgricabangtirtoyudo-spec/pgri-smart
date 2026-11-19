package id.or.pgricabangtirtoyudo.smart

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.delay

class NativeActivity : FragmentActivity() {
    private var tujuan by mutableStateOf("")
    private var terkunci by mutableStateOf(false)
    private var terakhirKeluar = 0L

    private fun petaTujuan(i: Intent?): String {
        val u = (i?.getStringExtra("url") ?: i?.data?.toString() ?: "").lowercase()
        return when {
            u.contains("aduan") -> "aduan"
            u.contains("agenda") -> "agenda"
            u.contains("pesan") -> "pesan"
            u.contains("iuran") -> "iuran"
            else -> ""
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notif.buatKanal(this)
        Pal.mode = Kunci.mode(this)
        tujuan = petaTujuan(intent)
        terkunci = Kunci.aktif(this) && Sesi.token(this).isNotEmpty() && Kunci.tersedia(this)
        cekPembaruan()
        setContent {
            val sis = isSystemInDarkTheme()
            SideEffect { Pal.sistemGelap = sis }
            TemaPgri {
                val minta = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33) minta.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                var splash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) { delay(1200); splash = false }
                Box(Modifier.fillMaxSize()) {
                    Aplikasi(
                        bukaWeb = { url -> startActivity(Intent(this@NativeActivity, MainActivity::class.java).apply { data = Uri.parse(url) }) },
                        tujuan = tujuan, tujuanSelesai = { tujuan = "" }
                    )
                    if (terkunci) LayarKunci { Kunci.minta(this@NativeActivity, "Buka PGRI SMART") { ok -> if (ok) terkunci = false } }
                    if (splash) LayarPembuka()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        tujuan = petaTujuan(intent)
    }

    override fun onStop() { super.onStop(); terakhirKeluar = SystemClock.elapsedRealtime() }

    override fun onStart() {
        super.onStart()
        if (terakhirKeluar > 0 && Kunci.aktif(this) && Sesi.token(this).isNotEmpty() && Kunci.tersedia(this) &&
            SystemClock.elapsedRealtime() - terakhirKeluar > 30_000) terkunci = true
    }

    /** Bila ada versi baru di Play Store, tawarkan pembaruan langsung. */
    private fun cekPembaruan() {
        try {
            val m = AppUpdateManagerFactory.create(this)
            m.appUpdateInfo.addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    try { @Suppress("DEPRECATION") m.startUpdateFlowForResult(info, AppUpdateType.IMMEDIATE, this, 7001) } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
    }
}

@Composable
fun LayarPembuka() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val versi = try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "" } catch (e: Exception) { "" }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A2552), Navy, NavyTerang))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoPgri(120.dp)
            Spacer(Modifier.height(18.dp))
            Text("PGRI SMART", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            Text("PGRI Cabang Tirtoyudo", color = Color(0xFFCFD8EC), fontSize = 14.sp)
        }
        Text(if (versi.isNotEmpty()) "Versi $versi" else "", color = Color(0xFF9FB1D6), fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp))
    }
}

@Composable
fun LayarKunci(buka: () -> Unit) {
    LaunchedEffect(Unit) { buka() }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A2552), Navy, NavyTerang))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LogoPgri(96.dp)
            Text("Aplikasi Terkunci", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text("Gunakan sidik jari atau PIN layar HP untuk membuka.", color = Color(0xFFCFD8EC), fontSize = 13.sp)
            Button(buka, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Navy)) {
                Icon(Icons.Filled.Fingerprint, null); Spacer(Modifier.width(8.dp)); Text("Buka Kunci", fontWeight = FontWeight.Bold)
            }
        }
    }
}
