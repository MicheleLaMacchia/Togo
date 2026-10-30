package it.togo.app.presentation.activelist

import it.togo.app.domain.model.ListItem
import it.togo.app.domain.model.ListItemStatus
import it.togo.app.domain.model.StandardUnit
import java.util.UUID

internal fun listItem(
    id: String = UUID.randomUUID().toString(),
    productId: Long,
    quantity: Double,
    unit: StandardUnit,
    brand: String? = null,
    variant: String? = null,
    condition: String? = null,
    isChecked: Boolean = false,
): ListItem = ListItem.create(
    id = id,
    productId = productId,
    quantity = quantity,
    unit = unit.name,
    brand = brand,
    variant = variant,
    condition = condition,
    createdAt = 1_000L,
    statusChangedAt = 1_000L,
    status = if (isChecked) ListItemStatus.PURCHASED else ListItemStatus.TO_BUY,
)
