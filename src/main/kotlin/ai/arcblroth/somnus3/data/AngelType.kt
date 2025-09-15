package ai.arcblroth.somnus3.data

import ai.arcblroth.somnus3.Constants
import dev.kord.common.Color
import kotlin.math.nextUp
import kotlin.random.Random

enum class AngelType(
    val uiEmoji: String,
    val uiName: String,
    val flavor: String,
    val rarity: Rarity,
    val weight: Int,
    val sleepModifier: Double,
    val hpDamageModifier: Int,
    val digModifierRange: ClosedFloatingPointRange<Double>,
    val eatModifierRange: ClosedFloatingPointRange<Double>,
    val learnModifierRange: ClosedFloatingPointRange<Double>,
    val gameModifierRange: ClosedFloatingPointRange<Double>,
    val worshipModifierRange: ClosedFloatingPointRange<Double>,
) {
    LUNA(
        "\uD83C\uDF19",
        "Luna",
        "The moon in the night sky.",
        Rarity.C,
        0,
        1.0,
        1,
        1.0..1.0,
        1.0..1.0,
        1.0..1.0,
        1.0..1.0,
        1.0..1.0,
    ),
    FELINE(
        "\uD83D\uDC08",
        "Feline",
        "You sleep in the night, I'll sleep in the day!",
        Rarity.C,
        30,
        1.5,
        1,
        1.0..1.5,
        1.25..1.5,
        1.0..1.25,
        1.0..1.25,
        2.0..3.0,
    ),
    LUPINE(
        "\uD83D\uDC3A",
        "Lupine",
        "He who rests clears their mind for the day ahead.",
        Rarity.C,
        30,
        1.25,
        1,
        1.25..1.75,
        1.0..2.0,
        1.0..1.25,
        1.25..1.75,
        1.0..1.25,
    ),
    OVINE(
        "\uD83D\uDC11",
        "Ovine",
        "1 sheep, 2 sheep, 3 sheep, 4 sheep...",
        Rarity.R,
        10,
        2.0,
        1,
        1.0..2.0,
        1.5..2.5,
        1.25..2.25,
        1.25..1.5,
        1.25..2.0,
    ),
    FLAMBOYAE(
        "\uD83E\uDDA9",
        "Flamboyae",
        "There is an art in how we sleep.",
        Rarity.R,
        10,
        1.75,
        1,
        1.0..1.75,
        1.5..2.5,
        1.5..1.75,
        2.0..3.0,
        1.25..2.25,
    ),
    DELPHINIDAE(
        "\uD83D\uDC2C",
        "Delphinidae",
        "Asleep and awake at the same time!",
        Rarity.R,
        10,
        2.5,
        1,
        1.0..1.25,
        1.0..1.75,
        2.0..3.5,
        1.5..2.5,
        1.25..2.0,
    ),
    DRACO(
        "\uD83D\uDC09",
        "Draco",
        "The clouds are soft today, little one.",
        Rarity.SR,
        4,
        3.0,
        1,
        1.75..2.5,
        2.25..3.0,
        1.75..2.75,
        1.0..2.0,
        2.0..2.75,
    ),
    GRYPHO(
        "\uD83E\uDD85",
        "Grypho",
        "Have courage, for the sun will return.",
        Rarity.SR,
        4,
        3.0,
        1,
        1.5..2.0,
        2.0..2.75,
        2.25..3.25,
        2.0..3.25,
        1.75..2.25,
    ),
    UNICORN(
        "\uD83E\uDD84",
        "Unicorn",
        "Sometimes, dreams do come true!",
        Rarity.SSR,
        1,
        3.0,
        1,
        2.0..3.75,
        2.25..3.25,
        3.5..4.0,
        2.5..3.5,
        2.0..4.0,
    ),
    NIGHTMARE(
        "\uD83D\uDE08",
        "Nightmare",
        "Some dreams should never come true.",
        Rarity.SSR,
        1,
        4.0,
        2,
        2.25..3.25,
        3.0..4.0,
        3.0..4.5,
        2.5..3.0,
        2.25..3.25,
    ),
    ;

    enum class Rarity(
        val color: Color,
    ) {
        C(Constants.COLOR),
        R(Color(0xc9d0d6)),
        SR(Color(0x6fd1c2)),
        SSR(Color(0xcfabff)),
    }

    val summonRate get() = weight.toDouble() / TOTAL_WEIGHT

    fun fillAngelData(data: AngelData) {
        with(data) {
            digModifier = nextDoubleInclusive(digModifierRange)
            eatModifier = nextDoubleInclusive(eatModifierRange)
            learnModifier = nextDoubleInclusive(learnModifierRange)
            gameModifier = nextDoubleInclusive(gameModifierRange)
            worshipModifier = nextDoubleInclusive(worshipModifierRange)
        }
    }

    companion object {
        val TOTAL_WEIGHT = values().sumOf { it.weight }

        fun summonType(): AngelType {
            val result = Random.Default.nextInt(TOTAL_WEIGHT)
            var accumulatedWeight = 0
            values().forEach {
                accumulatedWeight += it.weight
                if (result < accumulatedWeight) {
                    return it
                }
            }
            throw AssertionError("this should be unreachable (got a random weight greater than TOTAL_WEIGHT)")
        }
    }
}

private fun nextDoubleInclusive(range: ClosedFloatingPointRange<Double>) =
    Random.Default.nextDouble(range.start, range.endInclusive.nextUp())
