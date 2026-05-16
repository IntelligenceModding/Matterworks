package de.artemis.matterworks.common.datagen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModItems;
import net.minecraft.data.PackOutput;

public class ModLanguageProvider extends net.neoforged.neoforge.common.data.LanguageProvider {
    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, Matterworks.MOD_ID, locale);
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.matterworks", "Matterworks");
        add("tooltip.matterworks.stored_matter", "Stored Matter: %s");
        add("tooltip.matterworks.energy", "Energy: %s / %s FE");
        add("tooltip.matterworks.progress", "Progress: %s / %s");
        add("tooltip.matterworks.raw_matter_tank", "Raw Matter: %s / %s mB");
        add("tooltip.matterworks.refined_matter_tank", "Refined Matter: %s / %s mB");
        add("tooltip.matterworks.matter_sludge_tank", "Matter Sludge: %s / %s mB");
        add("tooltip.matterworks.unstable_matter_tank", "Unstable Matter: %s / %s mB");
        add("effect.matterworks.molecular_displacement", "Molecular Displacement");
        add("item.minecraft.potion.effect.molecular_displacement", "Potion of Molecular Displacement");
        add("item.minecraft.splash_potion.effect.molecular_displacement", "Splash Potion of Molecular Displacement");
        add("item.minecraft.lingering_potion.effect.molecular_displacement", "Lingering Potion of Molecular Displacement");
        add("item.minecraft.tipped_arrow.effect.molecular_displacement", "Arrow of Molecular Displacement");
        add("tooltip.matterworks.constructor_matter", "Matter Dust: %s / %s");
        add("tooltip.matterworks.matter_power_bank", "Automatically charges FE items in inventory, hotbar, and armor.");
        add("tooltip.matterworks.matter_power_bank.auto_charge_enabled", "Auto-Charge: Enabled");
        add("tooltip.matterworks.matter_power_bank.auto_charge_disabled", "Auto-Charge: Disabled");
        add("tooltip.matterworks.matter_power_bank.toggle", "Right-click to toggle auto-charge.");
        add("enchantment.matterworks.crystal_tuning", "Crystal Tuning");
        add("tooltip.matterworks.crystal_tuning_book", "Improves the charge of crystals mined from Power Crystal Ore.");
        add("tooltip.matterworks.encoded_with", "Encoded With: %s");
        add("tooltip.matterworks.pattern_progress", "Pattern Progress: %s / %s");
        add("tooltip.matterworks.power_crystal_charge", "Charge: %s / %s");
        add("tooltip.matterworks.crimson_power_crystal", "Speeds machine processing but increases FE use while charged.");
        add("tooltip.matterworks.azure_power_crystal", "Prevents machine process failure while charged.");
        add("tooltip.matterworks.verdant_power_crystal", "Reduces FE use and expands internal FE storage while charged.");

        add("fluid_type.matterworks.raw_matter", "Raw Matter");
        add("fluid_type.matterworks.refined_matter", "Refined Matter");
        add("fluid_type.matterworks.matter_sludge", "Matter Sludge");
        add("fluid_type.matterworks.unstable_matter", "Unstable Matter");
        addItem(ModItems.RAW_MATTER_BUCKET, "Raw Matter Bucket");
        addItem(ModItems.REFINED_MATTER, "Refined Matter Bucket");
        addItem(ModItems.MATTER_SLUDGE, "Matter Sludge Bucket");
        addItem(ModItems.UNSTABLE_MATTER, "Unstable Matter Bucket");
        addItem(ModItems.MATTER_DUST, "Matter Dust");
        addItem(ModItems.EMPTY_TEMPLATE, "Empty Template");
        addItem(ModItems.ENCODED_TEMPLATE, "Encoded Template");
        addItem(ModItems.CRIMSON_POWER_CRYSTAL, "Crimson Power Crystal");
        addItem(ModItems.AZURE_POWER_CRYSTAL, "Azure Power Crystal");
        addItem(ModItems.VERDANT_POWER_CRYSTAL, "Verdant Power Crystal");
        addItem(ModItems.MATTER_POWER_BANK, "Matter Power Bank");
        add("item.matterworks.encoded_template.filled", "Encoded Template (%s)");
        add("message.matterworks.matter_power_bank.auto_charge_enabled", "Matter Power Bank auto-charge enabled.");
        add("message.matterworks.matter_power_bank.auto_charge_disabled", "Matter Power Bank auto-charge disabled.");

        addBlock(ModBlocks.MATTER_RECYCLER, "Matter Recycler");
        addBlock(ModBlocks.MATTER_STABILIZER, "Matter Stabilizer");
        addBlock(ModBlocks.MATTER_ANALYZER, "Matter Analyzer");
        addBlock(ModBlocks.MATTER_CONSTRUCTOR, "Matter Constructor");
        addBlock(ModBlocks.MATTER_GENERATOR, "Matter Generator");
        addBlock(ModBlocks.MATTER_SEPARATOR, "Matter Separator");
        addBlock(ModBlocks.HARDENED_SLUDGE, "Hardened Sludge");
        addBlock(ModBlocks.POWER_CRYSTAL_CHARGER, "Power Crystal Charger");
        addBlock(ModBlocks.POWER_CRYSTAL_ORE, "Power Crystal Ore");
    }
}
