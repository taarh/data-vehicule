#!/bin/bash

# Check if environment is provided
ENV=${1:-dev}

if [ "$ENV" = "dev" ]; then
    echo "Starting development environment..."
    docker-compose -f docker-compose.dev.yml --env-file .env.dev up --build
elif [ "$ENV" = "prod" ]; then
    echo "Starting production environment..."
    docker-compose -f docker-compose.prod.yml --env-file .env.prod up --build
else
    echo "Invalid environment. Use 'dev' or 'prod'"
    exit 1
fi 