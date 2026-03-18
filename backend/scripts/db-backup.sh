#!/bin/bash
# 멍냥트립 로컬 DB 백업 스크립트
# 사용법: cd backend && bash scripts/db-backup.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKUP_DIR="$SCRIPT_DIR/../backup"
CONTAINER="meongnyang-db"
DB_USER="meongnyang"
DB_NAME="meongnyang"

mkdir -p "$BACKUP_DIR"
FILENAME="meongnyang_$(date +%Y%m%d_%H%M%S).sql.gz"
FILEPATH="$BACKUP_DIR/$FILENAME"

echo "==> DB 백업 시작: $FILENAME"
docker exec "$CONTAINER" pg_dump -U "$DB_USER" -d "$DB_NAME" | gzip > "$FILEPATH"

if [ $? -ne 0 ]; then
  echo "==> [오류] 백업 실패. 컨테이너가 실행 중인지 확인하세요."
  rm -f "$FILEPATH"
  exit 1
fi

echo "==> 완료: $FILEPATH"
echo "==> 파일 크기: $(du -sh "$FILEPATH" | cut -f1)"

# 최근 10개만 유지
EXCESS=$(ls -t "$BACKUP_DIR"/*.sql.gz 2>/dev/null | tail -n +11)
if [ -n "$EXCESS" ]; then
  echo "$EXCESS" | xargs rm -f
  echo "==> 오래된 백업 정리 완료"
fi

echo ""
echo "==> 보관 목록:"
ls -lh "$BACKUP_DIR"/*.sql.gz 2>/dev/null
