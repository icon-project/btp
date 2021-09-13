FROM node:lts-buster-slim

USER root
WORKDIR /root/moonbeam-launch
COPY . /root/moonbeam-launch
RUN apt-get update && apt-get install -y docker.io curl \
    && yarn

CMD ["yarn", "launch", "--parachain", "moonbase-0.12.1", "--relay", "westend-9040"]

HEALTHCHECK --interval=3s --timeout=3s --start-period=5s --retries=10 CMD ["curl", "-f", "http://127.0.0.1:34101/health" ]