package com.example.optoapp.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.withTranslation
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Paciente

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
        PdfDocument.PageInfo.Builder(PdfStyle.PAGE_W, PdfStyle.PAGE_H, ++pageNum).create(),
    )
    private var canvas: Canvas = page.canvas
    private var y = 0f

    /**
     * Dibuja la cabecera del PDF: título centrado + nombre comercial + tarjeta de paciente.
     */
    fun addHeader(paciente: Paciente, eval: EvaluacionClinica, opticaNombre: String = ""): RecetaPdfBuilder {
        initPage()

        val title = layoutText("FÓRMULA OPTOMÉTRICA", PdfStyle.titlePaint, contentWidth(), Layout.Alignment.ALIGN_CENTER)
        drawStaticLayout(title, PdfStyle.MARGIN, y)
        advance(title.height + 6f)

        // Nombre comercial de la óptica (más grande y negrita)
        if (opticaNombre.isNotBlank()) {
            val opticaPaint = TextPaint(PdfStyle.subtitlePaint).apply {
                textSize = 12f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            }
            val opticaLabel = layoutText(opticaNombre, opticaPaint, contentWidth(), Layout.Alignment.ALIGN_CENTER)
            drawStaticLayout(opticaLabel, PdfStyle.MARGIN, y)
            advance(opticaLabel.height + 8f)
        } else {
            advance(6f)
        }

        drawHorizontalRule()
        advance(14f)

        // Patient info card
        val innerW = contentWidth() - (2 * PdfStyle.CARD_PAD).toInt()
        val leftX = PdfStyle.MARGIN + PdfStyle.CARD_PAD
        val rightEdge = PdfStyle.PAGE_W - PdfStyle.MARGIN - PdfStyle.CARD_PAD
        val cardWidth = rightEdge - leftX
        val midX = leftX + cardWidth / 2

        val pacienteTitlePaint = TextPaint(PdfStyle.sectionPaint).apply { textSize = 13f }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfStyle.COLOR_TEXT
            textSize = 10.5f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        }
        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PdfStyle.COLOR_TEXT
            textSize = 10.5f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
        }

        // Título "Paciente" centrado
        val titleLabel = layoutText("Paciente", pacienteTitlePaint, innerW, Layout.Alignment.ALIGN_CENTER)
        val titleX = leftX + (cardWidth - titleLabel.width) / 2

        // Calcular la altura del contenido: cada fila = textSize * 1.5
        val rowH = (labelPaint.textSize * 1.3f)
        val fieldCount = 4
        val contentH = fieldCount * rowH
        val cardH = PdfStyle.CARD_PAD + titleLabel.height + 6f + contentH + PdfStyle.CARD_PAD

        ensureSpace(cardH + 16f)

        val cardRect = RectF(PdfStyle.MARGIN, y, PdfStyle.PAGE_W - PdfStyle.MARGIN, y + cardH)
        canvas.drawRoundRect(cardRect, PdfStyle.CARD_RADIUS, PdfStyle.CARD_RADIUS, PdfStyle.cardFillPaint)
        canvas.drawRoundRect(cardRect, PdfStyle.CARD_RADIUS, PdfStyle.CARD_RADIUS, PdfStyle.cardStrokePaint)

        // Título "Paciente" centrado dentro de la tarjeta
        drawStaticLayout(titleLabel, titleX, y + PdfStyle.CARD_PAD)
        val contentY = y + PdfStyle.CARD_PAD + titleLabel.height + 8f

        // Calcular el ancho máximo de las etiquetas para alinear los valores
        val leftLabels = listOf("Nombre: ", "HO: ", "Celular: ", "Evaluación: ")
        val rightLabels = listOf("Edad: ", "DNI: ", "Distrito: ")
        val maxLeftW = leftLabels.maxOf { labelPaint.measureText(it) }
        val maxRightW = rightLabels.maxOf { labelPaint.measureText(it) }
        val gap = 6f

        // Helper: dibuja label + value con alineación tabular
        fun drawFieldRow(yPos: Float, leftLabel: String, leftValue: String, rightLabel: String, rightValue: String) {
            // Columna izquierda
            canvas.drawText(leftLabel, leftX, yPos + labelPaint.textSize, labelPaint)
            canvas.drawText(leftValue, leftX + maxLeftW + gap, yPos + labelPaint.textSize, valuePaint)
            // Columna derecha
            canvas.drawText(rightLabel, midX, yPos + labelPaint.textSize, labelPaint)
            canvas.drawText(rightValue, midX + maxRightW + gap, yPos + labelPaint.textSize, valuePaint)
        }

        // Fila 1: Nombre + Edad
        drawFieldRow(contentY, "Nombre: ", paciente.nombreCompleto, "Edad: ", "${paciente.edad} años")

        // Fila 2: HO + DNI
        val hoVal = paciente.historiaOptometrica?.ifBlank { null } ?: "—"
        val dniVal = paciente.dni?.ifBlank { null } ?: "—"
        drawFieldRow(contentY + rowH, "HO: ", hoVal, "DNI: ", dniVal)

        // Fila 3: Celular + Distrito
        val celVal = paciente.telefono.ifBlank { "—" }
        val disVal = paciente.distrito?.ifBlank { null } ?: "—"
        drawFieldRow(contentY + rowH * 2, "Celular: ", celVal, "Distrito: ", disVal)

        // Fila 4: Evaluación
        val evalFecha = DateUtils.formatLocalized(eval.fecha)
        canvas.drawText("Evaluación: ", leftX, contentY + rowH * 3 + labelPaint.textSize, labelPaint)
        canvas.drawText(evalFecha, leftX + maxLeftW + gap, contentY + rowH * 3 + labelPaint.textSize, valuePaint)

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
            advance = { d -> advance(d) },
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
            if (dOd.isNotBlank()) appendLine("OD: $dOd")
            if (dOi.isNotBlank()) appendLine("OI: $dOi")
            if (eval.diagnostico.isNotBlank()) appendLine(eval.diagnostico)
            if (eval.otrosPresbicia) appendLine("Presbicia")
            if (eval.otrosAnisometropia) appendLine("Anisometropía")
            if (eval.otrosAmbliopia) appendLine("Ambliopía")
        }
        if (diag.isNotBlank()) sectionWithBadge("Diagnóstico", diag.trimEnd())
        sectionWithBadge("Tratamiento", "Uso de lentes correctores.")
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
            PdfStyle.MARGIN,
            blockTop,
            PdfStyle.MARGIN + PdfStyle.SECTION_BAR_W,
            blockTop + contentH,
        )
        canvas.drawRoundRect(barRect, 2f, 2f, PdfStyle.sectionBarPaint)

        drawStaticLayout(titleSl, PdfStyle.MARGIN + PdfStyle.SECTION_BAR_W + 12f, blockTop)
        drawStaticLayout(bodySl, PdfStyle.MARGIN + PdfStyle.SECTION_BAR_W + 12f, blockTop + titleSl.height + 8f)

        // Triángulo de base abajo centrado
        val triSize = 24f
        val triX = PdfStyle.PAGE_W - PdfStyle.MARGIN - triSize - 8f
        val triY = blockTop + 4f
        val triangle = Path().apply {
            moveTo(triX, triY) // vértice superior
            lineTo(triX + triSize / 2f, triY + triSize) // base derecha
            lineTo(triX - triSize / 2f, triY + triSize) // base izquierda
            close()
        }
        canvas.drawPath(triangle, PdfStyle.prismaTrianglePaint)

        advance(contentH + 12f)
        return this
    }

    /**
     * Dibuja "Seguimiento" con próxima cita (por defecto 1 año después).
     */
    fun addSeguimiento(eval: EvaluacionClinica): RecetaPdfBuilder {
        val proximaCitaStr = eval.proximaCita?.let { DateUtils.formatLocalized(it) }
            ?: DateUtils.formatLocalized(eval.fecha.plusYears(1))
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
        val b = layoutText(body.trim(), PdfStyle.bodyBoldPaint, innerW)
        val contentH = t.height + 8f + b.height
        val blockH = contentH + 16f
        if (!spaceAvailable(blockH)) newPage()

        val blockTop = y
        val barRect = RectF(
            PdfStyle.MARGIN,
            blockTop,
            PdfStyle.MARGIN + PdfStyle.SECTION_BAR_W,
            blockTop + contentH,
        )
        canvas.drawRoundRect(barRect, 2f, 2f, PdfStyle.sectionBarPaint)

        drawStaticLayout(t, PdfStyle.MARGIN + PdfStyle.SECTION_BAR_W + 12f, blockTop)
        drawStaticLayout(b, PdfStyle.MARGIN + PdfStyle.SECTION_BAR_W + 12f, blockTop + t.height + 8f)
        advance(contentH + 12f)
    }

    /**
     * Finaliza el documento y retorna el [PdfDocument] con pie de página.
     */
    fun build(): PdfDocument {
        advance(4f)
        drawHorizontalRule()
        advance(8f)

        val footerText = "Documento generado con OptoApp · No sustituye la valoración clínica presencial ni la firma del profesional."
        val footerLayout = layoutText(
            footerText,
            PdfStyle.smallPaint,
            contentWidth(),
            Layout.Alignment.ALIGN_CENTER,
        )
        val footerH = footerLayout.height + 8f
        if (!spaceAvailable(footerH)) newPage()
        drawStaticLayout(footerLayout, PdfStyle.MARGIN, y)
        advance(footerLayout.height.toFloat())

        finishPage()
        return doc
    }

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

    private fun spaceAvailable(need: Float): Boolean = y + need <= PdfStyle.PAGE_H - PdfStyle.BOTTOM_SAFE

    private fun ensureSpace(need: Float) {
        if (!spaceAvailable(need)) newPage()
    }

    private fun newPage() {
        finishPage()
        page = doc.startPage(
            PdfDocument.PageInfo.Builder(PdfStyle.PAGE_W, PdfStyle.PAGE_H, ++pageNum).create(),
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
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
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
