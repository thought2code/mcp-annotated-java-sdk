# Git Commit Rules

When committing code, you MUST follow these rules:

## Commit message format

Use Conventional Commits:

`<type>(<scope>): <subject>`

Allowed types:

- feat
- fix
- docs
- style
- refactor
- perf
- test
- build
- ci
- chore

Examples:

feat(auth): add oauth login support

fix(order): prevent duplicate payment processing

Rules:

- subject must be lowercase
- subject max length 72 chars
- no period at the end
- use imperative mood
- commit message must be written in English

Before committing:

- run git diff --cached
- verify commit message format
