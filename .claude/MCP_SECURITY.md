# MCP Tools Security

## Overview

MCP Chrome and Mobile tools are restricted to the `manual-qa` agent only to prevent accidental or unauthorized browser/device automation from the main agent or other agents.

## How It Works

### 1. Permissions Layer
All MCP tools are allowed in `.claude/settings.local.json`:
```json
"allow": [
  "mcp__claude-in-chrome__*",
  "mcp__mobile__*"
]
```

### 2. Hook-Based Restriction
`PreToolUse` hooks block these tools unless a marker file exists:
- **Marker File**: `.claude/.manual-qa-active`
- **Location**: Project root `.claude/` directory
- **Purpose**: Signals that manual-qa agent is active

### 3. Hook Logic
```bash
if [ ! -f ".claude/.manual-qa-active" ]; then
  echo '🚫 BLOCK: MCP Chrome/Mobile tools restricted to manual-qa agent only.'
  exit 2  # Block the tool call
fi
```

### 4. manual-qa Agent Workflow

When `manual-qa` agent is launched:

1. **START**: Creates marker file
   ```bash
   touch .claude/.manual-qa-active
   ```

2. **WORK**: Uses MCP Chrome/Mobile tools freely
   - Browser automation via `mcp__claude-in-chrome__*`
   - Mobile testing via `mcp__mobile__*`

3. **END**: Deletes marker file
   ```bash
   rm .claude/.manual-qa-active
   ```

## Security Benefits

- **Prevents Accidental Automation**: Main agent cannot accidentally navigate browser/device
- **Clear Separation**: Only specialized testing agent has UI automation access
- **Audit Trail**: Marker file presence indicates active testing session
- **Delegation Enforcement**: Forces proper workflow through Task tool

## Usage by Main Agent/EM

Main agent and EM MUST delegate UI testing:

```
DO THIS:
Launch manual-qa agent via Task tool:
  subagent_type: "manual-qa"
  prompt: "Test [feature] on [platform]..."

DON'T DO THIS:
Call mcp__claude-in-chrome__* or mcp__mobile__* directly
(Will be blocked by hooks)
```

## File Lifecycle

| State | File Exists | MCP Tools Access |
|-------|-------------|------------------|
| Normal operation | ❌ No | 🚫 Blocked for all agents |
| manual-qa active | ✅ Yes | ✅ Allowed (manual-qa only) |
| manual-qa done | ❌ No | 🚫 Blocked for all agents |

## Troubleshooting

### "BLOCK: MCP Chrome tools restricted" error

**Cause**: Trying to use MCP tools without marker file

**Solution**:
1. Don't use MCP tools directly from main agent
2. Launch `manual-qa` agent via Task tool
3. manual-qa will handle marker file automatically

### Marker file left behind

**Symptom**: `.claude/.manual-qa-active` exists after testing

**Fix**:
```bash
rm .claude/.manual-qa-active
```

**Prevention**: manual-qa agent should always clean up in final step

## Configuration Files

- **Permissions**: `.claude/settings.local.json` (permissions.allow)
- **Hooks**: `.claude/settings.local.json` (hooks.PreToolUse)
- **Agent Instructions**: `.claude/commands/team.md` (manual-qa section)
- **Gitignore**: `.gitignore` (excludes marker file)

## Related Documentation

- `/team` command: `.claude/commands/team.md`
- Settings: `.claude/settings.local.json`
- Agent descriptions: See HARD RULE #10 in team.md