package com.example.optoapp.ui

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object MainSources {
    const val STRINGS_XML = "res/values/strings.xml"
    const val ANALISIS_NEGOCIO_SCREEN = "java/com/example/optoapp/ui/screens/AnalisisNegocioScreen.kt"
    const val ANALISIS_DETALLE_SCREEN = "java/com/example/optoapp/ui/screens/AnalisisDetalleScreen.kt"

    // Gradle runs unit tests with the module directory as cwd, while IDE runners may use the repo root.
    fun file(relative: String): File =
        listOf(File("src/main", relative), File("optoapp/src/main", relative))
            .firstOrNull { it.isFile }
            ?: error("Main source file not found: $relative")

    fun read(relative: String): String = file(relative).readText(Charsets.UTF_8)

    fun stringValue(name: String): String? {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file(STRINGS_XML))
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .firstOrNull { it.getAttribute("name") == name }
            ?.textContent
    }
}
