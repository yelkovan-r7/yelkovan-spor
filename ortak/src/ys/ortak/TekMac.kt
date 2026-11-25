package ys.ortak

import org.jsoup.Jsoup

/**
 * Birçok spor sitesinin ortak liste şablonu:
 *   <a class="single-match" href="...?id=<id>" data-matchtype="tv|Futbol|...">
 *     <div class="event">7/24 | 23:00 | Lig</div> <div class="home">Ev</div> <div class="away">Deplasman</div>
 *   </a>
 * [kanalTurleri]: data-matchtype değeri bunlardan biriyse satır 7/24 kanal sayılır.
 */
object TekMac {
    fun ayristir(html: String, kanalTurleri: Set<String> = setOf("tv")): List<Kanal> =
        Jsoup.parse(html).select("a.single-match[href*=id=]").mapNotNull { a ->
            val id = a.attr("href").substringAfter("id=").substringBefore("&").ifBlank { return@mapNotNull null }
            val tur = a.attr("data-matchtype").trim()
            val ev = a.selectFirst(".home")?.text()?.trim().orEmpty()
            val dep = a.selectFirst(".away")?.text()?.trim().orEmpty()
            val olay = a.selectFirst(".event")?.text()?.trim().orEmpty()
            val kanalMi = tur.lowercase() in kanalTurleri.map { it.lowercase() } || "7/24" in olay
            if (kanalMi) {
                val ad = ev.ifBlank { olay.removePrefix("7/24").trim() }.ifBlank { a.text() }
                Kanal(ad, id, grup = "Kanallar")
            } else {
                val saat = olay.substringBefore("|").trim()
                val lig = olay.substringAfter("|", "").trim().ifBlank { null }
                val ad = if (ev.isNotBlank()) "$saat $ev - $dep".trim() else olay
                val spor = a.selectFirst(".date")?.text()?.trim()?.ifBlank { null } ?: tur.ifBlank { "Maçlar" }
                Kanal(ad, id, a.selectFirst("img")?.attr("src")?.takeIf { it.startsWith("http") }, spor, lig)
            }
        }
}
