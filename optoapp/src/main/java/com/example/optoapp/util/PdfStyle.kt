package com.example.optoapp.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint

object PdfStyle {

    // ─── Layout dimensions ──────────────────────────────────────────────────

    const val PAGE_W = 595
    const val PAGE_H = 842
    const val MARGIN = 48f
    const val BOTTOM_SAFE = 56f
    const val TABLE_CELL_PAD = 8f
    const val ACCENT_BAR_H = 5f
    const val CARD_RADIUS = 10f
    const val CARD_PAD = 14f
    const val SECTION_BAR_W = 4f

    // ─── Colors (raw ARGB ints, no Android dependency) ──────────────────────

    val COLOR_ACCENT: Int = 0xFF1E5B8C.toInt()
    val COLOR_ACCENT_LIGHT: Int = 0xFFE8F0F8.toInt()
    val COLOR_TEXT: Int = 0xFF1C1C1E.toInt()
    val COLOR_TEXT_MUTED: Int = 0xFF585C66.toInt()
    val COLOR_BORDER: Int = 0xFFD2DAE4.toInt()
    val COLOR_CARD_FILL: Int = 0xFFFCFDFF.toInt()
    val COLOR_RULE: Int = 0xFFE6EBF2.toInt()

    // ─── Paint presets ──────────────────────────────────────────────────────

    val accentBarPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_ACCENT }
    }

    val cardFillPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_CARD_FILL
            style = Paint.Style.FILL
        }
    }

    val cardStrokePaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
    }

    val headerStripPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_ACCENT_LIGHT }
    }

    val rulePaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_RULE
            strokeWidth = 1f
        }
    }

    val sectionBarPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_ACCENT }
    }

    val gridStrokePaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
    }

    val altRowPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(30, 30, 91, 140) }
    }

    // ─── TextPaint presets ──────────────────────────────────────────────────

    val titlePaint: TextPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
    }

    val subtitlePaint: TextPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 10f
        }
    }

    val sectionPaint: TextPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ACCENT
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
    }

    val bodyBoldPaint: TextPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
    }

    val bodyPaint: TextPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
    }

    val labelPaint: TextPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
    }

    val rxValuePaint: TextPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
    }

    val tableHeaderPaint: TextPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ACCENT
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
    }

    val prismaTrianglePaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ACCENT
            style = Paint.Style.FILL
            alpha = 60
        }
    }

    val smallPaint: TextPaint by lazy {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 8.5f
        }
    }
}
