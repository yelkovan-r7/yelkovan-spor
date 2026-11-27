#!/usr/bin/env bash
# InatTV sağlık kontrolü: alan adı, kanal listesi ve auth.php'nin yayın adresi vermesi.
# Not: yayın sunucusu Cloudflare ile Türkiye'yi engelleyebildiği için akışın kendisi denenmiyor.
source "$(dirname "$0")/../saglik/ortak.sh"

SITE=$(alan_bul "phptojs" https://www.inattvizle500.top/ https://www.inattvizle501.top/ https://www.inatgiris.com/) || { hata "Güncel alan adı bulunamadı"; bitir; }
tamam "Alan adı: $SITE"
sayfa_iceriyor "$(istek -L -A "$UA_TARAYICI" "$SITE/")" 'data-target="viptv"' "Kanal listesi"
json_kontrol "$(istek -A "$UA_TARAYICI" -e "$SITE/" -H "Origin: $SITE" --data "channel=100001" "$SITE/auth.php")" 'j.URL' "Yayın adresi (auth.php)"
bitir
