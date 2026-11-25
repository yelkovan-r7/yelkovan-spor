#!/usr/bin/env bash
# Tüm eklentilerin saglik.sh kontrollerini çalıştırır, bozulanları rapor.md dosyasına yazar.
# Çıkış kodu: 0 = hepsi sağlıklı, 1 = en az bir eklenti bozuk.
cd "$(dirname "$0")/.."
RAPOR="${RAPOR:-saglik/rapor.md}"
: > "$RAPOR"
bozuk=0

for betik in */saglik.sh; do
  eklenti="${betik%%/*}"
  echo "== $eklenti"
  cikti=$(mktemp)
  if ! SAGLIK_CIKTI="$cikti" bash "$betik"; then
    bozuk=1
    { echo "### $eklenti"; sed 's/^/- /' "$cikti"; echo; } >> "$RAPOR"
  fi
  rm -f "$cikti"
done

exit $bozuk
