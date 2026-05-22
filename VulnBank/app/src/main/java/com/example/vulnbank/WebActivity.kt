package com.example.vulnbank

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class WebActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)
        setContentView(webView)

        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = false
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        webView.settings.domStorageEnabled = false

        val url = intent.getStringExtra("url") ?: "https://example.com"
        webView.loadUrl(url)
    }
}
