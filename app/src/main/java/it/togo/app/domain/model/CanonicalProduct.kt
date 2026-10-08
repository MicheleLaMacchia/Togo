package it.togo.app.domain.model

data class CanonicalProduct(
    val id: Long,
    val name: String,
    val level3Id: Long,
    val isUserDefined: Boolean = false
)