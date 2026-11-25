#!/usr/bin/env bash
# MahsunSports sağlık kontrolü: güncel alan adı, liste betiği ve checklist m3u8 sunucusu.
source "$(dirname "$0")/../saglik/ortak.sh"

SITE=$(alan_bul "event.html?id=" https://mahsunsports.xyz/ https://mahsunsports55.xyz/) || { hata "Güncel alan adı bulunamadı"; bitir; }
tamam "Alan adı: $SITE"
betik=$(istek -L -A "$UA_TARAYICI" "$SITE/" | grep -oE "<script[^>]+src=\"[^\"]*script[0-9]*\.js\"" | sed -E "s/.*src=\"//; s/\"$//" | head -1)
[[ "$betik" != http* ]] && betik="$SITE/${betik#/}"
js=$(istek -A "$UA_TARAYICI" -e "$SITE/" "$betik")
sayfa_iceriyor "$js" 'const channels' "Kanal listesi betiği"
sunucu=$(istek -A "$UA_TARAYICI" -e "$SITE/" "$SITE/event.html?id=androstreamlivebs2" | grep -oE 'https://[^"'"'"' ]+/checklist/' | head -1)
link_acik "${sunucu}androstreamlivebs2.m3u8" "beIN Sports 2 yayını"
bitir
