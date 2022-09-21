#!/bin/bash

export CWD=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
export BASE_DIR=${CWD}/../
export VARS="${CWD}/.vars.json"

echo "CWD=$CWD"
echo "PWD=$PWD"

function props() {
    echo "$(sed -e 's/^"//' -e 's/"$//' <<< $(cat ${VARS} | jq ${1}))"
}
