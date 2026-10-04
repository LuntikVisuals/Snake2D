package com.luntik.snake

enum class BpRewardType { COINS, GEMS, TIX, SKIN, CHARACTER, APPEARANCE, APPLE, XP_CRATE, NONE }

data class BpReward(
    val type: BpRewardType,
    val amount: Int = 0,
    val itemId: String = "",
    val title: String = ""
)

data class BpLevel(
    val level: Int,
    val tixRequired: Int,
    val free: BpReward,
    val premium: BpReward
)

object BattlePassData {
    const val SEASON_ID = "s1_bikini_bottom"
    const val SEASON_NAME = "Бикини Боттом"
    const val MAX_LEVEL = 40
    const val SEASON_DAYS = 2

    fun tixForLevel(level: Int): Int {
        if (level <= 1) return 100
        if (level <= 5) return 100 + (level - 1) * 10
        if (level >= 40) return 1000
        return 100 + 4 * 10 + (level - 5) * 20
    }

    val levels: List<BpLevel> = (1..MAX_LEVEL).map { lv ->
        val free = when (lv) {
            1 -> BpReward(BpRewardType.COINS, 50, title = "50 монет")
            5 -> BpReward(BpRewardType.GEMS, 5, title = "5 гемов")
            10 -> BpReward(BpRewardType.COINS, 150, title = "Сезонный ящик")
            15 -> BpReward(BpRewardType.COINS, 100, title = "100 монет")
            20 -> BpReward(BpRewardType.GEMS, 10, title = "10 гемов")
            25 -> BpReward(BpRewardType.GEMS, 20, title = "Легендарный ящик")
            30 -> BpReward(BpRewardType.COINS, 200, title = "200 монет")
            35 -> BpReward(BpRewardType.GEMS, 15, title = "15 гемов")
            40 -> BpReward(BpRewardType.APPEARANCE, itemId = "viper", title = "Облик Гадюка")
            else -> if (lv % 3 == 0) BpReward(BpRewardType.COINS, 25 + lv, title = "${25 + lv} монет")
            else BpReward(BpRewardType.TIX, 5, title = "5 tix")
        }
        val premium = when (lv) {
            5 -> BpReward(BpRewardType.SKIN, itemId = "patrick", title = "Патрик")
            10 -> BpReward(BpRewardType.SKIN, itemId = "squidward", title = "Сквидвард")
            15 -> BpReward(BpRewardType.SKIN, itemId = "gary", title = "Гэри")
            20 -> BpReward(BpRewardType.CHARACTER, itemId = "spongebob_char", title = "Спанч Боб (персонаж)")
            25 -> BpReward(BpRewardType.SKIN, itemId = "krab", title = "Мистер Крабс")
            30 -> BpReward(BpRewardType.SKIN, itemId = "doodle_bob", title = "Нарисованный Спанч")
            35 -> BpReward(BpRewardType.SKIN, itemId = "plankton", title = "Планктон")
            40 -> BpReward(BpRewardType.SKIN, itemId = "spongebob", title = "Облик Спанч Боб")
            else -> if (lv % 2 == 0) BpReward(BpRewardType.GEMS, 3 + lv / 5, title = "${3 + lv / 5} гемов")
            else BpReward(BpRewardType.COINS, 40 + lv * 2, title = "${40 + lv * 2} монет")
        }
        BpLevel(lv, tixForLevel(lv), free, premium)
    }

    fun cumulativeTixForLevel(level: Int): Int =
        (1..level.coerceAtMost(MAX_LEVEL)).sumOf { tixForLevel(it) }

    fun levelFromTix(tix: Int): Int {
        var sum = 0
        for (lv in 1..MAX_LEVEL) {
            sum += tixForLevel(lv)
            if (tix < sum) return lv - 1
        }
        return MAX_LEVEL
    }
}
