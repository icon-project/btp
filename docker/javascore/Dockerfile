ARG GOLOOP_IMAGE=goloop:latest
FROM ${GOLOOP_IMAGE} as build-stage

RUN apk add git
RUN apk add gradle

ENV ROOT=/goloop
ENV JAVASCORE_SRC=/goloop/btp/javascore
ENV DIST_DIR=/dist
WORKDIR ${ROOT}

#copy javascore code & solidity code base into the container for future build & provision
ADD javascore ${JAVASCORE_SRC}

#build javascore jars
WORKDIR ${JAVASCORE_SRC}
RUN gradle bsh:clean
RUN gradle bmv:clean
RUN gradle bsh:optimizedJar
RUN gradle bmv:optimizedJar

#copy jars to dist
WORKDIR ${ROOT}
RUN mkdir /dist

RUN cp ${JAVASCORE_SRC}/bsh/build/libs/bsh-optimized.jar ${DIST_DIR}
RUN cp ${JAVASCORE_SRC}/bmv/build/libs/bmv-optimized.jar ${DIST_DIR}
RUN cp ${JAVASCORE_SRC}/bmc/bmc-0.1.0-optimized.jar ${DIST_DIR}/bmc-optimized.jar

RUN cp ${JAVASCORE_SRC}/irc31-0.1.0-optimized.jar ${DIST_DIR}
RUN cp ${JAVASCORE_SRC}/nativecoin-0.1.0-optimized.jar ${DIST_DIR}

RUN git clone https://github.com/icon-project/java-score-examples.git
WORKDIR /goloop/java-score-examples
RUN gradle irc2-token:clean
RUN gradle irc2-token:optimizedJar
WORKDIR ${ROOT}
RUN cp java-score-examples/irc2-token/build/libs/irc2-token-0.9.1-optimized.jar ${DIST_DIR}/irc2-token-optimized.jar

