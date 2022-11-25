FROM node:lts-alpine

RUN mkdir -p /app
WORKDIR /app

RUN npm install --save-dev hardhat
ADD hardhat.config.js /app
