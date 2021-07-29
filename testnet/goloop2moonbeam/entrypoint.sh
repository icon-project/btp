#!/bin/sh
set -e

cp -u /btpsimple/moonbeam/* /btpsimple/config/
source provision.sh

if [ "$BTPSIMPLE_OFFSET" != "" ] && [ -f "$BTPSIMPLE_OFFSET" ]; then
    export BTPSIMPLE_OFFSET=$(cat ${BTPSIMPLE_OFFSET})
fi

if [ "$BTPSIMPLE_CONFIG" != "" ] && [ ! -f "$BTPSIMPLE_CONFIG" ]; then
    UNSET="BTPSIMPLE_CONFIG"
    CMD="btpsimple save $BTPSIMPLE_CONFIG"
    if [ "$BTPSIMPLE_KEY_SECRET" != "" ] && [ ! -f "$BTPSIMPLE_KEY_SECRET" ]; then
        mkdir -p $(dirname $BTPSIMPLE_KEY_SECRET)
        echo -n $(date|md5sum|head -c16) > $BTPSIMPLE_KEY_SECRET
    fi
    if [ "$BTPSIMPLE_KEY_STORE" != "" ] && [ ! -f "$BTPSIMPLE_KEY_STORE" ]; then
        UNSET="$UNSET BTPSIMPLE_KEY_STORE"
        CMD="$CMD --save_key_store=$BTPSIMPLE_KEY_STORE"
    fi
    if [ "$BTPSIMPLE_OFFSET" != "" ] && [ -f "$BTPSIMPLE_OFFSET" ]; then
        export BTPSIMPLE_OFFSET=$(cat ${BTPSIMPLE_OFFSET})
    fi

    if [ "$BTPSIMPLE_SRC_ADDRESS" != "" ] && [ -f "$BTPSIMPLE_SRC_ADDRESS" ]; then
    export BTPSIMPLE_SRC_ADDRESS=$(cat ${BTPSIMPLE_SRC_ADDRESS})
    fi
    if [ "$BTPSIMPLE_SRC_ENDPOINT" != "" ] && [ -f "$BTPSIMPLE_SRC_ENDPOINT" ]; then
        export BTPSIMPLE_SRC_ENDPOINT=$(cat ${BTPSIMPLE_SRC_ENDPOINT})
    fi
    if [ "$BTPSIMPLE_DST_ADDRESS" != "" ] && [ -f "$BTPSIMPLE_DST_ADDRESS" ]; then
        export BTPSIMPLE_DST_ADDRESS=$(cat ${BTPSIMPLE_DST_ADDRESS})
    fi
    if [ "$BTPSIMPLE_DST_ENDPOINT" != "" ] && [ -f "$BTPSIMPLE_DST_ENDPOINT" ]; then
        export BTPSIMPLE_DST_ENDPOINT=$(cat ${BTPSIMPLE_DST_ENDPOINT})
    fi
    sh -c "unset $UNSET ; $CMD"
fi
exec "$@"
