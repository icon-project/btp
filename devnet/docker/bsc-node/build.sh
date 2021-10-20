docker build -t bsc-build -<<EOF
# Build BSC in an aphine builder container
FROM golang:1.16-alpine3.13

RUN apk add --no-cache make gcc musl-dev linux-headers git

# Checkout latest version on Feb 8th 2021
RUN cd / && git clone https://github.com/web3labs/bsc \
    && cd ./bsc && git checkout master && make geth

# Update btcd version entry to fix checksum issue
#RUN sed -i 's/btcd v0.20.0-beta/btcd v0.21.0-beta/' go.mod
#RUN go mod download github.com/btcsuite/btcd
#RUN go mod download golang.org/x/crypto
EOF

#docker cp <containerID>:/home/bsc/build/bin/ .