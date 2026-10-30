package it.togo.app.ui.theme

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test di analisi statica per la Dark Mode (Story 6.3).
 *
 * Impedisce regressioni: i componenti in scope devono leggere solo i token
 * TOGO (niente MaterialTheme.colorScheme, niente colori hardcoded) e la
 * variante `values-night` deve dichiarare un tema finestra scuro.
 */
class DarkModeTokenUsageTest {

    private val scopedComponents = listOf(
        "presentation/activelist/ItemRow.kt",
        "presentation/activelist/CategoryHeader.kt",
        "presentation/activelist/VoiceFab.kt",
        "presentation/activelist/EmptyState.kt",
        "presentation/activelist/ActiveListScreen.kt",
        "presentation/voice/VoiceBottomSheet.kt",
        "presentation/voice/VoiceConfirmationSheet.kt",
        "presentation/additem/AddItemBottomSheet.kt",
        "presentation/additem/DuplicateProductWarningDialog.kt",
        "presentation/additem/DuplicateLevel3WarningDialog.kt",
        "presentation/additem/NewProductForm.kt",
        "presentation/additem/DuplicateResolutionDialog.kt",
        "presentation/additem/ProductCategorizationWizard.kt",
        "presentation/additem/QuantityUnitPicker.kt",
        "presentation/rules/LearnedRulesScreen.kt",
        "presentation/history/HistoryScreen.kt",
        "presentation/search/SearchResultItem.kt",
    )

    @Test
    fun `scoped components do not read MaterialTheme colorScheme`() {
        scopedComponents.forEach { relative ->
            val source = mainSource(relative).readText()
            assertFalse(
                "$relative non deve usare MaterialTheme.colorScheme: la dark mode passa dai token TOGO",
                source.contains("MaterialTheme.colorScheme"),
            )
        }
    }

    @Test
    fun `presentation layer does not hardcode white black or hex colors`() {
        // Allowlist volutamente vuota: i colori fissi necessari (backdrop
        // dell'overlay vocale, testo su verde/ambra condivisi) vivono nei token
        // TogoColorTokens, non nei componenti.
        val files = presentationFiles()
        assertTrue("Nessun sorgente trovato sotto presentation/", files.isNotEmpty())

        files.forEach { file ->
            val source = stripCommentsAndStrings(file.readText())
            assertFalse(
                "${file.name} non deve usare Color.White",
                source.contains("Color.White"),
            )
            assertFalse(
                "${file.name} non deve usare Color.Black",
                source.contains("Color.Black"),
            )
            assertFalse(
                "${file.name} non deve usare Color(0x...)",
                source.contains("Color(0x"),
            )
            assertFalse(
                "${file.name} non deve usare android.graphics.Color",
                source.contains("android.graphics.Color"),
            )
        }
    }

    @Test
    fun `night theme declares Theme Togo with a dark parent`() {
        val nightTheme = appFile("src/main/res/values-night/themes.xml")
        assertTrue("values-night/themes.xml deve esistere", nightTheme.exists())

        val source = nightTheme.readText()
        assertTrue(
            "values-night/themes.xml deve dichiarare Theme.Togo",
            source.contains("name=\"Theme.Togo\""),
        )
        assertTrue(
            "Theme.Togo dark deve ereditare da android:Theme.Material.NoActionBar",
            source.contains("parent=\"android:Theme.Material.NoActionBar\""),
        )
        assertFalse(
            "Theme.Togo dark non deve ereditare dal parent Light",
            source.contains("android:Theme.Material.Light.NoActionBar"),
        )
        assertTrue(
            "Theme.Togo dark deve impostare android:windowBackground",
            source.contains("android:windowBackground"),
        )
    }

    @Test
    fun `light theme keeps the light parent and a light window background`() {
        val lightTheme = appFile("src/main/res/values/themes.xml")
        assertTrue("values/themes.xml deve esistere", lightTheme.exists())

        val source = lightTheme.readText()
        assertTrue(
            "Theme.Togo light deve ereditare da android:Theme.Material.Light.NoActionBar",
            source.contains("parent=\"android:Theme.Material.Light.NoActionBar\""),
        )
        assertTrue(
            "Theme.Togo light deve impostare android:windowBackground",
            source.contains("android:windowBackground"),
        )
    }

    @Test
    fun `window background colors match the light and dark base surfaces`() {
        val lightColors = appFile("src/main/res/values/colors.xml").readText()
        val darkColors = appFile("src/main/res/values-night/colors.xml").readText()

        assertTrue(
            "values/colors.xml deve definire togo_window_background = #FFFFFF",
            lightColors.contains("togo_window_background") && lightColors.contains("#FFFFFFFF"),
        )
        assertTrue(
            "values-night/colors.xml deve definire togo_window_background = #0B0F17",
            darkColors.contains("togo_window_background") && darkColors.contains("#FF0B0F17"),
        )
    }

