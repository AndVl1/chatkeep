---
description: Team-A (Full) - 5-agent team with health checks and strict protocol
---

# Team-A: Full Engineering Team

**5 agents | Health checks | Feature tracking | E2E verification**

Best for: Complex features, production-critical work

**Project**: Chatkeep (Telegram bot + Spring Boot backend + Koog AI)

---

## YOUR TEAM

| Agent | Role |
|-------|------|
| **analyst** | Requirements, research, edge cases |
| **architect** | Design, APIs, data model, implementation plan |
| **developer** | Fullstack implementation |
| **qa** | Testing, security, code review |
| **devops** | Infrastructure, CI/CD, deployment |

---

## ANTI-PATTERNS TO AVOID

| Problem | Prevention |
|---------|------------|
| Over-ambition | ONE feature per cycle |
| Premature completion | VERIFY phase is MANDATORY |
| Environmental degradation | Git commits + state file |
| Skipping health checks | ALWAYS run tests first |

---

## WORKFLOW

```
HEALTH CHECK → DISCOVER → DESIGN → DO → VERIFY
      │            │          │      │       │
   tests       analyst   architect  dev   qa+devops
```

---

## EXECUTION

### Step 0: Health Check (MANDATORY)

```bash
git status && git log --oneline -5
./gradlew test || echo "No tests"
```

If tests fail → Fix first!

---

### Step 1: Create Feature List

Create `.claude/features.json`:
```json
{
  "project": "[Name]",
  "features": [
    {"id": "F001", "name": "Feature", "status": "failing"}
  ]
}
```

---

### Step 2: Create State File

Create `.claude/team-state.md`:
```markdown
# TEAM STATE
## Task: [User request]
## Current: Phase 1

## Phase 1: DISCOVER - analyst - pending
## Phase 2: DESIGN - architect - pending
## Phase 3: DO - developer - pending
## Phase 4: VERIFY - qa - pending

## Recovery: Continue from Current Phase, DELEGATE to agents!
```

---

### Step 3: Execute Phases

For each phase:
1. Run baseline test
2. Launch agent
3. Update state file
4. Git commit
5. Run tests again

**Phase 1: DISCOVER** → Launch analyst
**Phase 2: DESIGN** → Launch architect
**Phase 3: DO** → Launch developer
**Phase 4: VERIFY** → Launch qa (+devops if needed)

---

### Step 4: Complete

Update features.json: `"status": "passing"`
Summary commit: `feat(F001): [Feature] - verified`

---

## HARD RULES

1. ONE feature per cycle
2. Health check first
3. Sequential phases
4. NEVER skip VERIFY
5. E2E tests required
6. NEVER implement yourself
7. Git after each phase
8. Update state file always

---

## TASK: $ARGUMENTS

Run health check → Create state → Execute phases → Verify complete
