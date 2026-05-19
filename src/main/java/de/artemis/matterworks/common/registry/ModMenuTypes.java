package de.artemis.matterworks.common.registry;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.menu.MatterAnalyzerMenu;
import de.artemis.matterworks.common.menu.MatterBatteryCoreMenu;
import de.artemis.matterworks.common.menu.MatterBatteryPreviewMenu;
import de.artemis.matterworks.common.menu.MatterConstructorMenu;
import de.artemis.matterworks.common.menu.EnergyCellMenu;
import de.artemis.matterworks.common.menu.MatterFilterMenu;
import de.artemis.matterworks.common.menu.MatterFluidTankMenu;
import de.artemis.matterworks.common.menu.MatterGeneratorMenu;
import de.artemis.matterworks.common.menu.MatterNetworkControllerMenu;
import de.artemis.matterworks.common.menu.MatterNetworkMonitorMenu;
import de.artemis.matterworks.common.menu.MatterPylonMenu;
import de.artemis.matterworks.common.menu.MatterRecyclerMenu;
import de.artemis.matterworks.common.menu.MatterSeparatorMenu;
import de.artemis.matterworks.common.menu.MatterStorageBarrelMenu;
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

    public static final DeferredHolder<MenuType<?>, MenuType<EnergyCellMenu>> MATTER_ENERGY_CELL =
            MENU_TYPES.register("matter_energy_cell",
                    () -> IMenuTypeExtension.create(EnergyCellMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MatterBatteryCoreMenu>> MATTER_BATTERY_CORE =
            MENU_TYPES.register("matter_battery_core",
                    () -> IMenuTypeExtension.create(MatterBatteryCoreMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MatterBatteryPreviewMenu>> MATTER_BATTERY_PREVIEW =
            MENU_TYPES.register("matter_battery_preview",
                    () -> IMenuTypeExtension.create(MatterBatteryPreviewMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MatterFluidTankMenu>> MATTER_FLUID_TANK =
            MENU_TYPES.register("matter_fluid_tank",
                    () -> IMenuTypeExtension.create(MatterFluidTankMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MatterStorageBarrelMenu>> MATTER_STORAGE_BARREL =
            MENU_TYPES.register("matter_storage_barrel",
                    () -> IMenuTypeExtension.create(MatterStorageBarrelMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MatterPylonMenu>> MATTER_PYLON =
            MENU_TYPES.register("matter_pylon",
                    () -> IMenuTypeExtension.create(MatterPylonMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MatterNetworkControllerMenu>> MATTER_NETWORK_CONTROLLER =
            MENU_TYPES.register("matter_network_controller",
                    () -> IMenuTypeExtension.create(MatterNetworkControllerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MatterNetworkMonitorMenu>> MATTER_NETWORK_MONITOR =
            MENU_TYPES.register("matter_network_monitor",
                    () -> IMenuTypeExtension.create(MatterNetworkMonitorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MatterFilterMenu>> MATTER_FILTER =
            MENU_TYPES.register("matter_filter",
                    () -> IMenuTypeExtension.create(MatterFilterMenu::new));

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
