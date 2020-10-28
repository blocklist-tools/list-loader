# List Loader

Downloads, parses, and uploads all lists to the Blocklist API. It also is able to load historical lists exported from the github-history-generator project.

```
# Run full lists update
docker-compose up -d

# Import history from github-history-generator project
docker-compose run --rm list-loader --blocklist [UUID] --import /opt/history-files/[JSON-FILE] > [JSON-FILE].log

# Example history import
docker-compose run --rm list-loader --blocklist af813f51-0846-47d0-98a4-744f4652fa48 --import /opt/history-files/StevenBlack-hosts-mvps.org-results.json > StevenBlack-hosts-mvps.org-results.log
```