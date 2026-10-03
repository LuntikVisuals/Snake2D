package com.luntik.snake

enum class Rarity(val title: String, val color: Long, val weight: Int) {
    COMMON("Обычная", 0xFF9E9E9E, 50),
    RARE("Редкая", 0xFF42A5F5, 30),
    EPIC("Эпическая", 0xFFAB47BC, 15),
    LEGENDARY("Легендарная", 0xFFFFA726, 5)
}

enum class GameMode(
    val title: String,
    val desc: String,
    val speedMs: Long,
    val coinMul: Float,
    val xpMul: Float,
    val wallsKill: Boolean = true,
    val hasObstacles: Boolean = false,
    val timeLimitSec: Int? = null
) {
    CLASSIC("Классика", "Обычная скорость, стены убивают", 140L, 1f, 1f, wallsKill = true),
    SPEED("Скорость", "Быстрее, больше монет и XP", 90L, 1.6f, 1.5f, wallsKill = true),
    NO_WALLS("Без стен", "Выход с края — появление с другой стороны", 130L, 1.2f, 1.2f, wallsKill = false),
    OBSTACLES("Препятствия", "На поле есть блоки, которых нужно избегать", 150L, 1.4f, 1.3f, wallsKill = true, hasObstacles = true),
    TIME_ATTACK("На время", "60 секунд — набери максимум очков", 120L, 1.5f, 1.4f, wallsKill = true, timeLimitSec = 60),
    FEEDING("Поедание", "Большая карта, 4 врага, кто съел больше", 110L, 2f, 1.8f, wallsKill = false)
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

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val rewardCoins: Int
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
        CaseType("basic", "Обычный кейс", 80, mapOf(Rarity.COMMON to 55, Rarity.RARE to 30, Rarity.EPIC to 12, Rarity.LEGENDARY to 3)),
        CaseType("rare", "Редкий кейс", 150, mapOf(Rarity.COMMON to 35, Rarity.RARE to 40, Rarity.EPIC to 20, Rarity.LEGENDARY to 5)),
        CaseType("premium", "Премиум кейс", 250, mapOf(Rarity.COMMON to 20, Rarity.RARE to 40, Rarity.EPIC to 28, Rarity.LEGENDARY to 12)),
        CaseType("epic", "Эпический кейс", 400, mapOf(Rarity.COMMON to 10, Rarity.RARE to 30, Rarity.EPIC to 40, Rarity.LEGENDARY to 20)),
        CaseType("legend", "Легендарный кейс", 600, mapOf(Rarity.COMMON to 5, Rarity.RARE to 20, Rarity.EPIC to 40, Rarity.LEGENDARY to 35)),
        CaseType("void", "Кейс Бездны", 350, mapOf(Rarity.COMMON to 15, Rarity.RARE to 25, Rarity.EPIC to 35, Rarity.LEGENDARY to 25))
    )

    val achievements = listOf(
        Achievement("first_game", "Первый шаг", "Сыграй первую партию", 20),
        Achievement("first_win", "Победитель", "Заполни поле полностью", 50),
        Achievement("score_100", "Сотня", "Набери 100 очков за партию", 30),
        Achievement("score_300", "Триста", "Набери 300 очков за партию", 80),
        Achievement("length_20", "Длинный хвост", "Достигни длины 20", 40),
        Achievement("open_3_cases", "Коллекционер", "Открой 3 кейса", 35),
        Achievement("skins_5", "Стилист", "Разблокируй 5 скинов", 60)
    )

    fun skin(id: String) = skins.find { it.id == id } ?: skins.first()

    fun openCase(type: CaseType): SnakeSkin {
        val pool = type.weights.flatMap { (r, w) -> List(w.coerceAtLeast(0)) { r } }
        if (pool.isEmpty()) return skins.first()
        val rarity = pool.random()
        val candidates = skins.filter { it.rarity == rarity }
        return if (candidates.isNotEmpty()) candidates.random() else skins.random()
    }
}
