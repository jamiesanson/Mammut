package io.github.koss.mammut.feature.webview

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import io.github.koss.mammut.R

class MammutWebViewActivity: AppCompatActivity(R.layout.mammut_web_view_activity) {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra(EXTRA_TITLE)
        val url = intent.getStringExtra(EXTRA_URL)!!
        val redirectUrl = intent.getStringExtra(EXTRA_RESULT_URL)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            if (title != null) {
                actionBar.title = title
                actionBar.subtitle = url
            } else {
                actionBar.title = url
            }
        }

        val webView = findViewById<WebView>(R.id.web_view)
        webView.loadUrl(url)
        webView.settings.javaScriptEnabled = true

        // No title provided. Use the website's once it's loaded...
        if (title == null) {
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    if (redirectUrl != null && request?.url?.toString()?.startsWith(redirectUrl) == true) {
                        setResult(MammutWebViewFallback.RESULT_CODE, Intent(Intent.ACTION_VIEW, request.url))
                        finish()
                        return true
                    }

                    return false
                }
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    if (actionBar != null) {
                        actionBar.title = view.title
                        actionBar.subtitle = url
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        /**
         * Optional title resource for the actionbar / toolbar.
         */
        val EXTRA_TITLE = "${MammutWebViewActivity::class.java.name}.EXTRA_TITLE"

        /**
         * Mandatory file to load and display.
         */
        val EXTRA_URL = "${MammutWebViewActivity::class.java.name}.EXTRA_URL"

        /**
         * The result URL to return when redirected to
         */
        val EXTRA_RESULT_URL = "${MammutWebViewActivity::class.java.name}.EXTRA_RESULT_URL"
    }
}