package it.togo.app.domain.time

import java.time.ZoneId

/**
 * Fornisce il fuso orario corrente del dispositivo.
 *
 * Le regole di dominio (ad esempio la label `Scaduto`) ricevono il fuso tramite
 * questa interfaccia, così i test possono fissarlo senza dipendere dalla
 * configurazione della macchina.
 */
interface ZoneIdProvider {

    /** Fuso orario attualmente in uso. */
    fun current(): ZoneId
}
