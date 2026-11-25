package ys.taraftarium24

import com.lagradost.cloudstream3.app
import org.jsoup.Jsoup
import ys.ortak.*

// Giriş: taraftarium24.io → taraftariumNNNN.xyz
// Liste: <a href="/channel.html?id=b2" class="channel-item" data-category="24-7"><div class="channel-name">…</div>
// Yayın: channel.html içindeki CONFIG = { baseUrl: 'https://…/' } + <id>/mono.m3u8
class Taraftarium24 : CanliSite() {
    override var mainUrl = "https://taraftarium1090.xyz"
    override var name = "Taraftarium24"
    override val grupSirasi = listOf("Kanallar", "Futbol", "Basketbol", "Diğer")

    private val adaylar = listOf("https://taraftarium1090.xyz/", "https://taraftarium1089.xyz/")
    private val gecitler = listOf("https://taraftarium24.io/", "https://taraftarium24.ch/")
    private var taban = "https://g1b.d72577a9dd0ec74.cfd/"

    private suspend fun hazirla() {
        Spor.alanBul("taraftarium", adaylar, "channel.html?id=", gecitler = gecitler,
            gecitKalibi = Regex("""https?://(?:www\.)?taraftarium\d+\.[a-z]+/?"""))?.let { mainUrl = it.trimEnd('/') }
    }

    override suspend fun kanallar(): List<Kanal> {
        hazirla()
        val doc = Jsoup.parse(app.get("$mainUrl/", headers = Spor.H).text, "$mainUrl/")
        return doc.select("a.channel-item[href*=channel.html?id=]").mapNotNull { a ->
            val id = a.attr("href").substringAfter("id=").substringBefore("&").ifBlank { return@mapNotNull null }
            val ad = a.selectFirst(".channel-name")?.text()?.trim()?.ifBlank { null } ?: return@mapNotNull null
            val grup = when (a.attr("data-category")) {
                "24-7" -> "Kanallar"; "football" -> "Futbol"; "basketball" -> "Basketbol"; else -> "Diğer"
            }
            Kanal(ad, id, grup = grup, aciklama = a.selectFirst(".channel-status")?.text())
        }
    }

    override suspend fun akislar(kanal: Kanal): List<Akis> {
        hazirla()
        runCatching { app.get("$mainUrl/channel.html?id=${kanal.veri}", headers = Spor.H, referer = "$mainUrl/").text }.getOrNull()
            ?.let { Regex("""baseUrl\s*:\s*['"]([^'"]+)""").find(it)?.groupValues?.get(1) }?.let { taban = it }
        return listOf(Akis("${taban.trimEnd('/')}/${kanal.veri}/mono.m3u8", "$mainUrl/", headers = mapOf("Origin" to mainUrl)))
    }
}
