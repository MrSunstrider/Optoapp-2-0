package com.example.optoapp.util

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.withTranslation
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Paciente
import java.time.LocalDate

/**
 * Builder para generar [PdfDocument] de fórmula optométrica / resumen clínico.
 *
 * Cada método retorna `this` para encadenamiento. Llamar a [build] al final
 * para obtener el documento completo con pie de página.
 */
class RecetaPdfBuilder {

    private val doc = PdfDocument()
    private var pageNum = 0
    private var page = doc.startPage(
        PdfDocument.PageInfo.Builder(PdfStyle.PAGE_W, PdfStyle.PAGE_H, ++pageNum).create()
    )
    private var canvas: Canvas = page.canvas
    private var y = 0f

    // ─── Public API ─────────────────────────────────────────────────────────

    /**
     * Dibuja la cabecera del PDF: título centrado + tarjeta de paciente.
     */
    fun addHeader(paciente: Paciente, eval: EvaluacionClinica): RecetaPdfBuilder {
        initPage()

        // Título centrado
        val title = layoutText("FÓRMULA OPTOMÉTRICA", PdfStyle.titlePaint, contentWidth(), Layout.Alignment.ALIGN_CENTER)
        drawStaticLayout(title, PdfStyle.MARGIN, y)
        advance(title.height + 12f)

        drawHorizontalRule()
        advance(12f)

        // Patient info card – estructura tipo ficha
        val infoLines = buildString {
            append("Nombre: ${paciente.nombreCompleto}")
            append("    Edad: ${paciente.edad} años")
            if (paciente.dni?.isNotBlank() == true) append("    DNI: ${paciente.dni}")
            appendLine()
            if (paciente.distrito?.isNotBlank() == true) append("Distrito: ${paciente.distrito}")
            if (paciente.telefono.isNotBlank()) append("    Celular: ${paciente.telefono}")
            appendLine()
            append("Fecha de evaluación: ${DateUtils.formatLocalized(eval.fecha)}")
        }
        val innerW = contentWidth() - (2 * PdfStyle.CARD_PAD).toInt()
        val infoBody = layoutText(infoLines.trimEnd(), PdfStyle.bodyPaint, innerW)
        val cardH = PdfStyle.CARD_PAD + infoBody.height + PdfStyle.CARD_PAD

        ensureSpace(cardH + 20f)

        val cardRect = RectF(
            PdfStyle.MARGIN, y,
            PdfStyle.PAGE_W - PdfStyle.MARGIN, y + cardH
        )
        canvas.drawRoundRect(cardRect, PdfStyle.CARD_RADIUS, PdfStyle.CARD_RADIUS, PdfStyle.cardFillPaint)
        canvas.drawRoundRect(cardRect, PdfStyle.CARD_RADIUS, PdfStyle.CARD_RADIUS, PdfStyle.cardStrokePaint)
        drawStaticLayout(infoBody, PdfStyle.MARGIN + PdfStyle.CARD_PAD, y + PdfStyle.CARD_PAD)
        advance(cardH + 20f)

        return this
    }

    /**
     * Dibuja la tabla de refracción, AV, ADD y DIP.
     * Delega a [RefraccionTableBuilder.draw] extraído a RecetaRefraccionTable.kt.
     */
    fun addRefraccion(eval: EvaluacionClinica): RecetaPdfBuilder {
        RefraccionTableBuilder.draw(
            eval = eval,
            canvas = canvas,
            yPos = y,
            ensureSpace = { need -> ensureSpace(need) },
            drawSl = { sl, x, sy -> drawStaticLayout(sl, x, sy) },
            layoutText = { text, paint, width, align ->
                layoutText(text, paint, width, align)
            },
            advance = { d -> advance(d) }
        )
        return this
    }

