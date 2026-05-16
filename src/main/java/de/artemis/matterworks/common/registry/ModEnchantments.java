package de.artemis.matterworks.common.registry;

import de.artemis.matterworks.Matterworks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public final class ModEnchantments {
    public static final ResourceKey<Enchantment> CRYSTAL_TUNING = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "crystal_tuning")
    );

    private ModEnchantments() {
    }
}
