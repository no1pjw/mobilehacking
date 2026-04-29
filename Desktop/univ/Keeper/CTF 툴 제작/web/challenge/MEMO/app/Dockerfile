FROM node:22-slim

RUN apt-get update && apt-get install -y openssl && \
    rm -rf /var/lib/apt/lists/*

RUN npm install -g corepack --force && corepack enable

COPY . /app
WORKDIR /app

RUN yarn install

RUN if [ -f /app/src/images/FLAG.png ]; then \
    name=$(openssl rand -hex 8) && \
    mv /app/src/images/FLAG.png /app/src/images/flag_$name.png; \
    fi

CMD ["yarn", "start:dev"]
