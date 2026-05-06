import { SupabaseClient } from "@supabase/supabase-js";
import { useEffect } from "react";

export function useSupabaseRealtime(
  supabase: SupabaseClient,
  table: string,
  opticaId: string,
  onUpdate: (payload: any) => void
) {
  useEffect(() => {
    const channel = supabase
      .channel(`realtime_${table}`)
      .on(
        "postgres_changes",
        {
          event: "*",
          schema: "public",
          table: table,
          filter: `optica_id=eq.${opticaId}`,
        },
        (payload) => {
          onUpdate(payload);
        }
      )
      .subscribe();

    return () => {
      supabase.removeChannel(channel);
    };
  }, [supabase, table, opticaId, onUpdate]);
}
