// fr/nokane/btoommods/radar/RadarCapability.java
package fr.nokane.btoommods.radar;

import fr.nokane.btoommods.Btoommods;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;

public final class RadarCapability {

    @CapabilityInject(RadarData.class)
    public static Capability<RadarData> CAP = null;

    public static final ResourceLocation ID =
            new ResourceLocation(Btoommods.MOD_ID, "radar");

    public static class Provider implements ICapabilitySerializable<INBT> {
        private final RadarDataImpl data = new RadarDataImpl();
        private final LazyOptional<RadarData> opt = LazyOptional.of(() -> data);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            return cap == CAP ? opt.cast() : LazyOptional.empty();
        }

        @Override
        public INBT serializeNBT() {
            CompoundNBT tag = new CompoundNBT();
            tag.putBoolean("implant", data.hasImplant());
            tag.putInt("boosters", data.getBoosters());
            tag.putLong("lastMoveTick", data.getLastMoveTick());
            return tag;
        }

        @Override
        public void deserializeNBT(INBT nbt) {
            CompoundNBT tag = (CompoundNBT) nbt;
            data.setImplant(tag.getBoolean("implant"));
            data.setBoosters(tag.getInt("boosters"));
            data.setLastMoveTick(tag.getLong("lastMoveTick"));
        }
    }

    public static void registerManually() {
        CapabilityManager.INSTANCE.register(
                RadarData.class,
                new Capability.IStorage<RadarData>() {
                    @Override
                    public INBT writeNBT(Capability<RadarData> cap, RadarData inst, Direction side) {
                        return null;
                    }

                    @Override
                    public void readNBT(Capability<RadarData> cap, RadarData inst, Direction side, INBT nbt) {
                    }
                },
                RadarDataImpl::new
        );
    }
}
