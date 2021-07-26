#!/bin/sh
set -e

source /btpsimple/scripts/deploy_solidity.sh
source /btpsimple/scripts/deploy_javascore.sh

while true
do
    latest_blocknumber_goloop()
    sleep 1
done
