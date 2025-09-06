package fr.nokane.btoommods.particle.client;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;

public class GasCloudParticle extends SpriteTexturedParticle {
    private final IAnimatedSprite sprites;

    protected GasCloudParticle(ClientWorld world, double x, double y, double z,
                               double vx, double vy, double vz, IAnimatedSprite sprites) {
        super(world, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.hasPhysics = false;
        this.gravity = 0.005F;
        this.lifetime = 40 + random.nextInt(20);
        this.quadSize = 0.6F + random.nextFloat() * 0.4F;
        // si ton PNG est déjà jaune, laisse le blanc :
        this.rCol = 1F; this.gCol = 1F; this.bCol = 1F;
        this.alpha = 0.35F;
        this.pickSprite(sprites);
    }

    @Override public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override public void tick() {
        super.tick();
        this.alpha *= 0.985F;
        this.xd *= 0.96; this.yd *= 0.96; this.zd *= 0.96;
    }

    public static class Provider implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite sprites;
        public Provider(IAnimatedSprite sprites){ this.sprites = sprites; }

        @Override
        public Particle createParticle(BasicParticleType type, ClientWorld world,
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new GasCloudParticle(world, x, y, z, vx, vy, vz, sprites);
        }
    }
}
