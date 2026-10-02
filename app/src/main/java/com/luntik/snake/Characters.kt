package com.luntik.snake

enum class CharRarity(val title: String, val color: Long) {
    COMMON("Обычная", 0xFF9E9E9E),
    LEGENDARY("Легендарная", 0xFFFFA726),
    MYTHIC("Мифическая", 0xFFE040FB)
}

data class AbilityEffect(
    val id: String,
    val title: String,
    val durationSec: Float,
    val onSelf: Boolean,
    val description: String
)

data class CharacterAbility(
    val id: String,
    val name: String,
    val cooldownSec: Int,
    val effects: List<AbilityEffect>
)

data class SnakeCharacter(
    val id: String,
    val name: String,
    val rarity: CharRarity,
    val headColor: Long,
    val bodyColor: Long,
    val description: String,
    val ability: CharacterAbility?,
    val unlockPrice: Int = 0,
    val unlockOnlyCase: Boolean = false
)

object CharacterData {
    val all = listOf(
        SnakeCharacter(
            id = "basic",
            name = "Обычная",
            rarity = CharRarity.COMMON,
            headColor = 0xFF9AFFB0,
            bodyColor = 0xFF3DFF6E,
            description = "Базовая змейка. Способность: ускорение.",
            ability = CharacterAbility(
                id = "boost",
                name = "Ускорение",
                cooldownSec = 10,
                effects = listOf(
                    AbilityEffect("speed_1", "Ускорение 1", 3f, onSelf = true, "Скорость + на 3 сек")
                )
            ),
            unlockPrice = 0
        ),
        SnakeCharacter(
            id = "jester",
            name = "Шут",
            rarity = CharRarity.LEGENDARY,
            headColor = 0xFFFFE082,
            bodyColor = 0xFFFF7043,
            description = "Шут. Способность «Пошутил» — неуязвимость.",
            ability = CharacterAbility(
                id = "joke",
                name = "Пошутил",
                cooldownSec = 18,
                effects = listOf(
                    AbilityEffect("invuln_100", "Неуязвимый 100", 5f, onSelf = true, "Не получаешь урон 5 сек")
                )
            ),
            unlockPrice = 800
        ),
        SnakeCharacter(
            id = "thor",
            name = "Тор",
            rarity = CharRarity.MYTHIC,
            headColor = 0xFFB3E5FC,
            bodyColor = 0xFF7C4DFF,
            description = "Мифический Тор. Молния: голод, заморозка врагов, скорость себе.",
            ability = CharacterAbility(
                id = "lightning",
                name = "Молния",
                cooldownSec = 20,
                effects = listOf(
                    AbilityEffect("hunger_1", "Голод 1", 2f, onSelf = false, "Каждые 2 сек −1 еда у врагов"),
                    AbilityEffect("freeze", "Заморозка", 2f, onSelf = false, "Замораживает всех кроме тебя на 2 сек"),
                    AbilityEffect("speed_1", "Скорость 1", 5f, onSelf = true, "Скорость + на 5 сек")
                )
            ),
            unlockPrice = 0,
            unlockOnlyCase = true
        )
    )

    fun byId(id: String) = all.find { it.id == id } ?: all.first()
}
