FROM golang:1.13-alpine as builder
RUN apk --no-cache --update add git gcc musl-dev linux-headers ca-certificates tzdata bash

RUN wget -q https://github.com/markbates/refresh/releases/download/v1.4.11/refresh_1.4.11_linux_amd64.tar.gz \
    && tar -xzf refresh_1.4.11_linux_amd64.tar.gz && mv refresh /usr/local/bin/refresh && chmod u+x /usr/local/bin/refresh

WORKDIR /btpsimple
COPY . .

ENV PATH $PATH:/btpsimple/build/bin
# ENV BTPSIMPLE_BASE_DIR=/btpsimple/data
# ENV BTPSIMPLE_CONFIG=/btpsimple/config/config.json
# ENV BTPSIMPLE_KEY_STORE=/btpsimple/config/keystore.json
# ENV BTPSIMPLE_KEY_SECRET=/btpsimple/config/keysecret
# ENV BTPSIMPLE_LOG_WRITER_FILENAME=/btpsimple/data/btpsimple.log

RUN chmod +x entrypoint.sh
ENTRYPOINT ["/btpsimple/entrypoint.sh"]

RUN go build -v -o ./build/bin/btpsimple ./cmd/btpsimple
CMD btpsimple start --config config.json 

FROM alpine:3.12 as dist
RUN apk --no-cache --update add ca-certificates tzdata bash
WORKDIR /btpsimple
ENV PATH $PATH:/btpsimple/bin

COPY --from=builder /btpsimple/build/bin/btpsimple /btpsimple/bin/btpsimple
COPY --from=builder /btpsimple/entrypoint.sh /btpsimple/entrypoint.sh

RUN chmod +x entrypoint.sh
ENTRYPOINT ["/btpsimple/entrypoint.sh"]

CMD btpsimple start --config config.json 