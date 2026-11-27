package ys.nebulasports

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class NebulaSportsPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(NebulaSports())
    }
}
