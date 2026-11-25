package ys.papazsports

import ys.ortak.VipTv

class PapazSports : VipTv() {
    override var mainUrl = "https://www.papazsports1036.pro"
    override var name = "PapazSports"
    override val anahtar = "papazsports"
    override val adaylar = listOf("https://www.papazsports1036.pro/", "https://www.papazsports1032.pro/")
    // Kısa bağlantı her zaman güncel adrese yönlendiriyor.
    override val gecitler = listOf("https://bit.ly/m/papazsports")
    override val gecitKalibi = Regex("""https?://(?:www\.)?papazsports\d+\.[a-z]+/?""")
}
