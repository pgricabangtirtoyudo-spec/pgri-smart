package id.or.pgricabangtirtoyudo.smart

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
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
    private var chooserCb: ValueCallback<Array<Uri>>? = null
    private var izinMenunggu: PermissionRequest? = null

    private val pilihFile = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        chooserCb?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(r.resultCode, r.data))
        chooserCb = null
    }
    private val izinKamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        val p = izinMenunggu
        izinMenunggu = null
        if (p != null) {
            if (ok) p.grant(p.resources) else p.deny()
        }
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
                    pilihFile.launch(params.createIntent())
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
            val dari = intent?.data
            val awal = if (dari != null && dalam(dari.host) && dari.scheme == "https") dari.toString() else APP_URL
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
