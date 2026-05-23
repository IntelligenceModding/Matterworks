package de.artemis.matterworks.common.registry;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.item.EncodedTemplateItem;
import de.artemis.matterworks.common.item.MatterFilterItem;
import de.artemis.matterworks.common.item.NetworkDataCardItem;
import de.artemis.matterworks.common.item.NetworkRemoteTerminalItem;
import de.artemis.matterworks.common.item.MatterPowerBankItem;
import de.artemis.matterworks.common.item.PowerCrystalItem;
import de.artemis.matterworks.common.item.UnstableMatterBucketItem;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Matterworks.MOD_ID);

    private static <T extends Item> DeferredItem<T> register(String name, Function<Item.Properties, T> itemFactory, UnaryOperator<Item.Properties> properties) {
        return ITEMS.registerItem(name, itemFactory, properties.apply(new Item.Properties()));
    }

    public static final DeferredItem<BucketItem> RAW_MATTER_BUCKET = register(
            "raw_matter_bucket",
            properties -> new BucketItem(ModFluids.RAW_MATTER.get(), properties.craftRemainder(Items.BUCKET).stacksTo(1)),
            UnaryOperator.identity()
    );

    public static final DeferredItem<BucketItem> REFINED_MATTER_BUCKET = register(
            "refined_matter_bucket",
            properties -> new BucketItem(ModFluids.REFINED_MATTER.get(), properties.craftRemainder(Items.BUCKET).stacksTo(1)),
            UnaryOperator.identity()
    );

    public static final DeferredItem<BucketItem> UNSTABLE_MATTER_BUCKET = register(
            "unstable_matter_bucket",
            properties -> new UnstableMatterBucketItem(ModFluids.UNSTABLE_MATTER.get(), properties.stacksTo(1).rarity(Rarity.UNCOMMON)),
            UnaryOperator.identity()
    );

    public static final DeferredItem<BucketItem> MATTER_SLUDGE_BUCKET = register(
            "matter_sludge_bucket",
            properties -> new BucketItem(ModFluids.MATTER_SLUDGE.get(), properties.craftRemainder(Items.BUCKET).stacksTo(1)),
            UnaryOperator.identity()
    );

    public static final DeferredItem<Item> MATTER_DUST = register(
            "matter_dust",
            Item::new,
            UnaryOperator.identity()
    );

    public static final DeferredItem<Item> MATTER_SINGULARITY = register(
            "matter_singularity",
            Item::new,
            properties -> properties.rarity(Rarity.RARE)
    );

    public static final DeferredItem<Item> ENTROPIC_MATTER = register(
            "entropic_matter",
            Item::new,
            properties -> properties.rarity(Rarity.UNCOMMON)
    );

    public static final DeferredItem<Item> STABILIZED_MATTER_PLATE = register(
            "stabilized_matter_plate",
            Item::new,
            UnaryOperator.identity()
    );

    public static final DeferredItem<Item> CONDUCTIVE_MATTER_COIL = register(
            "conductive_matter_coil",
            Item::new,
            properties -> properties.rarity(Rarity.UNCOMMON)
    );

    public static final DeferredItem<Item> VERDANT_CRYSTAL_LATTICE = register(
            "verdant_crystal_lattice",
            Item::new,
            properties -> properties.rarity(Rarity.UNCOMMON)
    );

    public static final DeferredItem<Item> CRIMSON_FLUX_COIL = register(
            "crimson_flux_coil",
            Item::new,
            properties -> properties.rarity(Rarity.UNCOMMON)
    );

    public static final DeferredItem<Item> MULTIBLOCK_GLASS_PANE = register(
            "multiblock_glass_pane",
            Item::new,
            UnaryOperator.identity()
    );

    public static final DeferredItem<Item> BATTERY_CORE_ASSEMBLY = register(
            "battery_core_assembly",
            Item::new,
            properties -> properties.rarity(Rarity.RARE)
    );

    public static final DeferredItem<Item> EMPTY_TEMPLATE = register(
            "empty_template",
            Item::new,
            UnaryOperator.identity()
    );

    public static final DeferredItem<MatterFilterItem> MATTER_ITEM_FILTER = register(
            "matter_item_filter",
            properties -> new MatterFilterItem(properties, false),
            properties -> properties.rarity(Rarity.UNCOMMON)
    );

    public static final DeferredItem<MatterFilterItem> MATTER_FLUID_FILTER = register(
            "matter_fluid_filter",
            properties -> new MatterFilterItem(properties, true),
            properties -> properties.rarity(Rarity.UNCOMMON)
    );

    public static final DeferredItem<EncodedTemplateItem> ENCODED_TEMPLATE = register(
            "encoded_template",
            EncodedTemplateItem::new,
            properties -> properties.rarity(Rarity.UNCOMMON)
    );

    public static final DeferredItem<NetworkDataCardItem> NETWORK_DATA_CARD = register(
            "network_data_card",
            NetworkDataCardItem::new,
            properties -> properties.rarity(Rarity.UNCOMMON).stacksTo(1)
    );

    public static final DeferredItem<NetworkRemoteTerminalItem> NETWORK_REMOTE_TERMINAL = register(
            "network_remote_terminal",
            NetworkRemoteTerminalItem::new,
            properties -> properties.rarity(Rarity.UNCOMMON).stacksTo(1)
    );

    public static final DeferredItem<PowerCrystalItem> CRIMSON_POWER_CRYSTAL = register(
            "crimson_power_crystal",
            properties -> new PowerCrystalItem(properties.rarity(Rarity.UNCOMMON), ChatFormatting.RED, "tooltip.matterworks.crimson_power_crystal"),
            UnaryOperator.identity()
    );

    public static final DeferredItem<PowerCrystalItem> AZURE_POWER_CRYSTAL = register(
            "azure_power_crystal",
            properties -> new PowerCrystalItem(properties.rarity(Rarity.UNCOMMON), ChatFormatting.AQUA, "tooltip.matterworks.azure_power_crystal"),
            UnaryOperator.identity()
    );

    public static final DeferredItem<PowerCrystalItem> VERDANT_POWER_CRYSTAL = register(
            "verdant_power_crystal",
            properties -> new PowerCrystalItem(properties.rarity(Rarity.UNCOMMON), ChatFormatting.GREEN, "tooltip.matterworks.verdant_power_crystal"),
            UnaryOperator.identity()
    );

    public static final DeferredItem<MatterPowerBankItem> MATTER_POWER_BANK = register(
            "matter_power_bank",
            MatterPowerBankItem::new,
            properties -> properties.rarity(Rarity.UNCOMMON)
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
