#!/bin/bash
export TERM=xterm
export PATH=/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin

DOMAIN="didim.fmapp.kr"
CERT_DIR="/etc/letsencrypt/live/$DOMAIN"
KEYSTORE_PATH="/Users/admin/InquiryList/SSL/keystore.p12"
KEYSTORE_PASS="didim7977"
KEY_ALIAS="tomcat"

# 만료일 문자열 가져오기
END_DATE_RAW=$(sudo openssl x509 -enddate -noout -in "$CERT_DIR/cert.pem" | cut -d= -f2)
END_DATE_NO_GMT=$(echo "$END_DATE_RAW" | sed 's/ GMT//')

# 날짜를 초단위로 변환 (macOS용)
DAYS_LEFT_UNIX=$(date -j -f "%b %d %T %Y" "$END_DATE_NO_GMT" "+%s" 2>/dev/null)

#날짜변환 실패 (터미널에서 직접 sh 실행 시 변환 안됨)
#crontab에서 자동화 sh 실행 시 날짜 변환 되어 스크립트가 실행됨
if [ -z "$DAYS_LEFT_UNIX" ]; then
    echo "날짜 변환 실패로 인해 갱신을 진행하지 않습니다."
    exit 1
else
    NOW_UNIX=$(date +%s)
    DAYS_LEFT=$(( (DAYS_LEFT_UNIX - NOW_UNIX) / 86400 ))
fi

echo "현재 인증서 남은 일 수: $DAYS_LEFT 일"

if [ "$DAYS_LEFT" -le 30 ]; then
    echo "=== 1. 인증서 갱신 시작 ==="
    sudo /usr/local/bin/certbot renew --non-interactive
    if [ $? -ne 0 ]; then
        echo "ERROR: 인증서 갱신 실패"
        exit 1
    fi

    echo "=== 2. PKCS#12 키스토어 재생성 ==="
    sudo openssl pkcs12 -export \
      -in "$CERT_DIR/cert.pem" \
      -inkey "$CERT_DIR/privkey.pem" \
      -certfile "$CERT_DIR/chain.pem" \
      -name "$KEY_ALIAS" \
      -out "$KEYSTORE_PATH" \
      -password pass:"$KEYSTORE_PASS"
    if [ $? -ne 0 ]; then
        echo "ERROR: keystore 생성 실패"
        exit 1
    fi

    echo "=== 3. Spring Boot 서비스 재시작 ==="
    sudo launchctl unload /Library/LaunchDaemons/com.inquiry.ssl.springBoot.plist
    sudo launchctl load /Library/LaunchDaemons/com.inquiry.ssl.springBoot.plist
    if [ $? -ne 0 ]; then
        echo "ERROR: Spring Boot 서비스 재시작 실패"
        exit 1
    fi

    echo "=== 완료 ==="
else
    echo "인증서가 아직 $DAYS_LEFT 일 남아 갱신하지 않습니다."
fi
