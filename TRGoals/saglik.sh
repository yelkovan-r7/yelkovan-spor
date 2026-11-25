#!/usr/bin/env bash
# TRGoals sağlık kontrolü: alan adı, kanal listesi ve auth.php'nin yayın adresi vermesi.
# Not: yayın sunucusu Cloudflare ile Türkiye'yi engelleyebildiği için akışın kendisi denenmiyor.
source "$(dirname "$0")/../saglik/ortak.sh"

SITE=$(alan_bul "phptojs" https://www.trgoals183.top/ https://www.trgoals184.top/ https://www.trgoals182.top/) || { hata "Güncel alan adı bulunamadı"; bitir; }
tamam "Alan adı: $SITE"
sayfa_iceriyor "$(istek -L -A "$UA_TARAYICI" "$SITE/")" 'data-target="viptv"' "Kanal listesi"
json_kontrol "$(istek -A "$UA_TARAYICI" -e "$SITE/" -H "Origin: $SITE" --data "channel=100002" "$SITE/auth.php")" 'j.URL' "Yayın adresi (auth.php)"
bitir
