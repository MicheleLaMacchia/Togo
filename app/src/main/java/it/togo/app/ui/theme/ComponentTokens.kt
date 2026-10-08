package it.togo.app.ui.theme

import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Token a livello componente (da DESIGN.md §7).
 *
 * Ogni componente espone i suoi valori visivi come proprietà tipizzate.
 * I componenti UI leggono SOLO da qui — niente valori hardcoded.
 */
data class AppBarTokens(
    val background: Color,
    val borderBottomWidth: Dp = 2.dp,
    val borderBottomColor: Color,
    val height: Dp = 64.dp,
)

data class ItemRowTokens(
    val minHeight: Dp = 48.dp,
    val borderWidth: Dp = 1.5.dp,
    val borderColor: Color,
    val background: Color,
    val paddingHorizontal: Dp = 16.dp,
    val paddingVertical: Dp = 12.dp,
    val radius: androidx.compose.ui.graphics.RoundedCornerSize,
)

data class CheckboxUtilityTokens(
    val size: Dp = 26.dp,
    val borderWidth: Dp = 2.dp,
    val borderColor: Color,
    val radius: androidx.compose.ui.graphics.RoundedCornerSize,
    val checkColor: Color,
    val checkedBackground: Color,
)

data class CategoryHeaderTokens(
    val background: Color,
    val borderLeftWidth: Dp = 4.dp,
    val borderLeftColor: Color,
    val paddingHorizontal: Dp = 12.dp,
    val paddingVertical: Dp = 8.dp,
)

data class QuantityBadgeTokens(
    val background: Color,
    val textColor: Color,
    val radius: androidx.compose.ui.graphics.RoundedCornerSize,
    val paddingHorizontal: Dp = 8.dp,
    val paddingVertical: Dp = 4.dp,
)

data class VoiceFabTokens(
    val size: Dp = 64.dp,
    val background: Color,
    val textColor: Color,
    val borderWidth: Dp = 2.dp,
    val borderColor: Color,
    val radius: androidx.compose.ui.graphics.RoundedCornerSize,
)

data class BottomSheetTokens(
    val background: Color,
    val borderTopWidth: Dp = 2.dp,
    val borderTopColor: Color,
    val radiusTop: androidx.compose.ui.graphics.RoundedCornerSize,
)

data class DuplicateDialogTokens(
    val background: Color,
    val borderWidth: Dp = 2.dp,
    val borderColor: Color,
    val radius: androidx.compose.ui.graphics.RoundedCornerSize,
)

/** Container di tutti i token component-level. */
data class TogoComponentTokens(
    val appBar: AppBarTokens,
    val itemRow: ItemRowTokens,
    val checkboxUtility: CheckboxUtilityTokens,
    val categoryHeader: CategoryHeaderTokens,
    val quantityBadge: QuantityBadgeTokens,
    val voiceFab: VoiceFabTokens,
    val bottomSheet: BottomSheetTokens,
    val duplicateDialog: DuplicateDialogTokens,
)

/** Factory per costruire i token component dal tema corrente. */
object TogoComponentTokens {
    fun from(
        colors: TogoColorScheme,
        shapes: TogoShapes,
    ): TogoComponentTokens = TogoComponentTokens(
        appBar = AppBarTokens(
            background = colors.surfaceBase,
            borderBottomColor = colors.borderCrisp,
        ),
        itemRow = ItemRowTokens(
            borderColor = colors.borderCrisp,
            background = colors.surfaceCard,
            radius = shapes.md,
        ),
        checkboxUtility = CheckboxUtilityTokens(
            borderColor = colors.borderCrisp,
            radius = shapes.sm,
            checkColor = colors.inkInverse,
            checkedBackground = colors.accentAction,
        ),
        categoryHeader = CategoryHeaderTokens(
            background = colors.surfaceSubtle,
            borderLeftColor = colors.borderCrisp,
        ),
        quantityBadge = QuantityBadgeTokens(
            background = colors.badgeBg,
            textColor = colors.badgeInk,
            radius = shapes.sm,
        ),
        voiceFab = VoiceFabTokens(
            background = colors.accentAction,
            textColor = colors.inkInverse,
            borderColor = colors.borderCrisp,
            radius = shapes.full,
        ),
        bottomSheet = BottomSheetTokens(
            background = colors.surfaceBase,
            borderTopColor = colors.borderCrisp,
            radiusTop = shapes.lg,
        ),
        duplicateDialog = DuplicateDialogTokens(
            background = colors.surfaceBase,
            borderColor = colors.borderCrisp,
            radius = shapes.lg,
        ),
    )
}

/** CompositionLocal per i token component TOGO. */
internal val LocalTogoComponentTokens: CompositionLocal<TogoComponentTokens> =
    staticCompositionLocalOf {
        error("TogoComponentTokens non disponibile. Avvolgere il contenuto in TogoTheme { ... }")
    }

/** Estensione per accedere ai token component da qualsiasi composable dentro TogoTheme. */
val androidx.compose.ui.platform.CompositionLocalProvider.componentTokens: TogoComponentTokens
    get() = LocalTogoComponentTokens.current