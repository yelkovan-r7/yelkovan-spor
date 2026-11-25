package ys.leotv

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.base64DecodeArray
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import ys.ortak.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

// Ana sayfa: const serverData = [{sport, channel, home_name, away_name, time, league, home_icon, player_type}]
//            const iframeCdn = "https://player…/player/player2.php?id=";
// Oynatıcı: <div id="_vGuard" data-val="<b64 kaydırma>" data-enc="<b64 IV dizisi>">, var keyArray = [..],
//           var primaryStream = decryptUrl("<b64>")  → AES/CBC, anahtar/IV baytları = sayı - kaydırma.
class LeoTV : CanliSite() {
    override var mainUrl = "https://leograndtv11.com"
    override var name = "LeoTV"
    override val grupSirasi = listOf("beIN Sports", "S Sport & Tivibu", "Tabii Spor", "Ulusal & Spor Kanalları", "Futbol", "Basketbol")

    private val adaylar = listOf("https://leograndtv11.com/", "https://leograndtv10.com/")
    private var oynatici = "https://player.leograndtv11.com/player/player2.php?id="
    private var oynatici1x = "https://player.leograndtv11.com/player/onexbet.php?id="

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Oge(
        val sport: String? = null, val channel: String? = null,
        @JsonProperty("home_name") val ev: String? = null, @JsonProperty("away_name") val dep: String? = null,
        val time: String? = null, val league: String? = null, @JsonProperty("home_icon") val logo: String? = null,
        @JsonProperty("player_type") val tip: String? = null,
    )

    private fun kanalGrubu(ad: String): String {
        val k = ad.lowercase()
        return when {
            "bein" in k -> "beIN Sports"
            "s-sport" in k || "s sport" in k || "plus" in k || "tivibu" in k -> "S Sport & Tivibu"
            "tabii" in k -> "Tabii Spor"
            else -> "Ulusal & Spor Kanalları"
        }
    }

    override suspend fun kanallar(): List<Kanal> {
        Spor.alanBul("leotv", adaylar, "serverData")?.let { mainUrl = it.trimEnd('/') }
        val s = app.get("$mainUrl/", headers = Spor.H).text
        Regex("""iframeCdn\s*=\s*"([^"]+)"""").find(s)?.let { oynatici = it.groupValues[1] }
        Regex("""iframeXbetCdn\s*=\s*"([^"]+)"""").find(s)?.let { oynatici1x = it.groupValues[1] }
        val json = Regex("""const\s+serverData\s*=\s*(\[[\s\S]*?\]);""").find(s)?.groupValues?.get(1) ?: return emptyList()
        return tryParseJson<List<Oge>>(json).orEmpty().mapNotNull { o ->
            val kanal = o.channel?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val ev = o.ev?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val veri = if (o.tip == "onexbet") "1x:$kanal" else kanal
            val logo = o.logo?.takeIf { it.startsWith("http") }
            if (o.sport.equals("tv", true)) Kanal(ev, veri, logo, kanalGrubu(ev))
            else Kanal(listOfNotNull(o.time, "$ev - ${o.dep.orEmpty()}").joinToString(" "), veri, logo,
                o.sport?.replaceFirstChar { it.uppercase() } ?: "Maçlar", o.league)
        }
    }

    override suspend fun akislar(kanal: Kanal): List<Akis> {
        val oyn = if (kanal.veri.startsWith("1x:")) oynatici1x + kanal.veri.removePrefix("1x:") else oynatici + kanal.veri
        val h = app.get(oyn, headers = Spor.H, referer = "$mainUrl/").text
        Spor.m3u8(h)?.let { return listOf(Akis(it, Spor.kok(oyn))) }
        val val64 = Regex("""id=["']_vGuard["'][^>]*data-val=["']([^"']+)""").find(h)?.groupValues?.get(1) ?: return emptyList()
        val enc64 = Regex("""id=["']_vGuard["'][^>]*data-enc=["']([^"']+)""").find(h)?.groupValues?.get(1) ?: return emptyList()
        val anahtar = Regex("""var\s+keyArray\s*=\s*(\[[^\]]+\]);""").find(h)?.groupValues?.get(1) ?: return emptyList()
        val sifreli = Regex("""var\s+primaryStream\s*=\s*decryptUrl\(['"]([^'"]+)""").find(h)?.groupValues?.get(1) ?: return emptyList()
        val kayma = base64Decode(val64).trim().toInt()
        fun bayt(dizi: String) = tryParseJson<List<Int>>(dizi).orEmpty().map { (it - kayma).toByte() }.toByteArray()
        val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(bayt(anahtar), "AES"), IvParameterSpec(bayt(base64Decode(enc64))))
        val url = String(c.doFinal(base64DecodeArray(sifreli)), Charsets.UTF_8).trim()
        return listOf(Akis(url, Spor.kok(oyn)))
    }
}
