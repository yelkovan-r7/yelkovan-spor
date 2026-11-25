package ys.kacakspor

import ys.ortak.AndroAgi

// Kanallar ana sayfada <a data-name="…" data-stream="androstreamlive…">; oynatıcı /izle?id=.
class KacakSpor : AndroAgi() {
    override var mainUrl = "https://www.kacakspor5.com"
    override var name = "KaçakSpor"
    override val anahtar = "kacakspor"
    override val adaylar = listOf("https://www.kacakspor5.com/", "https://www.kacakspor4.com/")
    override val oynatici = "izle?id="
}
