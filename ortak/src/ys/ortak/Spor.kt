package ys.ortak

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.utils.JsUnpacker

/** Spor eklentilerinin ortak yardımcıları. */
object Spor {
    const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36"
    val H = mapOf("User-Agent" to UA)

    private val alanlar = mutableMapOf<String, Pair<Long, String>>()

    /** "https://www.site12.top/yol" → "https://www.site12.top/" */
    fun kok(url: String): String = (Regex("""^https?://[^/]+""").find(url)?.value ?: url).trimEnd('/') + "/"

    /**
     * Sitenin güncel alan adını bulur. Spor siteleri sık adres değiştirir; eski adresler çoğunlukla
     * yenisine yönlendirir. Adaylar sırayla denenir, yönlendirme izlenir ve sayfada [isaret] aranır.
     * Hiçbiri tutmazsa sonu sayılı adreslerde (ör. atomsportv520.top) sayı ileri doğru taranır.
     * Bulunan adres 30 dakika hatırlanır.
     */
    suspend fun alanBul(anahtar: String, adaylar: List<String>, isaret: String, ileri: Int = 8): String? {
        alanlar[anahtar]?.let { (t, a) -> if (System.currentTimeMillis() - t < 30 * 60 * 1000) return a }
        suspend fun dene(u: String): String? = runCatching {
            val r = app.get(u, headers = H, timeout = 12L)
            if (r.code == 200 && isaret in r.text) kok(r.url) else null
        }.getOrNull()

        var bulunan: String? = null
        for (a in adaylar) { bulunan = dene(a); if (bulunan != null) break }
        if (bulunan == null) {
            val kalip = Regex("""^(https?://[^/]*?)(\d+)(\.[a-z]+)(/.*)?$""")
            loop@ for (a in adaylar) {
                val m = kalip.find(a) ?: continue
                val n = m.groupValues[2].toIntOrNull() ?: continue
                for (i in 1..ileri) {
                    bulunan = dene(m.groupValues[1] + (n + i) + m.groupValues[3] + "/")
                    if (bulunan != null) break@loop
                }
            }
        }
        bulunan?.let { alanlar[anahtar] = System.currentTimeMillis() to it }
        return bulunan
    }

    /** Metindeki ilk m3u8 adresi (JSON kaçışları temizlenir). */
    fun m3u8(metin: String): String? =
        Regex("""https?:\\?/\\?/[^"'\s<>]+?\.m3u8[^"'\s<>]*""").find(metin)?.value?.replace("\\/", "/")

    fun m3u8Hepsi(metin: String): List<String> =
        Regex("""https?:\\?/\\?/[^"'\s<>]+?\.m3u8[^"'\s<>]*""").findAll(metin).map { it.value.replace("\\/", "/") }.distinct().toList()

    /**
     * eval(function(p,a,c,k,e,d){…}('4(3(2(1("0"))))',5,5,'<base64>|atob|…')) kalıbındaki base64 yükünü
     * çalıştırmadan çözer (sondaki "=" dolgusu p içinde durur).
     */
    fun paketliBase64(html: String): String? {
        val m = Regex("""\}\('[^']*?"0(=*)"[^']*',\d+,\d+,'([^'|]+)""").find(html) ?: return null
        return runCatching { base64Decode(m.groupValues[2] + m.groupValues[1]) }.getOrNull()
    }

    /** Klasik p,a,c,k,e,d paketini çalıştırmadan açar. */
    fun ac(html: String): String = runCatching { JsUnpacker(html).unpack() }.getOrNull() ?: html

    /**
     * Maç yayınları için birçok sitenin kullandığı ortak servis (streamsport365 "cinema").
     * Sayfanın kendi oynatıcısının yaptığı istekle aynıdır; maç kimliğiyle m3u8 döner.
     */
    suspend fun cinema(id: String, referer: String): String? = runCatching {
        val r = app.post(
            "https://streamsport365.com/cinema",
            headers = H + mapOf("Accept" to "*/*"),
            referer = referer,
            json = mapOf("AppId" to "5000", "AppVer" to "1", "VpcVer" to "1.0.12", "Language" to "en", "Token" to "", "VideoId" to id)
        ).text
        Regex(""""URL"\s*:\s*"([^"]+)"""").find(r)?.groupValues?.get(1)?.replace("\\/", "/")
    }.getOrNull()
}
