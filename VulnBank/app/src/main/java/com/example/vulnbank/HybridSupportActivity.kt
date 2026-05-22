package com.example.vulnbank

import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class HybridSupportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val username = Session.username ?: return finish()
        val store = BankStore(this)
        val webView = WebView(this)
        setContentView(webView)
        WebView.setWebContentsDebuggingEnabled(true)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(SupportBridge(store, username), "NeoSupport")
        webView.loadDataWithBaseURL(
            "https://help.vulnbank.local/",
            """
            <html>
              <body style="font-family:sans-serif;background:#f8fafc;padding:24px;color:#111827;">
                <h2>고객센터</h2>
                <p>계좌, 카드, 송금 문제를 빠르게 확인해드릴게요.</p>
                <button style="font-size:18px;padding:14px;border:0;border-radius:12px;background:#2563eb;color:white;"
                  onclick="document.getElementById('r').innerText=NeoSupport.quickCheck()">계좌 상태 확인</button>
                <pre id="r" style="white-space:pre-wrap;background:white;padding:16px;border-radius:12px;"></pre>
              </body>
            </html>
            """.trimIndent(),
            "text/html",
            "UTF-8",
            null
        )
    }

    class SupportBridge(private val store: BankStore, private val username: String) {
        @JavascriptInterface
        fun quickCheck(): String = store.supportDiagnostics(username)
    }
}
