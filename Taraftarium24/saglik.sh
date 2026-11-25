#!/usr/bin/env bash
# Taraftarium24 sağlık kontrolü: güncel alan adı, kanal listesi ve oynatıcıdaki yayın sunucusu.
source "$(dirname "$0")/../saglik/ortak.sh"

SITE=$(alan_bul "channel.html?id=" https://taraftarium1090.xyz/ https://taraftarium1089.xyz/) || { hata "Güncel alan adı bulunamadı"; bitir; }
tamam "Alan adı: $SITE"
ana=$(istek -L -A "$UA_TARAYICI" "$SITE/")
sayfa_iceriyor "$ana" 'class="channel-item"' "Kanal listesi"
oyn=$(istek -L -A "$UA_TARAYICI" -e "$SITE/" "$SITE/channel.html?id=b2")
sayfa_iceriyor "$oyn" "baseUrl" "Oynatıcıda yayın sunucusu"
bitir
