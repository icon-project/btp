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

PYSCORE_DIST_DIR = $(BUILD_ROOT)/build/pyscore
PYSCORE_TESTNET_DIR=${BUILD_ROOT}/testnet/goloop/pyscore

$(PYSCORE_DIST_DIR)/%:
	$(eval MODULE := $(patsubst $(PYSCORE_DIST_DIR)/%,%,$@))
	mkdir -p $@ ; \
	cp -r pyscore/lib pyscore/$(MODULE) $@/

dist-py-bmc: $(PYSCORE_DIST_DIR)/bmc
	cd $(PYSCORE_DIST_DIR)/bmc ; \
	echo '{"version": "0.0.1","main_module": "bmc.bmc","main_score": "BTPMessageCenter"}' > package.json ; \
	zip -r -v $(PYSCORE_DIST_DIR)/bmc.zip bmc lib package.json -x *__pycache__* -x *tests*

dist-py-bmv: $(PYSCORE_DIST_DIR)/bmv
	cd $(PYSCORE_DIST_DIR)/bmv ; \
	echo '{"version": "0.0.1","main_module": "bmv.icon.icon","main_score": "BTPMessageVerifier"}' > package.json ; \
	zip -r -v $(PYSCORE_DIST_DIR)/bmv.zip bmv lib package.json -x *__pycache__* -x *tests*

dist-py-irc2: $(PYSCORE_DIST_DIR)/token_bsh
	cd $(PYSCORE_DIST_DIR)/token_bsh ; \
	echo '{"version": "0.0.1","main_module": "token_bsh.token_bsh","main_score": "TokenBSH"}' > package.json ; \
	zip -r -v $(PYSCORE_DIST_DIR)/token_bsh.zip token_bsh lib package.json -x *__pycache__* -x *tests* -x *sample* ; \
	cd token_bsh/sample/irc2_token ; \
    zip -r -v $(PYSCORE_DIST_DIR)/irc2_token.zip * -x *__pycache__* -x *tests*

dist-py: dist-py-bmc dist-py-bmv dist-py-irc2

clean-dist-py:
	rm -rf $(PYSCORE_DIST_DIR)/*

BTPSIMPLE_IMAGE = btpsimple:$(GL_TAG)
BTPSIMPLE_DOCKER_DIR = $(BUILD_ROOT)/build/btpsimple

btpsimple-image: btpsimple-linux dist-py
	@ echo "[#] Building image $(BTPSIMPLE_IMAGE) for $(GL_VERSION)"
	@ rm -rf $(BTPSIMPLE_DOCKER_DIR)
	@ \
	BIN_DIR=$(abspath $(LINUX_BIN_DIR)) \
	BIN_VERSION=$(GL_VERSION) \
	BUILD_TAGS="$(GOBUILD_TAGS)" \
	DIST_DIR="$(PYSCORE_DIST_DIR)" \
	$(BUILD_ROOT)/docker/btpsimple/build.sh $(BTPSIMPLE_IMAGE) $(BUILD_ROOT) $(BTPSIMPLE_DOCKER_DIR)

.PHONY: test

test :
	$(GOBUILD_ENVS) $(GOTEST) $(GOBUILD_FLAGS) ./... $(GOTEST_FLAGS)

.DEFAULT_GOAL := all
all : $(BUILD_TARGETS)

cp_pyscore_testnet: dist-py
	rsync -a ${PYSCORE_DIST_DIR}/*.zip ${PYSCORE_TESTNET_DIR}

build-docker: cp_pyscore_testnet
	docker-compose build
	
run-docker: cp_pyscore_testnet
	docker-compose up
	
clean-docker:
	docker-compose down -v --remove-orphans
	sudo rm -rf build/*

run-test-scenario:
	docker-compose exec goloop sh /goloop/bin/scenario_test.sh