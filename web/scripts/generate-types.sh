#!/bin/bash
# Regenerate Supabase TypeScript types from the linked project.
# Usage: ./scripts/generate-types.sh
# Requires: supabase CLI linked to the project

cd "$(dirname "$0")/.." || exit 1
echo "Generating database.types.ts..."
supabase gen types typescript --linked > src/lib/supabase/database.types.ts
echo "Done."
