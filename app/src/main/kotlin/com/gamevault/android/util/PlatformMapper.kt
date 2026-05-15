package com.gamevault.android.util

import com.gamevault.android.R

/**
 * Maps GameVault server platform IDs to EmulationStation / EmuDeck folder names.
 * The user's roms root is expected to follow the standard ES directory layout.
 */
object PlatformMapper {
    private val map = mapOf(
        "switch"      to "switch",
        "ps2"         to "ps2",
        "ps3"         to "ps3",
        "psx"         to "psx",
        "psp"         to "psp",
        "psvita"      to "psvita",
        "gamecube"    to "gc",
        "wii"         to "wii",
        "wiiu"        to "wiiu",
        "3ds"         to "n3ds",
        "ds"          to "nds",
        "n64"         to "n64",
        "nes"         to "nes",
        "snes"        to "snes",
        "gb"          to "gb",
        "gba"         to "gba",
        "gbc"         to "gbc",
        "genesis"     to "genesis",
        "megadrive"   to "megadrive",
        "sms"         to "mastersystem",
        "gamegear"    to "gamegear",
        "sega32x"     to "sega32x",
        "segacd"      to "segacd",
        "saturn"      to "saturn",
        "dreamcast"   to "dreamcast",
        "tg16"        to "tg16",
        "atari2600"   to "atari2600",
        "atari7800"   to "atari7800",
        "jag"         to "atarijaguar",
        "lynx"        to "atarilynx",
        "neogeo"      to "neogeo",
        "arcade"      to "arcade",
        "mame"        to "mame",
        "pc"          to "pc",
    )

    fun getFolderName(platformId: String): String = map[platformId] ?: platformId

    fun allFolderNames(): List<String> = map.values.distinct().sorted()

    fun getDrawableRes(platformId: String): Int? = when (platformId) {
        "gba"              -> R.drawable.ic_platform_gba
        "ps2"              -> R.drawable.ic_platform_ps2
        "ps3"              -> R.drawable.ic_platform_ps3
        "psx"              -> R.drawable.ic_platform_psx
        "psp"              -> R.drawable.ic_platform_psp
        "gamecube"         -> R.drawable.ic_platform_gamecube
        "wii"              -> R.drawable.ic_platform_wii
        "wiiu"             -> R.drawable.ic_platform_wiiu
        "3ds"              -> R.drawable.ic_platform_3ds
        "ds"               -> R.drawable.ic_platform_ds
        "n64"              -> R.drawable.ic_platform_n64
        "nes"              -> R.drawable.ic_platform_nes
        "snes"             -> R.drawable.ic_platform_snes
        "gb"               -> R.drawable.ic_platform_gb
        "gbc"              -> R.drawable.ic_platform_gbc
        "genesis", "megadrive" -> R.drawable.ic_platform_genesis
        "sms"              -> R.drawable.ic_platform_sms
        "gamegear"         -> R.drawable.ic_platform_gamegear
        "switch"           -> R.drawable.ic_platform_switch
        "atari2600"        -> R.drawable.ic_platform_atari2600
        "atari7800"        -> R.drawable.ic_platform_atari7800
        "jag"              -> R.drawable.ic_platform_jag
        "lynx"             -> R.drawable.ic_platform_lynx
        "tg16"             -> R.drawable.ic_platform_tg16
        "sega32x"          -> R.drawable.ic_platform_sega32x
        else               -> null
    }
}
