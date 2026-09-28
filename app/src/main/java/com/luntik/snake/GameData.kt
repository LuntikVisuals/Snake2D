package com.luntik.snake

enum class Rarity(val title: String, val color: Long, val weight: Int) {
    COMMON("Обычная", 0xFF9E9E9E, 50),
    RARE("Редкая", 0xFF42A5F5, 30),
    EPIC("Эпическая", 0xFFAB47BC, 15),
    LEGENDARY("Легендарная", 0xFFFFA726, 5)
}

enum class GameMode(val title: String, val desc: String, val speedMs: Long, val coinMul: Float, val xpMul: Float) {
    CLASSIC("Классика", "Обычная скорость, стены убивают", 140L, 1f, 1f),
    SPEED("Скорость", "Быстрее, больше монет и XP", 90L, 1.6f, 1.5f)
}

data class SnakeSkin(
    val id: String,
    val name: String,
    val rarity: Rarity,
    val price: Int,
    val headColor: Long,
    val bodyColor: Long,
    val unlockOnlyCase: Boolean = false
)

data class CaseType(
    val id: String,
    val name: String,
    val price: Int,
    val weights: Map<Rarity, Int>
)

object ShopData {
    val skins = listOf(
        SnakeSkin("default", "Пиксель", Rarity.COMMON, 0, 0xFF9AFFB0, 0xFF3DFF6E),
        SnakeSkin("mint", "Мята", Rarity.COMMON, 50, 0xFFE0FFE8, 0xFF69F0AE),
        SnakeSkin("ice", "Лёд", Rarity.RARE, 150, 0xFFE3F2FD, 0xFF4FC3F7),
        SnakeSkin("lava", "Лава", Rarity.RARE, 200, 0xFFFFE0B2, 0xFFFF7043),
        SnakeSkin("neon", "Неон", Rarity.EPIC, 400, 0xFFF3E5F5, 0xFFE040FB),
        SnakeSkin("gold", "Золото", Rarity.EPIC, 500, 0xFFFFF8E1, 0xFFFFD54F),
        SnakeSkin("glass", "Стекло", Rarity.LEGENDARY, 0, 0xFFFFFFFF, 0xFFB0BEC5, unlockOnlyCase = true),
        SnakeSkin("void", "Бездна", Rarity.LEGENDARY, 0, 0xFFE8EAF6, 0xFF7C4DFF, unlockOnlyCase = true)
    )

    val cases = listOf(
        CaseType(
            "basic",
            "Обычный кейс",
            100,
            mapOf(Rarity.COMMON to 55, Rarity.RARE to 30, Rarity.EPIC to 12, Rarity.LEGENDARY to 3)
        ),
        CaseType(
            "rare",
            "Редкий кейс",
            250,
            mapOf(Rarity.COMMON to 20, Rarity.RARE to 45, Rarity.EPIC to 25, Rarity.LEGENDARY to 10)
        )
    )

    fun skin(id: String) = skins.find { it.id == id } ?: skins.first()

    fun openCase(type: CaseType): SnakeSkin {
        val pool = type.weights.flatMap { (r, w) -> List(w) { r } }
        val rarity = pool.random()
        val candidates = skins.filter { it.rarity == rarity }
        return candidates.random()
    }
}
