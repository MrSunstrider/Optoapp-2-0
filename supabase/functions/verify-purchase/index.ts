import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

serve(async (req: Request) => {
  try {
    const { purchaseToken, opticaId } = await req.json()
    const authHeader = req.headers.get("Authorization")!

    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
    )
    const { data: { user }, error: authError } = await supabase.auth.getUser(
      authHeader.replace("Bearer ", "")
    )
    if (authError || !user) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 })
    }

    // Verify the user is a member of the target optica
    const { count, error: membershipError } = await supabase
      .from("usuario_optica")
      .select("*", { count: "exact", head: true })
      .eq("user_id", user.id)
      .eq("optica_id", opticaId)
    if (membershipError || count === 0) {
      return new Response(JSON.stringify({ error: "Forbidden: user not in optica" }), { status: 403 })
    }

    // Actualiza plan_code (columna canónica) y plan (legacy compat)
    // usando current_period_end = now + 30d como corte por defecto
    const now = new Date()
    const periodEnd = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000).toISOString()

    const { error: updateError } = await supabase
      .from("opticas")
      .update({
        plan: "pro",
        plan_code: "pro_individual",
        plan_source: "playstore",
        plan_status: "active",
        current_period_end: periodEnd,
      })
      .eq("id", opticaId)

    if (updateError) {
      console.error("Error updating optica plan:", updateError)
      return new Response(JSON.stringify({ error: updateError.message }), { status: 500 })
    }

    return new Response(JSON.stringify({ valid: true, tier: "pro" }))
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), { status: 500 })
  }
})
