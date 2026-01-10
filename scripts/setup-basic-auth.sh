#!/bin/bash
# Basic Auth Setup Script for Prometheus
# This script should be run on the production server (89.125.243.104)
#
# Usage: sudo ./scripts/setup-basic-auth.sh [username] [password]

set -e

# Configuration
HTPASSWD_FILE="/root/chatkeep/docker/nginx/.htpasswd"
DEFAULT_USERNAME="admin"

echo "=========================================="
echo "Basic Auth Setup for Prometheus"
echo "=========================================="
echo ""

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo "ERROR: Please run as root (use sudo)"
    exit 1
fi

# Step 1: Install apache2-utils (for htpasswd command)
echo "Step 1: Installing apache2-utils..."
if ! command -v htpasswd &> /dev/null; then
    apt-get update
    apt-get install -y apache2-utils
    echo "✓ apache2-utils installed"
else
    echo "✓ apache2-utils already installed"
fi

# Step 2: Get username and password
echo ""
echo "Step 2: Configuring credentials..."

if [ -n "$1" ]; then
    USERNAME="$1"
else
    read -p "Enter username [${DEFAULT_USERNAME}]: " USERNAME
    USERNAME=${USERNAME:-$DEFAULT_USERNAME}
fi

if [ -n "$2" ]; then
    PASSWORD="$2"
else
    read -sp "Enter password: " PASSWORD
    echo ""
    read -sp "Confirm password: " PASSWORD2
    echo ""

    if [ "$PASSWORD" != "$PASSWORD2" ]; then
        echo "✗ Passwords do not match"
        exit 1
    fi
fi

# Step 3: Create .htpasswd file
echo ""
echo "Step 3: Creating .htpasswd file..."

# Create directory if it doesn't exist
mkdir -p "$(dirname "$HTPASSWD_FILE")"

# Create or update .htpasswd file
htpasswd -cb "$HTPASSWD_FILE" "$USERNAME" "$PASSWORD"

if [ $? -eq 0 ]; then
    echo "✓ .htpasswd file created at ${HTPASSWD_FILE}"
else
    echo "✗ Failed to create .htpasswd file"
    exit 1
fi

# Step 4: Set proper permissions
echo ""
echo "Step 4: Setting permissions..."
chmod 644 "$HTPASSWD_FILE"
echo "✓ Permissions set"

# Step 5: Verify
echo ""
echo "Step 5: Verifying..."
if [ -f "$HTPASSWD_FILE" ]; then
    echo "✓ File exists and contains:"
    cat "$HTPASSWD_FILE"
else
    echo "✗ File not found"
    exit 1
fi

echo ""
echo "=========================================="
echo "Basic Auth Setup Complete!"
echo "=========================================="
echo ""
echo "Username: ${USERNAME}"
echo "File: ${HTPASSWD_FILE}"
echo ""
echo "Next steps:"
echo "  1. Restart nginx: docker restart chatkeep-nginx"
echo "  2. Test access: https://prometheus.chatmoderatorbot.ru"
echo ""
echo "To add more users:"
echo "  htpasswd -b ${HTPASSWD_FILE} <username> <password>"
echo ""
