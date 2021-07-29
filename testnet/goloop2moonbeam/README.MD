# Docker integration of BTP betwwen ICON and MOONBEAM

This document will walk you through setting up a Docker environment to demonstrate how to run BTP between ICON and MOONBEAM.  
Note: tested on macos.

## Prerequisites:  
```
- docker version 20.10.5+
- docker-compose version 1.29.0+
- make
- zip
- go version 1.16.3
```

## Setup steps:  


### Build image goloop/gochain-icon:0.9.7

```
cd ~/work
git clone https://github.com/icon-project/goloop.git
cd ~/work/goloop
git fetch --tags
git checkout v0.9.7 -b v0.9.7
GL_TAG=0.9.7 make gochain-icon-image
```

### Build image btpsimple:latest

```
cd ~/work
git clone git@github.com:icon-project/btp.git
cd ~/work/btp
git fetch
git checkout goloop2moonbeam
make btpsimple-image
```

### Run docker-compose

```
cd ~/work/btp/testnet/goloop2moonbeam
make build
make run
```

Now all that remains is to wait for the provisioning process to be completed. The folder `~/work/btp/goloop2moonbeam/config` will contain all of the configuration files.
Note: Run `make clean` to remove the old test data.
