package com.dshmulti.app

import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class WebActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "url"
    }

    private lateinit var web: WebView
    private lateinit var title: TextView
    private lateinit var errorView: LinearLayout
    private lateinit var errorMsg: TextView
    private var current = ""

    private fun dp(v: Int) = Ui.dp(this, v)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Ui.BAR
        window.navigationBarColor = Ui.BG

        current = CoreStore.normalize(
            intent?.getStringExtra(EXTRA_URL) ?: CoreStore.last(this) ?: ""
        )
        if (current.isBlank()) {
            finish()
            return
        }

        buildUi()
        load(current)

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
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
            fitsSystemWindows = true
        }

        // 顶栏
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Ui.BAR)
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }
        bar.addView(iconButton("‹") { onBackPressedDispatcher.onBackPressed() })

        title = Ui.text(this, "", 15f, Ui.TEXT, true).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(dp(6), 0, dp(6), 0)
        }
        bar.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        bar.addView(iconButton("⟳") { load(current) })
        bar.addView(iconButton("⇄") { switchDialog() })
        root.addView(bar)

        // 网页 + 错误页
        val frame = FrameLayout(this)

        web = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = false
                allowFileAccess = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            setBackgroundColor(Ui.BG)
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    hideError()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    hideError()
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        showError(error?.description?.toString() ?: "加载失败")
                    }
                }
            }
            webChromeClient = WebChromeClient()
        }
        frame.addView(web, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))

        errorView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Ui.BG)
            visibility = View.GONE
            setPadding(dp(28), dp(28), dp(28), dp(28))
        }
        errorView.addView(Ui.text(this, "连不上这个核心", 18f, Ui.TEXT, true))
        errorMsg = Ui.text(this, "", 13f, Ui.SUB).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(6))
        }
        errorView.addView(errorMsg)
        errorView.addView(pill("重试", true) { load(current) })
        errorView.addView(pill("换个核心", false) { switchDialog() })
        frame.addView(errorView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))

        root.addView(frame, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
    }

    private fun iconButton(label: String, onClick: () -> Unit): TextView =
        Ui.text(this, label, 20f, Ui.TEXT).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener { onClick() }
        }

    private fun pill(label: String, filled: Boolean, onClick: () -> Unit): TextView =
        Ui.text(this, label, 14f, if (filled) 0xFF0B1220.toInt() else Ui.TEXT, true).apply {
            gravity = Gravity.CENTER
            background = Ui.round(
                this@WebActivity, 22,
                if (filled) Ui.ACCENT else Ui.CARD,
                if (filled) 0 else Ui.STROKE, 1
            )
            setPadding(dp(22), dp(11), dp(22), dp(11))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(14)
            layoutParams = lp
            setOnClickListener { onClick() }
        }

    private fun load(url: String) {
        current = url
        CoreStore.setLast(this, url)
        title.text = CoreStore.nameOf(this, url) ?: CoreStore.pretty(url)
        hideError()
        web.loadUrl(url)
    }

    private fun showError(msg: String) {
        errorMsg.text = "${CoreStore.pretty(current)}\n$msg"
        errorView.visibility = View.VISIBLE
    }

    private fun hideError() {
        if (::errorView.isInitialized) errorView.visibility = View.GONE
    }

    private fun switchDialog() {
        val cores = CoreStore.load(this)
        if (cores.isEmpty()) return
        val names = cores.map {
            if (it.url == current) "${it.name}  ✓" else it.name
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("切换核心")
            .setItems(names) { _, which ->
                val core = cores[which]
                if (core.url != current) load(core.url)
            }
            .setNeutralButton("管理核心") { _, _ ->
                finish()
            }
            .show()
    }

    override fun onDestroy() {
        if (::web.isInitialized) {
            web.loadUrl("about:blank")
            web.destroy()
        }
        super.onDestroy()
    }
}
