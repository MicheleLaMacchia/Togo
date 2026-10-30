package it.togo.app.privacy

import android.Manifest
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * Audit privacy statico della Story 6.4.
 *
 * Non registra audio e non esegue il flusso vocale: legge manifest e sorgenti
 * per dimostrare che il modulo voce non usa API audio/file/log/rete e che il
 * testo riconosciuto non viene passato a repository o DAO.
 *
 * Allowlist esplicita: `LEARNED_RULE.userExpression` è l'unica persistenza
 * testuale ammessa. Non è un log dell'utterance completa: salva l'espressione
 * normalizzata che l'utente ha confermato per ricordare una categoria (Epic 5).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoicePrivacyAuditTest {

    @Before
    fun setup() {
        stopKoin()
    }

    private val requestedPermissions: Set<String> by lazy {
        val context = RuntimeEnvironment.getApplication()
        context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            ?.toSet()
            .orEmpty()
    }

    /**
     * Permesso app-scoped aggiunto da AndroidX core per i receiver dinamici
     * non esportati. Non è un permesso di piattaforma, non dà accesso a rete,
     * audio o storage: va allowlistato esplicitamente, non ignorato.
     */
    private val dynamicReceiverPermission: String by lazy {
        "${RuntimeEnvironment.getApplication().packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
    }

    private val voiceSourcePaths = listOf(
        "app/src/main/java/it/togo/app/data/voice",
        "app/src/main/java/it/togo/app/presentation/voice",
        "app/src/main/java/it/togo/app/domain/voice",
        "app/src/main/java/it/togo/app/domain/parser",
        "app/src/main/java/it/togo/app/domain/usecase/ParseVoiceCommandUseCase.kt",
        "app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt",
    )

    private fun resolveProjectFile(relativePath: String): File {
        val candidates = listOf(
            File(relativePath),
            File("..", relativePath),
            File("../..", relativePath),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("sorgente non trovato: $relativePath")
    }

    private fun voiceSourceFiles(): List<File> {
        val files = voiceSourcePaths.flatMap { relative ->
            val file = resolveProjectFile(relative)
            if (file.isDirectory) {
                file.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .toList()
            } else {
                listOf(file)
            }
        }
        check(files.isNotEmpty()) { "nessun sorgente voce trovato" }
        return files
    }

    @Test
    fun `manifest has no network permissions`() {
        assertTrue("il manifest deve dichiarare almeno un permesso", requestedPermissions.isNotEmpty())
        assertFalse(
            "INTERNET non deve essere dichiarato: l'app non invia testo a endpoint remoti",
            "android.permission.INTERNET" in requestedPermissions,
        )
        assertFalse(
            "ACCESS_NETWORK_STATE non deve essere dichiarato",
            "android.permission.ACCESS_NETWORK_STATE" in requestedPermissions,
        )
    }

    @Test
    fun `manifest declares only vibrate record audio and app dynamic receiver permission`() {
        val expected = setOf(
            Manifest.permission.VIBRATE,
            Manifest.permission.RECORD_AUDIO,
            dynamicReceiverPermission,
        )
        assertEquals(
            "permessi inattesi nel manifest, trovati: $requestedPermissions",
            expected,
            requestedPermissions,
        )
    }

    @Test
    fun `voice code contains no audio file or log APIs`() {
        val files = voiceSourceFiles()
        assertTrue("il modulo voce deve contenere sorgenti", files.size >= 10)
        // `TogoLog` è il facade applicativo usato dalla UI per eventi non
        // vocali (es. tap sulla TopBar). Il divieto resta per le API di log
        // dirette: le chiamate TogoLog non devono ricevere testo vocale.
        val forbidden = Regex(
            """MediaRecorder|AudioRecord|FileOutputStream|FileWriter|writeText|writeBytes|""" +
                """openOutputStream|createTempFile|SharedPreferences|DataOutputStream|PrintWriter|""" +
                """Timber|\.amr|\.wav|\.pcm|(?<!Togo)Log\.|println\("""
        )
        val violations = files.mapNotNull { file ->
            forbidden.find(file.readText())?.let { "${file.path}: ${it.value}" }
        }
        assertTrue("API audio/file/log vietate nel modulo voce: $violations", violations.isEmpty())
    }

    @Test
    fun `voice code does not pass recognized text to repositories`() {
        val forbiddenRepositories = listOf(
            "ListItemRepository",
            "CatalogRepository",
        )
        val violations = voiceSourceFiles().flatMap { file ->
            val text = file.readText()
            forbiddenRepositories.filter { text.contains(it) }.map { "${file.path}: $it" }
        }
        assertTrue(
            "il testo vocale non deve raggiungere repository/DAO: $violations",
            violations.isEmpty(),
        )
    }

    @Test
    fun `voice code has no network imports`() {
        val networkMarkers = listOf(
            "java.net",
            "okhttp",
            "HttpURLConnection",
            "Retrofit",
            "URLConnection",
            "HttpClient",
            "Socket",
        )
        val violations = voiceSourceFiles().flatMap { file ->
            val text = file.readText()
            networkMarkers.filter { text.contains(it) }.map { "${file.path}: $it" }
        }
        assertTrue("import di rete vietati nel modulo voce: $violations", violations.isEmpty())
    }

    @Test
    fun `recognized text is not saved across configuration changes`() {
        val markers = listOf("rememberSaveable", "SavedStateHandle", "onSaveInstanceState")
        val violations = voiceSourceFiles().flatMap { file ->
            val text = file.readText()
            markers.filter { text.contains(it) }.map { "${file.path}: $it" }
        }
        assertTrue(
            "il testo riconosciuto non deve essere persistito nello stato: $violations",
            violations.isEmpty(),
        )
    }

    @Test
    fun `learned rule expression is the documented persistence exception`() {
        val entity = resolveProjectFile(
            "app/src/main/java/it/togo/app/data/database/entity/LearnedRuleEntity.kt"
        )
        val entitySource = entity.readText()
        assertTrue(
            "la persistenza ammessa è LEARNED_RULE.userExpression",
            entitySource.contains("tableName = \"LEARNED_RULE\"") &&
                entitySource.contains("val userExpression: String"),
        )

        val voiceUsesRules = voiceSourceFiles().filter {
            it.readText().contains("LEARNED_RULE") ||
                it.readText().contains("LearnedRulesRepository")
        }
        assertTrue(
            "il modulo voce non deve leggere/scrivere direttamente LEARNED_RULE: $voiceUsesRules",
            voiceUsesRules.isEmpty(),
        )
    }
}
