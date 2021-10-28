UID=$(shell id -u)
GID=$(shell id -g)

build-image:
	docker build --tag bsc-node . --build-arg KEYSTORE_PASS="Perlia0" --build-arg USER_ID=$(UID) --build-arg GROUP_ID=$(GID)