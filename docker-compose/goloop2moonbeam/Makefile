clean:
	docker-compose down -v --remove-orphans
	rm -rf data
	rm -rf config

build:
	docker-compose build

run:
	docker-compose up --remove-orphans

transfer_icx:
	docker-compose exec btp_icon sh /btpsimple/scripts/transfer_icx.sh

transfer_dev:
	docker-compose exec btp_icon sh /btpsimple/scripts/transfer_dev.sh

transfer_movr_from_icon:
	docker-compose exec btp_icon sh /btpsimple/scripts/transfer_movr_from_icon.sh

transfer_movr_from_moonbeam:
	docker-compose exec btp_icon sh /btpsimple/scripts/transfer_movr_from_moonbeam.sh

transfer_icx_from_icon:
	docker-compose exec btp_icon sh /btpsimple/scripts/transfer_icx_from_icon.sh

transfer_icx_from_moonbeam:
	docker-compose exec btp_icon sh /btpsimple/scripts/transfer_icx_from_moonbeam.sh

# To run with gochain, please build gochain image from goloop source. 
# Command line to build gochain: GL_TAG=0.9.9 make gochain-icon-image
build_with_gochain:
	docker-compose -f docker-compose-gochain.yml build

run_with_gochain:
	docker-compose -f docker-compose-gochain.yml up --remove-orphans 