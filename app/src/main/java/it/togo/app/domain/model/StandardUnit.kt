package it.togo.app.domain.model

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

    /** Unità compatibili per conversione (stessa dimensione fisica) */
    val compatibleUnits: List<StandardUnit>
        get() = when (this) {
            GRAM, KILOGRAM -> listOf(GRAM, KILOGRAM)
            MILLILITER, LITER -> listOf(MILLILITER, LITER)
            PIECE, PACKAGE, BOTTLE, JAR, CAN, BOX, ROLL, POT -> listOf(this)
        }

    companion object {
        fun fromCode(code: String): StandardUnit? = values().firstOrNull { it.code == code.uppercase() } ?: values().firstOrNull { it.name == code.uppercase() }
    }
}