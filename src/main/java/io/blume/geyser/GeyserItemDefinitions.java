package io.blume.geyser;

import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.util.CreativeCategory;
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class GeyserItemDefinitions {

    public static final Identifier WHEAT_SEEDS = Identifier.of("minecraft", "wheat_seeds");

    public record SeedDefinition(
        @NotNull String displayName,
        @NotNull Identifier bedrockId,
        @NotNull Identifier modelId,
        @NotNull String icon
    ) {
    }

    public static final List<SeedDefinition> SEEDS = List.of(
        new SeedDefinition(
            "Carrot Seeds",
            Identifier.of("blume", "carrot_seeds"),
            Identifier.of("blume", "carrot_seeds"),
            "blume:carrot_seeds"
        ),
        new SeedDefinition(
            "Potato Seeds",
            Identifier.of("blume", "potato_seeds"),
            Identifier.of("blume", "potato_seeds"),
            "blume:potato_seeds"
        ),
        new SeedDefinition(
            "Mixed Seeds",
            Identifier.of("blume", "mixed_seeds"),
            Identifier.of("blume", "mixed_seeds"),
            "blume:mixed_seeds"
        )
    );

    private GeyserItemDefinitions() {
    }

    public static @NotNull CustomItemDefinition toDefinition(@NotNull SeedDefinition seed) {
        return CustomItemDefinition.builder(seed.bedrockId(), seed.modelId())
            .displayName(seed.displayName())
            .bedrockOptions(CustomItemBedrockOptions.builder()
                .icon(seed.icon())
                .creativeCategory(CreativeCategory.NATURE))
            .build();
    }
}
