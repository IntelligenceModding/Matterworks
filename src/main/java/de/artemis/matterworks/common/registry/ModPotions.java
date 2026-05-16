package de.artemis.matterworks.common.registry;

import de.artemis.matterworks.Matterworks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, Matterworks.MOD_ID);

    public static final DeferredHolder<Potion, Potion> MOLECULAR_DISPLACEMENT =
            POTIONS.register("molecular_displacement",
                    () -> new Potion(new MobEffectInstance(ModMobEffects.MOLECULAR_DISPLACEMENT, 600, 0)));

    public static final DeferredHolder<Potion, Potion> LONG_MOLECULAR_DISPLACEMENT =
            POTIONS.register("long_molecular_displacement",
                    () -> new Potion("molecular_displacement", new MobEffectInstance(ModMobEffects.MOLECULAR_DISPLACEMENT, 1200, 0)));

    public static final DeferredHolder<Potion, Potion> STRONG_MOLECULAR_DISPLACEMENT =
            POTIONS.register("strong_molecular_displacement",
                    () -> new Potion("molecular_displacement", new MobEffectInstance(ModMobEffects.MOLECULAR_DISPLACEMENT, 400, 1)));

    public static final DeferredHolder<Potion, Potion> VOLATILE_MOLECULAR_DISPLACEMENT =
            POTIONS.register("volatile_molecular_displacement",
                    () -> new Potion("molecular_displacement", new MobEffectInstance(ModMobEffects.MOLECULAR_DISPLACEMENT, 240, 2)));

    private ModPotions() {
    }

    public static void register(IEventBus eventBus) {
        POTIONS.register(eventBus);
        NeoForge.EVENT_BUS.addListener(ModPotions::registerBrewingRecipes);
    }

    private static void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
        var builder = event.getBuilder();
        builder.addMix(Potions.AWKWARD, ModItems.MATTER_DUST.get(), MOLECULAR_DISPLACEMENT);
        builder.addMix(MOLECULAR_DISPLACEMENT, Items.REDSTONE, LONG_MOLECULAR_DISPLACEMENT);
        builder.addMix(MOLECULAR_DISPLACEMENT, Items.GLOWSTONE_DUST, STRONG_MOLECULAR_DISPLACEMENT);
        builder.addMix(STRONG_MOLECULAR_DISPLACEMENT, ModItems.CRIMSON_POWER_CRYSTAL.get(), VOLATILE_MOLECULAR_DISPLACEMENT);
    }
}
