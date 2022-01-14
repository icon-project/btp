#!/bin/sh

GOLOOP_PID=${GOLOOP_PID:-/var/run/goloop.pid}

server_start() {
  source /goloop/venv/bin/activate
  PID=$(server_pid)
  if [ -z "${PID}" ]; then
    goloop server start > /dev/null 2>&1 &
    echo "$!" > ${GOLOOP_PID}
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
