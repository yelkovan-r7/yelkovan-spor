package ys.nebulasports

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import ys.ortak.*

/**
 * Nebula Sports bir Stremio eklentisi. Ortak ücretsiz bağlantı kapandı; artık hesapsız, ücretsiz
 * "kişisel bağlantı" veriliyor: POST /sports/configure (quick=1) → 302 /sports/i/<yapılandırma>/configure
 * Bu bağlantı cihazda bir kez üretilip saklanır.
 *  katalog: /sports/i/<yap>/catalog/sports/<katalog>.json → {metas:[{id,name,poster,genres,releaseInfo,isLive}]}
 *  yayın:   /sports/i/<yap>/stream/sports/<id>.json      → {streams:[{name,title,url | externalUrl}]}
 * Kanal katalogları (reklam izleme / bağış ile açılıyor) alınmaz; yalnızca ücretsiz maç yayınları.
 */
class NebulaSports : CanliSite() {
    override var mainUrl = "https://sports.nebulapro.xyz"
    override var name = "NebulaSports"
    override var lang = "en"
    override val grupSirasi = listOf("Şu An Canlı", "Futbol", "Basketbol", "Tenis", "Motor Sporları", "Dövüş", "Amerikan Futbolu", "Buz Hokeyi", "Beyzbol", "Kriket", "Rugby")

    private val anahtar = "ys_nebula_yapilandirma"

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Meta(val id: String? = null, val name: String? = null, val poster: String? = null,
                    val genres: List<String>? = null, val releaseInfo: String? = null, val isLive: Boolean? = null,
                    val description: String? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Katalog(val metas: List<Meta>? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Yayin(val name: String? = null, val title: String? = null, val url: String? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Yayinlar(val streams: List<Yayin>? = null)

    /** Kişisel yapılandırma yolu (/sports/i/<…>). Yoksa ücretsiz olarak bir kez üretilir. */
    private suspend fun yapilandirma(yenile: Boolean = false): String? {
        if (!yenile) getKey<String>(anahtar)?.let { return it }
        val r = app.post("$mainUrl/sports/configure", headers = Spor.H, data = mapOf("quick" to "1"), allowRedirects = false)
        val yer = r.headers["Location"] ?: r.headers["location"] ?: return null
        val yol = Regex("""/sports/i/[^/?#]+""").find(yer)?.value ?: return null
        setKey(anahtar, yol)
        return yol
    }

    private suspend fun getir(yolEki: String): String? {
        var yol = yapilandirma() ?: return null
        var r = app.get("$mainUrl$yol/$yolEki", headers = Spor.H)
        if (r.code == 404 || r.code == 410 || r.code == 401) {   // bağlantı geçersiz olduysa yenisini üret
            yol = yapilandirma(yenile = true) ?: return null
            r = app.get("$mainUrl$yol/$yolEki", headers = Spor.H)
        }
        return r.text
    }

    private fun turkce(spor: String?) = when (spor?.lowercase()) {
        "football", "soccer" -> "Futbol"; "basketball" -> "Basketbol"; "tennis" -> "Tenis"
        "motor sports", "motorsport", "motogp", "f1", "formula 1" -> "Motor Sporları"
        "fight", "boxing", "mma", "ufc", "wwe" -> "Dövüş"; "american football", "nfl" -> "Amerikan Futbolu"
        "hockey", "ice hockey", "nhl" -> "Buz Hokeyi"; "baseball", "mlb" -> "Beyzbol"
        "cricket" -> "Kriket"; "rugby" -> "Rugby"; null, "" -> "Diğer"
        else -> spor.replaceFirstChar { it.uppercase() }
    }

    override suspend fun kanallar(): List<Kanal> {
        val liste = mutableListOf<Kanal>()
        tryParseJson<Katalog>(getir("catalog/sports/streamed-events-live-now.json") ?: "")?.metas.orEmpty().forEach { m ->
            liste += Kanal(m.name ?: return@forEach, m.id ?: return@forEach, m.poster, "Şu An Canlı", m.description)
        }
        tryParseJson<Katalog>(getir("catalog/sports/all.json") ?: "")?.metas.orEmpty().forEach { m ->
            val ad = listOfNotNull(m.releaseInfo?.takeIf { m.isLive != true }, m.name).joinToString(" · ")
            liste += Kanal(ad.ifBlank { return@forEach }, m.id ?: return@forEach, m.poster, turkce(m.genres?.firstOrNull()), m.description)
        }
        return liste
    }

    override suspend fun akislar(kanal: Kanal): List<Akis> {
        val id = java.net.URLEncoder.encode(java.net.URLDecoder.decode(kanal.veri, "UTF-8"), "UTF-8").replace("+", "%20")
        val y = tryParseJson<Yayinlar>(getir("stream/sports/$id.json") ?: return emptyList()) ?: return emptyList()
        return y.streams.orEmpty().mapNotNull { s ->
            val url = s.url ?: return@mapNotNull null   // externalUrl (bağış/reklam) olanlar atlanır
            val ad = listOfNotNull(s.name?.substringAfter("·")?.trim(), s.title?.lineSequence()?.firstOrNull()).joinToString(" ")
            Akis(url, "$mainUrl/", ad.ifBlank { null })
        }
    }
}
