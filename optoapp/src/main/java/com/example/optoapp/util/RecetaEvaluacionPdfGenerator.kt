package com.example.optoapp.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Paciente
import java.io.File
import java.io.FileOutputStream

/**
 * Genera un PDF de fórmula optométrica / resumen clínico a partir de la última evaluación.
 *
 * Delega todo el dibujo a [RecetaPdfBuilder] y mantiene solo la creación del archivo,
 * la apertura del PDF y la compatibilidad backward de la firma del método [generate].
 */
object RecetaEvaluacionPdfGenerator {

    fun generate(context: Context, paciente: Paciente, eval: EvaluacionClinica): File {
        val dir = File(context.cacheDir, "recetas").apply { mkdirs() }
        val safeName = "formula_${paciente.id.take(12)}_${eval.fecha}.pdf"
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val out = File(dir, safeName)

        val doc = RecetaPdfBuilder()
            .addHeader(paciente, eval)
            .addRefraccion(eval)
            .addDiagnostico(eval)
            .addCondicionesAsociadas(eval)
            .addPrismas(eval)
            .addQueratometria(eval)
            .addContactologia(eval)
            .addPlanTratamiento(eval)
            .addObservaciones(eval)
            .addSeguimiento(eval)
            .build()

        FileOutputStream(out).use { fos -> doc.writeTo(fos) }
        doc.close()
        return out
    }

    fun openPdf(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Abrir PDF de fórmula"))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No hay ninguna app para abrir PDF; instala un visor de PDF.", Toast.LENGTH_LONG).show()
        }
    }
}
