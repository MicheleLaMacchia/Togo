package it.togo.app.domain.wizard

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.TaxonomyLevel
import java.util.UUID

/**
 * Bozza accumulata durante il wizard di categorizzazione.
 *
 * Stato immutabile: ogni modifica ritorna nuova istanza (pattern copy).
 * Non persistito finché non confermato via `CreateCanonicalProductUseCase`.
 */
data class NewProductDraft(
    /** Step corrente del wizard */
    val currentStep: WizardStep = WizardStep.SELECT_L1,

    // Selezione tassonomia
    val selectedLevel1: TaxonomyLevel.Level1? = null,
    val selectedLevel2: TaxonomyLevel.Level2? = null,
    val selectedLevel3: TaxonomyLevel.Level3? = null,
    val isNewLevel3: Boolean = false,        // true se L3 creato dall'utente ("+ Nuovo...")
    val newLevel3Name: String = "",           // Nome per nuovo L3 (se isNewLevel3)

    // Dettagli prodotto
    val productName: String = "",
    val compatibleUnits: List<StandardUnit> = emptyList(),
    val selectedUnit: StandardUnit? = null,
    val quantity: Double = 1.0,
    val brand: String? = null,
    val variant: String? = null,
    val condition: String? = null,

    // Validazione
    val errors: Map<String, String> = emptyMap()
) {

    /** True se può procedere allo step successivo */
    val canProceed: Boolean
        get() = when (currentStep) {
            WizardStep.SELECT_L1 -> selectedLevel1 != null
            WizardStep.SELECT_L2 -> selectedLevel2 != null
            WizardStep.SELECT_L3 -> selectedLevel3 != null && (isNewLevel3.not() || newLevel3Name.isNotBlank())
            WizardStep.ENTER_DETAILS -> productName.isNotBlank() && selectedUnit != null && quantity > 0
            WizardStep.CONFIRM -> true
        }

    /** True se può tornare indietro */
    val canGoBack: Boolean = currentStep != WizardStep.SELECT_L1

    /** Step successivo */
    val nextStep: WizardStep?
        get() = when (currentStep) {
            WizardStep.SELECT_L1 -> WizardStep.SELECT_L2
            WizardStep.SELECT_L2 -> WizardStep.SELECT_L3
            WizardStep.SELECT_L3 -> WizardStep.ENTER_DETAILS
            WizardStep.ENTER_DETAILS -> WizardStep.CONFIRM
            WizardStep.CONFIRM -> null
        }

    /** Step precedente */
    val previousStep: WizardStep?
        get() = when (currentStep) {
            WizardStep.SELECT_L1 -> null
            WizardStep.SELECT_L2 -> WizardStep.SELECT_L1
            WizardStep.SELECT_L3 -> WizardStep.SELECT_L2
            WizardStep.ENTER_DETAILS -> WizardStep.SELECT_L3
            WizardStep.CONFIRM -> WizardStep.ENTER_DETAILS
        }

    // --- Mutatori per step ---

    fun withLevel1(level1: TaxonomyLevel.Level1): NewProductDraft = copy(
        currentStep = WizardStep.SELECT_L2,
        selectedLevel1 = level1,
        selectedLevel2 = null,
        selectedLevel3 = null,
        isNewLevel3 = false,
        newLevel3Name = "",
        productName = "",
        compatibleUnits = emptyList(),
        selectedUnit = null,
        quantity = 1.0,
        brand = null,
        variant = null,
        condition = null,
        errors = emptyMap()
    )

    fun withLevel2(level2: TaxonomyLevel.Level2): NewProductDraft = copy(
        currentStep = WizardStep.SELECT_L3,
        selectedLevel2 = level2,
        selectedLevel3 = null,
        isNewLevel3 = false,
        newLevel3Name = "",
        productName = "",
        compatibleUnits = emptyList(),
        selectedUnit = null,
        quantity = 1.0,
        brand = null,
        variant = null,
        condition = null,
        errors = emptyMap()
    )

    fun withLevel3(level3: TaxonomyLevel.Level3): NewProductDraft = copy(
        currentStep = WizardStep.ENTER_DETAILS,
        selectedLevel3 = level3,
        isNewLevel3 = false,
        newLevel3Name = "",
        productName = level3.name,
        compatibleUnits = emptyList(),
        selectedUnit = null,
        quantity = 1.0,
        brand = null,
        variant = null,
        condition = null,
        errors = emptyMap()
    )

    fun withNewLevel3(name: String): NewProductDraft = copy(
        isNewLevel3 = true,
        newLevel3Name = name.trim(),
        selectedLevel3 = null
    )

    fun withProductName(name: String): NewProductDraft = copy(
        productName = name.trim()
    )

    fun withUnits(units: List<StandardUnit>, defaultUnit: StandardUnit? = null): NewProductDraft = copy(
        compatibleUnits = units,
        selectedUnit = defaultUnit ?: units.firstOrNull()
    )

    fun withSelectedUnit(unit: StandardUnit): NewProductDraft = copy(
        selectedUnit = unit
    )

    fun withQuantity(qty: Double): NewProductDraft = copy(
        quantity = qty
    )

    fun withAttributes(brand: String?, variant: String?, condition: String?): NewProductDraft = copy(
        brand = brand?.takeIf { it.isNotBlank() },
        variant = variant?.takeIf { it.isNotBlank() },
        condition = condition?.takeIf { it.isNotBlank() }
    )

    fun withStep(step: WizardStep): NewProductDraft = copy(currentStep = step)

    fun withError(key: String, message: String): NewProductDraft = copy(
        errors = errors + (key to message)
    )

    fun clearError(key: String): NewProductDraft = copy(
        errors = errors - key
    )

    /** Costruisce CanonicalProduct per persistenza (valido solo a CONFIRM) */
    fun toCanonicalProduct(): CanonicalProduct {
        require(currentStep == WizardStep.CONFIRM) { "Draft non completo per creazione prodotto" }
        require(productName.isNotBlank()) { "Nome prodotto obbligatorio" }
        require(selectedUnit != null) { "Unità obbligatoria" }
        require(quantity > 0) { "Quantità deve essere > 0" }
        require(selectedLevel1 != null && selectedLevel2 != null) { "L1 e L2 obbligatori" }

        val finalLevel3Id = selectedLevel3?.id ?: -1L // -1 = nuovo, DAO assegnerà ID
        val finalLevel3Name = selectedLevel3?.name ?: newLevel3Name

        return CanonicalProduct(
            id = finalLevel3Id, // Sarà assegnato da DB
            name = productName,
            level3Id = finalLevel3Id,
            isUserDefined = true,
            level3Name = finalLevel3Name,
            level2Name = selectedLevel2?.name,
            level2Id = selectedLevel2?.id,
            level1Name = selectedLevel1?.name,
            level1Id = selectedLevel1?.id,
            compatibleUnits = compatibleUnits,
            showBrand = brand != null,
            showVariant = variant != null,
            showCondition = condition != null,
            commonBrands = if (brand != null) listOf(brand) else emptyList(),
            commonConditions = if (condition != null) listOf(condition) else emptyList()
        )
    }

    /** Reset per nuovo wizard */
    companion object {
        fun empty(): NewProductDraft = NewProductDraft()
    }
}