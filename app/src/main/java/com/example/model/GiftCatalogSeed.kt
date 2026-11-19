package com.example.model

data class RechargePackage(
    val id: String,
    val label: String,
    val labelHi: String,
    val amountInr: Long,
    val coinsAmount: Long,
    val bonusLabel: String
)

data class VipPlan(
    val tier: String,
    val title: String,
    val titleHi: String,
    val priceCoins: Long,
    val durationDays: Int,
    val perksEn: List<String>,
    val perksHi: List<String>
)

fun formatIndianCoins(amount: Long): String {
    val negative = amount < 0
    val digits = kotlin.math.abs(amount).toString()
    if (digits.length <= 3) return if (negative) "-$digits" else digits
    val last3 = digits.takeLast(3)
    val rest = digits.dropLast(3)
    val grouped = StringBuilder()
    var count = 0
    for (i in rest.length - 1 downTo 0) {
        grouped.append(rest[i])
        count++
        if (count % 2 == 0 && i > 0) {
            grouped.append(',')
        }
    }
    val prefix = if (negative) "-" else ""
    return "$prefix${grouped.reverse()},$last3"
}

object GiftCatalogSeed {
    val defaultGifts: List<GiftItem> = listOf(
        GiftItem(
            id = "gift_rose",
            name = "Rose",
            nameHi = "गुलाब",
            category = "LOVE",
            priceCoins = 10L,
            diamondValue = 5L,
            animationType = "FLOAT_2D",
            iconKey = "rose",
            active = true
        ),
        GiftItem(
            id = "gift_heart",
            name = "Neon Heart",
            nameHi = "नियॉन दिल",
            category = "LOVE",
            priceCoins = 25L,
            diamondValue = 12L,
            animationType = "FLOAT_2D",
            iconKey = "heart",
            active = true
        ),
        GiftItem(
            id = "gift_teddy",
            name = "Teddy Bear",
            nameHi = "टेडी बियर",
            category = "LOVE",
            priceCoins = 50L,
            diamondValue = 25L,
            animationType = "FLOAT_2D",
            iconKey = "teddy",
            active = true
        ),
        GiftItem(
            id = "gift_peacock_feather",
            name = "Mor Pankh",
            nameHi = "मोर पंख",
            category = "FESTIVAL",
            priceCoins = 99L,
            diamondValue = 50L,
            animationType = "FLOAT_2D",
            iconKey = "peacock",
            active = true
        ),
        GiftItem(
            id = "gift_love_couple",
            name = "Love Couple",
            nameHi = "लव कपल",
            category = "COUPLE",
            priceCoins = 150L,
            diamondValue = 75L,
            animationType = "ORBIT_3D",
            iconKey = "couple",
            active = true
        ),
        GiftItem(
            id = "gift_love_ring",
            name = "Love Ring",
            nameHi = "लव रिंग",
            category = "COUPLE",
            priceCoins = 300L,
            diamondValue = 160L,
            animationType = "ORBIT_3D",
            iconKey = "ring",
            active = true
        ),
        GiftItem(
            id = "gift_shahi_dhol",
            name = "Shahi Dhol",
            nameHi = "शाही ढोल",
            category = "FESTIVAL",
            priceCoins = 499L,
            diamondValue = 260L,
            animationType = "ORBIT_3D",
            iconKey = "dhol",
            active = true
        ),
        GiftItem(
            id = "gift_sports_bike",
            name = "Sports Bike",
            nameHi = "स्पोर्ट्स बाइक",
            category = "LUXURY",
            priceCoins = 500L,
            diamondValue = 280L,
            animationType = "ORBIT_3D",
            iconKey = "bike",
            active = true
        ),
        GiftItem(
            id = "gift_romantic_couple",
            name = "Romantic Couple Show",
            nameHi = "रोमांटिक कपल शो",
            category = "SPECIAL",
            priceCoins = 800L,
            diamondValue = 450L,
            animationType = "FULLSCREEN_ROYAL",
            iconKey = "romantic_couple",
            active = true
        ),
        GiftItem(
            id = "gift_luxury_car",
            name = "Luxury Car",
            nameHi = "लक्ज़री कार",
            category = "LUXURY",
            priceCoins = 1200L,
            diamondValue = 680L,
            animationType = "FULLSCREEN_ROYAL",
            iconKey = "car",
            active = true
        ),
        GiftItem(
            id = "gift_private_jet",
            name = "Private Jet",
            nameHi = "प्राइवेट जेट",
            category = "VIP",
            priceCoins = 2500L,
            diamondValue = 1500L,
            animationType = "FULLSCREEN_ROYAL",
            iconKey = "jet",
            active = true
        ),
        GiftItem(
            id = "gift_diamond_crown",
            name = "Diamond Crown",
            nameHi = "डायमंड क्राउन",
            category = "PREMIUM",
            priceCoins = 3500L,
            diamondValue = 2100L,
            animationType = "FULLSCREEN_ROYAL",
            iconKey = "crown",
            active = true
        ),
        GiftItem(
            id = "gift_royal_palace",
            name = "Royal Palace",
            nameHi = "रॉयल पैलेस",
            category = "ROYAL",
            priceCoins = 5000L,
            diamondValue = 3200L,
            animationType = "FULLSCREEN_ROYAL",
            iconKey = "palace",
            active = true
        ),
        GiftItem(
            id = "gift_dream_palace",
            name = "Dream Palace",
            nameHi = "ड्रीम पैलेस",
            category = "FESTIVAL",
            priceCoins = 8888L,
            diamondValue = 5500L,
            animationType = "FULLSCREEN_ROYAL",
            iconKey = "dream_palace",
            active = true
        ),
        GiftItem(
            id = "gift_rolls_royce",
            name = "Rolls-Royce Phantom",
            nameHi = "रोल्स-रॉयस फैंटम",
            category = "LUXURY",
            priceCoins = 15000L,
            diamondValue = 9500L,
            animationType = "FULLSCREEN_ROYAL",
            iconKey = "rolls_royce",
            active = true
        ),
        GiftItem(
            id = "gift_golden_yacht",
            name = "Golden Yacht",
            nameHi = "गोल्डन यॉट",
            category = "LUXURY",
            priceCoins = 25000L,
            diamondValue = 16000L,
            animationType = "FULLSCREEN_ROYAL",
            iconKey = "yacht",
            active = true
        ),
        GiftItem(
            id = "gift_kohinoor_crown",
            name = "Kohinoor Taj",
            nameHi = "कोहिनूर ताज",
            category = "PREMIUM",
            priceCoins = 50000L,
            diamondValue = 33000L,
            animationType = "FULLSCREEN_ROYAL",
            iconKey = "kohinoor",
            active = true
        ),
        GiftItem(
            id = "gift_shahi_raj_tilak",
            name = "Shahi Raj Tilak",
            nameHi = "शाही राज तिलक",
            category = "ROYAL",
            priceCoins = 100000L,
            diamondValue = 65000L,
            animationType = "FULLSCREEN_ROYAL",
            iconKey = "tilak",
            active = true
        ),
        GiftItem(
            id = "gift_maharaja_rath_2l",
            name = "Maharaja Rath (2L)",
            nameHi = "महाराजा रथ (2 लाख)",
            category = "ROYAL",
            priceCoins = 200000L,
            diamondValue = 135000L,
            animationType = "FULLSCREEN_ROYAL",
            iconKey = "chariot",
            active = true
        ),
        GiftItem(
            id = "gift_golden_phoenix_2l",
            name = "Golden Phoenix (2L)",
            nameHi = "गोल्डन फीनिक्स (2 लाख)",
            category = "VIP",
            priceCoins = 200000L,
            diamondValue = 135000L,
            animationType = "FULLSCREEN_ROYAL",
            iconKey = "phoenix",
            active = true
        ),
        GiftItem(
            id = "gift_raj_tilak_maha_3l",
            name = "Maha Raj Tilak (3L)",
            nameHi = "महा राज तिलक (3 लाख)",
            category = "ROYAL",
            priceCoins = 300000L,
            diamondValue = 205000L,
            animationType = "FULLSCREEN_ROYAL",
            iconKey = "royal_tilak",
            active = true
        ),
        GiftItem(
            id = "gift_cosmic_dragon_3l",
            name = "Cosmic Dragon (3L)",
            nameHi = "कॉस्मिक ड्रैगन (3 लाख)",
            category = "SPECIAL",
            priceCoins = 300000L,
            diamondValue = 205000L,
            animationType = "FULLSCREEN_ROYAL",
            iconKey = "dragon",
            active = true
        ),
        GiftItem(
            id = "gift_emperor_galaxy_5l",
            name = "Emperor Galaxy (5L)",
            nameHi = "सम्राट गैलेक्सी (5 लाख)",
            category = "PREMIUM",
            priceCoins = 500000L,
            diamondValue = 350000L,
            animationType = "FULLSCREEN_ROYAL",
            iconKey = "galaxy",
            active = true
        )
    )

