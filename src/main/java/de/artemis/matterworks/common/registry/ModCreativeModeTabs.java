package de.artemis.matterworks.common.registry;

import de.artemis.matterworks.Matterworks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Matterworks.MOD_ID);

    @SuppressWarnings("unused")
    public static final Supplier<CreativeModeTab> MATTERWORKS_CREATIVE_TAB = CREATIVE_MODE_TAB.register("matterworks_creative_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> ModBlocks.MATTER_RECYCLER.get().asItem().getDefaultInstance())
                    .title(Component.translatable("itemGroup.matterworks"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.RAW_MATTER_BUCKET.get());
                        output.accept(ModItems.REFINED_MATTER_BUCKET.get());
                        output.accept(ModItems.MATTER_SLUDGE_BUCKET.get());
                        output.accept(ModItems.UNSTABLE_MATTER_BUCKET.get());
                        output.accept(ModItems.MATTER_DUST.get());
                        output.accept(ModItems.ENTROPIC_MATTER.get());
                        output.accept(ModItems.EMPTY_TEMPLATE.get());
                        output.accept(ModItems.MATTER_ITEM_FILTER.get());
                        output.accept(ModItems.MATTER_FLUID_FILTER.get());
                        output.accept(ModItems.ENCODED_TEMPLATE.get());
                        output.accept(ModItems.CRIMSON_POWER_CRYSTAL.get());
                        output.accept(ModItems.AZURE_POWER_CRYSTAL.get());
                        output.accept(ModItems.VERDANT_POWER_CRYSTAL.get());
                        output.accept(ModItems.MATTER_POWER_BANK.get().createChargedStack());
                        addPotionVariants(output, ModPotions.MOLECULAR_DISPLACEMENT);
                        addPotionVariants(output, ModPotions.LONG_MOLECULAR_DISPLACEMENT);
                        addPotionVariants(output, ModPotions.STRONG_MOLECULAR_DISPLACEMENT);
                        addPotionVariants(output, ModPotions.VOLATILE_MOLECULAR_DISPLACEMENT);
                        output.accept(createCrystalTuningBook(itemDisplayParameters.holders(), 1));
                        output.accept(createCrystalTuningBook(itemDisplayParameters.holders(), 2));
                        output.accept(createCrystalTuningBook(itemDisplayParameters.holders(), 3));
                        output.accept(ModBlocks.MATTER_RECYCLER.get());
                        output.accept(ModBlocks.MATTER_STABILIZER.get());
                        output.accept(ModBlocks.MATTER_ANALYZER.get());
                        output.accept(ModBlocks.MATTER_CONSTRUCTOR.get());
                        output.accept(ModBlocks.MATTER_GENERATOR.get());
                        output.accept(ModBlocks.MATTER_ENERGY_CELL.get());
                        output.accept(ModBlocks.MATTER_FLUID_TANK.get());
                        output.accept(ModBlocks.MATTER_STORAGE_BARREL.get());
                        output.accept(ModBlocks.MATTER_PYLON.get());
                        output.accept(ModBlocks.MATTER_NETWORK_CONTROLLER.get());
                        output.accept(ModBlocks.MATTER_NETWORK_MONITOR.get());
                        output.accept(ModBlocks.CREATIVE_SOURCE.get());
                        output.accept(ModBlocks.CREATIVE_SINK.get());
                        output.accept(ModBlocks.MATTER_SEPARATOR.get());
                        output.accept(ModBlocks.HARDENED_SLUDGE.get());
                        output.accept(ModBlocks.POWER_CRYSTAL_CHARGER.get());
                        output.accept(ModBlocks.POWER_CRYSTAL_ORE.get());
                    })
                    .build());

    private ModCreativeModeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }

    private static void addPotionVariants(CreativeModeTab.Output output, net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.alchemy.Potion, net.minecraft.world.item.alchemy.Potion> potion) {
        output.accept(PotionContents.createItemStack(Items.POTION, potion));
        output.accept(PotionContents.createItemStack(Items.SPLASH_POTION, potion));
        output.accept(PotionContents.createItemStack(Items.LINGERING_POTION, potion));
        output.accept(PotionContents.createItemStack(Items.TIPPED_ARROW, potion));
    }

    private static ItemStack createCrystalTuningBook(HolderLookup.Provider holders, int level) {
        ItemStack stack = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(
                holders.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModEnchantments.CRYSTAL_TUNING),
                level
        ));
        stack.set(
                DataComponents.LORE,
                ItemLore.EMPTY.withLineAdded(
                        Component.translatable("tooltip.matterworks.crystal_tuning_book")
                                .withStyle(ChatFormatting.GRAY)
                )
        );
        return stack;
    }
}
