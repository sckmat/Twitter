FROM node:20-alpine AS build
WORKDIR /app

COPY frontend/package*.json ./

RUN npm ci || npm install
COPY frontend/ .
RUN npm run build

FROM nginx:alpine

COPY --from=build /app/build /usr/share/nginx/html
COPY deploy/docker/nginx-spa.conf /etc/nginx/conf.d/default.conf