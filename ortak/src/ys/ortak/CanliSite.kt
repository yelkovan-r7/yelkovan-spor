package ys.ortak

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson

/** Listede görünen tek bir kanal veya maç. [veri] siteye özgü kimlik/adres (yayın çözerken kullanılır). */
data class Kanal(
    val ad: String,
    val veri: String,
    val logo: String? = null,
    val grup: String = "Kanallar",
    val aciklama: String? = null,
)

/** Oynatıcıya verilecek akış. */
data class Akis(
    val url: String,
    val referer: String = "",
    val ad: String? = null,
    val headers: Map<String, String> = emptyMap(),
)

/**
 * Canlı spor sitesi eklentilerinin ortak iskeleti.
 * Eklenti yalnızca [kanallar] (liste) ve [akislar] (yayın adresi) yazar; ana sayfa grupları,
 * arama, yayın sayfası ve oynatma burada.
 * Telemetri yoktur: eklentiler yalnızca yayın sitelerine bağlanır.
 */
abstract class CanliSite : MainAPI() {
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Live)

    /** Ana sayfada gösterilecek grupların sırası; listede olmayan gruplar sona eklenir. */
    open val grupSirasi: List<String> = listOf("Maçlar", "Kanallar")

    /** Sitedeki güncel kanal/maç listesi. */
    abstract suspend fun kanallar(): List<Kanal>

    /** Kanalın oynatılabilir akış(lar)ı. */
    abstract suspend fun akislar(kanal: Kanal): List<Akis>

    private var onbellek: Pair<Long, List<Kanal>>? = null

    /** Liste 3 dakika önbellekte tutulur (maç listesi sık değişir). */
    suspend fun liste(): List<Kanal> {
        onbellek?.let { (t, l) -> if (System.currentTimeMillis() - t < 3 * 60 * 1000 && l.isNotEmpty()) return l }
        val l = runCatching { kanallar() }.getOrElse { emptyList() }.distinctBy { it.veri }
        onbellek = System.currentTimeMillis() to l
        return l
    }

    override val mainPage = mainPageOf("hepsi" to "Hepsi")

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        if (page > 1) return newHomePageResponse(emptyList(), hasNext = false)
        val gruplar = liste().groupBy { it.grup }
        val sirali = grupSirasi.filter { it in gruplar } + gruplar.keys.filter { it !in grupSirasi }
        val listeler = sirali.map { g -> HomePageList(g, gruplar.getValue(g).map { it.kart() }, isHorizontalImages = true) }
        return newHomePageResponse(listeler, hasNext = false)
    }

    private fun Kanal.kart() = newLiveSearchResponse(ad, toJson(), TvType.Live) { posterUrl = logo }

    override suspend fun search(query: String): List<SearchResponse> {
        val q = query.lowercase().trim()
        return liste().filter { q in it.ad.lowercase() }.map { it.kart() }
    }

    override suspend fun load(url: String): LoadResponse {
        val k = parseJson<Kanal>(url)
        return newLiveStreamLoadResponse(k.ad, url, url) {
            posterUrl = k.logo
            plot = listOfNotNull(k.aciklama, "$name • ${k.grup}").joinToString("\n")
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val k = parseJson<Kanal>(data)
        val bulunan = runCatching { akislar(k) }.getOrElse { emptyList() }.distinctBy { it.url }
        bulunan.forEachIndexed { i, a ->
            val tip = if (".mp4" in a.url.substringBefore("?")) ExtractorLinkType.VIDEO else ExtractorLinkType.M3U8
            callback(newExtractorLink(name, a.ad ?: if (bulunan.size > 1) "$name ${i + 1}" else name, a.url, tip) {
                referer = a.referer
                quality = Qualities.Unknown.value
                headers = mapOf("User-Agent" to Spor.UA) + a.headers
            })
        }
        return bulunan.isNotEmpty()
    }
}
