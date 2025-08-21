#!/bin/bash
set -e

if [[ "$EUID" -eq 0 ]]
then echo "No run as root stupid."
	exit
fi
./mvnw clean package
sudo docker build -t eventscraper:latest .
sudo docker save eventscraper:latest > eventscraper.tar
scp eventscraper.tar root@debian:/opt/
ssh root@debian << EOF
cd /opt
docker load < eventscraper.tar
cd eventscraper/
docker compose up -d
EOF
	
