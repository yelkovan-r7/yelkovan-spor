#!/usr/bin/env bash
# LeoTV sağlık kontrolü: güncel alan adı ve kanal verisi.
source "$(dirname "$0")/../saglik/ortak.sh"

SITE=$(alan_bul "serverData" https://leograndtv11.com/ https://leograndtv10.com/) || { hata "Güncel alan adı bulunamadı"; bitir; }
tamam "Alan adı: $SITE"
ana=$(istek -L -A "$UA_TARAYICI" "$SITE/")
grep -qE 'const serverData = \[\s*\{' <<<"$ana" && tamam "Kanal verisi" || hata "Kanal verisi boş (const serverData = [])"
bitir
