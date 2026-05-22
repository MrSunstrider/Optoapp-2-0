import "@supabase/functions-js/edge-runtime.d.ts"
import { createClient } from "@supabase/supabase-js"

const supabaseUrl = Deno.env.get("SUPABASE_URL")!
const supabaseServiceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!

interface TrackReleasePayload {
  version: string
  apk_download_url: string
  release_notes?: string
}

Deno.serve(async (req) => {
  // Only accept POST from CI
  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), { status: 405 })
  }

  try {
    const payload: TrackReleasePayload = await req.json()

    if (!payload.version || !payload.apk_download_url) {
      return new Response(
        JSON.stringify({ error: "Missing required fields: version, apk_download_url" }),
        { status: 400 },
      )
    }

    const supabase = createClient(supabaseUrl, supabaseServiceRoleKey)

    // Upsert: if version exists, update; otherwise insert
    const { error } = await supabase
      .from("app_releases")
      .upsert(
        {
          version: payload.version,
          apk_download_url: payload.apk_download_url,
          release_notes: payload.release_notes ?? "",
        },
        { onConflict: "version" },
      )

    if (error) {
      console.error("Error inserting release:", error)
      return new Response(JSON.stringify({ error: error.message }), { status: 500 })
    }

    return new Response(
      JSON.stringify({ success: true, version: payload.version }),
      { headers: { "Content-Type": "application/json" } },
    )
  } catch (err) {
    console.error("Invalid request:", err)
    return new Response(JSON.stringify({ error: "Invalid request" }), { status: 400 })
  }
})
