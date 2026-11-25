package ys.atomspor

import com.lagradost.cloudstream3.app
import org.jsoup.Jsoup
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

    private suspend fun sayfa(yol: String) =
        Jsoup.parse(app.get("https://$teletv/load/$yol", headers = Spor.H, referer = "$mainUrl/").text)

    override suspend fun kanallar(): List<Kanal> {
        hazirla()
        val liste = mutableListOf<Kanal>()
        for (yol in listOf("channels.php", "matches.php")) {
            runCatching { sayfa(yol) }.getOrNull()?.select("a.single-match[href*=id=]")?.forEach { a ->
                val id = a.attr("href").substringAfter("id=")
                val tur = a.attr("data-matchtype")
                if (tur == "tv") {
                    liste += Kanal(a.selectFirst(".event")?.text()?.removePrefix("7/24")?.trim()?.ifBlank { null } ?: a.text(), id)
                } else {
                    val ev = a.selectFirst(".home")?.text().orEmpty()
                    val dep = a.selectFirst(".away")?.text().orEmpty()
                    val olay = a.selectFirst(".event")?.text().orEmpty()
                    val saat = olay.substringBefore("|").trim()
                    val ad = if (ev.isNotBlank()) "$saat $ev - $dep" else olay
                    liste += Kanal(ad.trim(), id, a.selectFirst("img")?.attr("src"), tur.ifBlank { "Maçlar" }, olay.substringAfter("|").trim())
                }
            }
        }
        return liste
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