    /**
     * Dibuja la sección de diagnóstico combinado con condiciones asociadas.
     */
    fun addDiagnostico(eval: EvaluacionClinica): RecetaPdfBuilder {
        val diag = buildString {
            val dOd = eval.diagnosticoOd.firstOrNull().orEmpty()
            val dOi = eval.diagnosticoOi.firstOrNull().orEmpty()
            if (dOd.isNotBlank() && dOi.isNotBlank()) {
                appendLine("OD: $dOd   |   OI: $dOi")
            } else if (dOd.isNotBlank()) {
                appendLine("OD: $dOd")
            } else if (dOi.isNotBlank()) {
                appendLine("OI: $dOi")
            }
            if (eval.diagnostico.isNotBlank()) {
                appendLine(eval.diagnostico)
            }
            // Presbicia y otras condiciones
            val condiciones = buildList {
                if (eval.otrosPresbicia) add("Presbicia")
                if (eval.otrosAnisometropia) add("Anisometropía")
                if (eval.otrosAmbliopia) add("Ambliopía")
            }
            if (condiciones.isNotEmpty()) {
                appendLine("Condiciones: ${condiciones.joinToString(", ")}")
            }
        }
        if (diag.isNotBlank()) sectionWithBadge("Diagnóstico", diag.trimEnd())
        return this
    }

    /**
     * Dibuja "Prismas" con triángulo de base abajo centrado.
     */
    fun addPrismas(eval: EvaluacionClinica): RecetaPdfBuilder {
        val hasOd = eval.prismaOdValor.isNotBlank()
        val hasOi = eval.prismaOiValor.isNotBlank()
        if (!hasOd && !hasOi) return this

        val prismaText = buildString {
            if (hasOd) appendLine("OD: ${eval.prismaOdValor}Δ  base ${eval.prismaOdBase}")
            if (hasOi) appendLine("OI: ${eval.prismaOiValor}Δ  base ${eval.prismaOiBase}")
        }

        val innerW = contentWidth() - PdfStyle.SECTION_BAR_W.toInt() - 12
        val titleSl = layoutText("Prismas", PdfStyle.sectionPaint, innerW)
        val bodySl = layoutText(prismaText.trimEnd(), PdfStyle.bodyPaint, innerW)
        val contentH = titleSl.height + 8f + bodySl.height + 12f
        val blockH = contentH + 16f
        if (!spaceAvailable(blockH)) newPage()

        val blockTop = y
        // Section bar
        val barRect = RectF(
            PdfStyle.MARGIN, blockTop,
            PdfStyle.MARGIN + PdfStyle.SECTION_BAR_W, blockTop + contentH
        )
        canvas.drawRoundRect(barRect, 2f, 2f, PdfStyle.sectionBarPaint)

        drawStaticLayout(titleSl, PdfStyle.MARGIN + PdfStyle.SECTION_BAR_W + 12f, blockTop)
        drawStaticLayout(bodySl, PdfStyle.MARGIN + PdfStyle.SECTION_BAR_W + 12f, blockTop + titleSl.height + 8f)

        // Triángulo de base abajo centrado
        val triSize = 24f
        val triX = PdfStyle.PAGE_W - PdfStyle.MARGIN - triSize - 8f
        val triY = blockTop + 4f
        val triangle = Path().apply {
            moveTo(triX, triY)                          // vértice superior
            lineTo(triX + triSize / 2f, triY + triSize)  // base derecha
            lineTo(triX - triSize / 2f, triY + triSize)  // base izquierda
            close()
        }
        canvas.drawPath(triangle, PdfStyle.prismaTrianglePaint)

        advance(contentH + 18f)
        return this
    }

    /**
     * Dibuja "Seguimiento" con próxima cita (por defecto 1 año después).
     */
    fun addSeguimiento(eval: EvaluacionClinica): RecetaPdfBuilder {
        val proximaCitaStr = if (eval.proximaCita != null) {
            DateUtils.formatLocalized(eval.proximaCita!!)
        } else {
            // Por defecto: 1 año después de la evaluación
            val oneYearLater = eval.fecha.plusYears(1)
            DateUtils.formatLocalized(oneYearLater)
        }
        val block = buildString {
            append("Próxima cita sugerida: $proximaCitaStr")
        }
        sectionWithBadge("Seguimiento", block)
        return this
    }

