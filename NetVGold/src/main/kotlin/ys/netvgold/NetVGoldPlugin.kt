package ys.netvgold

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class NetVGoldPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(NetVGold())
    }
}
