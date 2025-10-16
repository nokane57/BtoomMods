package fr.nokane.btoommods.entity;

import fr.nokane.btoommods.Btoommods;
import fr.nokane.btoommods.entity.item.*;
import fr.nokane.btoommods.entity.misc.BlazingFireFieldEntity;
import fr.nokane.btoommods.entity.misc.GasCloudFieldEntity;
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

    // === Cracker BIM ===
    public static final RegistryObject<EntityType<CrackerBimEntity>> CRACKER_BIM =
            ENTITIES.register("cracker_bim_projectile", () ->
                    EntityType.Builder.<CrackerBimEntity>of(CrackerBimEntity::new, EntityClassification.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(64)
                            .updateInterval(10)
                            .build(new ResourceLocation(Btoommods.MOD_ID, "cracker_bim_projectile").toString())
            );

    // === Blazing BIM ===
    public static final RegistryObject<EntityType<BlazingBimEntity>> BLAZING_BIM =
            ENTITIES.register("blazing_bim_projectile", () ->
                    EntityType.Builder.<BlazingBimEntity>of(BlazingBimEntity::new, EntityClassification.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(64)
                            .updateInterval(10)
                            .build(new ResourceLocation(Btoommods.MOD_ID, "blazing_bim_projectile").toString())
            );

    public static final RegistryObject<EntityType<BlazingFireFieldEntity>> BLAZING_FIRE_FIELD =
            ENTITIES.register("blazing_fire_field", () ->
                    EntityType.Builder.<BlazingFireFieldEntity>of(BlazingFireFieldEntity::new, EntityClassification.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(32)
                            .updateInterval(20)
                            .build(new ResourceLocation(Btoommods.MOD_ID, "blazing_fire_field").toString())
            );

    // === Gas BIM ===
    public static final RegistryObject<EntityType<GasBimEntity>> GAS_BIM =
            ENTITIES.register("gas_bim_projectile", () ->
                    EntityType.Builder.<GasBimEntity>of(GasBimEntity::new, EntityClassification.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(64)
                            .updateInterval(10)
                            .build(new ResourceLocation(Btoommods.MOD_ID, "gas_bim_projectile").toString())
            );

    public static final RegistryObject<EntityType<GasDisabledBimEntity>> GAS_BIM_DISABLED =
            ENTITIES.register("gas_bim_disabled_projectile", () ->
                    EntityType.Builder.<GasDisabledBimEntity>of(GasDisabledBimEntity::new, EntityClassification.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(64)
                            .updateInterval(10)
                            .build(new ResourceLocation(Btoommods.MOD_ID, "gas_bim_disabled_projectile").toString())
            );

    public static final RegistryObject<EntityType<GasCloudFieldEntity>> GAS_CLOUD_FIELD =
            ENTITIES.register("gas_cloud_field", () ->
                    EntityType.Builder.<GasCloudFieldEntity>of(GasCloudFieldEntity::new, EntityClassification.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(64)
                            .updateInterval(20)
                            .build(new ResourceLocation(Btoommods.MOD_ID, "gas_cloud_field").toString())
            );

    // === Remote BIM ===
    public static final RegistryObject<EntityType<RemoteBimEntity>> REMOTE_BIM =
            ENTITIES.register("remote_bim", () ->
                    EntityType.Builder.<RemoteBimEntity>of(RemoteBimEntity::new, EntityClassification.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(256) // ✅ Longue portée
                            .updateInterval(10)
                            .build(new ResourceLocation(Btoommods.MOD_ID, "remote_bim").toString())
            );

    // === Timer BIM ===
    public static final RegistryObject<EntityType<TimerBimProjectileEntity>> TIMER_BIM_PROJECTILE =
            ENTITIES.register("timer_bim_projectile", () ->
                    EntityType.Builder.<TimerBimProjectileEntity>of(TimerBimProjectileEntity::new, EntityClassification.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(new ResourceLocation(Btoommods.MOD_ID, "timer_bim_projectile").toString())
            );

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
