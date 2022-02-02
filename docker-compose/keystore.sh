#!/bin/sh

ensure_key_secret() {
  if [ $# -lt 1 ] ; then
    echo "Usage: ensure_key_secret SECRET_PATH"
    return 1
  fi
  local KEY_SECRET=$1
  if [ ! -f "${KEY_SECRET}" ]; then
    mkdir -p $(dirname ${KEY_SECRET})
    echo -n $(date|md5sum|head -c16) > ${KEY_SECRET}
  fi
  echo ${KEY_SECRET}
}

ensure_key_store() {
  if [ $# -lt 2 ] ; then
    echo "Usage: ensure_key_store KEYSTORE_PATH SECRET_PATH"
    return 1
  fi
  local KEY_STORE=$1
  local KEY_SECRET=$(ensure_key_secret $2)
  if [ ! -f "${KEY_STORE}" ]; then
    goloop ks gen --out $KEY_STORE -p $(cat ${KEY_SECRET}) > /dev/null 2>&1
  fi
  echo ${KEY_STORE}
}