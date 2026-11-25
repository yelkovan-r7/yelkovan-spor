package ys.trgoals

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class TRGoalsPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(TRGoals())
    }
}
