#!/usr/bin/env bash
# Betmatik TV sağlık kontrolü: alan adı, kanal API'si ve yayın tabanı (domain.php).
# Not: yayın sunucusu Cloudflare korumalı olduğundan m3u8'in kendisi burada denenmiyor.
source "$(dirname "$0")/../saglik/ortak.sh"

SITE=$(alan_bul "channels.php" https://betmatiktv156.com/ https://betmatiktv157.com/ https://betmatiktv155.com/) || { hata "Güncel alan adı bulunamadı"; bitir; }
tamam "Alan adı: $SITE"
API=$(istek -L -A "$UA_TARAYICI" "$SITE/" | grep -oE "fetch\(['\"]https?://[^/'\"]+/channels\.php" | grep -oE 'https?://[^/]+' | head -1)
sayfa_iceriyor "$(istek -A "$UA_TARAYICI" -e "$SITE/" "$API/channels.php")" 'single-match' "Kanal listesi ($API)"
json_kontrol "$(istek -A "$UA_TARAYICI" -e "$SITE/" "$API/domain.php")" 'j.baseurl' "Yayın tabanı adresi"
bitir
