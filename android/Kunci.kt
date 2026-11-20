package id.or.pgricabangtirtoyudo.smart

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Pengaturan aplikasi + kunci sidik jari / PIN HP. */
object Kunci {
    private fun p(c: Context) = c.getSharedPreferences("pgri_set", Context.MODE_PRIVATE)
    fun aktif(c: Context) = p(c).getBoolean("kunci", false)
    fun setAktif(c: Context, v: Boolean) { p(c).edit().putBoolean("kunci", v).apply() }
    fun mode(c: Context): String = p(c).getString("tampilan", "sistem") ?: "sistem"
    fun setMode(c: Context, v: String) { p(c).edit().putString("tampilan", v).apply() }

    private fun auth(): Int = if (Build.VERSION.SDK_INT >= 30) (BIOMETRIC_WEAK or DEVICE_CREDENTIAL) else BIOMETRIC_WEAK
    fun tersedia(c: Context): Boolean = BiometricManager.from(c).canAuthenticate(auth()) == BiometricManager.BIOMETRIC_SUCCESS

    /** Tampilkan dialog sidik jari / PIN. hasil(true) bila lolos. */
    fun minta(a: FragmentActivity, judul: String, hasil: (Boolean) -> Unit) {
        try {
            val cb = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(r: BiometricPrompt.AuthenticationResult) { hasil(true) }
                override fun onAuthenticationError(code: Int, msg: CharSequence) { hasil(false) }
            }
            val bp = BiometricPrompt(a, ContextCompat.getMainExecutor(a), cb)
            val b = BiometricPrompt.PromptInfo.Builder().setTitle(judul).setSubtitle("PGRI SMART").setAllowedAuthenticators(auth())
            if (Build.VERSION.SDK_INT < 30) b.setNegativeButtonText("Batal")
            bp.authenticate(b.build())
        } catch (e: Exception) { hasil(false) }
    }
}
