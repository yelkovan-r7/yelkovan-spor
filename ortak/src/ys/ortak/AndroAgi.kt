package ys.ortak

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import org.jsoup.Jsoup

/**
 * "androstream" yayın ağını paylaşan siteler (KralSportsHD, MahsunSports, KaçakSpor…).
 * Kanal yayını: oynatıcı sayfasındaki "…/checklist/" sunucuları + <id>.m3u8.
 * "androstreamlivech<maçId>" maçları ortak maç servisine (streamsport365) gider.
 * Liste iki biçimde olabiliyor:
 *  - script4.js: const channels = [{ title, url }] (JS nesnesi) + const futbolMatches = [...] (JSON)
 *  - ana sayfada <a data-name="…" data-stream="androstreamlive…">
 */
abstract class AndroAgi : CanliSite() {
    abstract val anahtar: String
    abstract val adaylar: List<String>
    open val gecitler: List<String> = emptyList()
    open val gecitKalibi: Regex? = null
    /** Oynatıcı sayfasının yolu (sunucu listesini buradan okuruz). */
    open val oynatici = "event.html?id="
    override val grupSirasi = listOf("Önemli Maçlar", "Kanallar", "Futbol", "Basketbol", "Voleybol", "Tenis")

    private var sunucular = listOf(
        "https://andro.evrenesoglu235.click/checklist/", "https://andro.evrenesoglu237.click/checklist/",
        "https://andro.evrenesoglu239.click/checklist/", "https://andro.evrenesoglu241.click/checklist/",
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Mac(val time: String? = null, val league: String? = null, val title: String? = null,
                   val url: String? = null, val live: Boolean? = null)

    protected suspend fun hazirla() {
        Spor.alanBul(anahtar, adaylar, "androstreamlive", gecitler = gecitler, gecitKalibi = gecitKalibi)
            ?.let { mainUrl = it.trimEnd('/') }
    }

    override suspend fun kanallar(): List<Kanal> {
        hazirla()
        val ana = app.get("$mainUrl/", headers = Spor.H).text
        val betik = Regex("""<script[^>]+src="([^"]*script\d*\.js)"""").find(ana)?.groupValues?.get(1)
        val liste = mutableListOf<Kanal>()
        if (betik != null) {
            val adres = if (betik.startsWith("http")) betik else "$mainUrl/${betik.trimStart('/')}"
            runCatching { app.get(adres, headers = Spor.H, referer = "$mainUrl/").text }.getOrNull()?.let { liste += betiktenOku(it) }
        }
        // Ana sayfadaki data-stream öğeleri (betik yoksa ya da ek kanallar için).
        Jsoup.parse(ana).select("[data-stream^=androstreamlive]").forEach { e ->
            val ad = e.attr("data-name").ifBlank { e.attr("title") }.ifBlank { e.text() }.trim()
            if (ad.isNotBlank()) liste += Kanal(ad, e.attr("data-stream"), e.selectFirst("img")?.attr("abs:src"), "Kanallar")
        }
        return liste
    }

    private fun betiktenOku(js: String): List<Kanal> {
        val liste = mutableListOf<Kanal>()
        Regex("""const channels\s*=\s*\[([\s\S]*?)\n\s*\]""").find(js)?.groupValues?.get(1)?.let { blok ->
            Regex("""title:\s*["']([^"']+)["']\s*,\s*url:\s*["'][^"']*id=([^"'&]+)""").findAll(blok).forEach {
                liste += Kanal(it.groupValues[1], it.groupValues[2], grup = "Kanallar")
            }
        }
        val gruplar = mapOf("karsilasmalar" to "Önemli Maçlar", "futbolMatches" to "Futbol",
            "basketbolMatches" to "Basketbol", "voleybolMatches" to "Voleybol", "tenisMatches" to "Tenis")
        Regex("""const (\w+)\s*=\s*(\[[\s\S]*?\n\]);?""").findAll(js).forEach { m ->
            val grup = gruplar[m.groupValues[1]] ?: return@forEach
            tryParseJson<List<Mac>>(m.groupValues[2])?.forEach { mac ->
                val id = mac.url?.substringAfter("id=")?.substringBefore("&") ?: return@forEach
                liste += Kanal(listOfNotNull(mac.time, mac.title).joinToString(" "), id, grup = grup,
                    aciklama = listOfNotNull(mac.league?.takeIf { it != "Unknown League" }, if (mac.live == true) "Şu an canlı" else null)
                        .joinToString(" • ").ifBlank { null })
            }
        }
        return liste
    }

    override suspend fun akislar(kanal: Kanal): List<Akis> {
        hazirla()
        val id = kanal.veri
        if (id.startsWith("androstreamlivech")) {
            val macId = id.removePrefix("androstreamlivech").removePrefix("stream")
            return listOfNotNull(Spor.cinema(macId, "$mainUrl/")?.let { Akis(it, "$mainUrl/") })
        }
        runCatching { app.get("$mainUrl/$oynatici$id", headers = Spor.H, referer = "$mainUrl/").text }.getOrNull()
            ?.let { s -> Regex("""https:\\?/\\?/[^"'\s]+?\\?/checklist\\?/""").findAll(s).map { it.value.replace("\\/", "/") }.distinct().toList() }
            ?.takeIf { it.isNotEmpty() }?.let { sunucular = it }
        return sunucular.mapIndexed { i, s -> Akis("$s$id.m3u8", "$mainUrl/", "Sunucu ${i + 1}") }
    }
}
