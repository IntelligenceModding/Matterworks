package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.block.PowerCrystalRevealBlock;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModEnchantments;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.upgrade.PowerCrystalData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;
import org.joml.Vector3f;

public class PowerCrystalOreBlockEntity extends BlockEntity {
    public static final int REVEAL_DURATION = 100;
    private static final double PREVIEW_CENTER_Y = 0.35D;
    private static final double PREVIEW_ITEM_Y = 0.22D;
    private static final String REVEAL_TICKS_KEY = "RevealTicksRemaining";
    private static final String REWARD_INDEX_KEY = "RewardIndex";
    private static final String REWARD_CHARGE_KEY = "RewardChargePercent";
    private static final String PREVIEW_OFFSET_INDEX_KEY = "PreviewOffsetIndex";
    private static final String PREVIEW_ENTITY_UUID_KEY = "PreviewEntityUuid";

    private int revealTicksRemaining;
    private int rewardIndex = -1;
    private int rewardChargePercent;
    private int previewOffsetIndex;
    private UUID previewEntityUuid;

    public PowerCrystalOreBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.POWER_CRYSTAL_ORE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PowerCrystalOreBlockEntity blockEntity) {
        if (level.isClientSide() || !(state.getBlock() instanceof PowerCrystalRevealBlock)) {
            return;
        }

        if (blockEntity.revealTicksRemaining > 0) {
            blockEntity.revealTicksRemaining--;
            blockEntity.updatePreviewEntity((ServerLevel) level);
            blockEntity.spawnCycleParticles((ServerLevel) level);
            blockEntity.setChanged();
        }

        if (blockEntity.revealTicksRemaining <= 0) {
            blockEntity.finishReveal((ServerLevel) level, pos);
        }
    }

    public boolean startReveal(Player player) {
        if (level == null || revealTicksRemaining > 0) {
            return false;
        }

        RandomSource random = level.getRandom();
        int tuningLevel = getCrystalTuningLevel(player);
        revealTicksRemaining = REVEAL_DURATION;
        rewardIndex = random.nextBoolean() ? random.nextInt(3) : -1;
        rewardChargePercent = rewardIndex >= 0 ? rollRewardChargePercent(random, tuningLevel) : 0;
        previewOffsetIndex = rewardIndex >= 0
                ? Math.floorMod(rewardIndex - getCycleIndexForElapsedTicks(REVEAL_DURATION - 1), 3)
                : 0;
        if (level instanceof ServerLevel serverLevel) {
            if (rewardIndex >= 0) {
                spawnPreviewEntity(serverLevel);
                spawnStartParticles(serverLevel);
            } else {
                revealTicksRemaining = 0;
                spawnFailParticles(serverLevel);
                setChanged();
                return false;
            }
        }
        setChanged();
        return rewardIndex >= 0;
    }

    public boolean isRevealing() {
        return getBlockState().getBlock() instanceof PowerCrystalRevealBlock && revealTicksRemaining > 0;
    }

    public ItemStack getPreviewStack(long gameTime) {
        int elapsedTicks = Mth.clamp(REVEAL_DURATION - revealTicksRemaining, 0, REVEAL_DURATION);
        int index = getPreviewIndexForElapsedTicks(elapsedTicks);
        return getCrystalStackForIndex(index);
    }

    public void cancelReveal() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        double centerX = worldPosition.getX() + 0.5D;
        double centerY = worldPosition.getY() + PREVIEW_CENTER_Y;
        double centerZ = worldPosition.getZ() + 0.5D;
        removePreviewEntity(serverLevel);
        revealTicksRemaining = 0;
        rewardIndex = -1;
        rewardChargePercent = 0;
        previewOffsetIndex = 0;
        serverLevel.playSound(null, centerX, centerY, centerZ, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 0.55F, 0.65F);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(REVEAL_TICKS_KEY, revealTicksRemaining);
        tag.putInt(REWARD_INDEX_KEY, rewardIndex);
        tag.putInt(REWARD_CHARGE_KEY, rewardChargePercent);
        tag.putInt(PREVIEW_OFFSET_INDEX_KEY, previewOffsetIndex);
        if (previewEntityUuid != null) {
            tag.putUUID(PREVIEW_ENTITY_UUID_KEY, previewEntityUuid);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        revealTicksRemaining = tag.getInt(REVEAL_TICKS_KEY);
        rewardIndex = tag.getInt(REWARD_INDEX_KEY);
        rewardChargePercent = tag.getInt(REWARD_CHARGE_KEY);
        previewOffsetIndex = tag.getInt(PREVIEW_OFFSET_INDEX_KEY);
        previewEntityUuid = tag.hasUUID(PREVIEW_ENTITY_UUID_KEY) ? tag.getUUID(PREVIEW_ENTITY_UUID_KEY) : null;
    }

    private void finishReveal(ServerLevel level, BlockPos pos) {
        ItemStack rewardStack = createRewardStack();
        if (!rewardStack.isEmpty()) {
            spawnFinalParticles(level);
        }
        removePreviewEntity(level);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        if (!rewardStack.isEmpty()) {
            ItemEntity rewardEntity = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, rewardStack);
            rewardEntity.setDeltaMovement(0.0D, 0.0D, 0.0D);
            level.addFreshEntity(rewardEntity);
        }
    }

    private void spawnPreviewEntity(ServerLevel level) {
        if (rewardIndex < 0) {
            return;
        }

        ItemStack previewStack = getPreviewStack(level.getGameTime());
        if (previewStack.isEmpty()) {
            return;
        }

        ItemEntity previewEntity = new ItemEntity(level, worldPosition.getX() + 0.5D, worldPosition.getY() + PREVIEW_ITEM_Y, worldPosition.getZ() + 0.5D, previewStack);
        previewEntity.setNoGravity(true);
        previewEntity.setDeltaMovement(0.0D, 0.0D, 0.0D);
        previewEntity.setPos(worldPosition.getX() + 0.5D, worldPosition.getY() + PREVIEW_ITEM_Y, worldPosition.getZ() + 0.5D);
        previewEntity.setNeverPickUp();
        previewEntity.setUnlimitedLifetime();
        previewEntity.setInvulnerable(true);
        level.addFreshEntity(previewEntity);
        previewEntityUuid = previewEntity.getUUID();
    }

    private void updatePreviewEntity(ServerLevel level) {
        if (rewardIndex < 0) {
            removePreviewEntity(level);
            return;
        }

        ItemEntity previewEntity = getPreviewEntity(level);
        if (previewEntity == null) {
            spawnPreviewEntity(level);
            previewEntity = getPreviewEntity(level);
        }

        if (previewEntity == null) {
            return;
        }

        double centerX = worldPosition.getX() + 0.5D;
        double centerY = worldPosition.getY() + PREVIEW_ITEM_Y;
        double centerZ = worldPosition.getZ() + 0.5D;
        previewEntity.setPos(centerX, centerY, centerZ);
        previewEntity.setPosRaw(centerX, centerY, centerZ);
        previewEntity.setDeltaMovement(0.0D, 0.0D, 0.0D);
        previewEntity.setNoGravity(true);
        previewEntity.setPickUpDelay(32767);
        previewEntity.setItem(getPreviewStack(level.getGameTime()));
    }

    private void removePreviewEntity(ServerLevel level) {
        ItemEntity previewEntity = getPreviewEntity(level);
        if (previewEntity != null) {
            previewEntity.discard();
        }
        previewEntityUuid = null;
    }

    private ItemEntity getPreviewEntity(ServerLevel level) {
        if (previewEntityUuid == null) {
            return null;
        }

        Entity entity = level.getEntity(previewEntityUuid);
        return entity instanceof ItemEntity itemEntity ? itemEntity : null;
    }

    private int getPreviewIndexForElapsedTicks(int elapsedTicks) {
        if (rewardIndex < 0) {
            return -1;
        }
        return Math.floorMod(previewOffsetIndex + getCycleIndexForElapsedTicks(elapsedTicks), 3);
    }

    private static int getCycleIndexForElapsedTicks(int elapsedTicks) {
        int total = 0;
        int index = 0;
        while (total < elapsedTicks) {
            int interval = getIntervalForElapsedTicks(total);
            total += interval;
            index++;
        }
        return index % 3;
    }

    private static int getIntervalForElapsedTicks(int elapsedTicks) {
        float progress = Mth.clamp((float) elapsedTicks / (float) REVEAL_DURATION, 0.0F, 1.0F);
        return Math.max(1, Mth.floor(1.0F + progress * 9.0F));
    }

    private void spawnStartParticles(ServerLevel level) {
        double centerX = worldPosition.getX() + 0.5D;
        double centerY = worldPosition.getY() + PREVIEW_CENTER_Y;
        double centerZ = worldPosition.getZ() + 0.5D;
        level.sendParticles(ParticleTypes.END_ROD, centerX, centerY, centerZ, 14, 0.12D, 0.10D, 0.12D, 0.02D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centerX, centerY, centerZ, 10, 0.10D, 0.08D, 0.10D, 0.02D);
        playSparkleSound(level, centerX, centerY, centerZ, 0.80F, 1.30F);
    }

    private void spawnCycleParticles(ServerLevel level) {
        if (rewardIndex < 0) {
            return;
        }

        int elapsedTicks = Mth.clamp(REVEAL_DURATION - revealTicksRemaining, 0, REVEAL_DURATION);
        float revealProgress = elapsedTicks / (float) REVEAL_DURATION;
        double centerX = worldPosition.getX() + 0.5D;
        double centerY = worldPosition.getY() + PREVIEW_CENTER_Y;
        double centerZ = worldPosition.getZ() + 0.5D;

        if (elapsedTicks % 4 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, centerX, centerY, centerZ, 2, 0.05D, 0.05D, 0.05D, 0.01D);
        }
        if (elapsedTicks % 7 == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centerX, centerY, centerZ, 2, 0.06D, 0.04D, 0.06D, 0.01D);
        }
        if (elapsedTicks % 12 == 0) {
            playSparkleSound(level, centerX, centerY, centerZ, 0.35F, 1.45F - revealProgress * 0.25F);
        }
    }

    private void spawnFinalParticles(ServerLevel level) {
        Vector3f color = getCrystalColor(rewardIndex);
        double centerX = worldPosition.getX() + 0.5D;
        double centerY = worldPosition.getY() + PREVIEW_CENTER_Y;
        double centerZ = worldPosition.getZ() + 0.5D;
        spawnCircularDustBurst(level, centerX, centerY, centerZ, color, 18, 0.22D, 0.08D, 1.35F);
        spawnCircularDustBurst(level, centerX, centerY, centerZ, brighten(color, 0.25F), 30, 0.34D, 0.12D, 0.95F);
        spawnMagicCircleBurst(level, centerX, centerY, centerZ, color, 48, 0.40D, 0.05D, 0.16D, 1.0F);
        spawnMagicCircleBurst(level, centerX, centerY, centerZ, brighten(color, 0.18F), 64, 0.52D, 0.08D, -0.20D, 0.8F);
        spawnMagicCircleBurst(level, centerX, centerY + 0.08D, centerZ, brighten(color, 0.28F), 36, 0.30D, 0.10D, 0.26D, 0.9F);
        level.sendParticles(ParticleTypes.END_ROD, centerX, centerY, centerZ, 26, 0.18D, 0.10D, 0.18D, 0.03D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centerX, centerY, centerZ, 28, 0.18D, 0.08D, 0.18D, 0.03D);
        level.sendParticles(ParticleTypes.ENCHANT, centerX, centerY, centerZ, 42, 0.18D, 0.10D, 0.18D, 0.55D);
        level.playSound(null, centerX, centerY, centerZ, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.05F, 1.15F);
        level.playSound(null, centerX, centerY, centerZ, SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.BLOCKS, 0.75F, 1.55F);
        level.playSound(null, centerX, centerY, centerZ, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.45F, 1.85F);
    }

    private void spawnFailParticles(ServerLevel level) {
        double centerX = worldPosition.getX() + 0.5D;
        double centerY = worldPosition.getY() + PREVIEW_CENTER_Y;
        double centerZ = worldPosition.getZ() + 0.5D;
        level.sendParticles(new DustParticleOptions(new Vector3f(0.45F, 0.45F, 0.45F), 0.9F), centerX, centerY, centerZ, 8, 0.10D, 0.06D, 0.10D, 0.01D);
        level.sendParticles(ParticleTypes.SMOKE, centerX, centerY, centerZ, 4, 0.05D, 0.02D, 0.05D, 0.0D);
        level.playSound(null, centerX, centerY, centerZ, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 0.55F, 0.65F);
    }

    private void spawnCircularDustBurst(ServerLevel level, double centerX, double centerY, double centerZ, Vector3f color, int count, double horizontalSpeed, double verticalSpeed, float size) {
        DustParticleOptions options = new DustParticleOptions(color, size);
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D * i) / count;
            double speedX = Math.cos(angle) * horizontalSpeed;
            double speedZ = Math.sin(angle) * horizontalSpeed;
            double speedY = verticalSpeed + ((i & 1) == 0 ? 0.015D : -0.005D);
            level.sendParticles(options, centerX, centerY, centerZ, 1, speedX, speedY, speedZ, 0.0D);
        }
    }

    private void spawnMagicCircleBurst(ServerLevel level, double centerX, double centerY, double centerZ, Vector3f color, int count, double radialSpeed, double verticalSpeed, double tangentialSpeed, float size) {
        DustParticleOptions options = new DustParticleOptions(color, size);
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D * i) / count;
            double radialX = Math.cos(angle) * radialSpeed;
            double radialZ = Math.sin(angle) * radialSpeed;
            double tangentX = -Math.sin(angle) * tangentialSpeed;
            double tangentZ = Math.cos(angle) * tangentialSpeed;
            double speedX = radialX + tangentX;
            double speedZ = radialZ + tangentZ;
            double speedY = verticalSpeed + Math.sin(angle * 2.0D) * 0.02D;
            level.sendParticles(options, centerX, centerY, centerZ, 1, speedX, speedY, speedZ, 0.0D);
            if ((i & 3) == 0) {
                level.sendParticles(ParticleTypes.END_ROD, centerX, centerY, centerZ, 1, speedX * 0.7D, speedY * 0.7D, speedZ * 0.7D, 0.0D);
            }
            if ((i & 5) == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centerX, centerY, centerZ, 1, speedX * 0.85D, speedY * 0.85D, speedZ * 0.85D, 0.0D);
            }
            if ((i & 7) == 0) {
                level.sendParticles(ParticleTypes.ENCHANT, centerX, centerY, centerZ, 1, speedX * 0.6D, speedY * 0.5D, speedZ * 0.6D, 0.0D);
            }
        }
    }

    private void playSparkleSound(ServerLevel level, double centerX, double centerY, double centerZ, float volume, float pitch) {
        level.playSound(null, centerX, centerY, centerZ, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, volume, pitch);
        level.playSound(null, centerX, centerY, centerZ, SoundEvents.AMETHYST_CLUSTER_HIT, SoundSource.BLOCKS, volume * 0.55F, pitch + 0.12F);
    }

    private static Vector3f brighten(Vector3f color, float amount) {
        return new Vector3f(
                Mth.clamp(color.x() + amount, 0.0F, 1.0F),
                Mth.clamp(color.y() + amount, 0.0F, 1.0F),
                Mth.clamp(color.z() + amount, 0.0F, 1.0F)
        );
    }

    private static Vector3f getCrystalColor(int index) {
        return switch (index) {
            case 0 -> new Vector3f(1.0F, 0.33F, 0.33F);
            case 1 -> new Vector3f(0.33F, 1.0F, 1.0F);
            case 2 -> new Vector3f(0.33F, 1.0F, 0.33F);
            default -> new Vector3f(0.70F, 0.70F, 0.70F);
        };
    }

    private ItemStack createRewardStack() {
        ItemStack stack = switch (rewardIndex) {
            case 0 -> ModItems.CRIMSON_POWER_CRYSTAL.get().getDefaultInstance();
            case 1 -> ModItems.AZURE_POWER_CRYSTAL.get().getDefaultInstance();
            case 2 -> ModItems.VERDANT_POWER_CRYSTAL.get().getDefaultInstance();
            default -> ItemStack.EMPTY;
        };
        if (!stack.isEmpty()) {
            PowerCrystalData.setChargePercent(stack, rewardChargePercent);
        }
        return stack;
    }

    private ItemStack getCrystalStackForIndex(int index) {
        ItemStack stack = switch (index) {
            case 0 -> ModItems.CRIMSON_POWER_CRYSTAL.get().getDefaultInstance();
            case 1 -> ModItems.AZURE_POWER_CRYSTAL.get().getDefaultInstance();
            case 2 -> ModItems.VERDANT_POWER_CRYSTAL.get().getDefaultInstance();
            default -> ItemStack.EMPTY;
        };
        if (!stack.isEmpty() && rewardChargePercent > 0) {
            PowerCrystalData.setChargePercent(stack, rewardChargePercent);
        }
        return stack;
    }

    private static int rollRewardChargePercent(RandomSource random, int crystalTuningLevel) {
        int chargePercent = Math.min(
                PowerCrystalData.MAX_CHARGE,
                PowerCrystalData.MIN_CHARGE + crystalTuningLevel * 10
        );
        while (chargePercent < PowerCrystalData.MAX_CHARGE) {
            float progress = (float) (chargePercent - PowerCrystalData.MIN_CHARGE)
                    / (float) (PowerCrystalData.MAX_CHARGE - PowerCrystalData.MIN_CHARGE);
            float continueChance = Mth.lerp(progress, 0.88F, 0.12F) + crystalTuningLevel * 0.04F;
            continueChance = Mth.clamp(continueChance, 0.0F, 0.97F);
            if (random.nextFloat() > continueChance) {
                break;
            }
            chargePercent++;
        }
        return chargePercent;
    }

    private int getCrystalTuningLevel(Player player) {
        if (level == null || player == null) {
            return 0;
        }

        HolderLookup.RegistryLookup<Enchantment> enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        return EnchantmentHelper.getItemEnchantmentLevel(
                enchantments.getOrThrow(ModEnchantments.CRYSTAL_TUNING),
                player.getMainHandItem()
        );
    }
}
