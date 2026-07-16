#!/usr/bin/env bash
set -euo pipefail

TOOL="cursor"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --tool) TOOL="$2"; shift 2 ;;
    *) echo "Unknown arg: $1"; exit 1 ;;
  esac
done

case "$TOOL" in
  cursor)
    mkdir -p "$ROOT/.cursor/rules"
    if [[ ! -f "$ROOT/.cursor/rules/loop-engine.mdc" ]]; then
      echo "Missing .cursor/rules/loop-engine.mdc"; exit 1
    fi
    if [[ ! -f "$ROOT/.cursor/mcp.json" ]]; then
      cat > "$ROOT/.cursor/mcp.json" <<'EOF'
{
  "mcpServers": {
    "spring-ai-loop-engine": {
      "url": "http://localhost:8080/sse"
    }
  }
}
EOF
    fi
    echo "Cursor setup complete:"
    echo "  - .cursor/rules/loop-engine.mdc"
    echo "  - .cursor/mcp.json"
    echo "See docs/cursor-setup.md"
    ;;
  *)
    echo "Unsupported tool: $TOOL (supported: cursor)"
    exit 1
    ;;
esac
