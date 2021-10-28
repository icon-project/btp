FROM alpine:3.14.0

ENV BSC_HOME=/bsc
ENV PATH $PATH:/${BSC_HOME}/bin
ENV BSC_DATADIR=${BSC_HOME}/node1

ARG USER_ID
ARG GROUP_ID

#Add User id & group from the host machine to the container
RUN addgroup -g $GROUP_ID user
RUN adduser --disabled-password --gecos '' --uid $USER_ID -G user user
RUN echo "$USER_ID ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/$USER_ID \
        && chmod 0440 /etc/sudoers.d/$USER_ID
RUN mkdir -p ${BSC_HOME}
RUN chown -R $USER_ID ${BSC_HOME}
USER user

# copy files for provisioning
COPY build-apline-3.13/bin ${BSC_HOME}/bin/
COPY keystore ${BSC_HOME}/keystore
COPY genesis.json ${BSC_HOME}/

WORKDIR ${BSC_HOME}

# TODO: automate keystore generation during provisioning
ARG KEYSTORE_PASS
RUN echo ${KEYSTORE_PASS} > .secret

COPY ./entrypoint ${BSC_HOME}
COPY ./config.toml ${BSC_HOME}
#COPY ./start.sh ${BSC_HOME}

EXPOSE 8545
EXPOSE 8546

ENTRYPOINT ${BSC_HOME}/entrypoint