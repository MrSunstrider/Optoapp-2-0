package com.example.optoapp.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint

// Separate from clinical PdfStyle so report exports can evolve without changing receta layout.
object ReportPdfTheme {
    const val PAGE_W = 595
    const val PAGE_H = 842
    const val MARGIN = 36f
    const val BOTTOM_SAFE = 44f
    const val HEADER_BAND_H = 56f
    const val KPI_RADIUS = 6f
    const val ROW_H = 17f
    const val TABLE_HEADER_H = 20f

    val COLOR_HEADER_BG: Int = 0xFF0F3D4C.toInt()
    val COLOR_HEADER_TEXT: Int = 0xFFFFFFFF.toInt()
    val COLOR_ACCENT: Int = 0xFF0D7377.toInt()
    val COLOR_TEXT: Int = 0xFF1A1F26.toInt()
    val COLOR_MUTED: Int = 0xFF5C6B7A.toInt()
    val COLOR_BORDER: Int = 0xFFD5DEE7.toInt()
    val COLOR_KPI_FILL: Int = 0xFFF0F7F8.toInt()
    val COLOR_TABLE_HEADER: Int = 0xFFE6F2F3.toInt()
    val COLOR_ZEBRA: Int = 0xFFF7FAFB.toInt()
    val COLOR_WARN: Int = 0xFFB45309.toInt()
    val COLOR_FOOTER_LINE: Int = 0xFFE0E7EE.toInt()

    val headerBandPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_HEADER_BG }
    }

    val accentPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_ACCENT }
    }

    val kpiFillPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_KPI_FILL
            style = Paint.Style.FILL
        }
    }

    val kpiStrokePaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
    }

    val tableHeaderFillPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_TABLE_HEADER }
    }

    val zebraFillPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_ZEBRA }
    }

    val rulePaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_FOOTER_LINE
            strokeWidth = 1f
        }
    }

    val headerTitlePaint: TextPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_HEADER_TEXT
            textSize = 16f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
    }

    val headerSubtitlePaint: TextPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 255, 255, 255)
            textSize = 10f
        }
    }

    val sectionPaint: TextPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ACCENT
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
    }

    val kpiLabelPaint: TextPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED
            textSize = 8.5f
        }
    }

    val kpiValuePaint: TextPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
    }

    val tableHeaderPaint: TextPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ACCENT
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
    }

    val bodyPaint: TextPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT
            textSize = 9.5f
        }
    }

    val bodyWarnPaint: TextPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WARN
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
    }

    val mutedPaint: TextPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED
            textSize = 9f
        }
    }

    val footerPaint: TextPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED
            textSize = 8f
        }
    }
}
