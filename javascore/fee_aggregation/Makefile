# Configuration
BUILD_ROOT=$(abspath ./)
GOCHAIN_LOCAL_ROOT ?= /Users/aceraizel/lecle/gochain-local
URI ?= http://localhost:9082/api/v3
KEY_STORE ?= ./data/godWallet.json
KEY_SECRET ?= gochain
NID ?= 3
STEP_LIMIT ?= 10000000000
CPS_ADDRESS ?= cx3bb82e349dbf618dda7830f7378d8fd0d6956aff

build: $(BUILD_ROOT)
	cd $(BUILD_ROOT) ; \
	./gradlew build ; \
	./gradlew optimizedJar ; \
	cp ./build/libs/fee-aggregation-system-1.0-optimized.jar $(GOCHAIN_LOCAL_ROOT) ; \
	cd $(GOCHAIN_LOCAL_ROOT) ; \
	./goloop rpc sendtx deploy ./fee-aggregation-system-1.0-optimized.jar --uri $(URI) --key_store $(KEY_STORE) --key_password $(KEY_SECRET) --nid $(NID) --step_limit $(STEP_LIMIT) --content_type application/java --param _cps_address=$(CPS_ADDRESS)
result:
	cd $(GOCHAIN_LOCAL_ROOT) ; \
	./goloop rpc txresult $(HASH) --uri $(URI)

clean:
	rm -rf $(BUILD_ROOT)/build

