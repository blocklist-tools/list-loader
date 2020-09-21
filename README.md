```
docker-compose up -d

docker-compose run --rm list-loader --blocklist [UUID] --import /opt/history-files/[JSON-FILE] > [JSON-FILE].log

docker-compose run --rm list-loader --blocklist af813f51-0846-47d0-98a4-744f4652fa48 --import /opt/history-files/StevenBlack-hosts-mvps.org-results.json > StevenBlack-hosts-mvps.org-results.log
```