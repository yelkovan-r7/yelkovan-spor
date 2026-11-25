package ys.kralsporhd

import ys.ortak.AndroAgi

// Liste harici bir betikten (…/script4.js) geliyor; yayınlar androstream ağında.
class KralSporHD : AndroAgi() {
    override var mainUrl = "https://kralsportshd8.xyz"
    override var name = "KralSportsHD"
    override val anahtar = "kralspor"
    override val adaylar = listOf("https://kralsportshd8.xyz/", "https://kralsportshd7.xyz/")
    override val gecitler = listOf("https://kralsportshd.com/giris2/", "https://kralsportshd.com/giris/")
    override val gecitKalibi = Regex("""https?://(?:www\.)?kralsportshd\d+\.[a-z]+/?""")
}
