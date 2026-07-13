#!/usr/bin/env bash
# =============================================================================
# PRE-COMMIT HOOK TESTS (TDD RED phase for Task 1.1)
#
# Tests the behavior of .githooks/pre-commit without relying on a real Supabase
# installation. Uses a mock supabase binary to simulate lint pass/fail.
#
# Usage: bash .githooks/test_pre_commit.sh
# =============================================================================

set -euo pipefail
PASS=0
FAIL=0

# --- helpers ---------------------------------------------------------------
assert_eq() {
    local desc="$1" expected="$2" actual="$3"
    if [ "$expected" = "$actual" ]; then
        echo "  ✅ PASS: $desc"
        PASS=$((PASS + 1))
    else
        echo "  ❌ FAIL: $desc (expected: $expected, got: $actual)"
        FAIL=$((FAIL + 1))
    fi
}

cleanup() {
    rm -rf "$TMPDIR"
    unset SUPABASE_MOCK_EXIT
}
trap cleanup EXIT

# --- setup ----------------------------------------------------------------
TMPDIR=$(mktemp -d)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
HOOK="$SCRIPT_DIR/pre-commit"

# Create a mock supabase binary in PATH
MOCK_BINDIR="$TMPDIR/bin"
mkdir -p "$MOCK_BINDIR"

cat > "$MOCK_BINDIR/supabase" <<'MOCKEOF'
#!/usr/bin/env bash
# Mock supabase CLI — exit code controlled by SUPABASE_MOCK_EXIT
exit "${SUPABASE_MOCK_EXIT:-0}"
MOCKEOF
chmod +x "$MOCK_BINDIR/supabase"

# Save original PATH and prepend mock dir
ORIGINAL_PATH="$PATH"
export PATH="$MOCK_BINDIR:$PATH"

# ===========================================================================
# SCENARIO 1: Hook exits 0 when no migration files are staged
# ===========================================================================
echo ""
echo "=== SCENARIO 1: Non-migration changes ==="

# Create a non-migration file and stage it
touch "$TMPDIR/some_app_file.kt"
(
    cd "$TMPDIR"
    git init --quiet
    git add some_app_file.kt
)

# Run the hook with MOCK supabase that would FAIL — but hook shouldn't invoke it
SUPABASE_MOCK_EXIT=1
set +e
output_1=$(
    cd "$TMPDIR"
    GIT_DIR="$TMPDIR/.git" bash "$HOOK" 2>&1
)
exit_1=$?
set -e

assert_eq "Exit code 0 for non-migration changes" "0" "$exit_1"
# Hook should not have called supabase at all (no migration files → skip)
assert_eq "Hook did not invoke supabase for non-migration changes" "" \
    "$(echo "$output_1" | grep -c "supabase" || true)"

# ===========================================================================
# SCENARIO 2: Hook exits 0 when migration files pass lint
# ===========================================================================
echo ""
echo "=== SCENARIO 2: Valid migration passes lint ==="

# Create a migration file and stage it
mkdir -p "$TMPDIR/supabase/migrations"
touch "$TMPDIR/supabase/migrations/20260712000001_test.sql"
(
    cd "$TMPDIR"
    git add supabase/migrations/20260712000001_test.sql
)

SUPABASE_MOCK_EXIT=0
set +e
output_2=$(
    cd "$TMPDIR"
    GIT_DIR="$TMPDIR/.git" bash "$HOOK" 2>&1
)
exit_2=$?
set -e

assert_eq "Exit code 0 for valid migration" "0" "$exit_2"

# ===========================================================================
# SCENARIO 3: Hook exits 1 when migration files fail lint
# ===========================================================================
echo ""
echo "=== SCENARIO 3: Invalid migration blocks commit ==="

SUPABASE_MOCK_EXIT=1
set +e
output_3=$(
    cd "$TMPDIR"
    GIT_DIR="$TMPDIR/.git" bash "$HOOK" 2>&1
)
exit_3=$?
set -e

assert_eq "Exit code 1 for invalid migration" "1" "$exit_3"
# Should print an error message about lint failure
assert_eq "Output mentions lint failure" "1" \
    "$(echo "$output_3" | grep -ci "lint" || true)"

# ===========================================================================
# SCENARIO 4: Hook skips gracefully when supabase CLI is missing
# ===========================================================================
echo ""
echo "=== SCENARIO 4: Missing supabase CLI — graceful skip ==="

# Remove the mock supabase from PATH
export PATH="$ORIGINAL_PATH"

# Stage a migration file (use the same TMPDIR git state)
(
    cd "$TMPDIR"
    git add -A 2>/dev/null || true
)

set +e
output_4=$(
    cd "$TMPDIR"
    GIT_DIR="$TMPDIR/.git" bash "$HOOK" 2>&1
)
exit_4=$?
set -e

assert_eq "Exit code 0 when supabase CLI is missing" "0" "$exit_4"
assert_eq "Warning message when supabase is missing" "1" \
    "$(echo "$output_4" | grep -ci "supabase not found\|not installed\|not available" || true)"

# ===========================================================================
# Summary
# ===========================================================================
echo ""
echo "=========================================="
echo "  Results: $PASS passed, $FAIL failed"
echo "=========================================="

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
exit 0
