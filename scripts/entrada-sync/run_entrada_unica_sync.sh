#!/usr/bin/env bash
set -eo pipefail
umask 077
BASE=/u01/workshop/java
cd "$BASE"
source "$HOME/.bash_profile"
source "$HOME/.config/p360/entrada-sync.env"
set -u
: "${P360_ENTRADA_MONGO_URI:?Falta configurar P360_ENTRADA_MONGO_URI}"
OUT="${1:?Indica un directorio NUEVO para la corrida}"
shift
mkdir -p "$(dirname "$OUT")"
exec 9>"$BASE/.entrada-unica-sync.lock"
flock -n 9 || { echo "Ya hay una conciliacion en ejecucion"; exit 73; }
JAVA="$BASE/jdk/jdk-17.0.12/bin/java"
RELEASE="$BASE/releases/entrada-sync-20260908/final"
set +e
"$JAVA" -Xms128m -Xmx768m -Dfile.encoding=UTF-8 \
  -Dlogback.configurationFile="$RELEASE/logback-sync.xml" \
  -Dp360.pubsub.publish.timeout.seconds=30 \
  -cp "$RELEASE/classes:$BASE/bin:$BASE/lib/*:$BASE/lib/mongodb/*:$BASE/libPubSub/*" \
  mx.com.liverpool.p360.services.core.reconciliation.EntradaUnicaSync "$OUT" "$@"
rc=$?
printf '%s\n' "$rc" > "${OUT}.exit"
printf 'FIN exit=%s fecha=%s\n' "$rc" "$(date -Iseconds)"
exit "$rc"
