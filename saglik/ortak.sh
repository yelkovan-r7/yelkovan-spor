#!/usr/bin/env bash
# Eklenti sağlık kontrolleri için ortak yardımcılar. Her eklentinin saglik.sh dosyası bunu yükler.
HATALAR=()
UA_TARAYICI="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36"

# Türkiye'de spor siteleri DNS ile engelli; yerelde SAGLIK_DOH=1 ile Cloudflare DNS-over-HTTPS kullan.
DOH=(); [[ -n "$SAGLIK_DOH" ]] && DOH=(--doh-url https://cloudflare-dns.com/dns-query)

istek() { curl -s -m 30 --retry 2 --retry-all-errors "${DOH[@]}" "$@"; }

# Adaylardan ilk açılanın (yönlendirme izlenerek) kök adresini yazar; sayfada işaret metni aranır.
# Kullanım: SITE=$(alan_bul "işaret" https://a1.top/ https://a2.top/)
alan_bul() {
  local isaret="$1"; shift
  local u son gecici; gecici=$(mktemp)
  for u in "$@"; do
    son=$(curl -s -m 20 -L "${DOH[@]}" -A "$UA_TARAYICI" -o "$gecici" -w "%{http_code} %{url_effective}" "$u")
    if [[ "${son%% *}" == "200" ]] && grep -qF -- "$isaret" "$gecici"; then
      rm -f "$gecici"; grep -oE "^https?://[^/]+" <<<"${son#* }"; return 0
    fi
  done
  rm -f "$gecici"; return 1
}

hata() { HATALAR+=("$1"); echo "  ✗ $1"; }
tamam() { echo "  ✓ $1"; }

# Site başka bir adrese yönlendiriyorsa (domain değişimi) haber ver.
adres_ayni() {
  local url="$1" son
  son=$(curl -s -o /dev/null -m 30 --retry 2 --retry-all-errors "${DOH[@]}" -L -A "$UA_TARAYICI" -w "%{http_code} %{url_effective}" "$url")
  if [[ "${son%% *}" != "200" ]]; then hata "Site açılmıyor: $url (HTTP ${son%% *})"
  elif [[ "${son#* }" != "$url" ]]; then hata "Site adres değiştirmiş: $url → ${son#* }"
  else tamam "Site adresi aynı ($url)"; fi
}

# JSON üzerinde JavaScript ifadesi çalıştırır; ifadede ayrıştırılmış JSON "j" adıyla kullanılır.
# (jq yerine node: hem Windows'ta hem GitHub sunucusunda hazır geliyor.)
json_js() {
  node -e '
    let s = ""; process.stdin.on("data", d => s += d).on("end", () => {
      try { const j = JSON.parse(s); const r = eval(process.argv[1]); console.log(r ?? ""); process.exit(r ? 0 : 1); }
      catch (e) { process.exit(1); }
    });' "$2" <<<"$1"
}

# JSON yanıtının ifadeyi sağladığını kontrol et: json_kontrol "$veri" 'j.liste.length > 0' "Ad"
json_kontrol() {
  local veri="$1" ifade="$2" ad="$3"
  if json_js "$veri" "$ifade" >/dev/null 2>&1; then tamam "$ad"
  else hata "$ad beklenen yapıda değil: $(head -c 200 <<<"$veri")"; fi
}

# JSON'dan tek bir değer oku: id=$(json_al "$veri" 'j.data[0].ID')
json_al() { json_js "$1" "$2" 2>/dev/null; }

# HTML sayfasında eklentinin dayandığı bir işaret (ör. class="tv-overview") hâlâ var mı.
# Site tasarımı değişince seçiciler bozulur; bu kontrol onu yakalar: sayfa_iceriyor "$html" 'class="grid-box"' "Bölüm kartları"
sayfa_iceriyor() {
  local html="$1" metin="$2" ad="$3"
  if grep -qF -- "$metin" <<<"$html"; then tamam "$ad"
  else hata "$ad bulunamadı (aranan: $metin)"; fi
}

# Video/akış linki gerçekten açılıyor mu (ilk baytı iste).
link_acik() {
  local url="$1" ad="$2" kod
  [[ -z "$url" ]] && { hata "$ad: link boş geldi"; return; }
  kod=$(curl -s -L -o /dev/null -m 30 --retry 2 "${DOH[@]}" -r 0-0 -A "$UA_TARAYICI" -w "%{http_code}" "$url")
  if [[ "$kod" == "200" || "$kod" == "206" ]]; then tamam "$ad"
  else hata "$ad açılmıyor (HTTP $kod): $url"; fi
}

bitir() {
  if (( ${#HATALAR[@]} )); then
    printf '%s\n' "${HATALAR[@]}" > "${SAGLIK_CIKTI:-/dev/null}"
    exit 1
  fi
}
