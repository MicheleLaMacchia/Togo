package it.togo.app.domain.model

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Unità di misura standard con supporto a conversioni e normalizzazione.
 *
 * - Unità base: GRAM (peso), MILLILITER (volume)
 * - Conversioni solo tra unità compatibili (stessa dimensione fisica)
 * - Quantità valide: > 0, finite, non NaN
 * - Formattazione intelligente: sceglie l'unità più leggibile (es. 1500 g → "1.5 kg")
 */
enum class StandardUnit(val code: String, val displayName: String) {
    GRAM("G", "g"),
    KILOGRAM("KG", "kg"),
    MILLILITER("ML", "ml"),
    LITER("L", "L"),
    PIECE("PC", "pz"),
    PACKAGE("PK", "conf"),
    BOTTLE("BT", "bt"),
    JAR("JR", "vas"),
    CAN("CN", "latt"),
    BOX("BX", "scat"),
    ROLL("RL", "rot"),
    POT("PT", "pent");

    /** Unità base per la dimensione fisica (GRAM per peso, MILLILITER per volume) */
    val baseUnit: StandardUnit
        get() = when (this) {
            GRAM, KILOGRAM -> GRAM
            MILLILITER, LITER -> MILLILITER
            else -> this // Unità non convertibili: la base è se stessa
        }

    /** Fattore di conversione verso l'unità base (es. KILOGRAM → 1000.0 GRAM) */
    val factorToBase: Double
        get() = when (this) {
            GRAM -> 1.0
            KILOGRAM -> 1000.0
            MILLILITER -> 1.0
            LITER -> 1000.0
            else -> 1.0 // Unità non convertibili
        }

    /** Unità compatibili per conversione (stessa dimensione fisica) */
    val compatibleUnits: List<StandardUnit>
        get() = when (this) {
            GRAM, KILOGRAM -> listOf(GRAM, KILOGRAM)
            MILLILITER, LITER -> listOf(MILLILITER, LITER)
            PIECE, PACKAGE, BOTTLE, JAR, CAN, BOX, ROLL, POT -> listOf(this)
        }

    /**
     * Converte una quantità da questa unità a un'altra.
     * Ritorna null se le unità sono incompatibili o la quantità non è valida.
     */
    fun convert(quantity: Double, to: StandardUnit): Double? {
        if (!isValidQuantity(quantity)) return null
        if (!isCompatibleWith(to)) return null
        return quantity * this.factorToBase / to.factorToBase
    }

    /**
     * Normalizza la quantità all'unità base della dimensione fisica.
     * Ritorna Pair(quantità normalizzata, unità base) o null se non valida.
     */
    fun normalize(quantity: Double): Pair<Double, StandardUnit>? {
        if (!isValidQuantity(quantity)) return null
        return Pair(quantity * this.factorToBase, this.baseUnit)
    }

    /**
     * Formatta la quantità scegliendo l'unità più leggibile.
     * Es: 1500 g → "1.5 kg", 500 g → "500 g", 2500 ml → "2.5 L"
     */
    fun format(quantity: Double): String {
        if (!isValidQuantity(quantity)) return "0 ${this.displayName}"

        return when (this) {
            GRAM -> {
                if (quantity >= 1000) {
                    val kg = roundToDecimals(quantity / 1000.0, 2)
                    "$kg kg"
                } else {
                    val g = roundToDecimals(quantity, 2)
                    "$g g"
                }
            }
            KILOGRAM -> {
                val kg = roundToDecimals(quantity, 2)
                "$kg kg"
            }
            MILLILITER -> {
                if (quantity >= 1000) {
                    val l = roundToDecimals(quantity / 1000.0, 2)
                    "$l L"
                } else {
                    val ml = roundToDecimals(quantity, 2)
                    "$ml ml"
                }
            }
            LITER -> {
                val l = roundToDecimals(quantity, 2)
                "$l L"
            }
            else -> {
                val v = roundToDecimals(quantity, 2)
                "$v ${this.displayName}"
            }
        }
    }

    /** Verifica se una quantità è valida (> 0, finita, non NaN) */
    fun isValidQuantity(quantity: Double): Boolean {
        return !quantity.isNaN() && !quantity.isInfinite() && quantity > 0
    }

    /** Verifica se questa unità è compatibile con un'altra per conversione */
    fun isCompatibleWith(other: StandardUnit): Boolean {
        return this.baseUnit == other.baseUnit
    }

    /** Arrotonda a n cifre decimali (half-up) */
    private fun roundToDecimals(value: Double, decimals: Int): Double {
        val factor = Math.pow(10.0, decimals.toDouble())
        return (value * factor).roundToLong() / factor
    }

    companion object {
        fun fromCode(code: String): StandardUnit? = values().firstOrNull { it.code == code.uppercase() } ?: values().firstOrNull { it.name == code.uppercase() }
    }
}