FROM canhlinh/truffle:5.3.0-alpine as truffle
FROM iconloop/goloop-icon:v0.9.7 as goloop
FROM btpsimple:latest

# setup truffle
COPY --from=truffle /usr/local/node /usr/local/node
RUN apk add --no-cache libstdc++ git bash jq curl
RUN apk add --no-cache --virtual .build-deps curl
ENV PATH $PATH:/usr/local/node/bin
RUN node -v
RUN yarn --version
RUN truffle version

# setup goloop tool
COPY --from=goloop /goloop/bin /goloop/bin
ENV PATH $PATH:/goloop/bin
RUN goloop version

ADD scripts /btpsimple/scripts
RUN chmod u+x /btpsimple/scripts/*.sh
ENV PATH $PATH:/btpsimple/scripts

ADD wait_for_it.sh /wait_for_it.sh
ADD entrypoint.sh /entrypoint.sh
RUN chmod u+x /entrypoint.sh && chmod u+x /wait_for_it.sh

ADD moonbeam /btpsimple/moonbeam
WORKDIR /btpsimple/config
ENV CONFIG_DIR /btpsimple/config

ENTRYPOINT [ "/entrypoint.sh" ]
