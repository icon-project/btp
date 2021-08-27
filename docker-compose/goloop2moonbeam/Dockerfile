ARG GOLOOP_IMAGE=iconloop/goloop-icon:latest
FROM ${GOLOOP_IMAGE} as goloop
FROM canhlinh/truffle:5.3.0-alpine as truffle
FROM btpsimple:latest

# install truffle cli
COPY --from=truffle /usr/local/node /usr/local/node
RUN apk add --no-cache libstdc++ git bash jq curl
RUN apk add --no-cache --virtual .build-deps curl
ENV PATH $PATH:/usr/local/node/bin
RUN node -v
RUN yarn --version
RUN truffle version

# install goloop cli
COPY --from=goloop /goloop/bin /goloop/bin
ENV PATH $PATH:/goloop/bin
RUN goloop version

# install eth cli
RUN npm install -g eth-cli
RUN eth -v

ADD scripts /btpsimple/scripts
RUN chmod u+x /btpsimple/scripts/*.sh
ENV PATH $PATH:/btpsimple/scripts

ADD entrypoint.sh /entrypoint.sh
RUN chmod u+x /entrypoint.sh

ADD moonbeam/keystore /moonbeam/keystore
WORKDIR /btpsimple/config
ENV CONFIG_DIR /btpsimple/config

ENTRYPOINT [ "/entrypoint.sh" ]
