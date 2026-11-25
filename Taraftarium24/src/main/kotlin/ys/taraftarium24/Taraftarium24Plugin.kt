package ys.taraftarium24

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class Taraftarium24Plugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Taraftarium24())
    }
}
