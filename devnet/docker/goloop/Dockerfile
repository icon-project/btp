FROM iconloop/goloop-icon:v0.9.7

RUN apk add --no-cache jq zip
RUN apk add --update sudo

ENV GOLOOP_PROVISION=/goloop/provisioning
ENV GOLOOP_PROVISION_CONFIG=${GOLOOP_PROVISION}/config
ENV GOLOOP_PROVISION_DATA=${GOLOOP_PROVISION}/data
RUN mkdir -p ${GOLOOP_PROVISION_CONFIG}
RUN mkdir -p ${GOLOOP_PROVISION_DATA}

ARG USER_ID
ARG GROUP_ID

#Add User id & group from the host machine to the container
RUN addgroup -g $GROUP_ID user
RUN adduser --disabled-password --gecos '' --uid $USER_ID -G user user
RUN sudo echo "$USER_ID ALL=(ALL) NOPASSWD: ALL" > /etc/sudoers.d/$USER_ID \
        && sudo chmod 0440 /etc/sudoers.d/$USER_ID

RUN chown -R $USER_ID ${GOLOOP_PROVISION} ${GOLOOP_PROVISION_CONFIG} ${GOLOOP_PROVISION_DATA}
RUN chmod -R 777 /var/run
USER user 

ENV GOLOOP_CONFIG=/goloop/config/goloop.server.json
ENV GOLOOP_KEY_SECRET=/goloop/config/goloop.keysecret
ENV GOLOOP_KEY_STORE=/goloop/config/goloop.keystore.json

COPY ./*.sh /goloop/bin/
COPY ./entrypoint /

RUN provision.sh
WORKDIR /goloop/config