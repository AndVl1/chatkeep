# Feature 1: Connect Group Selection

## Status: Phase 2 - Exploration

## Task
When user types `/connect` without chat_id, show inline keyboard with admin groups to select.

## Requirements
- Show inline keyboard when /connect without chat_id
- Display groups where user is admin
- Max 2 columns × 3 rows = 6 buttons per page
- Pagination: "Back" and "Next" buttons if more groups
- Button text = group names
- After selection → enter connect mode for that group

## Key Files Identified
- `AdminSessionHandler.kt` - handles /connect command (MODIFY)
- `AdminCommandHandler.kt` - has /mychats logic for finding admin chats (REFERENCE)
- `ChatService.kt` - getAllChats() method
- `AdminCacheService.kt` - admin check

## Exploration Findings
(To be filled after Phase 2)

## Implementation Notes
- Reuse isUserAdminInChat pattern from AdminCommandHandler
- Need callback handler for button clicks
- Pagination state management needed
