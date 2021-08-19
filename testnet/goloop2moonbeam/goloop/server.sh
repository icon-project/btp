#!/bin/sh

source keystore.sh

ensure_config() {
  # echo "ensure_config"

  local CONFIG=${1:-${GOLOOP_CONFIG:-/goloop/config/goloop.server.json}}
  if [ ! -f "${CONFIG}" ]; then
    export GOLOOP_KEY_SECRET=$(ensure_key_secret ${GOLOOP_KEY_SECRET:-/goloop/config/goloop.keysecret})
    export GOLOOP_KEY_STORE=$(ensure_key_store ${GOLOOP_KEY_STORE:-/goloop/config/goloop.keystore.json} ${GOLOOP_KEY_SECRET})
    if [ "$GOLOOP_CONFIG" != "" ]; then
      OLD_GOLOOP_CONFIG=$GOLOOP_CONFIG
      unset GOLOOP_CONFIG
    fi
    RESULT=$(goloop server save ${CONFIG} 2>&1)
    if [ "$OLD_GOLOOP_CONFIG" != "" ]; then
      export GOLOOP_CONFIG=${OLD_GOLOOP_CONFIG}
    fi
  fi
  echo ${CONFIG}
}

GOLOOP_PID=${GOLOOP_PID:-/var/run/goloop.pid}

server_start() {
  source /goloop/venv/bin/activate
  PID=$(server_pid)
  if [ -z "${PID}" ]; then
    export GOLOOP_LOG_WRITER_FILENAME=${GOLOOP_LOG_WRITER_FILENAME:-/goloop/data/goloop.log}
    export GOLOOP_CONFIG=$(ensure_config)
    goloop server start > /dev/null 2>&1 &
    echo "$!" > ${GOLOOP_PID}
    cat $GOLOOP_CONFIG | jq -r
  fi
}

ensure_server_start() {
  server_start

  RET=$(goloop system info > /dev/null 2>&1;echo $?)
  while [ "0" != "$RET" ]; do
    RET=$(goloop system info > /dev/null 2>&1;echo $?)
    sleep 1
  done
}

server_stop() {
  PID=$(server_pid)
  if [ ! -z "${PID}" ]; then
    local SIG=${1:-TERM}
    #kill -${SIG} ${PID}
    PGID=$(ps -o pid,pgid,comm | grep goloop | grep ${PID} | grep -v 'grep' | tr -s ' ' | cut -d ' ' -f 3)
    kill -${SIG} -${PGID}
    rm ${GOLOOP_PID}
  fi
}

server_pid() {
  if [ -f "${GOLOOP_PID}" ]; then
    PID=$(cat ${GOLOOP_PID})
    STATUS=$(ps -ef | grep $PID | grep -v grep)
    if [ ! -z "${STATUS}" ]; then
      echo ${PID}
    else
      rm ${GOLOOP_PID}
    fi
  fi
}
