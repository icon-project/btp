#!/bin/sh
set -e

cp -u /btpsimple/moonbeam/* /btpsimple/config/
source provision.sh
while true
do
    echo "ping..."
    sleep 1
done
