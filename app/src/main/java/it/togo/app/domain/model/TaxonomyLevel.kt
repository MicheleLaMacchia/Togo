package it.togo.app.domain.model

sealed interface TaxonomyLevel {
    val id: Long
    val name: String
    val sortOrder: Int

    data class Level1(
        override val id: Long,
        override val name: String,
        override val sortOrder: Int
    ) : TaxonomyLevel

    data class Level2(
        override val id: Long,
        override val name: String,
        override val sortOrder: Int,
        val level1Id: Long
    ) : TaxonomyLevel

    data class Level3(
        override val id: Long,
        override val name: String,
        override val sortOrder: Int,
        val level2Id: Long,
        val isUserDefined: Boolean = false
    ) : TaxonomyLevel
}