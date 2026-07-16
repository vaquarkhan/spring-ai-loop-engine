# scripts

Helper scripts for local developer onboarding.

## Scripts

| Script | Platform | Purpose |
|--------|----------|---------|
| `install.sh` | macOS / Linux | Ensures MCP config / editor rule files exist |
| `install.ps1` | Windows PowerShell | Same as above for Windows |

## Usage

```bash
# Unix
./scripts/install.sh --tool cursor

# Windows
./scripts/install.ps1 -Tool cursor
```

These scripts are convenience helpers for MCP `mcp.json` scaffolding. The Loop Engine itself is IDE-agnostic; paste the generated JSON into any MCP-capable client.

## Note

Prefer documenting MCP wiring in the root README’s **IDE Integration** section for a client-neutral overview. Use these scripts when you want a local file written automatically.
