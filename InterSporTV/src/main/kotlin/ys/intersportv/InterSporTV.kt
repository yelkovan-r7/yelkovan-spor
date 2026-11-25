package ys.intersportv

import com.lagradost.cloudstream3.app
import org.jsoup.Jsoup
import ys.ortak.*

// Ana sayfa: <a class="single-match" data-stream="betlivematch-<maçId>" data-name data-matchtype>
// Oynatıcı: <meta name="onexd"> + /<maçId>.m3u8 (çoğu zaman 403) → maç kimliği ortak maç servisinde de geçerli.
class InterSporTV : CanliSite() {
    override var mainUrl = "https://intersportv1.live"
    override var name = "InterSporTV"
    override val grupSirasi = listOf("Futbol", "Basketbol", "Voleybol", "Tenis")

    private val adaylar = listOf("https://intersportv1.live/", "https://intersportv2.live/")

    override suspend fun kanallar(): List<Kanal> {
        Spor.alanBul("intersportv", adaylar, "betlivematch-")?.let { mainUrl = it.trimEnd('/') }
        val doc = Jsoup.parse(app.get("$mainUrl/", headers = Spor.H).text, "$mainUrl/")
        return doc.select("a.single-match[data-stream^=betlivematch-]").map { a ->
            val saat = a.selectFirst(".date")?.text()?.substringBefore("/")?.trim().orEmpty()
            val ad = a.attr("data-name").ifBlank { a.attr("title") }
            Kanal("$saat $ad".trim(), a.attr("data-stream").removePrefix("betlivematch-"),
                a.selectFirst("img")?.attr("abs:src"), a.attr("data-matchtype").ifBlank { "Maçlar" },
                a.selectFirst(".event")?.text())
        }
    }

    override suspend fun akislar(kanal: Kanal): List<Akis> =
        listOfNotNull(Spor.cinema(kanal.veri, "$mainUrl/")?.let { Akis(it, "$mainUrl/") })
}
