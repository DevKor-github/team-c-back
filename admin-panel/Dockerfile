FROM node:20-alpine

# docker-cli와 함께 'docker-cli-compose' 그리고 'bash'를 설치합니다.
RUN apk add --no-cache docker-cli docker-cli-compose bash

WORKDIR /app
COPY package*.json ./
RUN npm install --omit=dev
COPY . .

# 권한 문제 방지
USER root

EXPOSE 3000
CMD ["node", "server.js"]
