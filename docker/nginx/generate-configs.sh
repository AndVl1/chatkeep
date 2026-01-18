#!/bin/bash
# Generate nginx configs for specified domain
# Usage: ./generate-configs.sh <domain>
# Example: ./generate-configs.sh chatmodtest.ru

set -e

DOMAIN="${1:-chatmoderatorbot.ru}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SITES_DIR="$SCRIPT_DIR/sites"

echo "Generating nginx configs for domain: $DOMAIN"

# Create sites directory if not exists
mkdir -p "$SITES_DIR"

# Function to generate config from template
generate_config() {
    local template_name="$1"
    local output_name="$2"

    # Read template and replace {{DOMAIN}} with actual domain
    sed "s/{{DOMAIN}}/$DOMAIN/g" "$SCRIPT_DIR/templates/$template_name" > "$SITES_DIR/$output_name"
    echo "Generated: $output_name"
}

# Generate all configs
generate_config "api.conf.template" "api.$DOMAIN.conf"
generate_config "miniapp.conf.template" "miniapp.$DOMAIN.conf"
generate_config "admin.conf.template" "admin.$DOMAIN.conf"
generate_config "grafana.conf.template" "grafana.$DOMAIN.conf"
generate_config "prometheus.conf.template" "prometheus.$DOMAIN.conf"
generate_config "www.conf.template" "www.$DOMAIN.conf"
generate_config "main.conf.template" "$DOMAIN.conf"

# Copy default.conf (no domain substitution needed)
if [ -f "$SCRIPT_DIR/templates/default.conf.template" ]; then
    cp "$SCRIPT_DIR/templates/default.conf.template" "$SITES_DIR/default.conf"
    echo "Copied: default.conf"
fi

echo "Done! Generated configs in $SITES_DIR"
