#!/bin/bash

DELAY=10

docker-compose down

docker-compose up -d

echo "****** Waiting for ${DELAY} seconds for containers to go up ******"
sleep $DELAY

docker exec mongo1 /scripts/rs-init.sh