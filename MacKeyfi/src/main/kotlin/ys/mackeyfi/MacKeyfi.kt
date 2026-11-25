package ys.mackeyfi

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Decode
import org.jsoup.Jsoup
import ys.ortak.*

// Kanallar: /tv/<kanal>/  → <div class="x-embed-container" data-player-url="…/match-center.php?id=5062">
// Oynatıcı: "streamradar" alan adı base64 ile sayfada (".e-aga-m.….sbs");
//   yayın = https://<alan>/bc2b05d321cb80050c5d035a9daeb26d/-/<id>/playlist.m3u8 + <site>/t?id=<id> yanıtının 6. öğesi (?verify=…)
class MacKeyfi : CanliSite() {
    override var mainUrl = "https://www.mackeyfi605.sbs"
    override var name = "MaçKeyfi"
    override val grupSirasi = listOf("beIN Sports", "S Sport & Tivibu", "Tabii Spor", "Ulusal & Spor Kanalları")

    private val adaylar = listOf("https://www.mackeyfi605.sbs/", "https://www.canlimaclar615.sbs/")

    private fun grubu(ad: String): String {
        val k = ad.lowercase()
        return when {
            "bein" in k -> "beIN Sports"
            "s sport" in k || "tivibu" in k || "smart" in k -> "S Sport & Tivibu"
            "tabii" in k -> "Tabii Spor"
            else -> "Ulusal & Spor Kanalları"
        }
    }

    override suspend fun kanallar(): List<Kanal> {
        Spor.alanBul("mackeyfi", adaylar, "/tv/", gecitKalibi = null)?.let { mainUrl = it.trimEnd('/') }
        val doc = Jsoup.parse(app.get("$mainUrl/", headers = Spor.H).text, "$mainUrl/")
        return doc.select("a[href*=/tv/]").mapNotNull { a ->
            val yol = a.attr("abs:href").substringAfter("/tv/").trim('/').ifBlank { return@mapNotNull null }
            val ad = a.text().trim().ifBlank { a.attr("title") }.ifBlank { yol.replace('-', ' ').uppercase() }
            Kanal(ad, yol, a.selectFirst("img")?.attr("abs:src"), grubu(ad))
        }
    }

    override suspend fun akislar(kanal: Kanal): List<Akis> {
        val sayfa = app.get("$mainUrl/tv/${kanal.veri}/", headers = Spor.H).text
        val oyn = Regex("""data-player-url="([^"#]+)""").find(sayfa)?.groupValues?.get(1) ?: return emptyList()
        val id = oyn.substringAfter("id=").substringBefore("&")
        val h = app.get(oyn, headers = Spor.H, referer = "$mainUrl/").text
        val alan = Regex("""streamradardomil=\[atob\("([^"]+)"\)""").find(h)?.groupValues?.get(1)
            ?.let { base64Decode(it).trimStart('.') } ?: "e-aga-m.943411d29aa8b67a6485827229af8b16.sbs"
        val yol = Regex(""""\+window\.streamradardomi\.substring\(1\)\+"(/[^"]+/)"\+window\.mainSource""").find(h)?.groupValues?.get(1)
            ?: "/bc2b05d321cb80050c5d035a9daeb26d/-/"
        val dogrula = runCatching {
            Regex(""""(\?verify=[^"]+)"""").find(app.get("${Spor.kok(oyn).trimEnd('/')}/t?id=$id", headers = Spor.H, referer = oyn).text)?.groupValues?.get(1)
        }.getOrNull().orEmpty()
        return listOf(Akis("https://$alan$yol$id/playlist.m3u8$dogrula", oyn, headers = mapOf("Origin" to Spor.kok(oyn).trimEnd('/'))))
    }
}
