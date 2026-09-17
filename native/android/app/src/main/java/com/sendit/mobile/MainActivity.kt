package com.sendit.mobile

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject

open class MainActivity : Activity() {
    protected open val sharing = false
    private var webView: WebView? = null
    private var sessionAtLoad: String? = null
    private val session by lazy { SessionStore(this) }
    private fun trusted(uri: Uri): Boolean = uri.scheme == "https" && uri.authority == Uri.parse(BuildConfig.WEB_ORIGIN).authority

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (sharing) {
            window.setGravity(Gravity.BOTTOM)
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.attributes = window.attributes.apply { dimAmount = 0.22f }
        } else if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
        }
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) || !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            setContentView(TextView(this).apply { text = "Android System WebView를 업데이트한 뒤 다시 시도해 주세요."; setPadding(32, 64, 32, 32) }); return
        }
        val web = WebView(this).also { webView = it }
        web.setBackgroundColor(if (sharing) Color.TRANSPARENT else Color.WHITE)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = false
        val origins = setOf(BuildConfig.WEB_ORIGIN)
        // Origin-scoped messaging; never expose credentials or bridge methods to external pages.
        WebViewCompat.addWebMessageListener(web, "SendITNative", origins) { _, message, sourceOrigin, isMainFrame, _ ->
            if (isMainFrame && trusted(sourceOrigin)) runCatching {
                val data = JSONObject(message.data ?: return@runCatching)
                when (data.optString("type")) {
                    "session" -> { val value = data.getString("value"); session.write(value); sessionAtLoad = value }
                    "close" -> if (sharing) finish()
                    "share-layout" -> if (sharing) {
                        updateShareWindow(data.optBoolean("expanded"))
                    }
                }
            }
        }
        sessionAtLoad = session.read()
        val injection = sessionAtLoad?.let { "localStorage.setItem('sendit-auth', ${JSONObject.quote(it)});" }
            ?: "localStorage.removeItem('sendit-auth');"
        val script = WebViewCompat.addDocumentStartJavaScript(web, injection, origins)
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) { script.remove() }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (trusted(request.url)) return false
                if (!sharing && request.isForMainFrame && request.url.scheme in listOf("https", "http"))
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                return true
            }
        }
        val container = FrameLayout(this).apply {
            setBackgroundColor(if (sharing) Color.TRANSPARENT else Color.rgb(247, 245, 248))
            addView(web, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
        if (!sharing && Build.VERSION.SDK_INT >= 30) {
            container.setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                val keyboard = insets.getInsets(WindowInsets.Type.ime())
                view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, keyboard.bottom))
                WindowInsets.CONSUMED
            }
        }
        setContentView(container)
        if (sharing) container.post { updateShareWindow(false) }
        container.requestApplyInsets()
        val url = if (sharing) {
            val shared = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty().take(10000)
            val title = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty().take(1000)
            Uri.parse(BuildConfig.WEB_ORIGIN + "/share-target").buildUpon()
                .appendQueryParameter("native", "android").appendQueryParameter("text", shared).appendQueryParameter("title", title).build().toString()
        } else BuildConfig.WEB_ORIGIN
        web.loadUrl(url)
    }
    override fun onResume() {
        super.onResume()
        val current = session.read()
        if (current != sessionAtLoad) {
            sessionAtLoad = current
            val js = current?.let { "localStorage.setItem('sendit-auth', ${JSONObject.quote(it)});location.reload();" }
                ?: "localStorage.removeItem('sendit-auth');location.reload();"
            webView?.takeIf { trusted(Uri.parse(it.url.orEmpty())) }?.evaluateJavascript(js, null)
        }
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (!sharing && webView?.canGoBack() == true) webView?.goBack() else finish() }
    override fun onDestroy() { webView?.destroy(); super.onDestroy() }

    private fun updateShareWindow(expanded: Boolean) {
        val density = resources.displayMetrics.density
        val height = if (expanded) maxOf((210 * density).toInt(), (resources.displayMetrics.heightPixels * 0.27).toInt()) else (120 * density).toInt()
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, height)
    }
}

class ShareActivity : MainActivity() { override val sharing = true }
