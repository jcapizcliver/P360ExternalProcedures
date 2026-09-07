#!/usr/bin/env bash
set -euo pipefail
cd /u01/workshop/java
java=./jdk/jdk-17.0.12/bin/java
release=/u01/workshop/java/releases/mandatory-incremental-20260907
mapfile -t old < <(pgrep -f 'java.*mx[.]com[.]liverpool[.]p360[.]services[.]core[.]amqp[.]P360ActiveMQBPMStage' || true)
if (( ${#old[@]} > 1 )); then echo 'Multiple listeners found; stop and inspect'; exit 2; fi
if (( ${#old[@]} == 1 )); then
  "$java" -cp 'bin:lib/*:libPubSub/*' mx.com.liverpool.p360.services.core.Apagalo localhost 23543
  for i in {1..30}; do kill -0 "${old[0]}" 2>/dev/null || break; sleep 1; done
  if kill -0 "${old[0]}" 2>/dev/null; then echo 'Listener did not stop; no replacement started'; exit 3; fi
fi
if [[ "${1:-}" == '--deploy' ]]; then python3 "$release/deploy.py"; fi
if [[ -f bpm_to_pubsub ]]; then
  mkdir -p "$release/previous-logs"
  mv bpm_to_pubsub "$release/previous-logs/bpm_to_pubsub.$(date +%Y%m%d-%H%M%S)"
fi
export P360_SERVER_PROPERTIES=/u01/Informatica/server.properties
export EXTERNAL_CONFIG_PATH=/u01/workshop/p360_contingencyservices.properties
export P360_COMPLETENESS_API_HOST=gcpcatpap02
nohup "$java" -cp 'bin:lib/*:libPubSub/*' -Dlogback.configurationFile=./logback.xml \
  mx.com.liverpool.p360.services.core.amqp.P360ActiveMQBPMStage \
  gcpcatpap02.liverpool.com.mx 61616 STATUS_CHANGES > bpm_to_pubsub 2>&1 < /dev/null &
echo "$!" > activemq-listener.pid
echo "LISTENER_PID=$!"
