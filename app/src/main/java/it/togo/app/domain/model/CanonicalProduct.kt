package it.togo.app.domain.model

data class CanonicalProduct(
    val id: Long,
    val name: String,
    val level3Id: Long,
    val isUserDefined: Boolean = false,
    // Helper fields for UI (popolati via join con tassonomia)
    val level3Name: String? = null,
    val level2Name: String? = null,
    val level2Id: Long? = null,
    val level1Name: String? = null,
    val level1Id: Long? = null,
    // Attributi per AddItem
    val compatibleUnits: List<StandardUnit> = emptyList(),
    val showBrand: Boolean = true,
    val showVariant: Boolean = true,
    val showCondition: Boolean = true,
    val commonBrands: List<String> = emptyList(),
    val commonConditions: List<String> = emptyList(),
)