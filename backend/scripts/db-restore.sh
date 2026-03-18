#!/bin/bash
# 멍냥트립 로컬 DB 복원 스크립트
# 사용법:
#   cd backend && bash scripts/db-restore.sh              # 최신 백업으로 복원
#   cd backend && bash scripts/db-restore.sh backup/파일명.sql.gz  # 특정 파일 지정

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKUP_DIR="$SCRIPT_DIR/../backup"
CONTAINER="meongnyang-db"
DB_USER="meongnyang"
DB_NAME="meongnyang"

# 복원 파일 결정
if [ -z "$1" ]; then
  FILE=$(ls -t "$BACKUP_DIR"/*.sql.gz 2>/dev/null | head -1)
  if [ -z "$FILE" ]; then
    echo "[오류] 백업 파일이 없습니다. 먼저 db-backup.sh를 실행하세요."
    exit 1
  fi
  echo "==> 최신 백업 파일 사용: $FILE"
else
  FILE="$1"
  if [ ! -f "$FILE" ]; then
    echo "[오류] 파일을 찾을 수 없습니다: $FILE"
    exit 1
  fi
fi

echo ""
echo "==> [주의] 기존 DB 데이터가 덮어씁니다."
echo "==> 복원 파일: $FILE"
read -p "==> 계속하시겠습니까? (y/N): " CONFIRM
if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
  echo "==> 복원 취소"
  exit 0
fi

echo "==> 복원 시작..."
gunzip -c "$FILE" | docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME"

if [ $? -eq 0 ]; then
  echo "==> 복원 완료"
else
  echo "==> [오류] 복원 실패"
  exit 1
fi
