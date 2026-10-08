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

    companion object {
        fun fromCode(code: String): StandardUnit? = values().firstOrNull { it.code == code.uppercase() }
            ?: values().firstOrNull { it.name == code.uppercase() }
    }
}