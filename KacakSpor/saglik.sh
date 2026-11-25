#!/usr/bin/env bash
# KaçakSpor sağlık kontrolü: alan adı, data-stream kanal listesi ve checklist m3u8 sunucusu.
source "$(dirname "$0")/../saglik/ortak.sh"

SITE=$(alan_bul "androstreamlive" https://www.kacakspor5.com/ https://www.kacakspor6.com/) || { hata "Güncel alan adı bulunamadı"; bitir; }
tamam "Alan adı: $SITE"
sayfa_iceriyor "$(istek -L -A "$UA_TARAYICI" "$SITE/")" 'data-stream="androstreamlive' "Kanal listesi"
sunucu=$(istek -A "$UA_TARAYICI" -e "$SITE/" "$SITE/izle?id=androstreamlivebs2" | grep -oE 'https:[^"'"'"' ]+checklist[^"'"'"' ]{0,2}' | tr -d '\' | head -1)
link_acik "${sunucu}androstreamlivebs2.m3u8" "beIN Sports 2 yayını"
bitir
