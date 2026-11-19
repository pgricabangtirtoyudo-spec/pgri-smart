package id.or.pgricabangtirtoyudo.smart

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

class NativeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notif.buatKanal(this)
        setContent {
            TemaPgri {
                val minta = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33) minta.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                Aplikasi(
                    bukaWeb = { url -> startActivity(Intent(this, MainActivity::class.java).apply { data = Uri.parse(url) }) }
                )
            }
        }
    }
}
