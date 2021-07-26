FROM goloop/gochain-icon:0.9.7

ENV GOCHAIN_CONFIG=/gochain/config/gochain.server.json
ENV GOCHAIN_DATA=/gochain/data/gochain
ENV GOCHAIN_LOGFILE=/gochain/data/gochain.log
ENV GOCHAIN_CLEAN_DATA=false
ENV GOCHAIN_PROVISION_CONFIG=/gochain/provision/config
ENV JAVAEE_BIN=/goloop/execman/bin/execman
ENV PYEE_VERIFY_PACKAGE=true


ADD gochain.keysecret $GOCHAIN_PROVISION_CONFIG/
ADD gochain.keystore.json $GOCHAIN_PROVISION_CONFIG/
ADD gochain.server.json $GOCHAIN_PROVISION_CONFIG/
ADD entrypoint /entrypoint
RUN chmod u+x /entrypoint

RUN echo "0x3" > $GOCHAIN_PROVISION_CONFIG/nid.icon

ENTRYPOINT [ "/entrypoint" ]
CMD /goloop/run.sh
