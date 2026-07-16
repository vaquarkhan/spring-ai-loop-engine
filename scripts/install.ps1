param(
    [ValidateSet("cursor")]
    [string]$Tool = "cursor"
)

$Root = Split-Path -Parent $PSScriptRoot

if ($Tool -eq "cursor") {
    $rulesDir = Join-Path $Root ".cursor\rules"
    New-Item -ItemType Directory -Force -Path $rulesDir | Out-Null
    $rulePath = Join-Path $rulesDir "loop-engine.mdc"
    if (-not (Test-Path $rulePath)) {
        Write-Error "Missing .cursor/rules/loop-engine.mdc"
        exit 1
    }
    $mcpPath = Join-Path $Root ".cursor\mcp.json"
    if (-not (Test-Path $mcpPath)) {
        @'
{
  "mcpServers": {
    "spring-ai-loop-engine": {
      "url": "http://localhost:8080/sse"
    }
  }
}
'@ | Set-Content -Path $mcpPath -Encoding utf8
    }
    Write-Host "Cursor setup complete:"
    Write-Host "  - .cursor/rules/loop-engine.mdc"
    Write-Host "  - .cursor/mcp.json"
    Write-Host "See docs/cursor-setup.md"
}
