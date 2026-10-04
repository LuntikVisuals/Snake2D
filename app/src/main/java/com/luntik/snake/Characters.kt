package com.luntik.snake

enum class CharRarity(val title: String, val color: Long) {
    COMMON("Обычная", 0xFF9E9E9E),
    RARE("Редкая", 0xFF42A5F5),
    LEGENDARY("Легендарная", 0xFFFFA726),
    MYTHIC("Мифическая", 0xFFE040FB),
    EXCLUSIVE("Эксклюзивная", 0xFFFFD700)
}

data class AbilityEffect(val id: String, val title: String, val durationSec: Float, val onSelf: Boolean, val description: String)
data class CharacterAbility(val id: String, val name: String, val cooldownSec: Int, val effects: List<AbilityEffect>)
data class SnakeCharacter(
    val id: String, val name: String, val rarity: CharRarity,
    val headColor: Long, val bodyColor: Long, val description: String,
    val ability: CharacterAbility?, val unlockPrice: Int = 0,
    val unlockOnlyCase: Boolean = false, val seasonOnly: Boolean = false
)

object CharacterData {
    val all = listOf(
        SnakeCharacter("basic", "Обычная", CharRarity.COMMON, 0xFF9AFFB0, 0xFF3DFF6E,
            "Базовая змейка. Способность: ускорение.",
            CharacterAbility("boost", "Ускорение", 10, listOf(AbilityEffect("speed_1", "Ускорение 1", 3f, true, "Скорость + на 3 сек"))), 0),
        SnakeCharacter("scout", "Разведчик", CharRarity.RARE, 0xFF80DEEA, 0xFF26A69A,
            "Быстрый старт. Короткое ускорение чаще.",
            CharacterAbility("dash", "Рывок", 8, listOf(AbilityEffect("speed_1", "Рывок", 2f, true, "Короткий буст 2 сек"))), 250),
        SnakeCharacter("jester", "Шут", CharRarity.LEGENDARY, 0xFFFFE082, 0xFFFF7043,
            "Шут. «Пошутил» — неуязвимость.",
            CharacterAbility("joke", "Пошутил", 18, listOf(AbilityEffect("invuln_100", "Неуязвимый 100", 5f, true, "Не получаешь урон 5 сек"))), 800),
        SnakeCharacter("frost", "Мороз", CharRarity.LEGENDARY, 0xFFE1F5FE, 0xFF4FC3F7,
            "Замедляет соперников коротким холодом.",
            CharacterAbility("freeze", "Холод", 16, listOf(AbilityEffect("freeze_1", "Заморозка", 2f, false, "Враги медленнее 2 сек"))), 750),
        SnakeCharacter("thor", "Тор", CharRarity.MYTHIC, 0xFFB3E5FC, 0xFF7C4DFF,
            "Молния: голод врагам, скорость себе.",
            CharacterAbility("lightning", "Молния", 20, listOf(
                AbilityEffect("hunger", "Голод 1", 2f, false, "Враги теряют очки"),
                AbilityEffect("speed_1", "Скорость 1", 5f, true, "Скорость + 5 сек")
            )), 1200),
        SnakeCharacter("shadow", "Тень", CharRarity.MYTHIC, 0xFFCE93D8, 0xFF4A148C,
            "Краткий фаз: проходит сквозь тела врагов.",
            CharacterAbility("phase", "Фаза", 22, listOf(AbilityEffect("invuln_100", "Фаза", 3f, true, "Проход сквозь врагов 3 сек"))), 1100),
        // Только баттлпасс сезон 1 — не в свободной продаже
        SnakeCharacter("spongebob_char", "Спанч Боб", CharRarity.EXCLUSIVE, 0xFFFFF59D, 0xFFFDD835,
            "Только баттлпасс «Бикини Боттом», ур.20 premium.",
            CharacterAbility("sponge", "Губка", 15, listOf(AbilityEffect("invuln_100", "Впитывание", 4f, true, "Неуязвимость 4 сек"))), 0, seasonOnly = true)
    )
    fun byId(id: String) = all.find { it.id == id } ?: all.first()
    fun visibleForSelect(unlocked: Set<String>) = all.filter { !it.seasonOnly || it.id in unlocked }
}
