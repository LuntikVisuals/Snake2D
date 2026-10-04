package com.luntik.snake

enum class Rarity(val title: String, val color: Long, val weight: Int) {
    COMMON("Обычная", 0xFF9E9E9E, 50),
    RARE("Редкая", 0xFF42A5F5, 30),
    EPIC("Эпическая", 0xFFAB47BC, 15),
    LEGENDARY("Легендарная", 0xFFFFA726, 5),
    SECRET("Секретная", 0xFFE91E63, 2),
    EXCLUSIVE("Эксклюзивная", 0xFFFFD700, 1)
}

enum class SnakeAppearance(val id: String, val title: String, val desc: String) {
    CLASSIC("classic", "Классика", "Стандартный облик"),
    RETRO("retro", "Ретро", "Кубическая змейка"),
    RETRO2("retro2", "Ретро 2", "Кружочки 2.0"),
    WORM("worm", "Глист", "Плавное тело"),
    VIPER("viper", "Гадюка", "Чешуя и злой взгляд"),
    NEON_FLOW("neon_flow", "Неон-поток", "Светящийся контур"),
    SHADOW("shadow_form", "Тень", "Тёмный силуэт"),
    CRYSTAL("crystal", "Кристалл", "Грани и блики")
}

enum class GameMode(
    val title: String, val desc: String, val speedMs: Long,
    val coinMul: Float, val xpMul: Float, val tixMul: Float = 1f,
    val wallsKill: Boolean = true, val hasObstacles: Boolean = false,
    val timeLimitSec: Int? = null, val bigMap: Boolean = false,
    val unlockLevel: Int = 1, val seasonal: Boolean = false
) {
    CLASSIC("Классика", "Обычная скорость, стены убивают", 140L, 1f, 1f, 1f, wallsKill = true),
    SPEED("Скорость", "Быстрее, больше наград", 90L, 1.6f, 1.5f, 1.2f),
    NO_WALLS("Без стен", "Выход с края — с другой стороны", 130L, 1.2f, 1.2f, 1.1f, wallsKill = false),
    OBSTACLES("Препятствия", "На поле блоки", 150L, 1.4f, 1.3f, 1.2f, hasObstacles = true, unlockLevel = 5),
    TIME_ATTACK("На время", "60 секунд — максимум очков", 120L, 1.5f, 1.4f, 1.3f, timeLimitSec = 60, unlockLevel = 3),
    BIG_FIELD("Большое поле", "Карта больше обычной", 135L, 1.3f, 1.3f, 1.2f, bigMap = true, unlockLevel = 8),
    FEEDING("Поедание", "Большая карта, 4 врага", 110L, 2f, 1.8f, 1.5f, wallsKill = false, unlockLevel = 10),
    KRUSTY("Красти Крабс", "Булочка → котлета по порядку. x2 tix", 125L, 1.5f, 1.5f, 2f, unlockLevel = 1, seasonal = true)
}

enum class MatchEvent(val title: String, val desc: String) {
    DOUBLE_FOOD("x2 Еда!", "Спавн двух еды"),
    DOUBLE_SPEED("x2 Скорость", "Ты быстрее"),
    BLOOD_NIGHT("Кровавая ночь", "Ты медленнее"),
    FOG("Туман", "Видимость урезана")
}

data class SnakeSkin(
    val id: String, val name: String, val rarity: Rarity, val price: Int,
    val headColor: Long, val bodyColor: Long,
    val unlockOnlyCase: Boolean = false, val effect: String = "none"
)
data class AppleSkin(val id: String, val name: String, val price: Int, val color: Long, val shape: String)
data class CaseType(val id: String, val name: String, val price: Int, val weights: Map<Rarity, Int>, val texture: String = "crate")
data class Achievement(val id: String, val title: String, val description: String, val rewardCoins: Int)

