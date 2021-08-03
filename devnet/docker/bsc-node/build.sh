docker build -t bsc-build -<<EOF
# Build BSC in an aphine builder container
FROM golang:1.16-alpine3.13 as builder

RUN apk add --no-cache gcc make git

WORKDIR /home
RUN git clone https://github.com/simsonraj/bsc
WORKDIR /home/bsc

# Update btcd version entry to fix checksum issue
RUN sed -i 's/btcd v0.20.0-beta/btcd v0.21.0-beta/' go.mod
RUN go mod download github.com/btcsuite/btcd
RUN go mod download golang.org/x/crypto

RUN make all
EOF

#docker cp <containerID>:/home/bsc/build/bin/ .