    @Test
    fun `both themes declare the same system bar attributes`() {
        val light = appFile("src/main/res/values/themes.xml").readText()
        val night = appFile("src/main/res/values-night/themes.xml").readText()
        val itemName = Regex("<item name=\"(android:[^\"]+)\"")
        val lightAttrs = itemName.findAll(light).map { it.groupValues[1] }.toSet()
        val nightAttrs = itemName.findAll(night).map { it.groupValues[1] }.toSet()

        assertTrue(
            "values/themes.xml deve impostare android:statusBarColor",
            lightAttrs.contains("android:statusBarColor"),
        )
        assertTrue(
            "values-night/themes.xml deve impostare android:statusBarColor",
            nightAttrs.contains("android:statusBarColor"),
        )
        assertTrue(
            "statusBarColor deve usare @color/togo_window_background (light)",
            light.contains("android:statusBarColor\">@color/togo_window_background</item>"),
        )
        assertTrue(
            "statusBarColor deve usare @color/togo_window_background (night)",
            night.contains("android:statusBarColor\">@color/togo_window_background</item>"),
        )
        assertTrue(
            "Le varianti light e night devono dichiarare gli stessi attributi: " +
                "light=$lightAttrs night=$nightAttrs",
            lightAttrs == nightAttrs,
        )
    }

    @Test
    fun `components in scope use the shared semantic tokens`() {
        val addItem = mainSource("presentation/additem/AddItemBottomSheet.kt").readText()
        assertTrue(
            "AddItemBottomSheet deve usare il token condiviso per il testo del pulsante verde",
            addItem.contains("tokens.bottomSheet.checkoutButtonContent"),
        )
        assertTrue(
            "AddItemBottomSheet deve dichiarare i colori disabilitati espliciti",
            addItem.contains("disabledContainerColor") && addItem.contains("disabledContentColor"),
        )

        val search = mainSource("presentation/search/SearchResultItem.kt").readText()
        assertTrue(
            "SearchResultItem deve passare accentHighlight come sfondo dell'evidenziazione",
            search.contains("highlightBackground = colors.accentHighlight"),
        )
        assertTrue(
            "SearchResultItem deve passare inkInverse come testo dell'evidenziazione",
            search.contains("highlightForeground = colors.inkInverse"),
        )

        val activeList = mainSource("presentation/activelist/ActiveListScreen.kt").readText()
        assertTrue(
            "ActiveListScreen deve usare i token snackbar",
            activeList.contains("tokens.snackbar.container") &&
                activeList.contains("tokens.snackbar.content") &&
                activeList.contains("tokens.snackbar.action"),
        )

        val dialog = mainSource("presentation/additem/DuplicateResolutionDialog.kt").readText()
        assertTrue(
            "DuplicateResolutionDialog deve usare i token del dialog",
            dialog.contains("tokens.duplicateDialog.background") &&
                dialog.contains("tokens.duplicateDialog.borderColor"),
        )
    }

    @Test
    fun `status and error texts use the readable ink tokens`() {
        val rules = mainSource("presentation/rules/LearnedRulesScreen.kt").readText()
        assertTrue(
            "LearnedRulesScreen deve usare successInk/warningInk per lo stato",
            rules.contains("colors.successInk") && rules.contains("colors.warningInk"),
        )

        val warningFiles = listOf(
            "presentation/additem/ProductCategorizationWizard.kt",
            "presentation/additem/QuantityUnitPicker.kt",
            "presentation/voice/VoiceBottomSheet.kt",
            "presentation/voice/VoiceConfirmationSheet.kt",
            "presentation/activelist/ActiveListScreen.kt",
        )
        warningFiles.forEach { relative ->
            assertTrue(
                "$relative deve usare warningInk per il testo di warning/errore",
                mainSource(relative).readText().contains("warningInk"),
            )
        }
    }

    private fun stripCommentsAndStrings(source: String): String =
        source
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("//.*"), "")
            .replace(Regex("\"(?:\\\\.|[^\"\\\\])*\""), "\"\"")

    private fun mainSource(relativeToPresentation: String): File =
        appFile("src/main/java/it/togo/app/$relativeToPresentation")

    private fun presentationFiles(): List<File> =
        appFile("src/main/java/it/togo/app/presentation")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

    private fun appFile(relativeToApp: String): File {
        val candidates = listOf(
            File(relativeToApp),
            File("app/$relativeToApp"),
            File("../$relativeToApp"),
            File("../app/$relativeToApp"),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error(
                "File non trovato: $relativeToApp (cercato in: " +
                    candidates.joinToString { it.absolutePath } + ")"
            )
    }
}
