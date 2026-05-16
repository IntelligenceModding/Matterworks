package de.artemis.matterworks.common.energy;

import de.artemis.matterworks.common.item.MatterPowerBankItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public final class EnergyItemHelper {
    private EnergyItemHelper() {
    }

    public static @Nullable IEnergyStorage getEnergyStorage(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getCapability(Capabilities.EnergyStorage.ITEM);
    }

    public static boolean canProvideEnergy(ItemStack stack) {
        IEnergyStorage storage = getEnergyStorage(stack);
        return storage != null && storage.extractEnergy(1, true) > 0;
    }

    public static boolean isPowerBank(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof MatterPowerBankItem;
    }

    public static boolean canReceiveEnergy(ItemStack stack) {
        IEnergyStorage storage = getEnergyStorage(stack);
        return storage != null && storage.receiveEnergy(1, true) > 0;
    }

    public static boolean isDepleted(ItemStack stack) {
        IEnergyStorage storage = getEnergyStorage(stack);
        return storage != null && storage.getEnergyStored() <= 0;
    }

    public static int transferEnergy(IEnergyStorage source, IEnergyStorage target, int maxTransfer) {
        if (maxTransfer <= 0) {
            return 0;
        }

        int extractable = source.extractEnergy(maxTransfer, true);
        if (extractable <= 0) {
            return 0;
        }

        int receivable = target.receiveEnergy(extractable, true);
        if (receivable <= 0) {
            return 0;
        }

        int extracted = source.extractEnergy(receivable, false);
        if (extracted <= 0) {
            return 0;
        }

        return target.receiveEnergy(extracted, false);
    }

    public static int chargePlayerEquipment(ItemStack sourceStack, Player player, int maxTransferPerTick) {
        IEnergyStorage sourceStorage = getEnergyStorage(sourceStack);
        if (sourceStorage == null || maxTransferPerTick <= 0) {
            return 0;
        }

        int transferred = 0;
        transferred += chargeList(sourceStack, sourceStorage, player.getInventory().items, maxTransferPerTick - transferred);
        transferred += chargeList(sourceStack, sourceStorage, player.getInventory().armor, maxTransferPerTick - transferred);
        transferred += chargeList(sourceStack, sourceStorage, player.getInventory().offhand, maxTransferPerTick - transferred);

        if (transferred > 0) {
            player.getInventory().setChanged();
        }
        return transferred;
    }

    private static int chargeList(ItemStack sourceStack, IEnergyStorage sourceStorage, NonNullList<ItemStack> stacks, int remainingTransfer) {
        if (remainingTransfer <= 0) {
            return 0;
        }

        int transferred = 0;
        for (ItemStack targetStack : stacks) {
            if (remainingTransfer - transferred <= 0) {
                break;
            }
            if (targetStack.isEmpty() || targetStack == sourceStack || isPowerBank(targetStack)) {
                continue;
            }

            IEnergyStorage targetStorage = getEnergyStorage(targetStack);
            if (targetStorage == null) {
                continue;
            }

            transferred += transferEnergy(sourceStorage, targetStorage, remainingTransfer - transferred);
        }
        return transferred;
    }
}
