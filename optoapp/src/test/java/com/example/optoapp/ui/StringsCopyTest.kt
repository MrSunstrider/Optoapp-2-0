package com.example.optoapp.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class StringsCopyTest {

    private val copyFiles = listOf(
        MainSources.STRINGS_XML,
        MainSources.ANALISIS_NEGOCIO_SCREEN,
        MainSources.ANALISIS_DETALLE_SCREEN,
    )

    private val voseo = Regex("(vendés|Intentalo|tenés|podés|querés|hacé|mirá|cobrá)")

    private val hardcodedText = Regex("""Text\(\s*"""")

    private val emojiRanges = listOf(0x1F300..0x1FAFF, 0x2600..0x27BF, 0x1F000..0x1F2FF)

    @Test
    fun copyFiles_useTuToneInsteadOfVoseo() {
        copyFiles.forEach { path ->
            val matches = voseo.findAll(MainSources.read(path)).map { it.value }.toList()
            assertTrue("$path uses voseo: $matches", matches.isEmpty())
        }
    }

    @Test
    fun copyFiles_containNoEmoji() {
        copyFiles.forEach { path ->
            val emojis = MainSources.read(path).codePoints().toArray()
                .filter { codePoint -> emojiRanges.any { codePoint in it } }
                .map { String(Character.toChars(it)) }
            assertTrue("$path contains emoji: $emojis", emojis.isEmpty())
        }
    }

    @Test
    fun analisisScreens_haveNoHardcodedTextLiterals() {
        listOf(MainSources.ANALISIS_NEGOCIO_SCREEN, MainSources.ANALISIS_DETALLE_SCREEN).forEach { path ->
            val source = MainSources.read(path)
            val offendingLines = hardcodedText.findAll(source)
                .map { match -> source.substring(0, match.range.first).count { it == '\n' } + 1 }
                .toList()
            assertTrue("$path has hardcoded Text literals at lines $offendingLines", offendingLines.isEmpty())
        }
    }
}
