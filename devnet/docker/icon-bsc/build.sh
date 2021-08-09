docker-compose rm
docker image rm icon-bsc_goloop
rm -rf work/*
docker-compose -f docker-compose.yml -f docker-compose.provision.yml up -d && docker-compose stop
#docker inspect btpsimple_src -f '{{ json .State.Health.Log }}' | jq .
