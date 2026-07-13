#!/usr/bin/env bash
# =============================================================================
# OptoApp Development Setup
#
# One-time setup script for new developers. Registers git hooks, installs
# Supabase CLI if missing, and validates the local environment.
#
# Usage: bash setup.sh
# =============================================================================

set -euo pipefail

echo "============================================"
echo "  OptoApp Development Setup"
echo "============================================"
echo ""

# --- Git hooks registration --------------------------------------------------
echo "[1/3] Registering git hooks..."
git config core.hooksPath .githooks
echo "  ✓ core.hooksPath = .githooks"
echo ""

# --- Supabase CLI check ------------------------------------------------------
echo "[2/3] Checking Supabase CLI..."
if command -v supabase &>/dev/null; then
    SUPABASE_VERSION=$(supabase --version 2>&1 | head -1)
    echo "  ✓ Supabase CLI found: $SUPABASE_VERSION"
else
    echo "  ⚠ Supabase CLI not found."
    echo "    Install via: npm install -g supabase"
    echo "    Docs: https://supabase.com/docs/guides/cli/getting-started"
fi
echo ""

# --- Docker check ------------------------------------------------------------
echo "[3/3] Checking Docker..."
if docker info &>/dev/null; then
    echo "  ✓ Docker is running"
else
    echo "  ⚠ Docker not available — local Supabase won't start."
    echo "    Install Docker Desktop: https://docs.docker.com/desktop/"
    echo "    Pre-commit hook will skip migration lint without Docker."
fi
echo ""

echo "============================================"
echo "  Setup complete!"
echo "============================================"
echo ""
echo "Next steps:"
echo "  1. Add local.properties with supabase.url and supabase.anon.key"
echo "  2. Run 'supabase start' to launch the local database"
echo "  3. Run './gradlew :optoapp:assembleDebug' to build"
echo ""
