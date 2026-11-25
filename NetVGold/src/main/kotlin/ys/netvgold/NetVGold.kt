package ys.netvgold

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import ys.ortak.*

/**
 * Kanal adresleri, birkaç spor sitesinden yayın adreslerini her gün toplayan herkese açık bir GitHub
 * deposundan gelir (icebug13/turbo-guacamoled). Klasörler:
 *  streams3/<kanal-adi>.m3u8               → AtomSpor ağı
 *  streams2/stream_androstreamlive<kod>.m3u8 → androstream ağı
 *  streams/stream_<kod>.m3u8                → Taraftarium ağı (Türkiye'den çoğu zaman 403)
 * Aynı kanalın kaynakları tek kanalda birleştirilir.
 */
class NetVGold : CanliSite() {
    override var mainUrl = "https://github.com/icebug13/turbo-guacamoled"
    override var name = "NetVGold"
    override val grupSirasi = listOf("beIN Sports", "S Sport & Tivibu", "Tabii Spor", "Exxen Spor", "Ulusal & Spor Kanalları", "Diğer")

    private val depo = "icebug13/turbo-guacamoled"
    private val ham = "https://raw.githubusercontent.com/$depo/main/"

    private val kaynakAdi = mapOf("streams3" to "AtomSpor", "streams2" to "Androstream", "streams" to "Taraftarium")
    private val kaynakSirasi = listOf("streams3", "streams2", "streams")

    // GitHub API'ye ulaşılamazsa kullanılacak liste (2026-11-25).
    private val yedek = listOf(
        "streams3/" to "a-spor bein-sports-1 bein-sports-2 bein-sports-3 bein-sports-4 bein-sports-5 bein-sports-max-1 bein-sports-max-2 s-sport s-sport-2 tabii tabii1 tabii2 tabii3 tabii4 tabii5 tabii6 tivibu-spor-1 tivibu-spor-2 tivibu-spor-3 trt-1 trt-spor",
        "streams2/stream_androstreamlive" to "a2 as atv bs1 bs2 bs3 bs4 bs5 bsm1 bsm2 cbcs es1 es2 exn exn1 exn2 exn3 exn4 exn5 exn6 exn7 exn8 fb gs ht nba sm1 sm2 sptstv ss1 ss2 ssplus1 tb tb1 tb2 tb3 tb4 tb5 tb6 tb7 tb8 tjk trt1 trts trtsy ts ts1 ts2 ts3 ts4 tv8 tv85",
        "streams/stream_" to "as atv b2 b3 b4 b5 bm1 bm2 eu1 eu2 ex1 ex2 ex3 ex4 ex5 ex6 ex7 ex8 nbatv patron smarts sms2 ss ss2 t1 t2 t3 t4 trt1 trtspor trtspor2 tv8 tv85",
    ).flatMap { (on, kodlar) -> kodlar.split(" ").map { "$on$it.m3u8" } }

    /** Dosya adındaki kısa kodu okunur kanal adına çevirir. */
    private fun kanalAdi(kod: String): String {
        val k = kod.lowercase()
        fun n(on: String) = k.removePrefix(on).trimStart('-').ifBlank { null }
        Regex("""^(?:bein-sports-|bs|b)(\d)$""").find(k)?.let { return "beIN Sports ${it.groupValues[1]}" }
        Regex("""^(?:bein-sports-max-|bsm|bm)(\d)$""").find(k)?.let { return "beIN Sports Max ${it.groupValues[1]}" }
        Regex("""^(?:tivibu-spor-|ts|t)(\d)$""").find(k)?.let { return "Tivibu Spor ${it.groupValues[1]}" }
        Regex("""^(?:tabii|tb)(\d)$""").find(k)?.let { return "Tabii Spor ${it.groupValues[1]}" }
        Regex("""^(?:exn|ex)(\d)$""").find(k)?.let { return "Exxen Spor ${it.groupValues[1]}" }
        Regex("""^(?:es|eu)(\d)$""").find(k)?.let { return "Eurosport ${it.groupValues[1]}" }
        Regex("""^(?:sm|sms)(\d)$""").find(k)?.let { return "Smart Spor ${it.groupValues[1]}" }
        return when (k) {
            "patron" -> "beIN Sports 1"
            "s-sport", "ss", "ss1" -> "S Sport"
            "s-sport-2", "ss2" -> "S Sport 2"
            "ssplus1" -> "S Sport Plus"
            "tabii", "tb" -> "Tabii Spor"
            "ts" -> "Tivibu Spor"
            "exn" -> "Exxen Spor"
            "a-spor", "as" -> "A Spor"
            "a2" -> "A2"
            "atv" -> "ATV"
            "trt-1", "trt1" -> "TRT 1"
            "trt-spor", "trts", "trtspor" -> "TRT Spor"
            "trtsy", "trtspor2" -> "TRT Spor Yıldız"
            "tv8" -> "TV8"
            "tv85" -> "TV8,5"
            "smarts" -> "Smart Spor 1"
            "nba", "nbatv" -> "NBA TV"
            "fb" -> "FB TV"
            "gs" -> "GS TV"
            "ht" -> "HT Spor"
            "tjk" -> "TJK TV"
            "cbcs" -> "CBC Sport"
            "sptstv" -> "Sports TV"
            else -> n("")?.uppercase() ?: kod
        }
    }

    private fun grubu(ad: String) = when {
        ad.startsWith("beIN") -> "beIN Sports"
        ad.startsWith("S Sport") || ad.startsWith("Tivibu") || ad.startsWith("Smart") -> "S Sport & Tivibu"
        ad.startsWith("Tabii") -> "Tabii Spor"
        ad.startsWith("Exxen") -> "Exxen Spor"
        ad in setOf("A Spor", "A2", "ATV", "TRT 1", "TRT Spor", "TRT Spor Yıldız", "TV8", "TV8,5", "HT Spor") -> "Ulusal & Spor Kanalları"
        else -> "Diğer"
    }

    private fun koddan(yol: String): String {
        val dosya = yol.substringAfterLast('/').removeSuffix(".m3u8")
        return dosya.removePrefix("stream_").removePrefix("androstreamlive")
    }

    data class Kaynak(val ad: String, val url: String)

    override suspend fun kanallar(): List<Kanal> {
        val yollar = runCatching {
            Regex(""""path"\s*:\s*"(streams\d?/[^"]+\.m3u8)"""")
                .findAll(app.get("https://api.github.com/repos/$depo/git/trees/main?recursive=1", headers = Spor.H).text)
                .map { it.groupValues[1] }.toList()
        }.getOrNull()?.takeIf { it.isNotEmpty() } ?: yedek

        return yollar.groupBy { kanalAdi(koddan(it)) }.map { (ad, liste) ->
            val kaynaklar = liste.sortedBy { kaynakSirasi.indexOf(it.substringBefore('/')) }
                .map { Kaynak(kaynakAdi[it.substringBefore('/')] ?: "Kaynak", ham + it) }
            Kanal(ad, kaynaklar.toJson(), grup = grubu(ad), aciklama = kaynaklar.joinToString(", ") { it.ad })
        }.sortedWith(compareBy({ it.grup }, { it.ad }))
    }

    override suspend fun akislar(kanal: Kanal): List<Akis> =
        tryParseJson<List<Kaynak>>(kanal.veri).orEmpty().map { Akis(it.url, ad = "$name ${it.ad}") }
}
