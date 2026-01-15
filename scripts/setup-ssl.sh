#!/bin/bash
# SSL Certificate Setup Script for chatmoderatorbot.ru
# This script should be run on the production server (89.125.243.104)
#
# Usage: sudo ./scripts/setup-ssl.sh

set -e

# Configuration
DOMAIN="chatmoderatorbot.ru"
EMAIL="admin@chatmoderatorbot.ru"
WEBROOT="/root/chatkeep/certbot/webroot"
CERT_PATH="/etc/letsencrypt/live/${DOMAIN}"

DOMAINS=(
    "chatmoderatorbot.ru"
    "www.chatmoderatorbot.ru"
    "miniapp.chatmoderatorbot.ru"
    "api.chatmoderatorbot.ru"
    "grafana.chatmoderatorbot.ru"
    "prometheus.chatmoderatorbot.ru"
    "admin.chatmoderatorbot.ru"
)

echo "=========================================="
echo "SSL Certificate Setup for chatmoderatorbot.ru"
echo "=========================================="
echo ""

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo "ERROR: Please run as root (use sudo)"
    exit 1
fi

# Step 1: Install certbot
echo "Step 1: Installing certbot..."
if ! command -v certbot &> /dev/null; then
    apt-get update
    apt-get install -y certbot
    echo "✓ Certbot installed"
else
    echo "✓ Certbot already installed"
fi

# Step 2: Create webroot directory
echo ""
echo "Step 2: Creating webroot directory..."
mkdir -p "${WEBROOT}"
echo "✓ Webroot created at ${WEBROOT}"

# Step 3: Stop nginx temporarily (if using standalone mode)
echo ""
echo "Step 3: Checking nginx status..."
if systemctl is-active --quiet nginx; then
    echo "Nginx is running. You have two options:"
    echo "  a) Use webroot mode (nginx keeps running)"
    echo "  b) Use standalone mode (nginx will be stopped temporarily)"
    echo ""
    read -p "Use webroot mode? (Y/n): " USE_WEBROOT
    USE_WEBROOT=${USE_WEBROOT:-Y}

    if [[ "$USE_WEBROOT" =~ ^[Nn]$ ]]; then
        echo "Stopping nginx for standalone mode..."
        systemctl stop nginx
        CERTBOT_MODE="standalone"
        NGINX_STOPPED=true
    else
        CERTBOT_MODE="webroot"
        NGINX_STOPPED=false
    fi
elif docker ps --format '{{.Names}}' | grep -q chatkeep-nginx; then
    echo "Nginx is running in Docker. Using webroot mode..."
    CERTBOT_MODE="webroot"
    NGINX_STOPPED=false
else
    echo "Nginx is not running. Using standalone mode..."
    CERTBOT_MODE="standalone"
    NGINX_STOPPED=false
fi

# Step 4: Obtain certificates
echo ""
echo "Step 4: Obtaining SSL certificates..."
echo "Mode: ${CERTBOT_MODE}"
echo "Domains: ${DOMAINS[*]}"
echo ""

# Build domain arguments
DOMAIN_ARGS=""
for domain in "${DOMAINS[@]}"; do
    DOMAIN_ARGS="${DOMAIN_ARGS} -d ${domain}"
done

if [ "$CERTBOT_MODE" = "standalone" ]; then
    certbot certonly --standalone \
        ${DOMAIN_ARGS} \
        --non-interactive \
        --agree-tos \
        --email "${EMAIL}" \
        --expand
else
    certbot certonly --webroot \
        -w "${WEBROOT}" \
        ${DOMAIN_ARGS} \
        --non-interactive \
        --agree-tos \
        --email "${EMAIL}" \
        --expand
fi

if [ $? -eq 0 ]; then
    echo "✓ SSL certificates obtained successfully"
else
    echo "✗ Failed to obtain SSL certificates"
    if [ "$NGINX_STOPPED" = true ]; then
        echo "Restarting nginx..."
        systemctl start nginx
    fi
    exit 1
fi

# Step 5: Restart nginx if it was stopped
if [ "$NGINX_STOPPED" = true ]; then
    echo ""
    echo "Step 5: Restarting nginx..."
    systemctl start nginx
    echo "✓ Nginx restarted"
fi

# Step 6: Set up auto-renewal
echo ""
echo "Step 6: Setting up auto-renewal..."

# Create renewal cron job
CRON_JOB="0 0 * * * root certbot renew --quiet --deploy-hook 'docker restart chatkeep-nginx || systemctl reload nginx'"
if ! crontab -l 2>/dev/null | grep -q "certbot renew"; then
    echo "$CRON_JOB" > /etc/cron.d/certbot-renew
    chmod 644 /etc/cron.d/certbot-renew
    echo "✓ Auto-renewal cron job created"
else
    echo "✓ Auto-renewal cron job already exists"
fi

# Test renewal (dry-run)
echo ""
echo "Step 7: Testing renewal process (dry-run)..."
certbot renew --dry-run
if [ $? -eq 0 ]; then
    echo "✓ Renewal test successful"
else
    echo "⚠ Renewal test failed - check configuration"
fi

# Step 8: Display certificate info
echo ""
echo "=========================================="
echo "SSL Setup Complete!"
echo "=========================================="
echo ""
echo "Certificate location: ${CERT_PATH}"
echo "Certificate files:"
echo "  - fullchain.pem: ${CERT_PATH}/fullchain.pem"
echo "  - privkey.pem: ${CERT_PATH}/privkey.pem"
echo ""
echo "Next steps:"
echo "  1. Uncomment HTTPS server blocks in nginx configs"
echo "  2. Uncomment HTTP -> HTTPS redirects"
echo "  3. Reload nginx: docker restart chatkeep-nginx"
echo ""
echo "Auto-renewal: Configured to run daily at midnight"
echo "Manual renewal: certbot renew"
echo ""