    val rechargePackages: List<RechargePackage> = listOf(
        RechargePackage("pkg_10", "Starter Pack", "स्टार्टर पैक", 10L, 100L, "+0%"),
        RechargePackage("pkg_49", "Popular Pack", "पॉपुलर पैक", 49L, 550L, "+12%"),
        RechargePackage("pkg_199", "Royal Silver", "रॉयल सिल्वर", 199L, 2400L, "+20%"),
        RechargePackage("pkg_499", "Royal Gold", "रॉयल गोल्ड", 499L, 6500L, "+30%"),
        RechargePackage("pkg_999", "VIP Maharaja", "VIP महाराजा", 999L, 14000L, "+40%"),
        RechargePackage("pkg_4999", "SVIP Emperor", "SVIP सम्राट", 4999L, 75000L, "+50%"),
        RechargePackage("pkg_9999", "Lakhpati King (1L)", "लखपति किंग (1 लाख)", 9999L, 100000L, "+52%"),
        RechargePackage("pkg_12999", "Royal Tilak (2L)", "रॉयल तिलक (2 लाख)", 12999L, 200000L, "+54%"),
        RechargePackage("pkg_18999", "Maharaja (3L)", "महाराजा (3 लाख)", 18999L, 300000L, "+58%"),
        RechargePackage("pkg_29999", "Supreme Sultan (5L)", "सुप्रीम सुल्तान (5 लाख)", 29999L, 500000L, "+66%")
    )

