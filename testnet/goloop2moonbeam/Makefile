clean:
	docker-compose down -v --remove-orphans
	rm -rf data
	rm -rf config

build:
	docker-compose build

run:
	docker-compose up --remove-orphans

run_transfer_icx:
	docker-compose exec btp_icon sh /btpsimple/scripts/transfer_ICX.sh

run_transfer_dev:
	docker-compose exec btp_icon sh /btpsimple/scripts/transfer_DEV.sh