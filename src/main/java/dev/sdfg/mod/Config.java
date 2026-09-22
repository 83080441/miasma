package dev.sdfg.mod;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    static {
        BUILDER.push("warpGeneration");
    }

    public static final ModConfigSpec.BooleanValue WARP_GEN_ENABLED = BUILDER
            .comment("If true, sparse Warps spawn on the Overworld surface during world generation.")
            .define("enabled", true);

    public static final ModConfigSpec.IntValue WARP_GEN_REGION_SIZE = BUILDER
            .comment("Region cell size in blocks. One Warp candidate per cell (default 384).")
            .defineInRange("regionSize", 384, 32, 4096);

    public static final ModConfigSpec.IntValue WARP_GEN_CHANCE_PERMILLE = BUILDER
            .comment("Chance out of 10000 that a region cell contains a Warp (180 = 18%).")
            .defineInRange("chancePermille", 180, 0, 10000);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(Identifier.parse(itemName));
    }
}
