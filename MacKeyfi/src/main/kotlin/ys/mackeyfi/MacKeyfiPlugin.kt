package ys.mackeyfi

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class MacKeyfiPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(MacKeyfi())
    }
}
