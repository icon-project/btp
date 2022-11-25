PWD = $(abspath ./)

JAVASCORE_PATH = $(PWD)/../javascore
GRADLE = ./gradlew

HARDHAT = npx hardhat
TS_NODE = npx ts-node
NETWORK = --network localhost
SOLIDITY_CONTRACTS = ./solidity/contracts

.DEFAULT_GOAL := all
all:
	@echo $(PWD)

.PHONY: build clean deploy

clean:
	$(HARDHAT) clean
	$(RM) -rf \
	    $(SOLIDITY_CONTRACTS)/bmc \
	    $(SOLIDITY_CONTRACTS)/bmv* \
	    $(SOLIDITY_CONTRACTS)/xcall

copy-src:
	@ echo ">>> Copy BMC/BMV/xCall solidity codes" ; \
	$(TS_NODE) scripts/copy_src.ts

compile: copy-src
	@ echo ">>> Compile solidity codes" ; \
	$(HARDHAT) compile

build-bmc-java:
	@ echo ">>> Build BMC javascore" ; \
	cd $(JAVASCORE_PATH) ; \
	$(GRADLE) bmc:optimizedJar

build-bmv-java:
	@ echo ">>> Build BMV javascore" ; \
	cd $(JAVASCORE_PATH) ; \
	$(GRADLE) bmv:bridge:optimizedJar

build-xcall-java:
	@ echo ">>> Build xCall javascore" ; \
	cd $(JAVASCORE_PATH) ; \
	$(GRADLE) xcall:optimizedJar

build-dapp-java:
	@ echo ">>> Build DApp sample javascore" ; \
	$(GRADLE) dapp-sample:optimizedJar

build-all: compile build-bmc-java build-bmv-java build-xcall-java build-dapp-java

deploy-bmc:
	@ echo ">>> $(HARDHAT) $(NETWORK) run scripts/deploy_bmc.ts" ; \
	E2E_DEMO_PATH=$(PWD) \
	JAVASCORE_PATH=$(JAVASCORE_PATH) \
	$(HARDHAT) $(NETWORK) run scripts/deploy_bmc.ts

deploy-bmv:
	@ echo ">>> $(HARDHAT) $(NETWORK) run scripts/deploy_bmv.ts" ; \
	E2E_DEMO_PATH=$(PWD) \
	JAVASCORE_PATH=$(JAVASCORE_PATH) \
	$(HARDHAT) $(NETWORK) run scripts/deploy_bmv.ts

deploy-xcall:
	@ echo ">>> $(HARDHAT) $(NETWORK) run scripts/deploy_xcall.ts" ; \
	E2E_DEMO_PATH=$(PWD) \
	JAVASCORE_PATH=$(JAVASCORE_PATH) \
	$(HARDHAT) $(NETWORK) run scripts/deploy_xcall.ts

deploy-dapp:
	@ echo ">>> $(HARDHAT) $(NETWORK) run scripts/deploy_dapp.ts" ; \
	E2E_DEMO_PATH=$(PWD) \
	$(HARDHAT) $(NETWORK) run scripts/deploy_dapp.ts

deploy-all: deploy-bmc deploy-bmv deploy-xcall deploy-dapp

start-nodes:
	@ echo ">>> Start nodes" ; \
	cd docker; \
	docker-compose up -d

stop-nodes:
	@ echo ">>> Stop nodes" ; \
	cd docker; \
	docker-compose down
