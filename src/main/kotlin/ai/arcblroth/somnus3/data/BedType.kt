package ai.arcblroth.somnus3.data

enum class BedType(
    val uiName: String,
    val description: String,
    val perks: String?,
    val cost: Int,
) {
    FLOOR("Floor", "The cold, hard ground.", null, 0),
    STRAW("Straw Bedding", "Perfect for camping.", "+5 Furry", 100),
    POLY("Polyester Bed", "Warning: not fire resistant!", "+2 Gaming", 200),
    COTTON("Cotton Bed", "Comfy and cool.", "+3 Test Taking Skills", 300),
    SILK("Silk Bed", "Fit for an emperor.", "+4 Heavenly Connections", 400),
    GOLD("Golden Hoard", "Faintly hums with draconic power.", "+5 Power", 500),
    DEMON("Roost of Sloth", "You feel drowsy just looking at it...", "+6 Power", 666),
}
