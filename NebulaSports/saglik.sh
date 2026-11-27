#!/usr/bin/env bash
# NebulaSports sağlık kontrolü: ücretsiz kişisel bağlantı üretimi, maç kataloğu ve bir maçın yayın listesi.
source "$(dirname "$0")/../saglik/ortak.sh"
SITE=https://sports.nebulapro.xyz
yer=$(istek -o /dev/null -A "$UA_TARAYICI" --data "quick=1" -w "%{redirect_url}" "$SITE/sports/configure")
yol=$(grep -oE '/sports/i/[^/?#]+' <<<"$yer")
[[ -n "$yol" ]] && tamam "Kişisel bağlantı üretildi" || { hata "Kişisel bağlantı üretilemedi ($yer)"; bitir; }
kat=$(istek -A "$UA_TARAYICI" "$SITE$yol/catalog/sports/all.json")
json_kontrol "$kat" 'j.metas.length > 0' "Maç kataloğu"
id=$(json_al "$kat" 'encodeURIComponent(j.metas[0].id)')
json_kontrol "$(istek -A "$UA_TARAYICI" "$SITE$yol/stream/sports/$id.json")" 'Array.isArray(j.streams)' "Yayın listesi"
bitir
