package com.example.optoapp.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Characterization tests for [RxGridRow] sealed class extracted to
 * RecetaRefraccionTable.kt.
 */
class RecetaRefraccionTableTest {

    @Test
    fun rxGridRow_head_constructs() {
        val head = RxGridRow.Head("Medida", "OD", "OI")
        assertEquals("Medida", head.l)
        assertEquals("OD", head.od)
        assertEquals("OI", head.oi)
    }

    @Test
    fun rxGridRow_three_constructs() {
        val three = RxGridRow.Three(
            "Fórmula (lejos)",
            "-1.00 / -0.50 × 90°",
            "-2.00 / -1.00 × 75°"
        )
        assertEquals("Fórmula (lejos)", three.label)
        assertEquals("-1.00 / -0.50 × 90°", three.od)
        assertEquals("-2.00 / -1.00 × 75°", three.oi)
    }

    @Test
    fun rxGridRow_merged_constructs() {
        val merged = RxGridRow.Merged("AV CC AO", "0.8")
        assertEquals("AV CC AO", merged.label)
        assertEquals("0.8", merged.value)
    }

    @Test
    fun rxGridRow_sealedClassExhaustive_whenHead() {
        val row: RxGridRow = RxGridRow.Head("M", "OD", "OI")
        val desc = when (row) {
            is RxGridRow.Head -> "head:${row.l}"
            is RxGridRow.Three -> "three:${row.label}"
            is RxGridRow.Merged -> "merged:${row.label}"
        }
        assertEquals("head:M", desc)
    }

    @Test
    fun rxGridRow_sealedClassExhaustive_whenThree() {
        val row: RxGridRow = RxGridRow.Three("X", "a", "b")
        val desc = when (row) {
            is RxGridRow.Head -> "head"
            is RxGridRow.Three -> "three:${row.od}/${row.oi}"
            is RxGridRow.Merged -> "merged"
        }
        assertEquals("three:a/b", desc)
    }

    @Test
    fun rxGridRow_sealedClassExhaustive_whenMerged() {
        val row: RxGridRow = RxGridRow.Merged("Y", "val")
        val desc = when (row) {
            is RxGridRow.Head -> "head"
            is RxGridRow.Three -> "three"
            is RxGridRow.Merged -> "merged:${row.value}"
        }
        assertEquals("merged:val", desc)
    }
}