    val vipPlans: List<VipPlan> = listOf(
        VipPlan(
            tier = "VIP",
            title = "ADDA VIP Royal",
            titleHi = "ADDA VIP रॉयल",
            priceCoins = 500L,
            durationDays = 30,
            perksEn = listOf(
                "Golden VIP Badge & Royal Profile Frame",
                "Special Neon Gold Name Color",
                "Exclusive VIP Gifts & Entry Banner",
                "+25% Bonus Room & Profile XP"
            ),
            perksHi = listOf(
                "गोल्डन VIP बैज और रॉयल प्रोफ़ाइल फ्रेम",
                "स्पेशल नियॉन गोल्ड नाम रंग",
                "एक्सक्लूसिव VIP गिफ्ट्स और एंट्री बैनर",
                "+25% बोनस रूम और प्रोफ़ाइल XP"
            )
        ),
        VipPlan(
            tier = "SVIP",
            title = "ADDA SVIP Supreme",
            titleHi = "ADDA SVIP सुप्रीम",
            priceCoins = 1500L,
            durationDays = 30,
            perksEn = listOf(
                "Animated SVIP Phoenix Badge & Frame",
                "Full-Screen Royal Room Entry Animation",
                "Neon Pink/Cyan Name Glow + Exclusive Backgrounds",
                "Priority Seat & +50% XP Boost"
            ),
            perksHi = listOf(
                "एनिमेटेड SVIP फीनिक्स बैज और फ्रेम",
                "फुल-स्क्रीन रॉयल रूम एंट्री एनिमेशन",
                "नियॉन पिंक/सियान नाम चमक + प्रीमियम बैकग्राउंड",
                "प्रायोरिटी सीट और +50% XP बूस्ट"
            )
        )
    )
}
