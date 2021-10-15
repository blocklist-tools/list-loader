#!/bin/sh
set -e

for file_with_path in $(ls -t ./history-files/*-results.json); do
  uuid=$(jq -r ".name" < "${file_with_path}")
  file_name=$(basename "${file_with_path}")
  echo "Running: docker-compose run --rm list-loader --blocklist ${uuid} --import \"/opt/history-files/${file_name}\" > \"./log/${file_name}.log\""
  docker-compose run --rm list-loader --blocklist ${uuid} --import "/opt/history-files/${file_name}" > "./log/${file_name}.log"
done
