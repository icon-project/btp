# Docker integration of BTP betwwen ICON and MOONBEAM

This document guide you how to run an docker environemnt to illustrate of running BTP between ICON and MOONBEAM.   
P/S: I just only tested on macos.

## Prerequisites:  
```
- docker version 20.10.5+
- docker-compose version 1.29.0+
- make
- go version 1.16.3
```

## Setup steps:  


### Build image goloop/gochain-icon:0.9.7

Clone goloop source code:
```
cd ~/work
git clone https://github.com/icon-project/goloop.git
```
Check out version 0.9.7
```
cd ~/work/goloop
git fetch --tags
git checkout v0.9.7 -b v0.9.7
```
Build image:
```
GL_TAG=0.9.7 make gochain-icon-image
```

### Build image btpsimple:latest

Clone btp source code:
```
cd ~/work
git clone git@github.com:icon-project/btp.git
cd ~/work/btp
git fetch
git checkout goloop2moonbeam
```

Build image:
```
make btpsimple-image
```

### Running docker-compose

```
cd ~/work/btp/goloop2moonbeam
make build
make run
```

Now just wait for the provisioning progress is done. The whole cofiguration files will appeared in the folder `~/work/btp/goloop2moonbeam/config`)

To clean the old test data run this command `make clean`.