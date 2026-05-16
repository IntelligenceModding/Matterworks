package de.artemis.matterworks.common.registry;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.menu.MatterAnalyzerMenu;
import de.artemis.matterworks.common.menu.MatterConstructorMenu;
import de.artemis.matterworks.common.menu.MatterGeneratorMenu;
import de.artemis.matterworks.common.menu.MatterRecyclerMenu;
import de.artemis.matterworks.common.menu.MatterSeparatorMenu;
import de.artemis.matterworks.common.menu.MatterStabilizerMenu;
import de.artemis.matterworks.common.menu.PowerCrystalChargerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, Matterworks.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<MatterRecyclerMenu>> MATTER_RECYCLER =
            MENU_TYPES.register("matter_recycler",
                    () -> IMenuTypeExtension.create(MatterRecyclerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MatterStabilizerMenu>> MATTER_STABILIZER =
            MENU_TYPES.register("matter_stabilizer",
                    () -> IMenuTypeExtension.create(MatterStabilizerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MatterAnalyzerMenu>> MATTER_ANALYZER =
            MENU_TYPES.register("matter_analyzer",
                    () -> IMenuTypeExtension.create(MatterAnalyzerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MatterConstructorMenu>> MATTER_CONSTRUCTOR =
            MENU_TYPES.register("matter_constructor",
                    () -> IMenuTypeExtension.create(MatterConstructorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MatterGeneratorMenu>> MATTER_GENERATOR =
            MENU_TYPES.register("matter_generator",
                    () -> IMenuTypeExtension.create(MatterGeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MatterSeparatorMenu>> MATTER_SEPARATOR =
            MENU_TYPES.register("matter_separator",
                    () -> IMenuTypeExtension.create(MatterSeparatorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PowerCrystalChargerMenu>> POWER_CRYSTAL_CHARGER =
            MENU_TYPES.register("power_crystal_charger",
                    () -> IMenuTypeExtension.create(PowerCrystalChargerMenu::new));

    private ModMenuTypes() {
    }

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
