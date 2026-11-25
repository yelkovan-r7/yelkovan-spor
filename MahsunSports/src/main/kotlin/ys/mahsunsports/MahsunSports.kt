package ys.mahsunsports

import ys.ortak.AndroAgi

// Liste sitenin kendi script4.js dosyasında; yayınlar androstream ağında.
class MahsunSports : AndroAgi() {
    override var mainUrl = "https://mahsunsports.xyz"
    override var name = "MahsunSports"
    override val anahtar = "mahsun"
    override val adaylar = listOf("https://mahsunsports.xyz/", "https://mahsunsports55.xyz/")
    override val gecitler = listOf("https://mahsunsports.com/")
    override val gecitKalibi = Regex("""https?://(?:www\.)?mahsunsports\d*\.[a-z]+/?""")
}
