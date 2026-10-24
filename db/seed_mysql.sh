#!/usr/bin/env bash
# Seed the MySQL "inventory" database.
# Usage:  MYSQL_PWD='yourRootPassword' ./db/seed_mysql.sh
#   (host/user default to localhost/root; override with MYSQL_HOST / MYSQL_USER)
set -euo pipefail

MYSQL_BIN="${MYSQL_BIN:-mysql}"
HOST="${MYSQL_HOST:-127.0.0.1}"
USER="${MYSQL_USER:-root}"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ -z "${MYSQL_PWD:-}" ]]; then
  echo "Set MYSQL_PWD to the MySQL root password first, e.g.:"
  echo "  MYSQL_PWD='...' $0"
  exit 1
fi

"$MYSQL_BIN" -h "$HOST" -u "$USER" < "$DIR/mysql_seed.sql"
echo "MySQL seed applied."
