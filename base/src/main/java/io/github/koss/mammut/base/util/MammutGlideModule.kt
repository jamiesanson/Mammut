package io.github.koss.mammut.base.util

import android.content.Context
import android.util.Log
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.executor.GlideExecutor
import com.bumptech.glide.module.AppGlideModule
import io.github.koss.mammut.base.BuildConfig

@GlideModule
class MammutGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)
        builder.setLogLevel(if (BuildConfig.DEBUG) Log.VERBOSE else Log.ERROR)
        builder.setSourceExecutor(
            GlideExecutor
                .newSourceBuilder()
                .setUncaughtThrowableStrategy { t: Throwable? ->
                    Log.e("MammutGlideModule", "Glide threw an exception", t)
                }
                .build()
        )
    }
}