UID=$(shell id -u)
GID=$(shell id -g)

remove:
	USER_ID=$(UID) GROUP_ID=$(GID) docker-compose down -v --remove-orphans
	docker rmi -f icon-bsc_btp-icon
	docker rm -f javascore-dist
	docker rmi -f btpsimple
	docker rmi -f btp/javascore
	rm -rf ./data
	rm -rf ./work
	rm -rf ../../../build/*	
	
clean:
	docker-compose down -v --remove-orphans
	rm -rf work/*

build:
	mkdir -p ./data/bsc/node1
	mkdir -p ./work
	make -C ../../../ dist-javascore dist-sol btpsimple-image	
	docker-compose build  --build-arg USER_ID=$(UID) --build-arg GROUP_ID=$(GID)

run:
	docker-compose up --remove-orphans -d

run-bsc:
	docker-compose up --remove-orphans -d binancesmartchain

solidity:
	make -C ../../../ dist-sol btpsimple-image
	docker-compose up --remove-orphans -d binancesmartchain

alice_transfer_eth:
	docker-compose exec btp-icon sh /btpsimple/bin/transfer_eth.sh

alice_transfer_icx:
	docker-compose exec btp-icon sh /btpsimple/bin/transfer_icx.sh

bob_transfer_eth:
	docker-compose exec btp-icon sh /btpsimple/bin/transfer_eth_bsc.sh

bob_transfer_bnb:
	docker-compose exec btp-icon sh /btpsimple/bin/transfer_bnb_bsc.sh