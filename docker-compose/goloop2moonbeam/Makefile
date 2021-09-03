clean:
	docker-compose down -v --remove-orphans
	rm -rf data config

build:
	docker-compose build

run:
	docker-compose up --remove-orphans

transfer_icx:
	docker-compose exec btp_icon sh /btpsimple/scripts/transfer_icx.sh

transfer_dev:
	docker-compose exec btp_icon sh /btpsimple/scripts/transfer_dev.sh