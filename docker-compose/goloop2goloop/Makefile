build:
	docker-compose build
	
run:
	docker-compose up
	
clean:
	docker-compose down -v --remove-orphans
	rm -rf ./config ./data

run-test-scenario:
	docker-compose exec goloop sh /goloop/bin/scenario_test.sh