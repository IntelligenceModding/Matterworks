package de.artemis.matterworks.common.blockentity;

import net.minecraft.network.chat.Component;

public interface CustomNamedBlockEntity {
    Component getDisplayName();

    String getCustomNameText();

    void setCustomNameText(String customName);
}
