package io.github.koss.mammut.feature.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import saschpe.android.customtabs.CustomTabsHelper

class MammutWebViewFallback(private val redirectUri: String?) : CustomTabsHelper.CustomTabFallback {

    /**
     * @param context The [Context] that wants to open the Uri
     * @param uri The [Uri] to be opened by the fallback
     */
    override fun openUri(context: Context?, uri: Uri?) {
        (context as? Activity)?.startActivityForResult(
                Intent(context, MammutWebViewActivity::class.java)
                        .putExtra(MammutWebViewActivity.EXTRA_URL, uri.toString())
                        .putExtra(MammutWebViewActivity.EXTRA_RESULT_URL, redirectUri),
                RESULT_CODE
        )
    }

    companion object {
        val RESULT_CODE = 12
    }
}