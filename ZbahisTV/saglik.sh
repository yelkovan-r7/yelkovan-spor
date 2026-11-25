#!/usr/bin/env bash
# Zbahis/Baywin TV sağlık kontrolü: alan adı, kanal API'si ve yayın tabanı (domain.php).
# Not: yayın sunucusu Cloudflare korumalı olduğundan m3u8'in kendisi burada denenmiyor.
source "$(dirname "$0")/../saglik/ortak.sh"

SITE=$(alan_bul "channels.php" https://101baywintv.live/ https://102baywintv.live/ https://100baywintv.live/) || { hata "Güncel alan adı bulunamadı"; bitir; }
tamam "Alan adı: $SITE"
API=$(istek -L -A "$UA_TARAYICI" "$SITE/" | grep -oE "fetch\(['\"]https?://[^/'\"]+/channels\.php" | grep -oE 'https?://[^/]+' | head -1)
sayfa_iceriyor "$(istek -A "$UA_TARAYICI" -e "$SITE/" "$API/channels.php")" 'single-match' "Kanal listesi ($API)"
json_kontrol "$(istek -A "$UA_TARAYICI" -e "$SITE/" "$API/domain.php")" 'j.baseurl' "Yayın tabanı adresi"
bitir
