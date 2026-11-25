#!/usr/bin/env bash
# PatronHD sağlık kontrolü: güncel alan adı, kanal/maç API'si ve yayın sunucusu adresi.
source "$(dirname "$0")/../saglik/ortak.sh"

SITE=$(alan_bul "channels.php" https://patronhd10.cfd/ https://patronhd8.cfd/) || { hata "Güncel alan adı bulunamadı"; bitir; }
tamam "Alan adı: $SITE"
API=$(istek -L -A "$UA_TARAYICI" "$SITE/" | grep -oE "https?://[^'\"]+/channels\.php" | head -1); API=${API%/channels.php}
[[ -n "$API" ]] && tamam "API: $API" || { hata "API adresi bulunamadı"; bitir; }
json_kontrol "$(istek -A "$UA_TARAYICI" -e "$SITE/" -H "Origin: $SITE" "$API/channels.php")" 'j.length > 5' "Kanal listesi"
json_kontrol "$(istek -A "$UA_TARAYICI" -e "$SITE/" -H "Origin: $SITE" "$API/domain.php")" 'j.baseurl' "Yayın sunucusu adresi"
bitir
