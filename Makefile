#-------------------------------------------------------------------------------
#
# 	Makefile for building target binaries.
#

# Configuration
BUILD_ROOT = $(abspath ./)
BIN_DIR = ./bin
LINUX_BIN_DIR = ./build/linux

GOBUILD = go build
GOBUILD_TAGS =
GOBUILD_ENVS = CGO_ENABLED=0
GOBUILD_LDFLAGS =
GOBUILD_FLAGS = -tags "$(GOBUILD_TAGS)" -ldflags "$(GOBUILD_LDFLAGS)"
GOBUILD_ENVS_LINUX = $(GOBUILD_ENVS) GOOS=linux GOARCH=amd64

GOTEST = go test
GOTEST_FLAGS = -test.short

# Build flags
GL_VERSION ?= $(shell git describe --always --tags --dirty)
GL_TAG ?= latest
BUILD_INFO = $(shell go env GOOS)/$(shell go env GOARCH) tags($(GOBUILD_TAGS))-$(shell date '+%Y-%m-%d-%H:%M:%S')

#
# Build scripts for command binaries.
#
CMDS = $(patsubst cmd/%,%,$(wildcard cmd/*))
.PHONY: $(CMDS) $(addsuffix -linux,$(CMDS))
define CMD_template
$(BIN_DIR)/$(1) $(1) : GOBUILD_LDFLAGS+=$$($(1)_LDFLAGS)
$(BIN_DIR)/$(1) $(1) :
	@ \
	rm -f $(BIN_DIR)/$(1) ; \
	echo "[#] go build ./cmd/$(1)"
	$$(GOBUILD_ENVS) \
	go build $$(GOBUILD_FLAGS) \
	    -o $(BIN_DIR)/$(1) ./cmd/$(1)

$(LINUX_BIN_DIR)/$(1) $(1)-linux : GOBUILD_LDFLAGS+=$$($(1)_LDFLAGS)
$(LINUX_BIN_DIR)/$(1) $(1)-linux :
	@ \
	rm -f $(LINUX_BIN_DIR)/$(1) ; \
	echo "[#] go build ./cmd/$(1)"
	$$(GOBUILD_ENVS_LINUX) \
	go build $$(GOBUILD_FLAGS) \
	    -o $(LINUX_BIN_DIR)/$(1) ./cmd/$(1)
endef
$(foreach M,$(CMDS),$(eval $(call CMD_template,$(M))))

# Build flags for each command
btpsimple_LDFLAGS = -X 'main.version=$(GL_VERSION)' -X 'main.build=$(BUILD_INFO)'
BUILD_TARGETS += btpsimple

linux : $(addsuffix -linux,$(BUILD_TARGETS))

CONTRACTS_DIST_DIR = $(BUILD_ROOT)/build/contracts
PYSCORE_DIST_DIR = $(CONTRACTS_DIST_DIR)/pyscore

$(PYSCORE_DIST_DIR)/%:
	$(eval MODULE := $(patsubst $(PYSCORE_DIST_DIR)/%.zip,%,$@))
	mkdir -p $(PYSCORE_DIST_DIR)/$(MODULE) ; \
	cp -r pyscore/lib pyscore/$(MODULE) $(PYSCORE_DIST_DIR)/$(MODULE)/

dist-py-bmc: $(PYSCORE_DIST_DIR)/bmc.zip
ifeq (,$(wildcard $(PYSCORE_DIST_DIR)/bmc.zip))
	cd $(PYSCORE_DIST_DIR)/bmc ; \
	echo '{"version": "0.0.1","main_module": "bmc.bmc","main_score": "BTPMessageCenter"}' > package.json ; \
	zip -r -v $(PYSCORE_DIST_DIR)/bmc.zip bmc lib package.json -x *__pycache__* -x *tests* ; \
	rm -rf $(PYSCORE_DIST_DIR)/bmc ;
endif

dist-py-bmv: $(PYSCORE_DIST_DIR)/bmv.zip
ifeq (,$(wildcard $(PYSCORE_DIST_DIR)/bmv.zip))
	cd $(PYSCORE_DIST_DIR)/bmv ; \
	echo '{"version": "0.0.1","main_module": "bmv.icon.icon","main_score": "BTPMessageVerifier"}' > package.json ; \
	zip -r -v $(PYSCORE_DIST_DIR)/bmv.zip bmv lib package.json -x *__pycache__* -x *tests* ; \
	rm -rf $(PYSCORE_DIST_DIR)/bmv ;
endif

dist-py-token: $(PYSCORE_DIST_DIR)/token.zip
ifeq (,$(wildcard $(PYSCORE_DIST_DIR)/token.zip))
	cd $(PYSCORE_DIST_DIR)/token ; \
	echo '{"version": "0.0.1","main_module": "token.token_bsh","main_score": "TokenBSH"}' > package.json ; \
	zip -r -v $(PYSCORE_DIST_DIR)/token.zip token lib package.json -x *__pycache__* -x *tests* -x *sample* ; \
	cd token/sample/irc2 ; \
    zip -r -v $(PYSCORE_DIST_DIR)/irc2.zip * -x *__pycache__* -x *tests* ; \
    rm -rf $(PYSCORE_DIST_DIR)/token ;
endif

dist-py: dist-py-bmc dist-py-bmv dist-py-token

clean-dist-py:
	rm -rf $(PYSCORE_DIST_DIR)/*

JAVASCORE_DIST_DIR = $(CONTRACTS_DIST_DIR)/javascore

$(JAVASCORE_DIST_DIR)/%:
	$(eval MODULE := $(patsubst $(JAVASCORE_DIST_DIR)/%,%,$@))
	mkdir -p $(JAVASCORE_DIST_DIR)

dist-java-bmc: $(JAVASCORE_DIST_DIR)/bmc.jar
	cd javascore ; \
    ./gradlew :bmc:optimizedJar ; \
    cp ./bmc/build/libs/bmc-?.?.?-*.jar $(JAVASCORE_DIST_DIR)/bmc.jar

dist-java-bmv-icon: $(JAVASCORE_DIST_DIR)/bmv-icon.jar
	cd javascore ; \
    ./gradlew :bmv:icon:optimizedJar ; \
    cp ./bmv/icon/build/libs/bmv-icon-?.?.?-*.jar $(JAVASCORE_DIST_DIR)/bmv-icon.jar

dist-java-bsh: $(JAVASCORE_DIST_DIR)/bsh.jar
	cd javascore ; \
    ./gradlew :bsh:optimizedJar ; \
    cp ./bsh/build/libs/bsh-?.?.?-*.jar $(JAVASCORE_DIST_DIR)/bsh.jar ; \
    ./gradlew :bsh:optimizedJarIRC2 ; \
    cp ./bsh/build/libs/irc2-?.?.?-*.jar $(JAVASCORE_DIST_DIR)/irc2.jar

dist-java-nativecoin: $(JAVASCORE_DIST_DIR)/nativecoin.jar
	cd javascore ; \
    ./gradlew :nativecoin:optimizedJar ; \
    cp ./nativecoin/build/libs/nativecoin-?.?.?-*.jar $(JAVASCORE_DIST_DIR)/nativecoin.jar; \
    ./gradlew :nativecoin:optimizedJarIRC31 ; \
    cp ./nativecoin/build/libs/irc31-?.?.?-*.jar $(JAVASCORE_DIST_DIR)/irc31.jar

dist-java-feeaggregation: $(JAVASCORE_DIST_DIR)/fee-aggregation.jar
	cd javascore ; \
    ./gradlew :fee-aggregation:optimizedJar ; \
    cp ./fee-aggregation/build/libs/fee-aggregation-?.?-*.jar $(JAVASCORE_DIST_DIR)/fee-aggregation.jar ; \

dist-java: dist-java-bmc dist-java-bmv-icon dist-java-bsh dist-java-nativecoin dist-java-feeaggregation

clean-java-build:
	cd javascore ; \
	rm -rf lib/build bmc/build bmv/icon/build bsh/build nativecoin/build score-util/build fee_aggregation/build

clean-dist-java:
	rm -rf $(JAVASCORE_DIST_DIR)/*

clean-dist:
	rm -rf $(CONTRACTS_DIST_DIR)/*

BTPSIMPLE_IMAGE = btpsimple:$(GL_TAG)
BTPSIMPLE_DOCKER_DIR = $(BUILD_ROOT)/build/btpsimple

btpsimple-image: btpsimple-linux dist-py dist-java
	@ echo "[#] Building image $(BTPSIMPLE_IMAGE) for $(GL_VERSION)"
	@ rm -rf $(BTPSIMPLE_DOCKER_DIR)
	@ \
	BIN_DIR=$(abspath $(LINUX_BIN_DIR)) \
	BIN_VERSION=$(GL_VERSION) \
	BUILD_TAGS="$(GOBUILD_TAGS)" \
	CONTRACTS_DIST_DIR="$(CONTRACTS_DIST_DIR)" \
	$(BUILD_ROOT)/docker/btpsimple/build.sh $(BTPSIMPLE_IMAGE) $(BUILD_ROOT) $(BTPSIMPLE_DOCKER_DIR)

.PHONY: test

test :
	$(GOBUILD_ENVS) $(GOTEST) $(GOBUILD_FLAGS) ./... $(GOTEST_FLAGS)

.DEFAULT_GOAL := all
all : $(BUILD_TARGETS)
