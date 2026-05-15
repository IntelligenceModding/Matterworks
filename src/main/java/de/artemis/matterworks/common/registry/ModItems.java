package de.artemis.matterworks.common.registry;

import de.artemis.matterworks.Matterworks;
import net.minecraft.world.item.Item;
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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
