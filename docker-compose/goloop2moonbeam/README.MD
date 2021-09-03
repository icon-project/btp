# Docker integration of BTP betwwen ICON and MOONBEAM

This document will walk you through setting up a Docker environment to demonstrate how to run BTP between ICON and MOONBEAM.   
(Tested on macos 11.4 and ubuntu 18.04+)

This is for development usage only, because running Blockchain Nodes requires larger computer resources. 

## Prerequisites:  
```
- docker version 20.10.5+
- docker-compose version 1.29.0+
- make
- zip
- go version 1.16.3
```

## Setup steps:  

For easy setup, we recommend to use a fresh ubuntu 18.04. Hence, run those commands to installe prerequisite tools
```
apt update
apt install git make zip golang-go docker.io docker-compose --fix-missing -y
sudo groupadd docker
sudo usermod -aG docker $USER
```

For Mac OS, we recommend to use homebrew for prerequisites
```
brew install git make go dokcer docker-compose
```

## Computer specs

At least 16 CPUs, 16 RAM.

For Mac OS, you need to setup docker engine resoures after install docker

### Build image btpsimple:latest

```
cd ~/work
git clone git@github.com:icon-project/btp.git
cd ~/work/btp
git fetch
git checkout icondao
make btpsimple-image
```

### Run docker-compose

```
cd ~/work/btp/docker-compose/goloop2moonbeam
make build
make run
```

Now all that remains is to wait for the provisioning process to be completed.    
The folder `~/work/btp/testnet/goloop2moonbeam/config` will contain all of the configuration files.   
Note: If you want a fresh test, run `make clean` to remove the old test data.

### Run test scenarios:

Keep the docker-compose running and open an other terminal.  
Run this command to transfer the native coin ICX from ICON to MOONBEAN
```
make transfer_icx
```

Run this command to transfer the native coin DEV from MOONBEAN to ICON:
```
make transfer_dev
```
