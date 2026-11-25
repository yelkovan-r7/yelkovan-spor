package ys.zbahistv

import ys.ortak.ZirveAgi

class ZbahisTV : ZirveAgi() {
    override var mainUrl = "https://101baywintv.live"
    override var name = "Zbahis TV"
    override val anahtar = "zbahis"
    override val adaylar = listOf("https://101baywintv.live/", "https://100baywintv.live/")
}
