remove:
	docker-compose down -v --remove-orphans
	rm -rf work/*
	docker rmi -f icon-bsc_btp-icon
	docker rm -f javascore-dist
	docker rmi -f btpsimple
	docker rmi -f btp/javascore
	
clean:
	docker-compose down -v --remove-orphans
	rm -rf work/*

build:
	make -C ../../../ dist-javascore dist-sol btpsimple-image
	docker-compose build

run:
	docker-compose up --remove-orphans -d

solidity:
	make -C ../../../ dist-sol btpsimple-image
	docker-compose up --remove-orphans -d binancesmartchain btp-icon

transfer_eth:
	docker-compose exec btp-icon sh /btpsimple/bin/transfer_eth.sh

transfer_icx:
	docker-compose exec btp-icon sh /btpsimple/bin/transfer_icx.sh