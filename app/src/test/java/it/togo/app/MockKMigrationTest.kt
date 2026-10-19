package it.togo.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test di analisi statica: verifica che nessun test importi più Mockito.
 *
 * Fix 10: Mockito → MockK — tutti i test devono usare io.mockk.*
 */
class MockKMigrationTest {

    private val testDir: File
        get() = File("app/src/test/java").let {
            if (it.exists()) it
            else File("src/test/java")
        }

    @Test
    fun `cartella test esiste`() {
        assertTrue(
            "Directory test non trovata in: ${testDir.absolutePath}",
            testDir.exists() && testDir.isDirectory
        )
    }

    @Test
    fun `nessun test importa Mockito`() {
        val testFiles = testDir.walk()
            .filter { it.extension == "kt" }
            .filter { it.name != "MockKMigrationTest.kt" } // questo test contiene la stringa cercata
            .toList()

        assertTrue("Nessun file .kt trovato in test/", testFiles.isNotEmpty())

        testFiles.forEach { file ->
            val content = file.readText()
            assertFalse(
                "${file.name} non deve importare Mockito",
                content.contains("import org.mockito")
            )
        }
    }

    @Test
    fun `tutti i test che fanno mock usano MockK`() {
        val testFiles = testDir.walk()
            .filter { it.extension == "kt" }
            .toList()

        val filesWithMock = testFiles.filter { file ->
            val content = file.readText()
            content.contains("mockk") || content.contains("io.mockk")
        }

        assertTrue(
            "Almeno alcuni test devono usare MockK (mockk/io.mockk)",
            filesWithMock.isNotEmpty()
        )
    }
}
