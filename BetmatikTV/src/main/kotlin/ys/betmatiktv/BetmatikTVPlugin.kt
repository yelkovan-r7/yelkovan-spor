package ys.betmatiktv

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class BetmatikTVPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(BetmatikTV())
    }
}
