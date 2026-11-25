#!/usr/bin/env bash
# Yeni eklenti iskeleti oluşturur (build.gradle.kts + Plugin sınıfı).
# Kullanım: bash araclar/yeni-eklenti.sh <Ad> <alanadi> '<"Tip1", "Tip2">' "<açıklama>" [dil]
# Örnek:    bash araclar/yeni-eklenti.sh DiziX dizix.com '"TvSeries"' "DiziX - yabancı diziler."
set -e
AD="$1"; ALAN="$2"; TIPLER="$3"; ACIKLAMA="$4"; DIL="${5:-tr}"
[[ -z "$AD" || -z "$ALAN" || -z "$TIPLER" ]] && { sed -n 2,4p "$0"; exit 1; }
PAKET=$(echo "$AD" | tr 'A-Z' 'a-z' | tr -d '-')
SINIF=$(echo "$AD" | tr -d '-')
KOK="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$KOK/$AD/src/main/kotlin/ys/$PAKET"
cat > "$KOK/$AD/build.gradle.kts" <<EOF
// Sürüm numarası tam sayı olmalı; her değişiklikte bir artırın ki CloudStream güncellemeyi görsün.
version = 1

cloudstream {
    description = "$ACIKLAMA"
    authors = listOf("yelkovan-r7")

    /**
     * 0: Kapalı, 1: Çalışıyor, 2: Yavaş, 3: Sadece beta
     */
    status = 1

    tvTypes = listOf($TIPLER)
    language = "$DIL"

    iconUrl = "https://www.google.com/s2/favicons?sz=64&domain=$ALAN"
}
EOF
cat > "$KOK/$AD/src/main/kotlin/ys/$PAKET/${SINIF}Plugin.kt" <<EOF
package ys.$PAKET

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class ${SINIF}Plugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI($SINIF())
    }
}
EOF
echo "oluşturuldu: $AD (paket ys.$PAKET, sınıf $SINIF)"
