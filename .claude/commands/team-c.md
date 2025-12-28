---
description: Team-C (Fast) - 3-agent lightweight team for quick iterations
---

# Team-C: Lightweight Team

**3 agents | Fast iterations | Minimal overhead**

Best for: Quick fixes, small features, prototyping

**Project**: Chatkeep (Telegram bot + Spring Boot backend + Koog AI)

---

## YOUR TEAM

| Agent | Role |
|-------|------|
| **analyst** | Quick requirements check |
| **developer** | Implementation |
| **qa** | Testing and review |

---

## WORKFLOW

```
ANALYZE → IMPLEMENT → VERIFY
    │          │          │
 analyst   developer     qa
```

Fast, sequential, no design phase.

---

## EXECUTION

### Step 1: Quick Plan

Create `.claude/team-state.md`:
```markdown
# TEAM STATE (Lightweight)
## Task: [User request]
## Current: Phase 1

## Phase 1: ANALYZE - analyst - pending
## Phase 2: IMPLEMENT - developer - pending
## Phase 3: VERIFY - qa - pending
```

---

### Step 2: Execute

**Phase 1: ANALYZE** (quick)
```
Launch: analyst
Task: "Quick analysis: [task]. Return key requirements and gotchas. Be brief."
```

**Phase 2: IMPLEMENT**
```
Launch: developer
Task: "Implement based on analyst notes. Keep it simple. Commit when done."
```

**Phase 3: VERIFY**
```
Launch: qa
Task: "Quick review and test. Flag any issues."
```

---

### Step 3: Done

```
✓ DONE

Files: [list]
Status: [verified/needs-work]
```

---

## RULES

1. Keep it fast - minimal documentation
2. Sequential - one agent at a time
3. ALWAYS verify - never skip qa
4. NEVER implement yourself - delegate
5. Simple commits - no feature tracking

---

## WHEN TO USE

- Bug fixes
- Small features
- Config changes
- Prototypes
- Spikes/experiments

For complex work → Use `/team-a` instead

---

## TASK: $ARGUMENTS

Quick plan → Analyze → Implement → Verify → Done
