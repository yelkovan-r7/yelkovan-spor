package ys.kralsporhd

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import ys.ortak.*

// Ana sayfa listeleri harici bir betikten geliyor (<script src=".../script4.js">):
//   const channels = [{ title: "...", url: "/event.html?id=androstreamlivebs1" }, ...]   (JS nesnesi)
//   const karsilasmalar / futbolMatches / basketbolMatches / ... = [ {...} ]                (JSON)
// Kanal yayını: event.html içindeki "…/checklist/" sunucuları + <id>.m3u8 (bs1 → androstreamlivekral).
// "androstreamlivech<maçId>" maçları başka oynatıcıya gider; o da streamsport365 "cinema" kullanıyor.
class KralSporHD : CanliSite() {
    override var mainUrl = "https://kralsportshd8.xyz"
    override var name = "KralSportsHD"
    override val grupSirasi = listOf("Önemli Maçlar", "Kanallar", "Futbol", "Basketbol", "Voleybol", "Tenis")

    private val adaylar = listOf("https://kralsportshd8.xyz/", "https://kralsportshd7.xyz/")
    private val gecitler = listOf("https://kralsportshd.com/giris2/", "https://kralsportshd.com/giris/")
    private var sunucular = listOf(
        "https://andro.evrenesoglu235.click/checklist/", "https://andro.evrenesoglu237.click/checklist/",
        "https://andro.evrenesoglu239.click/checklist/", "https://andro.evrenesoglu241.click/checklist/",
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Mac(val time: String? = null, val league: String? = null, val title: String? = null,
                   val url: String? = null, val id: String? = null, val live: Boolean? = null)

    private suspend fun hazirla() {
        Spor.alanBul("kralspor", adaylar, "event.html?id=", gecitler = gecitler,
            gecitKalibi = Regex("""https?://(?:www\.)?kralsportshd\d+\.[a-z]+/?"""))?.let { mainUrl = it.trimEnd('/') }
    }

    override suspend fun kanallar(): List<Kanal> {
        hazirla()
        val ana = app.get("$mainUrl/", headers = Spor.H).text
        val betik = Regex("""<script[^>]+src="([^"]+/script\d*\.js)"""").find(ana)?.groupValues?.get(1)
            ?: return emptyList()
        val js = app.get(betik, headers = Spor.H, referer = "$mainUrl/").text
        val liste = mutableListOf<Kanal>()

        Regex("""const channels\s*=\s*\[([\s\S]*?)\n\]""").find(js)?.groupValues?.get(1)?.let { blok ->
            Regex("""title:\s*"([^"]+)"\s*,\s*url:\s*"[^"]*id=([^"&]+)"""").findAll(blok).forEach {
                liste += Kanal(it.groupValues[1], it.groupValues[2], grup = "Kanallar")
            }
        }
        val gruplar = mapOf("karsilasmalar" to "Önemli Maçlar", "futbolMatches" to "Futbol",
            "basketbolMatches" to "Basketbol", "voleybolMatches" to "Voleybol", "tenisMatches" to "Tenis")
        Regex("""const (\w+)\s*=\s*(\[[\s\S]*?\n\]);?""").findAll(js).forEach { m ->
            val grup = gruplar[m.groupValues[1]] ?: return@forEach
            tryParseJson<List<Mac>>(m.groupValues[2])?.forEach { mac ->
                val id = mac.url?.substringAfter("id=")?.substringBefore("&") ?: return@forEach
                val ad = listOfNotNull(mac.time, mac.title).joinToString(" ")
                liste += Kanal(ad, id, grup = grup,
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
        // Güncel sunucu listesini oynatıcı sayfasından oku.
        runCatching { app.get("$mainUrl/event.html?id=$id", headers = Spor.H, referer = "$mainUrl/").text }.getOrNull()
            ?.let { s -> Regex("""https://[^"'\s]+/checklist/""").findAll(s).map { it.value }.distinct().toList() }
            ?.takeIf { it.isNotEmpty() }?.let { sunucular = it }
        val dosya = if (id == "androstreamlivebs1") "androstreamlivekral" else id
        return sunucular.mapIndexed { i, s -> Akis("$s$dosya.m3u8", "$mainUrl/", "Sunucu ${i + 1}") }
    }
}
