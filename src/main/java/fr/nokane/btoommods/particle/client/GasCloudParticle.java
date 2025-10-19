package fr.nokane.btoommods.particle.client;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GasCloudParticle extends SpriteTexturedParticle {

    private final IAnimatedSprite sprites;

    protected GasCloudParticle(ClientWorld world, double x, double y, double z,
                               double vx, double vy, double vz, IAnimatedSprite sprites) {
        super(world, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.hasPhysics = false;
        this.gravity = 0.0F; // on gère à la main
        this.lifetime = 80 + random.nextInt(60);
        this.quadSize = 0.7F + random.nextFloat() * 0.5F;
        this.rCol = 1F;
        this.gCol = 1F;
        this.bCol = 0.75F + random.nextFloat() * 0.15F;
        this.alpha = 0.45F;
        this.pickSprite(sprites);

        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
    }

    @Override
    public void tick() {
        super.tick();

        // expansion légère et adoucissement des vitesses
        this.quadSize *= 1.0015F;
        this.xd += (random.nextDouble() - 0.5) * 0.002;
        this.zd += (random.nextDouble() - 0.5) * 0.002;
        this.yd -= 0.00035; // descente douce
        this.alpha *= 0.986F;
        this.xd *= 0.97; this.yd *= 0.97; this.zd *= 0.97;

        if (this.age++ >= this.lifetime) this.remove();
    }


    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite sprites;
        public Provider(IAnimatedSprite sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(BasicParticleType type, ClientWorld world,
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new GasCloudParticle(world, x, y, z, vx, vy, vz, sprites);
        }
    }
}
