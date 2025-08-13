#!/bin/bash

# 삭제할 디렉토리 경로
TARGET_DIR="/Users/admin/InquiryList/upload/temp"
# 로그 파일 경로
LOG_FILE="/Users/admin/InquiryList/upload/delete_temp_file.log"

# 현재 날짜와 시간
NOW=$(date '+%Y-%m-%d %H:%M:%S')

# 파일 삭제 시도
if find "$TARGET_DIR" -type f -delete 2>/dev/null; then
    echo "[$NOW] 파일 삭제 성공 - 경로: $TARGET_DIR" >> "$LOG_FILE"
else
    echo "[$NOW] 파일 삭제 실패 - 경로: $TARGET_DIR" >> "$LOG_FILE"
fi
