package de.artemis.matterworks.common.datagen;

import de.artemis.matterworks.Matterworks;
import net.minecraft.data.PackOutput;

public class ModLanguageProvider extends net.neoforged.neoforge.common.data.LanguageProvider {
    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, Matterworks.MOD_ID, locale);
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.matterworks", "Matterworks");
    }
}
