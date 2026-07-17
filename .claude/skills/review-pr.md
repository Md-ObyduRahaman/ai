---
name: review-pr
description: Review a GitHub pull request for bugs, efficiency, reuse, and cleanup. Pass the PR URL as an argument.
---

# /review-pr

Run a comprehensive code review on a GitHub pull request against this repository.

## Usage

```
/review-pr <pr-url>
```

Example:
```
/review-pr https://github.com/Md-ObyduRahaman/ai/pull/1
```

## What it does

1. **Fetch the PR diff** — Gets the unified diff from GitHub
2. **8-angle analysis** — Runs parallel finder agents across:
   - **Angle A**: Line-by-line bug scan (null derefs, wrong conditions, off-by-one)
   - **Angle B**: Removed-behavior audit (ensures deleted code's invariants are preserved)
   - **Angle C**: Cross-file tracer (caller/callee breakage, route conflicts)
   - **Angle D**: Reuse (copy-paste, missed utility opportunities)
   - **Angle E**: Simplification (redundant state, dead code, deep nesting)
   - **Angle F**: Efficiency (repeated I/O, redundant allocation, wasted bandwidth)
   - **Angle G**: Altitude (fragile bandaids vs proper mechanisms)
   - **Angle H**: Conventions (CLAUDE.md compliance)
3. **Verify candidates** — Dedup, then adversarially verify each finding
4. **Post results** — Output a ranked review as a comment on the PR

## Output format

Ranked findings (max 8, most severe first):

```
### 1. 🐛 Bug — file:line — summary
Impact: concrete failure scenario
```

Followed by lower-severity findings (⚡ efficiency, ♻️ reuse, 🎨 cleanup) and ✅ nice touches.

## Notes

- Uses the stored GitHub token from `git credential` for API access
- Reviews are posted as PR comments
- Only findings with a concrete failure scenario are included
- Cleanup/altitude/conventions findings ranked below correctness bugs