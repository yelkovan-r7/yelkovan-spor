package ys.ortak

import com.lagradost.cloudstream3.app

/**
 * Aynı yayın ağını paylaşan siteler (Zbahis/Baywin TV, VegolTV, JestYayın…).
 * Liste: ana sayfadaki fetch('https://<api>/channels.php' | '/matches.php') → "single-match" şablonu.
 * Yayın: <api>/domain.php → {"baseurl": "https://…/"} ; akış = baseurl + <id> + "/mono.m3u8".
 * Maç satırları da bir kanal kimliğine (ör. trt1) bağlanıyor.
 */
abstract class ZirveAgi : CanliSite() {
    abstract val anahtar: String
    abstract val adaylar: List<String>
    open val gecitler: List<String> = emptyList()
    open val gecitKalibi: Regex? = null
    override val grupSirasi = listOf("Futbol", "Basketbol", "Kanallar")

    private var api = "https://data-reality.com"

    private suspend fun hazirla() {
        val alan = Spor.alanBul(anahtar, adaylar, "channels.php", gecitler = gecitler, gecitKalibi = gecitKalibi) ?: return
        mainUrl = alan.trimEnd('/')
        runCatching { app.get(alan, headers = Spor.H).text }.getOrNull()
            ?.let { Regex("""fetch\(['"](https?://[^/'"]+)/channels\.php""").find(it)?.groupValues?.get(1) }
            ?.let { api = it }
    }

    override suspend fun kanallar(): List<Kanal> {
        hazirla()
        return listOf("matches.php", "channels.php").flatMap { yol ->
            runCatching { app.get("$api/$yol", headers = Spor.H, referer = "$mainUrl/").text }.getOrNull()
                ?.let { TekMac.ayristir(it, kanalTurleri = emptySet()) }.orEmpty()
        }
    }

    override suspend fun akislar(kanal: Kanal): List<Akis> {
        hazirla()
        val id = kanal.veri
        if (id.startsWith("http")) return listOf(Akis(id, "$mainUrl/"))
        val taban = runCatching { app.get("$api/domain.php", headers = Spor.H, referer = "$mainUrl/").text }.getOrNull()
            ?.let { Regex(""""baseurl"\s*:\s*"([^"]+)"""").find(it)?.groupValues?.get(1)?.replace("\\/", "/") }
            ?: return emptyList()
        return listOf(Akis("${taban.trimEnd('/')}/$id/mono.m3u8", "$mainUrl/", headers = mapOf("Origin" to mainUrl)))
    }
}
