---
description: Team-B (Standard) - 5-agent team with simplified workflow
---

# Team-B: Standard Engineering Team

**5 agents | State tracking | No health checks**

Best for: Normal development, standard features

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

## WORKFLOW

```
DISCOVER → DESIGN → DO → VERIFY
    │          │       │      │
 analyst   architect  dev   qa+devops
```

Sequential execution. Max 2 agents parallel in VERIFY.

---

## EXECUTION

### Step 1: Create State File

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

Ask: **"Plan ready. Start Phase 1?"**

---

### Step 2: Execute Phases

**Phase 1: DISCOVER**
```
Launch: analyst
Task: "Analyze requirements for: [task]. Return requirements, edge cases."
```

**Phase 2: DESIGN**
```
Launch: architect
Task: "Design solution. Return architecture, API, implementation steps."
```

**Phase 3: DO**
```
Launch: developer
Task: "Implement architect's design. Commit after each change."
```

**Phase 4: VERIFY**
```
Launch: qa
Task: "Test and review. Run unit tests, check security."

Launch: devops (if needed)
Task: "Update infrastructure."
```

---

### Step 3: Report

```
✓ COMPLETE

Phases: 4
Files: [list]
Tests: [pass/fail]
```

---

## RULES

1. Sequential phases - finish one before next
2. Max 2 parallel - only in VERIFY
3. State in file - survives compaction
4. NEVER skip VERIFY
5. NEVER implement yourself - delegate
6. Update state file after every phase

---

## IF CONTEXT COMPACTED

1. Read `.claude/team-state.md`
2. Find `## Current: Phase N`
3. Continue from there
4. Do NOT implement yourself

---

## TASK: $ARGUMENTS

Create state file → Ask approval → Execute phases → Report
