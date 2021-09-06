ARG GOCHAIN_IMAGE=goloop/gochain-icon:latest
FROM ${GOCHAIN_IMAGE} as gochain

RUN apk add --no-cache jq curl

ENV PROVISION_CONFIG=/gochain/provision/config
ADD config $PROVISION_CONFIG
RUN echo "0x3" > $PROVISION_CONFIG/nid.icon

ADD entrypoint /entrypoint
RUN chmod u+x /entrypoint

EXPOSE 9080/tcp
EXPOSE 8080/tcp

ENTRYPOINT [ "/entrypoint" ]
CMD /goloop/run.sh

HEALTHCHECK --interval=3s --timeout=3s --start-period=5s --retries=10 CMD ["curl", "-f", "http://127.0.0.1:9080/metrics" ]
