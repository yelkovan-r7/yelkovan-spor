#!/usr/bin/env bash
# InterSporTV sağlık kontrolü: alan adı, maç listesi ve ortak maç servisinin akış vermesi.
source "$(dirname "$0")/../saglik/ortak.sh"

SITE=$(alan_bul "betlivematch-" https://intersportv1.live/ https://intersportv2.live/) || { hata "Güncel alan adı bulunamadı"; bitir; }
tamam "Alan adı: $SITE"
ana=$(istek -L -A "$UA_TARAYICI" "$SITE/")
sayfa_iceriyor "$ana" 'data-stream="betlivematch-' "Maç listesi"
id=$(grep -oE 'data-stream="betlivematch-[0-9]+"' <<<"$ana" | head -1 | grep -oE '[0-9]+')
r=$(istek -A "$UA_TARAYICI" -H "Content-Type: application/json" --data "{\"AppId\":\"5000\",\"AppVer\":\"1\",\"VpcVer\":\"1.0.12\",\"Language\":\"en\",\"Token\":\"\",\"VideoId\":\"$id\"}" https://streamsport365.com/cinema)
json_kontrol "$r" 'j.URL' "Maç servisi yayın adresi ($id)"
bitir
