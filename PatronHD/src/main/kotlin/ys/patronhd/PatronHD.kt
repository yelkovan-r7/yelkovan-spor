package ys.patronhd

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import ys.ortak.*

// Ana sayfa: const endpoints = { matches: 'https://patronsports2.cfd/matches.php', channels: '…/channels.php' }
// channels.php: [{Mac, Logo, URL:"/ch.html?id=b2"}]   matches.php: [{HomeTeam, AwayTeam, Time, URL, type, league}]
// Yayın: domain.php → {"baseurl": "https://…/"} + <id>/mono.m3u8;  id bir adresse (TRT vb.) doğrudan oynatılır.
class PatronHD : CanliSite() {
    override var mainUrl = "https://patronhd10.cfd"
    override var name = "PatronHD"
    override val grupSirasi = listOf("Kanallar", "Futbol", "Basketbol", "Voleybol", "Tenis")

    private val adaylar = listOf("https://patronhd10.cfd/", "https://patronhd8.cfd/")
    private var api = "https://patronsports2.cfd"

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Oge(
        @JsonProperty("Mac") val mac: String? = null, @JsonProperty("Logo") val logo: String? = null,
        @JsonProperty("URL") val url: String? = null, @JsonProperty("HomeTeam") val ev: String? = null,
        @JsonProperty("AwayTeam") val dep: String? = null, @JsonProperty("Time") val saat: String? = null,
        @JsonProperty("HomeLogo") val evLogo: String? = null, val type: String? = null, val league: String? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Maclar(val matches: List<Oge>? = null)

    private suspend fun hazirla() {
        Spor.alanBul("patronhd", adaylar, "channels.php")?.let { mainUrl = it.trimEnd('/') }
        runCatching { app.get("$mainUrl/", headers = Spor.H).text }.getOrNull()
            ?.let { Regex("""['"](https?://[^'"]+)/channels\.php""").find(it)?.groupValues?.get(1) }?.let { api = it }
    }

    private suspend fun getir(yol: String) =
        app.get("$api/$yol", headers = Spor.H + mapOf("Origin" to mainUrl), referer = "$mainUrl/").text

    private fun spor(t: String?) = when (t?.lowercase()) {
        "football", "futbol" -> "Futbol"; "basketball", "basketbol" -> "Basketbol"
        "volleyball", "voleybol" -> "Voleybol"; "tennis", "tenis" -> "Tenis"; else -> "Diğer"
    }

    override suspend fun kanallar(): List<Kanal> {
        hazirla()
        val liste = mutableListOf<Kanal>()
        tryParseJson<List<Oge>>(getir("channels.php")).orEmpty().forEach { o ->
            val id = o.url?.substringAfter("id=", "")?.ifBlank { null } ?: return@forEach
            liste += Kanal(o.mac?.trim() ?: id, id, o.logo?.let { if (it.startsWith("http")) it else "$api$it" }, "Kanallar")
        }
        val m = getir("matches.php")
        val maclar = tryParseJson<List<Oge>>(m) ?: tryParseJson<Maclar>(m)?.matches.orEmpty()
        maclar.forEach { o ->
            val id = o.url?.substringAfter("id=", "")?.ifBlank { null } ?: return@forEach
            val ad = listOfNotNull(o.saat, "${o.ev.orEmpty()} - ${o.dep.orEmpty()}").joinToString(" ")
            liste += Kanal(ad, id, o.evLogo, spor(o.type), o.league)
        }
        return liste
    }

    override suspend fun akislar(kanal: Kanal): List<Akis> {
        if (kanal.veri.startsWith("http")) return listOf(Akis(kanal.veri, "$mainUrl/"))
        hazirla()
        val taban = runCatching { Regex(""""baseurl"\s*:\s*"([^"]+)"""").find(getir("domain.php"))?.groupValues?.get(1)?.replace("\\/", "/") }
            .getOrNull() ?: "https://g1b.d72577a9dd0ec74.cfd/"
        return listOf(Akis("${taban.trimEnd('/')}/${kanal.veri}/mono.m3u8", "$mainUrl/", headers = mapOf("Origin" to mainUrl)))
    }
}
