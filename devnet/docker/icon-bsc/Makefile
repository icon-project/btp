remove:
	docker-compose down -v --remove-orphans	
	docker rmi -f icon-bsc_btp-icon
	docker rm -f javascore-dist
	docker rmi -f btpsimple
	docker rmi -f btp/javascore
	rm -rf work/*
	rm -rf ../../../build/*
	rm -rf ./data/bsc/node1
	
clean:
	docker-compose down -v --remove-orphans
	rm -rf work/*

build:
	make -C ../../../ dist-javascore dist-sol btpsimple-image
	mkdir -p ./data/bsc/node1
	docker-compose build

run:
	docker-compose up --remove-orphans -d

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