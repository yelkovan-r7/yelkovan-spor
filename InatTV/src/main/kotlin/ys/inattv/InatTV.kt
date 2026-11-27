package ys.inattv

import ys.ortak.VipTv

// TRGoals/PapazSports ile aynı yazılım (auth.php). Giriş: inatgiris.com → inattvizleNNN.top
class InatTV : VipTv() {
    override var mainUrl = "https://www.inattvizle500.top"
    override var name = "InatTV"
    override val anahtar = "inattv"
    override val adaylar = listOf("https://www.inattvizle500.top/", "https://www.inattvizle499.top/")
    override val gecitler = listOf("https://www.inatgiris.com/")
    override val gecitKalibi = Regex("""https?://(?:www\.)?inattvizle\d+\.[a-z]+/?""")
}
