#!/usr/bin/env bash
# ArdaSpor sağlık kontrolü: güncel alan adı, kanal listesi ve loadstream'deki paketli m3u8.
source "$(dirname "$0")/../saglik/ortak.sh"

SITE=$(alan_bul "/canli-mac/" https://ardaspor41.top/ https://ardaspor37.top/ https://ardaspor34.top/) || { hata "Güncel alan adı bulunamadı"; bitir; }
tamam "Alan adı: $SITE"
ana=$(istek -L -A "$UA_TARAYICI" "$SITE/")
sayfa_iceriyor "$ana" '/canli-mac/bein-sports-1' "Kanal listesi"
yukle=$(istek -A "$UA_TARAYICI" -e "$SITE/" "$SITE/channel/watch/bein-sports-1" | grep -oE 'loadstream\.php[^"]+' | head -1)
oyn=$(istek -A "$UA_TARAYICI" -e "$SITE/" "$SITE/$yukle")
b64=$(perl -ne 'if(/\}\(\x27[^\x27]*?"0(=*)"[^\x27]*\x27,\d+,\d+,\x27([^|\x27]+)/){print "$2$1"}' <<<"$oyn")
m3u8=$(base64 -d <<<"$b64" 2>/dev/null | grep -oE 'source:"[^"]+"' | cut -d'"' -f2)
link_acik "$m3u8" "beIN Sports 1 yayını"
bitir
