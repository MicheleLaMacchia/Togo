package it.togo.app.domain.time

import java.time.LocalDate
import java.time.ZoneId

/**
 * Sorgente del tempo del dominio.
 *
 * Vive in `domain/time` e non dipende da Android: le implementazioni di sistema
 * e i fake dei test possono essere iniettati senza trascinare il framework.
 */
interface Clock {

    /** Istante corrente in millisecondi dall'epoch Unix. */
    fun nowMillis(): Long

    /** Giorno corrente nel fuso orario indicato. */
    fun today(zoneId: ZoneId): LocalDate
}
