package ys.ardaspor

import com.lagradost.cloudstream3.app
import ys.ortak.*

// Ana sayfa: <a href="/canli-mac/<slug>"> (kanallar ve tarihli maçlar).
// Kanal: /canli-mac/<slug> → iframe /channel/watch/<slug> → iframe /loadstream.php?...&id=N
//        → paketli betikte base64 "window.config={match:{source:'<m3u8>'}}".
// Maç:   /canli-mac/<slug> → iframe /match.php?id=<maçId> → streamsport365 "cinema".
class ArdaSpor : CanliSite() {
    override var mainUrl = "https://ardaspor41.top"
    override var name = "ArdaSpor"

    private val adaylar = listOf("https://ardaspor41.top/", "https://ardaspor37.top/", "https://ardaspor34.top/")

    private suspend fun hazirla() {
        Spor.alanBul("ardaspor", adaylar, "/canli-mac/")?.let { mainUrl = it.trimEnd('/') }
    }

    private suspend fun getir(yol: String, referer: String = "$mainUrl/") =
        app.get(if (yol.startsWith("http")) yol else "$mainUrl/${yol.trimStart('/')}", headers = Spor.H, referer = referer)

    override suspend fun kanallar(): List<Kanal> {
        hazirla()
        val doc = getir("/").document
        return doc.select("a[href*=/canli-mac/]").mapNotNull { a ->
            val slug = a.attr("href").substringAfter("/canli-mac/").trim('/').ifBlank { return@mapNotNull null }
            val yazi = a.text().replace("▶", "").trim()
            val mac = Regex("""-\d{4}-\d{2}-\d{2}$""").containsMatchIn(slug)
            if (mac) {
                if (yazi.startsWith("BİTTİ")) return@mapNotNull null
                Kanal(yazi.removePrefix("CANLI").trim(), slug, a.selectFirst("img")?.attr("abs:src"), "Maçlar",
                    if (yazi.startsWith("CANLI")) "Şu an canlı" else null)
            } else Kanal(yazi, slug, a.selectFirst("img")?.attr("abs:src"), "Kanallar")
        }
    }

    override suspend fun akislar(kanal: Kanal): List<Akis> {
        hazirla()
        val sayfa = "$mainUrl/canli-mac/${kanal.veri}"
        val ic = getir(sayfa).document.select("iframe[src]").map { it.attr("src") }.firstOrNull { "sporx" !in it }
            ?: "/channel/watch/${kanal.veri}"
        if ("match.php" in ic) {
            val id = ic.substringAfter("id=").substringBefore("&")
            return listOfNotNull(Spor.cinema(id, "$mainUrl/")?.let { Akis(it, "$mainUrl/") })
        }
        val izle = getir(ic, sayfa)
        val yukle = Regex("""loadstream\.php[^"']+""").find(izle.text)?.value ?: return emptyList()
        val oyn = getir(yukle, izle.url).text
        val kaynak = Spor.paketliBase64(oyn)?.let { Regex("""source\s*:\s*"([^"]+)"""").find(it)?.groupValues?.get(1) }
            ?: Spor.m3u8(oyn) ?: return emptyList()
        return listOf(Akis(kaynak, "$mainUrl/"))
    }
}
