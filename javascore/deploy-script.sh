#!/bin/sh

if [ $# -eq 0 ]
  then
    echo "No arguments supplied, pass 'deployToLocal' or 'deployToSejong'"
fi

if [ "$1" = "deployToLocal" ]
  then
    # echo ">>>> Cleaning the project"
    # gradle BMC:clean
    # gradle bsh:clean
    # gradle bmv:clean
    # echo ">>>> Building Optimized Jar"
    # gradle BMC:optimizedJar
    # gradle bsh:optimizedJar
    # gradle bmv:optimizedJar
    echo ">>>> Deploying BMC"
    BMCLogs=$(gradle BMC:deployToLocal -PkeystoreName=./keys/keystore_god.json -PkeystorePass=gochain)
    BMC_SCORE_ADDRESS=$(echo "$BMCLogs" | grep -P '(cx)\w+' -o)
    echo "BMC Address: $BMC_SCORE_ADDRESS"
    echo ">>>> Deploying BSH"
    BSHLOGS=$(gradle bsh:deployToLocal -DBMC_ADDRESS="$BMC_SCORE_ADDRESS" -PkeystoreName=./keys/keystore_god.json -PkeystorePass=gochain)    
    BSH_SCORE=$(echo "$BSHLOGS" | grep -P '(cx)\w+' -o)
    echo "BSH Address: $BSH_SCORE"    
    echo ">>>> Deploying BMV"
    BMVLOGS=$(gradle bmv:deployToLocal -DBMC_ADDRESS="$BMC_SCORE_ADDRESS" -PkeystoreName=./keys/keystore_god.json -PkeystorePass=gochain)    
    BSH_SCORE=$(echo "$BMVLOGS" | grep -P '(cx)\w+' -o)
    BSH_TXN=$(echo "$BMVLOGS" | grep -P '(0x)\w+' -o)
    echo "BMV Address: $BSH_SCORE"    
    echo "BMV Deploy TXn: $BSH_TXN"    
fi


if [ "$1" = "deployToSejong" ]
  then
    # echo ">>>> Cleaning the project"
    # gradle BMC:clean
    # gradle bsh:clean
    # echo ">>>> Building Optimized Jar"
    # gradle BMC:optimizedJar
    # gradle bsh:optimizedJar
    echo ">>>> Deploying BMC"
    BMCLogs=$(gradle BMC:deployToSejong -PkeystoreName=keystore -PkeystorePass=Admin@123)
    BMC_SCORE_ADDRESS=$(echo "$BMCLogs" | grep -P '(cx)\w+' -o)
    echo "BMC Address: $BMC_SCORE_ADDRESS"
    echo ">>>> Deploying BSH"
    BSHLOGS=$(gradle bsh:deployToSejong -DBMC_ADDRESS="$BMC_SCORE_ADDRESS" -PkeystoreName=keystore -PkeystorePass=Admin@123)    
    BSH_SCORE=$(echo "$BSHLOGS" | grep -P '(cx)\w+' -o)
    echo "BSH Address: $BSH_SCORE"
     echo ">>>> Deploying BMV"
    BMVLOGS=$(gradle bmv:deployToSejong -DBMC_ADDRESS="$BMC_SCORE_ADDRESS" -PkeystoreName=keystore -PkeystorePass=Admin@123)    
    BSH_SCORE=$(echo "$BMVLOGS" | grep -P '(cx)\w+' -o)
    BSH_TXN=$(echo "$BMVLOGS" | grep -P '(0x)\w+' -o)
    echo "BMV Address: $BSH_SCORE"    
    echo "BMV Deploy TXn: $BSH_TXN"    
fi
  