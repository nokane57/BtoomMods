package fr.nokane.btoommods.entity;

import fr.nokane.btoommods.Btoommods;
import fr.nokane.btoommods.entity.item.CrackerBimEntity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITIES, Btoommods.MOD_ID);

    public static final RegistryObject<EntityType<CrackerBimEntity>> CRACKER_BIM =
            ENTITIES.register("cracker_bim_projectile", () ->
                    EntityType.Builder.<CrackerBimEntity>of(CrackerBimEntity::new, EntityClassification.MISC)
                            .sized(0.25F, 0.25F)          // petit projectile
                            .clientTrackingRange(64)
                            .updateInterval(10)
                            .build(new ResourceLocation(Btoommods.MOD_ID, "cracker_bim_projectile").toString())
            );

    public static void register(IEventBus bus) { ENTITIES.register(bus); }
}
