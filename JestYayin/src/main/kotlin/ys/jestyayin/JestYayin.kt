package ys.jestyayin

import ys.ortak.ZirveAgi

class JestYayin : ZirveAgi() {
    override var mainUrl = "https://www.jestyayin971.com"
    override var name = "JestYayın"
    override val anahtar = "jestyayin"
    override val adaylar = listOf("https://www.jestyayin971.com/", "https://www.jestyayin970.com/")
    // Kısa bağlantı her zaman güncel adrese yönlendiriyor.
    override val gecitler = listOf("https://t2m.co/jestyayin")
    override val gecitKalibi = Regex("""https?://(?:www\.)?jestyayin\d+\.[a-z]+/?""")
}
