package com.gamevault.android.util

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
}
