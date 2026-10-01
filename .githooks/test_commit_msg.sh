#!/usr/bin/env bash
# =============================================================================
# COMMIT-MSG HOOK TESTS
#
# Cursor tooling appends attribution trailers to agent commits; the repo forbids
# AI attribution. These tests drive .githooks/commit-msg directly on message
# files, so no git repository or Cursor install is needed.
#
# Usage: bash .githooks/test_commit_msg.sh
# =============================================================================

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOOK="$REPO_ROOT/.githooks/commit-msg"
TMPDIR_TEST="$(mktemp -d)"
trap 'rm -rf "$TMPDIR_TEST"' EXIT

PASS=0
FAIL=0

assert_eq() {
    local description="$1" expected="$2" actual="$3"
    if [ "$expected" = "$actual" ]; then
        echo "  ✅ PASS: $description"
        PASS=$((PASS + 1))
    else
        echo "  ❌ FAIL: $description"
        echo "     expected: $(printf '%q' "$expected")"
        echo "     actual:   $(printf '%q' "$actual")"
        FAIL=$((FAIL + 1))
    fi
}

run_hook_on() {
    local msg_file="$TMPDIR_TEST/COMMIT_EDITMSG"
    printf '%s' "$1" > "$msg_file"
    bash "$HOOK" "$msg_file"
    HOOK_EXIT=$?
    HOOK_RESULT="$(cat "$msg_file")"
}

echo ""
echo "=== SCENARIO 1: Cursor co-author trailer is stripped ==="
run_hook_on $'feat: add thing\n\nBody line.\n\nCo-authored-by: Cursor <cursoragent@cursor.com>\n'
assert_eq "Exit code 0" "0" "$HOOK_EXIT"
assert_eq "Trailer and its separator removed" $'feat: add thing\n\nBody line.' "$HOOK_RESULT"

echo ""
echo "=== SCENARIO 2: Made-with trailer is stripped, case-insensitively ==="
run_hook_on $'fix: bug\n\nmade-with: cursor\nCO-AUTHORED-BY: cursor <cursoragent@cursor.com>\n'
assert_eq "Exit code 0" "0" "$HOOK_EXIT"
assert_eq "Both attribution lines removed" "fix: bug" "$HOOK_RESULT"

echo ""
echo "=== SCENARIO 3: Human co-authors are preserved ==="
run_hook_on $'chore: pair work\n\nCo-authored-by: Jane Doe <jane@example.com>\nCo-authored-by: Cursor <cursoragent@cursor.com>\n'
assert_eq "Exit code 0" "0" "$HOOK_EXIT"
assert_eq "Only the Cursor trailer removed" $'chore: pair work\n\nCo-authored-by: Jane Doe <jane@example.com>' "$HOOK_RESULT"

echo ""
echo "=== SCENARIO 4: Messages without attribution are untouched ==="
original=$'docs: update guide\n\nMentions Cursor in prose, which is fine.\n'
run_hook_on "$original"
assert_eq "Exit code 0" "0" "$HOOK_EXIT"
assert_eq "Message byte-identical" "$original" "$HOOK_RESULT"$'\n'

echo ""
echo "=========================================="
echo "  Results: $PASS passed, $FAIL failed"
echo "=========================================="

[ "$FAIL" -eq 0 ]