object ShopData {
    val skins: List<SnakeSkin> = listOf(
        SnakeSkin("default", "Пиксель", Rarity.COMMON, 0, 0xFF9AFFB0, 0xFF3DFF6E),
        SnakeSkin("mint", "Мята", Rarity.COMMON, 50, 0xFFE0FFE8, 0xFF69F0AE),
        SnakeSkin("ember", "Уголь", Rarity.COMMON, 60, 0xFFFFCCBC, 0xFF8D6E63),
        SnakeSkin("sand", "Песок", Rarity.COMMON, 55, 0xFFFFF3E0, 0xFFFFB74D),
        SnakeSkin("sky", "Небо", Rarity.COMMON, 55, 0xFFE3F2FD, 0xFF64B5F6),
        SnakeSkin("rose", "Роза", Rarity.COMMON, 60, 0xFFFCE4EC, 0xFFF06292),
        SnakeSkin("lime", "Лайм", Rarity.COMMON, 50, 0xFFF9FBE7, 0xFFC0CA33),
        SnakeSkin("steel", "Сталь", Rarity.COMMON, 70, 0xFFECEFF1, 0xFF78909C),
        SnakeSkin("ice", "Лёд", Rarity.RARE, 150, 0xFFE3F2FD, 0xFF4FC3F7, effect = "glow"),
        SnakeSkin("lava", "Лава", Rarity.RARE, 200, 0xFFFFE0B2, 0xFFFF7043, effect = "glow"),
        SnakeSkin("ocean", "Океан", Rarity.RARE, 180, 0xFFB2EBF2, 0xFF00ACC1),
        SnakeSkin("forest", "Лес", Rarity.RARE, 160, 0xFFE8F5E9, 0xFF43A047),
        SnakeSkin("sunset", "Закат", Rarity.RARE, 170, 0xFFFFE0B2, 0xFFFF5252),
        SnakeSkin("berry", "Ягода", Rarity.RARE, 160, 0xFFF3E5F5, 0xFF8E24AA),
        SnakeSkin("honey", "Мёд", Rarity.RARE, 155, 0xFFFFF8E1, 0xFFFFC107),
        SnakeSkin("azure", "Лазурь", Rarity.RARE, 165, 0xFFE0F7FA, 0xFF00BCD4),
        SnakeSkin("neon", "Неон", Rarity.EPIC, 400, 0xFFF3E5F5, 0xFFE040FB, effect = "glow"),
        SnakeSkin("gold", "Золото", Rarity.EPIC, 500, 0xFFFFF8E1, 0xFFFFD54F, effect = "sparkle"),
        SnakeSkin("yinyang", "Инь-Янь", Rarity.EPIC, 450, 0xFFFFFFFF, 0xFF212121),
        SnakeSkin("toxic", "Токсик", Rarity.EPIC, 420, 0xFFF1F8E9, 0xFF76FF03, effect = "glow"),
        SnakeSkin("blood", "Кровь", Rarity.EPIC, 430, 0xFFFFEBEE, 0xFFC62828, effect = "trail"),
        SnakeSkin("royal", "Королевский", Rarity.EPIC, 480, 0xFFEDE7F6, 0xFF5E35B1, effect = "sparkle"),
        SnakeSkin("cyber", "Кибер", Rarity.EPIC, 460, 0xFFE8EAF6, 0xFF00E5FF, effect = "glow"),
        SnakeSkin("amber", "Янтарь", Rarity.EPIC, 440, 0xFFFFF3E0, 0xFFFF6F00),
        SnakeSkin("glass", "Стекло", Rarity.LEGENDARY, 0, 0xFFFFFFFF, 0xFFB0BEC5, true, "glow"),
        SnakeSkin("void", "Бездна", Rarity.LEGENDARY, 0, 0xFFE8EAF6, 0xFF7C4DFF, true, "trail"),
        SnakeSkin("dragon", "Дракон", Rarity.LEGENDARY, 0, 0xFFFFCDD2, 0xFFC62828, true, "scales"),
        SnakeSkin("phoenix", "Феникс", Rarity.LEGENDARY, 0, 0xFFFFE0B2, 0xFFFF6D00, true, "glow"),
        SnakeSkin("aurora", "Аврора", Rarity.LEGENDARY, 0, 0xFFE8F5E9, 0xFF00E676, true, "sparkle"),
        SnakeSkin("obsidian", "Обсидиан", Rarity.SECRET, 0, 0xFF212121, 0xFF424242, true, "trail"),
        SnakeSkin("prism", "Призма", Rarity.SECRET, 0, 0xFFFFFFFF, 0xFFFF4081, true, "sparkle"),
        SnakeSkin("ghost", "Призрак", Rarity.SECRET, 0, 0xFFE0E0E0, 0xFF9E9E9E, true, "glow"),
        SnakeSkin("patrick", "Патрик", Rarity.EXCLUSIVE, 0, 0xFFFFCDD2, 0xFFFF8A80, true, "stars"),
        SnakeSkin("squidward", "Сквидвард", Rarity.EXCLUSIVE, 0, 0xFFB2DFDB, 0xFF00897B, true, "stone"),
        SnakeSkin("gary", "Гэри", Rarity.EXCLUSIVE, 0, 0xFFE1BEE7, 0xFF8E24AA, true, "food"),
        SnakeSkin("krab", "Мистер Крабс", Rarity.EXCLUSIVE, 0, 0xFFFFCCBC, 0xFFD84315, true, "money"),
        SnakeSkin("spongebob", "Спанч Боб", Rarity.EXCLUSIVE, 0, 0xFFFFF59D, 0xFFFDD835, true, "sponge"),
        SnakeSkin("doodle_bob", "Нарисованный Спанч", Rarity.EXCLUSIVE, 0, 0xFFFAFAFA, 0xFF616161, true, "pencil"),
        SnakeSkin("plankton", "Планктон", Rarity.EXCLUSIVE, 0, 0xFFC8E6C9, 0xFF1B5E20, true, "laugh")
    )
    val appleSkins = listOf(
        AppleSkin("apple", "Яблоко", 0, 0xFFE53935, "apple"),
        AppleSkin("blackberry", "Ежевика", 80, 0xFF4A148C, "berry"),
        AppleSkin("raspberry", "Малина", 80, 0xFFE91E63, "berry"),
        AppleSkin("strawberry", "Клубника", 90, 0xFFF44336, "berry"),
        AppleSkin("mushroom", "Грибы", 100, 0xFFFFCC80, "mushroom"),
        AppleSkin("grape", "Виноград", 85, 0xFF7B1FA2, "grape"),
        AppleSkin("juice", "Сок", 95, 0xFFFF9800, "juice"),
        AppleSkin("orange", "Апельсин", 80, 0xFFFF6D00, "citrus"),
        AppleSkin("krab_burger", "Крабсбургер", 0, 0xFFFFB74D, "burger")
    )
    val cases = listOf(
        CaseType("basic", "Обычный кейс", 800, mapOf(Rarity.COMMON to 78, Rarity.RARE to 18, Rarity.EPIC to 3, Rarity.LEGENDARY to 1), "crate_wood"),
        CaseType("rare", "Редкий кейс", 1800, mapOf(Rarity.COMMON to 62, Rarity.RARE to 28, Rarity.EPIC to 8, Rarity.LEGENDARY to 2), "crate_blue"),
        CaseType("premium", "Премиум кейс", 3500, mapOf(Rarity.COMMON to 50, Rarity.RARE to 32, Rarity.EPIC to 14, Rarity.LEGENDARY to 4), "crate_purple"),
        CaseType("epic", "Эпический кейс", 7000, mapOf(Rarity.COMMON to 40, Rarity.RARE to 35, Rarity.EPIC to 20, Rarity.LEGENDARY to 5), "crate_gold"),
        CaseType("legend", "Легендарный кейс", 15000, mapOf(Rarity.COMMON to 30, Rarity.RARE to 35, Rarity.EPIC to 25, Rarity.LEGENDARY to 8, Rarity.SECRET to 2), "crate_legend"),
        CaseType("trash_bucket", "Помойное ведро", 12000, mapOf(Rarity.COMMON to 40, Rarity.RARE to 30, Rarity.EPIC to 20, Rarity.LEGENDARY to 5, Rarity.EXCLUSIVE to 5), "crate_bucket"),
        CaseType("krusty", "Красти Краб", 18000, mapOf(Rarity.COMMON to 34, Rarity.RARE to 30, Rarity.EPIC to 22, Rarity.LEGENDARY to 8, Rarity.EXCLUSIVE to 6), "crate_krab")
    )
    val achievements = listOf(
        Achievement("first_game", "Первый шаг", "Сыграй первую партию", 20),
        Achievement("first_win", "Победитель", "Заполни поле", 50),
        Achievement("score_100", "Сотня", "100 очков за партию", 30),
        Achievement("score_300", "Триста", "300 очков за партию", 80),
        Achievement("length_20", "Длинный хвост", "Длина 20", 40),
        Achievement("open_3_cases", "Коллекционер", "Открой 3 кейса", 35),
        Achievement("skins_5", "Стилист", "5 скинов", 60)
    )
    fun skin(id: String) = skins.find { it.id == id } ?: skins.first()
    fun apple(id: String) = appleSkins.find { it.id == id } ?: appleSkins.first()
    fun openCase(type: CaseType): SnakeSkin {
        val pool = type.weights.flatMap { (r, w) -> List(w.coerceAtLeast(0)) { r } }
        if (pool.isEmpty()) return skins.first()
        val rarity = pool.random()
        val candidates = skins.filter { it.rarity == rarity }
        return if (candidates.isNotEmpty()) candidates.random() else skins.random()
    }
    fun openXpCrate(): Pair<Rarity, Int> {
        val roll = (1..100).random()
        val rarity = when {
            roll <= 3 -> Rarity.SECRET
            roll <= 8 -> Rarity.LEGENDARY
            roll <= 18 -> Rarity.EPIC
            roll <= 33 -> Rarity.RARE
            roll <= 53 -> Rarity.COMMON
            else -> null
        }
        return (rarity ?: Rarity.COMMON) to if (rarity == null) 1 else 0
    }
}