    /**
     * Dibuja una sección genérica con badge de acento lateral.
     */
    fun sectionWithBadge(title: String, body: String) {
        if (body.isBlank()) return
        val innerW = contentWidth() - PdfStyle.SECTION_BAR_W.toInt() - 12
        val t = layoutText(title, PdfStyle.sectionPaint, innerW)
        val b = layoutText(body.trim(), PdfStyle.bodyPaint, innerW)
        val contentH = t.height + 8f + b.height
        val blockH = contentH + 16f
        if (!spaceAvailable(blockH)) newPage()

        val blockTop = y
        val barRect = RectF(
            PdfStyle.MARGIN, blockTop,
            PdfStyle.MARGIN + PdfStyle.SECTION_BAR_W, blockTop + contentH
        )
        canvas.drawRoundRect(barRect, 2f, 2f, PdfStyle.sectionBarPaint)

        drawStaticLayout(t, PdfStyle.MARGIN + PdfStyle.SECTION_BAR_W + 12f, blockTop)
        drawStaticLayout(b, PdfStyle.MARGIN + PdfStyle.SECTION_BAR_W + 12f, blockTop + t.height + 8f)
        advance(contentH + 18f)
    }

    /**
     * Finaliza el documento y retorna el [PdfDocument] con pie de página.
     */
    fun build(): PdfDocument {
        advance(8f)
        drawHorizontalRule()
        advance(14f)

        val footerText = "Documento generado con OptoApp · No sustituye la valoración clínica presencial ni la firma del profesional."
        val footerLayout = layoutText(
            footerText,
            PdfStyle.smallPaint,
            contentWidth(),
            Layout.Alignment.ALIGN_CENTER
        )
        val footerH = footerLayout.height + 8f
        if (!spaceAvailable(footerH)) newPage()
        drawStaticLayout(footerLayout, PdfStyle.MARGIN, y)
        advance(footerLayout.height.toFloat())

        finishPage()
        return doc
    }

    // ─── Internal helpers ───────────────────────────────────────────────────

    private fun initPage() {
        drawPageTopAccent()
        y = contentStartY()
    }

    private fun contentStartY(): Float = PdfStyle.ACCENT_BAR_H + 28f

    private fun drawPageTopAccent() {
        canvas.drawRect(0f, 0f, PdfStyle.PAGE_W.toFloat(), PdfStyle.ACCENT_BAR_H, PdfStyle.accentBarPaint)
    }

    private fun finishPage() {
        doc.finishPage(page)
    }

    private fun spaceAvailable(need: Float): Boolean =
        y + need <= PdfStyle.PAGE_H - PdfStyle.BOTTOM_SAFE

    private fun ensureSpace(need: Float) {
        if (!spaceAvailable(need)) newPage()
    }

    private fun newPage() {
        finishPage()
        page = doc.startPage(
            PdfDocument.PageInfo.Builder(PdfStyle.PAGE_W, PdfStyle.PAGE_H, ++pageNum).create()
        )
        canvas = page.canvas
        initPage()
    }

    private fun drawStaticLayout(sl: StaticLayout, x: Float, startY: Float) {
        canvas.withTranslation(x, startY) { sl.draw(this) }
    }

    private fun layoutText(
        text: String,
        paint: TextPaint,
        width: Int,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): StaticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(align)
        .setLineSpacing(0f, 1.22f)
        .setIncludePad(false)
        .build()

    private fun advance(yDelta: Float) {
        y += yDelta
    }

    private fun drawHorizontalRule() {
        canvas.drawLine(PdfStyle.MARGIN, y, PdfStyle.PAGE_W - PdfStyle.MARGIN, y, PdfStyle.rulePaint)
    }

    private fun contentWidth(): Int = (PdfStyle.PAGE_W - 2 * PdfStyle.MARGIN).toInt()
}
