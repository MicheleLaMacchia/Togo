package it.togo.app.domain.product

import it.togo.app.domain.model.StandardUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductUnitResolverTest {

    @Test
    fun `pasta e carne usano kg come default e offrono grammi e confezione`() {
        val pasta = product(
            name = "Spaghetti n. 5",
            level1 = "Dispensa",
            level2 = "Pasta e riso",
            level3 = "Pasta secca",
        )
        val carne = product(
            name = "Macinato di manzo",
            level1 = "Carne",
            level2 = "Carne bovina",
            level3 = "Carne bovina macinata",
        )

        assertEquals(listOf(StandardUnit.KILOGRAM, StandardUnit.GRAM, StandardUnit.PACKAGE), pasta)
        assertEquals(listOf(StandardUnit.KILOGRAM, StandardUnit.GRAM, StandardUnit.PACKAGE), carne)
    }

    @Test
    fun `latte e bibite usano litri come default`() {
        val latte = product("Latte intero", "Banco Frigo", "Latte e yogurt", "Latte fresco")
        val succo = product("Succo d'arancia", "Bevande", "Succhi e nettari", "Succhi di frutta")

        assertEquals(StandardUnit.LITER, latte.first())
        assertTrue(latte.contains(StandardUnit.MILLILITER))
        assertFalse(latte.contains(StandardUnit.PIECE))

        assertEquals(StandardUnit.LITER, succo.first())
        assertTrue(succo.contains(StandardUnit.BOTTLE))
    }

    @Test
    fun `vino e birra propongono la bottiglia come default`() {
        val vino = product("Chianti", "Bevande", "Vino", "Vino rosso")
        val birra = product("Birra chiara", "Bevande", "Birra", "Birra chiara")

        assertEquals(StandardUnit.BOTTLE, vino.first())
        assertTrue(vino.contains(StandardUnit.LITER))

        assertEquals(StandardUnit.BOTTLE, birra.first())
        assertTrue(birra.contains(StandardUnit.CAN))
    }

    @Test
    fun `uova usano pezzi e confezione`() {
        val uova = product("Uova fresche", "Banco Frigo", "Uova", "Uova fresche")

        assertEquals(listOf(StandardUnit.PIECE, StandardUnit.PACKAGE), uova)
    }

    @Test
    fun `yogurt usa grammi mentre il latte della stessa famiglia usa litri`() {
        val yogurt = product("Yogurt bianco", "Banco Frigo", "Latte e yogurt", "Yogurt")
        val latte = product("Latte parzialmente scremato", "Banco Frigo", "Latte e yogurt", "Latte fresco")

        assertEquals(StandardUnit.GRAM, yogurt.first())
        assertEquals(StandardUnit.LITER, latte.first())
    }

    @Test
    fun `oli usano bottiglie e litri, sale usa grammi`() {
        val olio = product("Olio extravergine", "Dispensa", "Oli e condimenti", "Oli")
        val sale = product("Sale fino", "Dispensa", "Oli e condimenti", "Sale e spezie")

        assertEquals(StandardUnit.BOTTLE, olio.first())
        assertTrue(olio.contains(StandardUnit.LITER))
        assertEquals(StandardUnit.GRAM, sale.first())
    }

    @Test
    fun `barattoli e scatolette espongono le unita di confezionamento corrette`() {
        val conserve = product("Pomodori pelati", "Dispensa", "Conserve", "Conserve di pomodoro")
        val salse = product("Pesto alla genovese", "Dispensa", "Salse e pesti", "Salse pronte")

        assertTrue(conserve.contains(StandardUnit.CAN))
        assertTrue(conserve.contains(StandardUnit.JAR))
        assertTrue(salse.contains(StandardUnit.JAR))
    }

    @Test
    fun `un livello 3 user-defined eredita la famiglia merceologica`() {
        val prodotto = product(
            name = "Fusilli bio",
            level1 = "Dispensa",
            level2 = "Pasta e riso",
            level3 = "Pasta bio",
        )

        assertEquals(StandardUnit.KILOGRAM, prodotto.first())
        assertTrue(prodotto.contains(StandardUnit.GRAM))
    }

    @Test
    fun `senza tassonomia il nome del prodotto fornisce un fallback utile`() {
        val latte = product(name = "Latte di avena", level1 = null, level2 = null, level3 = null)
        val detersivo = product(name = "Detersivo piatti", level1 = null, level2 = null, level3 = null)
        val sconosciuto = product(name = "Prodotto misterioso", level1 = null, level2 = null, level3 = null)

        assertEquals(StandardUnit.LITER, latte.first())
        assertEquals(StandardUnit.LITER, detersivo.first())
        assertEquals(listOf(StandardUnit.PIECE), sconosciuto)
    }

    @Test
    fun `un nome con contenitore non viene confuso con il contenuto liquido`() {
        val barattolo = product(
            name = "Barattolo di pomodori",
            level1 = null,
            level2 = null,
            level3 = null,
        )

        assertTrue(barattolo.contains(StandardUnit.JAR))
        assertEquals(StandardUnit.JAR, barattolo.first())
    }

    private fun product(
        name: String,
        level1: String?,
        level2: String?,
        level3: String?,
    ): List<StandardUnit> = ProductUnitResolver.resolve(
        level1Name = level1,
        level2Name = level2,
        level3Name = level3,
        productName = name,
    )
}
