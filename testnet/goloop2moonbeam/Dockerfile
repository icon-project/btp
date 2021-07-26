FROM node:16-alpine3.12 as nodejs
FROM goloop-icon:0.9.7 as goloop
FROM btpsimple:latest

# setup truffle
COPY --from=nodejs /usr/local /usr/local
COPY --from=nodejs /usr/local/bin/yarn /usr/local/bin/yarn
COPY --from=nodejs /opt/yarn-v1.22.5 /opt/yarn-v1.22.5

RUN apk add --virtual .build-deps libstdc++ git bash jq curl
RUN yarn global add --quite truffle@5.3.0

# setup goloop tool
COPY --from=goloop /goloop/bin /goloop/bin
ENV PATH $PATH:/goloop/bin
RUN goloop version

ADD scripts /btpsimple/scripts
RUN chmod u+x /btpsimple/scripts/*.sh
ENV PATH $PATH:/btpsimple/scripts

ADD entrypoint.sh /entrypoint.sh
RUN chmod u+x /entrypoint.sh

WORKDIR /btpsimple/config
ENTRYPOINT [ "/entrypoint.sh" ]