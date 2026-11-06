package it.togo.app.domain.action

import java.text.Normalizer
import java.util.Locale

/**
 * Normalizzazione dedicata al testo delle azioni "Cose da fare" (Epic 8).
 *
 * È separata da [it.togo.app.domain.catalog.ProductNameNormalizer] perché la
 * deduplica delle azioni deve ignorare maiuscole e spazi, ma **non** gli
 * accenti: "Caffè" e "Caffe" sono due azioni diverse.
 *
 * La forma normalizzata è:
 * - trim iniziale/finale;
 * - NFC per rendere equivalenti le forme composte e decomposte degli accenti;
 * - minuscole con [Locale.ROOT];
 * - spazi interni (inclusi tabulazioni e a capo) collassati a uno solo.
 *
 * La logica vive nel domain (AD-1): il data layer confronta solo la chiave già
 * normalizzata salvata nella colonna `testo_normalizzato` di `ACTION_ITEM`.
 */
object ActionTextNormalizer {

    private val WHITESPACE = Regex("\\s+")

    fun normalize(raw: String): String {
        val canonical = Normalizer.normalize(raw, Normalizer.Form.NFC)
        return WHITESPACE.replace(canonical.trim().lowercase(Locale.ROOT), " ").trim()
    }
}
