#!/usr/bin/env bash
# =============================================================================
# PRE-COMMIT HOOK TESTS
#
# Tests the behavior of .githooks/pre-commit without relying on a real Supabase,
# Docker, or GGA installation. Mock binaries simulate each tool; GGA is injected
# through GGA_CMD so the real reviewer is never invoked.
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

contains() {
    if echo "$1" | grep -qi "$2"; then echo "yes"; else echo "no"; fi
}

run_hook() {
    (
        cd "$TMPDIR"
        GIT_DIR="$TMPDIR/.git" bash "$HOOK" 2>&1
    )
}

cleanup() {
    rm -rf "$TMPDIR"
}
trap cleanup EXIT

# --- setup ----------------------------------------------------------------
TMPDIR=$(mktemp -d)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
HOOK="$SCRIPT_DIR/pre-commit"

MOCK_BINDIR="$TMPDIR/bin"
mkdir -p "$MOCK_BINDIR"

# supabase: `status` reports a running stack; `db lint` exits SUPABASE_MOCK_EXIT.
cat > "$MOCK_BINDIR/supabase" <<'MOCKEOF'
#!/usr/bin/env bash
echo "MOCK_SUPABASE_CALLED $*"
if [ "${1:-}" = "status" ]; then exit 0; fi
exit "${SUPABASE_MOCK_EXIT:-0}"
MOCKEOF

cat > "$MOCK_BINDIR/docker" <<'MOCKEOF'
#!/usr/bin/env bash
exit 0
MOCKEOF

cat > "$MOCK_BINDIR/gga-mock" <<'MOCKEOF'
#!/usr/bin/env bash
echo "MOCK_GGA_CALLED $*"
exit "${GGA_MOCK_EXIT:-0}"
MOCKEOF

chmod +x "$MOCK_BINDIR/supabase" "$MOCK_BINDIR/docker" "$MOCK_BINDIR/gga-mock"

ORIGINAL_PATH="$PATH"
export PATH="$MOCK_BINDIR:$PATH"
export GGA_CMD="$MOCK_BINDIR/gga-mock"
export GGA_MOCK_EXIT=0

(
    cd "$TMPDIR"
    git init --quiet
)

# ===========================================================================
# SCENARIO 1: Non-migration changes skip Supabase lint
# ===========================================================================
echo ""
echo "=== SCENARIO 1: Non-migration changes ==="

touch "$TMPDIR/some_app_file.kt"
(cd "$TMPDIR" && git add some_app_file.kt)

export SUPABASE_MOCK_EXIT=1
set +e
output_1=$(run_hook)
exit_1=$?
set -e

assert_eq "Exit code 0 for non-migration changes" "0" "$exit_1"
assert_eq "Hook did not invoke supabase for non-migration changes" "no" \
    "$(contains "$output_1" "MOCK_SUPABASE_CALLED")"

# ===========================================================================
# SCENARIO 2: Valid migration passes lint
# ===========================================================================
echo ""
echo "=== SCENARIO 2: Valid migration passes lint ==="

mkdir -p "$TMPDIR/supabase/migrations"
touch "$TMPDIR/supabase/migrations/20260712000001_test.sql"
(cd "$TMPDIR" && git add supabase/migrations/20260712000001_test.sql)

export SUPABASE_MOCK_EXIT=0
set +e
output_2=$(run_hook)
exit_2=$?
set -e

assert_eq "Exit code 0 for valid migration" "0" "$exit_2"
assert_eq "Hook ran supabase db lint" "yes" \
    "$(contains "$output_2" "MOCK_SUPABASE_CALLED db lint")"

# ===========================================================================
# SCENARIO 3: Invalid migration blocks commit
# ===========================================================================
echo ""
echo "=== SCENARIO 3: Invalid migration blocks commit ==="

export SUPABASE_MOCK_EXIT=1
set +e
output_3=$(run_hook)
exit_3=$?
set -e

assert_eq "Exit code 1 for invalid migration" "1" "$exit_3"
assert_eq "Output mentions lint failure" "yes" "$(contains "$output_3" "lint failed")"
assert_eq "GGA not run after lint failure" "no" "$(contains "$output_3" "MOCK_GGA_CALLED")"

# ===========================================================================
# SCENARIO 4: Missing supabase CLI — graceful skip
# ===========================================================================
echo ""
echo "=== SCENARIO 4: Missing supabase CLI — graceful skip ==="

NO_SUPABASE_BINDIR="$TMPDIR/bin-no-supabase"
mkdir -p "$NO_SUPABASE_BINDIR"
cp "$MOCK_BINDIR/docker" "$NO_SUPABASE_BINDIR/docker"
cp "$MOCK_BINDIR/gga-mock" "$NO_SUPABASE_BINDIR/gga-mock"
# Minimal PATH so a real Supabase CLI installed on the machine cannot leak in.
export PATH="$NO_SUPABASE_BINDIR:/usr/bin:/bin:/mingw64/bin"
export GGA_CMD="$NO_SUPABASE_BINDIR/gga-mock"
hash -r

set +e
output_4=$(run_hook)
exit_4=$?
set -e

assert_eq "Exit code 0 when supabase CLI is missing" "0" "$exit_4"
assert_eq "Warning message when supabase is missing" "yes" \
    "$(contains "$output_4" "supabase CLI not found")"
export GGA_CMD="$MOCK_BINDIR/gga-mock"

export PATH="$MOCK_BINDIR:$ORIGINAL_PATH"
export SUPABASE_MOCK_EXIT=0
(cd "$TMPDIR" && git rm --cached --quiet -r supabase && rm -rf supabase)

# ===========================================================================
# SCENARIO 5: GGA reviews every commit
# ===========================================================================
echo ""
echo "=== SCENARIO 5: GGA runs on non-migration commits ==="

export GGA_MOCK_EXIT=0
set +e
output_5=$(run_hook)
exit_5=$?
set -e

assert_eq "Exit code 0 when GGA approves" "0" "$exit_5"
assert_eq "Hook invoked gga run" "yes" "$(contains "$output_5" "MOCK_GGA_CALLED run")"

# ===========================================================================
# SCENARIO 6: GGA rejection blocks commit
# ===========================================================================
echo ""
echo "=== SCENARIO 6: GGA rejection blocks commit ==="

export GGA_MOCK_EXIT=1
set +e
output_6=$(run_hook)
exit_6=$?
set -e

assert_eq "Exit code 1 when GGA rejects" "1" "$exit_6"
assert_eq "Output mentions GGA failure" "yes" "$(contains "$output_6" "GGA review failed")"

# ===========================================================================
# SCENARIO 7: Missing GGA — graceful skip with warning
# ===========================================================================
echo ""
echo "=== SCENARIO 7: Missing GGA — graceful skip ==="

export GGA_CMD="gga-does-not-exist-$$"
set +e
output_7=$(run_hook)
exit_7=$?
set -e

assert_eq "Exit code 0 when GGA is missing" "0" "$exit_7"
assert_eq "Warning message when GGA is missing" "yes" "$(contains "$output_7" "GGA not found")"

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
