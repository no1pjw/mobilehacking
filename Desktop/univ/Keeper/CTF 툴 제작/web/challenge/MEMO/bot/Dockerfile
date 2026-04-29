FROM node:current-slim

RUN apt update && apt install -y wget && \
    wget -O ./google-chrome-stable.deb https://mirror.cs.uchicago.edu/google-chrome/pool/main/g/google-chrome-stable/google-chrome-stable_140.0.7339.127-1_amd64.deb && \
    apt-get install -y ./google-chrome-stable.deb && \
    rm ./google-chrome-stable.deb && \
    rm -rf /var/lib/apt/lists/*

COPY . /app
WORKDIR /app

RUN npm install --omit=dev

EXPOSE 5000

CMD ["npm", "start"]
