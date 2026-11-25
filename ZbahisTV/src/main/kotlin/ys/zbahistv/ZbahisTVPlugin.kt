package ys.zbahistv

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class ZbahisTVPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(ZbahisTV())
    }
}
