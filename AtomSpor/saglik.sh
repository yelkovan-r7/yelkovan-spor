#!/usr/bin/env bash
# AtomSpor sağlık kontrolü: güncel alan adı, teletv kanal listesi ve bir kanalın m3u8 akışı.
source "$(dirname "$0")/../saglik/ortak.sh"

SITE=$(alan_bul "teletv" https://www.atomsportv520.top/ https://atomsportv519.top/) || { hata "Güncel alan adı bulunamadı"; bitir; }
tamam "Alan adı: $SITE"
TELETV=$(istek -L -A "$UA_TARAYICI" "$SITE/" | grep -oE 'teletv[0-9]+\.[a-z]+' | head -1)
kanallar=$(istek -A "$UA_TARAYICI" -e "$SITE/" "https://$TELETV/load/channels.php")
sayfa_iceriyor "$kanallar" 'class="single-match' "Kanal listesi ($TELETV)"
id=$(grep -oE 'matches\?id=[^"]+' <<<"$kanallar" | head -1 | cut -d= -f2)
yayin=$(istek -A "$UA_TARAYICI" -e "$SITE/" "https://$TELETV/load/yayinlink.php?id=$id")
m3u8=$(grep -oE 'https?:[^"]+m3u8[^"]*' <<<"$yayin" | tr -d '\\' | head -1)
link_acik "$m3u8" "Kanal yayını ($id)"
bitir
