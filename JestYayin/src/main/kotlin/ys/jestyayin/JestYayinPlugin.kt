package ys.jestyayin

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class JestYayinPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(JestYayin())
    }
}
