package ys.vegoltv

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class VegolTVPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(VegolTV())
    }
}
