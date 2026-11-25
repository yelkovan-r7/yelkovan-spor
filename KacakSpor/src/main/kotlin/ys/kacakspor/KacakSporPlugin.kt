package ys.kacakspor

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class KacakSporPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(KacakSpor())
    }
}
