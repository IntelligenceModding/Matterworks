package de.artemis.matterworks.client.particle;

import de.artemis.matterworks.common.registry.ModFluids;
import de.artemis.matterworks.common.registry.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public abstract class RawMatterDripParticle extends TextureSheetParticle {
    private static final float RED = 0.82F;
    private static final float GREEN = 0.85F;
    private static final float BLUE = 0.88F;

    private final Fluid fluidType;

    protected RawMatterDripParticle(ClientLevel level, double x, double y, double z, Fluid fluidType, SpriteSet sprites) {
        super(level, x, y, z);
        this.setSize(0.01F, 0.01F);
        this.gravity = 0.06F;
        this.fluidType = fluidType;
        this.setColor(RED, GREEN, BLUE);
        this.pickSprite(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.preMoveUpdate();
        if (this.removed) {
            return;
        }

        this.yd -= this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.postMoveUpdate();
        if (this.removed) {
            return;
        }

        this.xd *= 0.98F;
        this.yd *= 0.98F;
        this.zd *= 0.98F;
        if (this.fluidType != null) {
            BlockPos blockPos = BlockPos.containing(this.x, this.y, this.z);
            FluidState fluidState = this.level.getFluidState(blockPos);
            if (fluidState.getType() == this.fluidType && this.y < blockPos.getY() + fluidState.getHeight(this.level, blockPos)) {
                this.remove();
            }
        }
    }

    protected void preMoveUpdate() {
        if (this.lifetime-- <= 0) {
            this.remove();
        }
    }

    protected void postMoveUpdate() {
    }

    public static class Dripping extends RawMatterDripParticle {
        private final ParticleOptions fallingParticle;

        public Dripping(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, ModFluids.RAW_MATTER.get(), sprites);
            this.fallingParticle = ModParticles.FALLING_RAW_MATTER.get();
            this.gravity *= 0.02F;
            this.lifetime = 40;
        }

        @Override
        protected void preMoveUpdate() {
            if (this.lifetime-- <= 0) {
                this.remove();
                this.level.addParticle(this.fallingParticle, this.x, this.y, this.z, this.xd, this.yd, this.zd);
            }
        }

        @Override
        protected void postMoveUpdate() {
            this.xd *= 0.02D;
            this.yd *= 0.02D;
            this.zd *= 0.02D;
        }
    }

    public static class Falling extends RawMatterDripParticle {
        protected final ParticleOptions landParticle;

        public Falling(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, ModFluids.RAW_MATTER.get(), sprites);
            this.landParticle = ModParticles.LANDING_RAW_MATTER.get();
            this.lifetime = (int) (64.0D / (Math.random() * 0.8D + 0.2D));
        }

        @Override
        protected void postMoveUpdate() {
            if (this.onGround) {
                this.remove();
                this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    public static class Landing extends RawMatterDripParticle {
        public Landing(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, null, sprites);
            this.lifetime = (int) (16.0D / (Math.random() * 0.8D + 0.2D));
            this.gravity = 0.0F;
        }
    }

    public static class DrippingProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public DrippingProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new Dripping(level, x, y, z, this.sprites);
        }
    }

    public static class FallingProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public FallingProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new Falling(level, x, y, z, this.sprites);
        }
    }

    public static class LandingProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public LandingProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new Landing(level, x, y, z, this.sprites);
        }
    }
}
