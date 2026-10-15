package com.example.optoapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerSectionsCopyTest {

    private val source = MainSources.read("java/com/example/optoapp/ui/screens/DrawerSections.kt")

    @Test
    fun `drawer has no hardcoded user-facing copy`() {
        val literalCopy = Regex("""(Text\(\s*"|text\s*=\s*"|contentDescription\s*=\s*"|ifBlank\s*\{\s*"|else\s+")""")
        val offending = source.lines().filter { literalCopy.containsMatchIn(it) }
        assertEquals("Hardcoded copy in DrawerSections.kt: $offending", emptyList<String>(), offending)
    }

    @Test
    fun `every drawer string resource it references exists`() {
        val referenced = Regex("""R\.string\.(\w+)""").findAll(source).map { it.groupValues[1] }.toSet()
        assertTrue("DrawerSections.kt should reference string resources", referenced.isNotEmpty())
        referenced.forEach { name -> assertNotNull("Missing string resource $name", MainSources.stringValue(name)) }
    }
}
