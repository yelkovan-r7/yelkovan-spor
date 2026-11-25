package ys.ortak

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import org.jsoup.Jsoup

/**
 * Aynı yazılımı kullanan siteler (TRGoals, PapazSports…).
 * Kanal: <div class="channel-item" data-name data-target="viptv|m3u8|frame" data-source="100002">
 * Maç:   <li data-category="futbol" data-source='[{"ad":"SD","tip":"vip","link":21099059}]'>
 *          <span class="home">…</span><span class="away">…</span><span class="saat">09:00</span>
 * Yayın: POST /auth.php  channel=<kaynak> (viptv) | id=<maç> (vip)  → {"URL": "…/<id>.js", "TOKEN": …}
 *        (".js" uzantılı adres aslında HLS listesi). Origin başlığı gerekli.
 */
abstract class VipTv : CanliSite() {
    abstract val anahtar: String
    abstract val adaylar: List<String>
    open val gecitler: List<String> = emptyList()
    open val gecitKalibi: Regex? = null
    override val grupSirasi = listOf("Kanallar", "Futbol", "Basketbol", "Voleybol", "Tenis")

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Kaynak(val ad: String? = null, val tip: String? = null, val link: String? = null)

    private suspend fun hazirla() {
        Spor.alanBul(anahtar, adaylar, "phptojs", gecitler = gecitler, gecitKalibi = gecitKalibi)
            ?.let { mainUrl = it.trimEnd('/') }
    }

    override suspend fun kanallar(): List<Kanal> {
        hazirla()
        val doc = Jsoup.parse(app.get("$mainUrl/", headers = Spor.H).text, "$mainUrl/")
        val liste = mutableListOf<Kanal>()
        doc.select(".channel-item[data-source], #tv-listesi li[data-source]").forEach { e ->
            val ad = e.attr("data-name").ifBlank { return@forEach }
            val k = listOf(Kaynak(ad, e.attr("data-target"), e.attr("data-source")))
            liste += Kanal(ad, toJson(k), e.selectFirst("img")?.attr("abs:src"), "Kanallar")
        }
        doc.select("li[data-category][data-source]").forEach { e ->
            val kaynaklar = tryParseJson<List<Kaynak>>(e.attr("data-source"))?.takeIf { it.isNotEmpty() } ?: return@forEach
            val ev = e.selectFirst(".home")?.text()?.trim().orEmpty()
            val dep = e.selectFirst(".away")?.text()?.trim().orEmpty()
            val saat = e.selectFirst(".saat")?.text()?.trim().orEmpty()
            val ad = "$saat $ev - $dep".trim()
            val spor = e.attr("data-category").replaceFirstChar { it.uppercase() }
            liste += Kanal(ad, toJson(kaynaklar), e.selectFirst("img")?.attr("abs:src"), spor)
        }
        return liste
    }

    override suspend fun akislar(kanal: Kanal): List<Akis> {
        hazirla()
        val kaynaklar = runCatching { parseJson<List<Kaynak>>(kanal.veri) }.getOrElse { return emptyList() }
        return kaynaklar.mapNotNull { k ->
            val link = k.link ?: return@mapNotNull null
            val url = when (k.tip) {
                "m3u8" -> link
                "viptv", "vip" -> runCatching {
                    val alan = if (k.tip == "viptv") "channel" else "id"
                    val r = app.post("$mainUrl/auth.php", headers = Spor.H + mapOf("Origin" to mainUrl, "X-Requested-With" to "XMLHttpRequest"),
                        referer = "$mainUrl/", data = mapOf(alan to link)).text
                    Regex(""""URL"\s*:\s*"([^"]+)"""").find(r)?.groupValues?.get(1)?.replace("\\/", "/")
                }.getOrNull()
                else -> null
            } ?: return@mapNotNull null
            Akis(url, "$mainUrl/", k.ad?.let { "$name $it" }, mapOf("Origin" to mainUrl))
        }
    }
}
