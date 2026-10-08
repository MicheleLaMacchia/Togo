package it.togo.app.domain

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DomainArchitectureTest {

    @Test
    fun `domain package has no android imports`() {
        val domainDir = File("src/main/java/it/togo/app/domain")
        assertTrue("Package domain non esiste: ${domainDir.absolutePath}", domainDir.exists())

        val kotlinFiles = domainDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

        val filesWithAndroidImports = kotlinFiles.filter { file ->
            file.readLines().any { line ->
                line.trimStart().startsWith("import android.") ||
                line.trimStart().startsWith("import androidx.")
            }
        }

        assertTrue(
            "I file nel package domain non devono importare android.* o androidx.*. " +
            "Violazioni: ${filesWithAndroidImports.map { it.name }}",
            filesWithAndroidImports.isEmpty()
        )
    }
}
