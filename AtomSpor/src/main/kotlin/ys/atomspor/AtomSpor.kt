package ys.atomspor

import com.lagradost.cloudstream3.app
import ys.ortak.*

// Sayfa listeleri ve yayın adresleri "teletvNN.top" arka ucundan geliyor:
//   /load/channels.php, /load/matches.php  → <a class="single-match" href="matches?id=...">
//   /load/yayinlink.php?id=<id>            → {"deismackanal": "<m3u8>"}  (yoksa maç → streamsport365)
class AtomSpor : CanliSite() {
    override var mainUrl = "https://www.atomsportv520.top"
    override var name = "AtomSpor"
    override val grupSirasi = listOf("Kanallar", "Futbol", "Basketbol", "Voleybol", "Tenis")

    private val adaylar = listOf("https://www.atomsportv520.top/", "https://atomsportv519.top/")
    private var teletv = "teletv15.top"

    private suspend fun hazirla() {
        val alan = Spor.alanBul("atomspor", adaylar, "teletv") ?: return
        mainUrl = alan.trimEnd('/')
        runCatching { app.get(alan, headers = Spor.H).text }.getOrNull()
            ?.let { Regex("""teletv\d+\.[a-z]+""").find(it)?.value }?.let { teletv = it }
    }

    override suspend fun kanallar(): List<Kanal> {
        hazirla()
        return listOf("channels.php", "matches.php").flatMap { yol ->
            runCatching { app.get("https://$teletv/load/$yol", headers = Spor.H, referer = "$mainUrl/").text }
                .getOrNull()?.let { TekMac.ayristir(it) }.orEmpty()
        }
    }

    override suspend fun akislar(kanal: Kanal): List<Akis> {
        hazirla()
        val r = runCatching {
            app.get("https://$teletv/load/yayinlink.php?id=${java.net.URLEncoder.encode(kanal.veri, "UTF-8")}",
                headers = Spor.H, referer = "$mainUrl/").text
        }.getOrNull().orEmpty()
        Spor.m3u8(r)?.let { return listOf(Akis(it, "$mainUrl/")) }
        // Kanal değilse maç: sayfanın yedek kaynağı.
        return listOfNotNull(Spor.cinema(kanal.veri, "$mainUrl/")?.let { Akis(it, "$mainUrl/") })
    }
}
