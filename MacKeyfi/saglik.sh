#!/usr/bin/env bash
# MaçKeyfi sağlık kontrolü: güncel alan adı, kanal listesi, oynatıcı ve doğrulama yanıtı.
source "$(dirname "$0")/../saglik/ortak.sh"

SITE=$(alan_bul "/tv/" https://www.mackeyfi605.sbs/ https://www.canlimaclar615.sbs/) || { hata "Güncel alan adı bulunamadı"; bitir; }
tamam "Alan adı: $SITE"
sayfa=$(istek -L -A "$UA_TARAYICI" "$SITE/tv/bein-sports-1/")
oyn=$(grep -oE 'data-player-url="[^"#]+' <<<"$sayfa" | head -1 | cut -d'"' -f2)
[[ -n "$oyn" ]] && tamam "Oynatıcı adresi" || { hata "Kanal sayfasında oynatıcı yok"; bitir; }
id=${oyn##*id=}
h=$(istek -L -A "$UA_TARAYICI" -e "$SITE/" "$oyn")
sayfa_iceriyor "$h" "streamradardomil" "Oynatıcıda yayın alan adı"
t=$(istek -A "$UA_TARAYICI" -e "$oyn" "$SITE/t?id=$id")
sayfa_iceriyor "$t" "?verify=" "Doğrulama anahtarı"
bitir
