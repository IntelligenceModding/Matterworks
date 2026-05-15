package de.artemis.matterworks.common.registry;

import de.artemis.matterworks.Matterworks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Arrays;
import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Matterworks.MOD_ID);

    @SuppressWarnings("unused")
    public static final Supplier<CreativeModeTab> MATTERWORKS_CREATIVE_TAB = CREATIVE_MODE_TAB.register("matterworks_creative_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> Blocks.OAK_PLANKS.asItem().getDefaultInstance())
                    .title(Component.translatable("itemGroup.matterworks"))
                    .displayItems((itemDisplayParameters, output) -> Arrays.stream(new Item[]{
                            Items.OAK_PLANKS
                    }).forEach(output::accept))
                    .build());

    private ModCreativeModeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
