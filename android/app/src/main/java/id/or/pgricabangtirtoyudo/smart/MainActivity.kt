package id.or.pgricabangtirtoyudo.smart

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.os.Message
import android.view.View
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class MainActivity : AppCompatActivity() {

    companion object {
        /** Alamat aplikasi. Jika suatu saat alamat Apps Script berubah, cukup ubah index.html di GitHub Pages, bukan APK ini. */
        const val APP_URL = "https://smart.pgricabangtirtoyudo.or.id/"
        private val HOST_DALAM = listOf(
            "smart.pgricabangtirtoyudo.or.id", "script.google.com", "googleusercontent.com", "accounts.google.com"
        )
    }

    private lateinit var web: WebView
    private lateinit var bar: ProgressBar
    private lateinit var err: View
    private var gagal = false
    private var tokenSso: String = ""
    private var chooserCb: ValueCallback<Array<Uri>>? = null
    private var izinMenunggu: PermissionRequest? = null

    private val pilihFile = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        chooserCb?.onReceiveValue(hasilPilih(r.resultCode, r.data))
        chooserCb = null
    }

    /** Ambil semua foto yang dipilih (satu atau banyak), apa pun cara pemilih mengembalikannya. */
    private fun hasilPilih(kode: Int, data: Intent?): Array<Uri>? {
        if (kode != RESULT_OK || data == null) return null
        val daftar = ArrayList<Uri>()
        val klip = data.clipData
        if (klip != null) {
            for (i in 0 until klip.itemCount) klip.getItemAt(i).uri?.let { daftar.add(it) }
        }
        if (daftar.isEmpty()) data.data?.let { daftar.add(it) }
        return if (daftar.isEmpty()) null else daftar.toTypedArray()
    }

    /** Intent pemilih berkas/galeri yang eksplisit (mendukung pilih banyak foto). */
    private fun intentGaleri(params: WebChromeClient.FileChooserParams): Intent {
        val jenis = params.acceptTypes.filter { it.isNotBlank() }
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = if (jenis.size == 1) jenis[0] else "*/*"
            if (jenis.size > 1) putExtra(Intent.EXTRA_MIME_TYPES, jenis.toTypedArray())
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, "Pilih foto")
    }
    private val izinKamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        val p = izinMenunggu
        izinMenunggu = null
        if (p != null) {
            if (ok) p.grant(p.resources) else p.deny()
        }
    }

    /** Hasil pemindaian QR: "" = belum ada, "OK:teks" = berhasil, "ERR:pesan" = gagal/dibatalkan. Dibaca situs lewat PgriNative.hasilQr(). */
    @Volatile private var hasilScan: String = ""

    private val izinNotif = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ambilTokenPush() }

    private fun ambilTokenPush() {
        try {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { t ->
                getSharedPreferences("pgri", MODE_PRIVATE).edit().putString("fcm", t).apply()
            }
        } catch (e: Exception) {
            // Firebase belum terpasang (google-services.json tidak ada): abaikan
        }
    }

    /** Dipanggil dari JavaScript situs (nama: PgriNative). Situs berjalan di dalam iframe, jadi hasil diambil lewat polling. */
    inner class Jembatan {
        @JavascriptInterface
        fun scanQr() {
            hasilScan = ""
            runOnUiThread {
                try {
                    val opsi = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
                    GmsBarcodeScanning.getClient(this@MainActivity, opsi).startScan()
                        .addOnSuccessListener { b -> hasilScan = "OK:" + (b.rawValue ?: "") }
                        .addOnCanceledListener { hasilScan = "ERR:Pemindaian dibatalkan." }
                        .addOnFailureListener { hasilScan = "ERR:Pemindai belum siap. Pastikan Google Play Services aktif, lalu coba lagi." }
                } catch (e: Exception) {
                    hasilScan = "ERR:Pemindai tidak tersedia di perangkat ini."
                }
            }
        }

        @JavascriptInterface
        fun hasilQr(): String {
            val h = hasilScan
            if (h.isNotEmpty()) hasilScan = ""
            return h
        }

        @JavascriptInterface
        fun daftarPush() {
            runOnUiThread {
                Notif.buatKanal(this@MainActivity)
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) izinNotif.launch(Manifest.permission.POST_NOTIFICATIONS) else ambilTokenPush()
            }
        }

        @JavascriptInterface
        fun tokenPush(): String = getSharedPreferences("pgri", MODE_PRIVATE).getString("fcm", "") ?: ""
    }

    /** Buka halaman tertentu dari tautan atau dari notifikasi yang diketuk. */
    private fun bukaDariIntent(i: Intent?) {
        if (i == null) return
        val u = i.getStringExtra("url")?.let { Uri.parse(it) } ?: i.data
        if (u != null && u.scheme == "https" && dalam(u.host)) web.loadUrl(u.toString())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        bukaDariIntent(intent)
    }

    private fun dalam(host: String?): Boolean {
        if (host == null) return false
        return HOST_DALAM.any { host == it || host.endsWith(".$it") }
    }

    private fun bukaLuar(u: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, u))
        } catch (e: ActivityNotFoundException) {
            // tidak ada aplikasi yang bisa membuka; abaikan
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Android 15+ menggambar aplikasi sampai ke bawah status bar; beri ruang agar isi tidak tertutup.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val b = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(b.left, b.top, b.right, b.bottom)
            WindowInsetsCompat.CONSUMED
        }
        Notif.buatKanal(this)
        web = findViewById(R.id.web)
        bar = findViewById(R.id.bar)
        err = findViewById(R.id.err)
        findViewById<Button>(R.id.retry).setOnClickListener {
            gagal = false
            err.visibility = View.GONE
            web.loadUrl(APP_URL)
        }

        val s = web.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.loadWithOverviewMode = true
        s.useWideViewPort = true
        s.setSupportMultipleWindows(true)
        s.javaScriptCanOpenWindowsAutomatically = true
        s.mediaPlaybackRequiresUserGesture = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        // versi aplikasi ikut dikirim, agar situs tahu kapan perlu menawarkan pembaruan
        val versi = try { packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0" } catch (e: Exception) { "1.0.0" }
        s.userAgentString = s.userAgentString + " PGRISmartApp/" + versi
        web.addJavascriptInterface(Jembatan(), "PgriNative")
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)

        web.setDownloadListener { url, _, _, _, _ -> bukaLuar(Uri.parse(url)) }

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                val u = request.url
                val skema = u.scheme ?: ""
                if ((skema == "https" || skema == "http") && dalam(u.host)) return false
                bukaLuar(u)
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                gagal = false
                bar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String?) {
                bar.visibility = View.GONE
                if (!gagal) err.visibility = View.GONE
                if (tokenSso.isNotEmpty()) {   // masuk otomatis dari panel admin aplikasi: titipkan sesi lalu muat ulang sekali
                    val tk = tokenSso; tokenSso = ""
                    view.evaluateJavascript("try{sessionStorage.setItem('tk','$tk')}catch(e){};location.reload()", null)
                }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    gagal = true
                    err.visibility = View.VISIBLE
                }
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                bar.progress = newProgress
            }

            override fun onShowFileChooser(
                webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, params: FileChooserParams
            ): Boolean {
                chooserCb?.onReceiveValue(null)
                chooserCb = filePathCallback
                return try {
                    pilihFile.launch(if (params.isCaptureEnabled) params.createIntent() else intentGaleri(params))
                    true
                } catch (e: ActivityNotFoundException) {
                    chooserCb = null
                    false
                }
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    val butuhKamera = request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                    if (!butuhKamera) {
                        request.deny()
                    } else if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        request.grant(request.resources)
                    } else {
                        izinMenunggu = request
                        izinKamera.launch(Manifest.permission.CAMERA)
                    }
                }
            }

            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                // tautan target=_blank (WhatsApp, Google Drive, dll.) dibuka di aplikasi luar, bukan menggantikan aplikasi ini
                val sementara = WebView(this@MainActivity)
                sementara.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                        bukaLuar(r.url)
                        return true
                    }
                }
                (resultMsg.obj as WebView.WebViewTransport).webView = sementara
                resultMsg.sendToTarget()
                return true
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) {
                    web.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        if (savedInstanceState == null) {
            val dariNotif = intent?.getStringExtra("url")?.let { Uri.parse(it) }
            val dari = dariNotif ?: intent?.data
            val frag = dari?.fragment.orEmpty()
            if (frag.startsWith("tk=") && Regex("[A-Za-z0-9_-]{8,80}").matches(frag.removePrefix("tk="))) tokenSso = frag.removePrefix("tk=")
            val awal = if (dari != null && dalam(dari.host) && dari.scheme == "https") dari.buildUpon().fragment(null).build().toString() else APP_URL
            web.loadUrl(awal)
        }
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
    }

    override fun onPause() {
        web.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        chooserCb?.onReceiveValue(null)
        chooserCb = null
        web.destroy()
        super.onDestroy()
    }
}
