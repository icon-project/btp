FROM goloop/gochain-icon:0.9.7


ENV PROVISION_CONFIG=/gochain/provision/config
ADD config $PROVISION_CONFIG
RUN echo "0x3" > $PROVISION_CONFIG/nid.icon

ADD entrypoint /entrypoint
RUN chmod u+x /entrypoint

EXPOSE 9080/tcp
EXPOSE 8080/tcp

ENTRYPOINT [ "/entrypoint" ]
CMD /goloop/run.sh
