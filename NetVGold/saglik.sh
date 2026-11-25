#!/usr/bin/env bash
# NetVGold sağlık kontrolü: kaynak deposunun dosya listesi ve AtomSpor/androstream kaynaklarından birer yayın.
source "$(dirname "$0")/../saglik/ortak.sh"

agac=$(istek -L "https://api.github.com/repos/icebug13/turbo-guacamoled/git/trees/main?recursive=1")
json_kontrol "$agac" 'j.tree.filter(t => /^streams\d?\/.+\.m3u8$/.test(t.path)).length > 20' "Kaynak deposu dosya listesi"
for f in streams3/bein-sports-1.m3u8 streams2/stream_androstreamlivebs1.m3u8; do
  u=$(istek -L "https://raw.githubusercontent.com/icebug13/turbo-guacamoled/main/$f" | grep -m1 '^http')
  link_acik "$u" "Yayın ($f)"
done
bitir
