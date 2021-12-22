ARG GOCHAIN_IMAGE=goloop/gochain-icon:latest
FROM ${GOCHAIN_IMAGE} as gochain

RUN apk add --no-cache jq curl

ENV PROVISION_CONFIG=/gochain/provision/config
ENV GOCHAIN_CONFIG=/gochain/config/gochain.server.json
ENV GOCHAIN_DATA=/gochain/data/iconee
ENV GOCHAIN_LOGFILE=/gochain/data/gochain.log
ENV GOCHAIN_CLEAN_DATA=false
ENV JAVAEE_BIN=/goloop/execman/bin/execman
ENV PYEE_VERIFY_PACKAGE=true

ADD config $PROVISION_CONFIG
RUN cat $PROVISION_CONFIG/gochain.server.json | jq -r .genesis.nid  > $PROVISION_CONFIG/nid.icon

ADD entrypoint /entrypoint
RUN chmod u+x /entrypoint

EXPOSE 9080/tcp
EXPOSE 8080/tcp

ENTRYPOINT [ "/entrypoint" ]
CMD /goloop/run.sh

HEALTHCHECK --interval=3s --timeout=3s --start-period=5s --retries=10 CMD ["curl", "-f", "http://127.0.0.1:9080/metrics" ]
