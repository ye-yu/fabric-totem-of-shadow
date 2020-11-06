package fp.yeyu.tos.mixins;

import fp.yeyu.tos.TotemOfShadowEntry;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.sound.BiomeMoodSound;
import net.minecraft.world.biome.*;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.ConfiguredFeatures;
import net.minecraft.world.gen.feature.DefaultBiomeFeatures;
import net.minecraft.world.gen.surfacebuilder.ConfiguredSurfaceBuilders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DefaultBiomeCreator.class)
public class DefaultBiomeCreatorMixin {
    private static final SpawnSettings.SpawnEntry spiritSpawnEntry = new SpawnSettings.SpawnEntry(TotemOfShadowEntry.INSTANCE.getSpiritEntity(), 1, 1, 2);

    @Inject(method = "createSmallEndIslands", at = @At("HEAD"), cancellable = true)
    private static void onCreateSmallEndIslandsFeatures(CallbackInfoReturnable<Biome> cir) {
        GenerationSettings.Builder builder = (new GenerationSettings.Builder()).surfaceBuilder(ConfiguredSurfaceBuilders.END).feature(GenerationStep.Feature.RAW_GENERATION, ConfiguredFeatures.END_ISLAND_DECORATED);
        SpawnSettings.Builder spawnSettings = new SpawnSettings.Builder();
        DefaultBiomeFeatures.addEndMobs(spawnSettings);
        spawnSettings.spawn(SpawnGroup.MONSTER, spiritSpawnEntry);
        final Biome smallEndIsland = (new Biome.Builder())
                .precipitation(Biome.Precipitation.NONE)
                .category(Biome.Category.THEEND)
                .depth(0.1F)
                .scale(0.2F)
                .temperature(0.5F)
                .downfall(0.5F)
                .effects((new BiomeEffects.Builder())
                        .waterColor(0x3f76e4)
                        .waterFogColor(0x050533)
                        .fogColor(0xa080a0)
                        .skyColor(0x0a160d)
                        .moodSound(BiomeMoodSound.CAVE).build())
                .spawnSettings(spawnSettings.build())
                .generationSettings(builder.build())
                .build();

        cir.setReturnValue(smallEndIsland);
    }
}
