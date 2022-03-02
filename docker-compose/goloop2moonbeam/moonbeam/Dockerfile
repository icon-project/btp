ARG MOONBEAM_VERSION=latest

FROM debian:buster-slim as base
FROM purestake/moonbeam:${MOONBEAM_VERSION}

USER root
COPY --from=base /usr/bin /usr/bin
RUN apt-get update && apt-get install -y curl

HEALTHCHECK --interval=3s --timeout=3s --start-period=5s --retries=10 CMD ["curl", "-f", "http://127.0.0.1:9933/health" ]