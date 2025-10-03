package fr.nokane.btoommods.item;

import fr.nokane.btoommods.config.ModConfigs;
import fr.nokane.btoommods.entity.item.TimerBimProjectileEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

public class TimerBimItem extends Item {

    public static final String NBT_ACTIVE      = "Active";           // boolean
    public static final String NBT_HAS_STARTED = "HasStarted";       // boolean
    public static final String NBT_REMAINING   = "RemainingTicks";   // int

    public TimerBimItem(Properties props) { super(props); }

    // ----- CONFIG -----
    public static int getMaxSeconds() { return ModConfigs.COMMON.TIMER_DEFAULT_SECONDS.get(); }
    public static int getMaxTicks()   { return getMaxSeconds() * 20; }

    // ----- USE -----
    @Override public UseAction getUseAnimation(ItemStack stack) { return UseAction.BOW; }
    @Override public int getUseDuration(ItemStack stack) { return 72000; }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return ActionResult.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, World world, net.minecraft.entity.LivingEntity living, int timeLeft) {
        if (world.isClientSide || !(living instanceof PlayerEntity)) return;

        PlayerEntity player = (PlayerEntity) living;
        int used = this.getUseDuration(stack) - timeLeft;
        float power = Math.min(1.0f, used / 20.0f); // 1s charge max
        float velocity = (float)(1.7f * power * ModConfigs.COMMON.TIMER_PROJECTILE_SPEED_MULT.get());

        CompoundNBT tag = stack.getOrCreateTag();
        boolean active     = tag.getBoolean(NBT_ACTIVE);
        boolean hasStarted = tag.getBoolean(NBT_HAS_STARTED);
        int remaining      = tag.contains(NBT_REMAINING) ? tag.getInt(NBT_REMAINING) : getMaxTicks();

        TimerBimProjectileEntity proj = new TimerBimProjectileEntity(world, player);
        ItemStack copy = stack.copy();
        copy.setCount(1);
        proj.setItem(copy);
        proj.setTimerState(active, hasStarted, remaining);
        proj.shootFromRotation(player, player.xRot, player.yRot, 0.0f, velocity, 0.9f);

        world.addFreshEntity(proj);

        if (!player.abilities.instabuild) stack.shrink(1);
    }

    // ----- INVENTORY TICK -----
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClientSide) return;
        tickTimer(stack, world, entity.getX(), entity.getY() + 0.5, entity.getZ(), true, null);
    }

    // ----- ITEMENTITY TICK -----
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        World world = entity.level;
        if (world.isClientSide) return false;

        boolean exploded = tickTimer(stack, world, entity.getX(), entity.getY() + 0.25, entity.getZ(), false, entity);
        return exploded; // true = on a géré l’explosion et supprimé l’ItemEntity
    }

    /**
     * Fait avancer le compte à rebours et déclenche l'explosion si nécessaire.
     */
    private boolean tickTimer(ItemStack stack, World world, double x, double y, double z,
                              boolean shrinkOnExplode, ItemEntity entityToRemove) {
        CompoundNBT tag = stack.getOrCreateTag();

        boolean active   = tag.getBoolean(NBT_ACTIVE);
        int remaining    = tag.contains(NBT_REMAINING) ? tag.getInt(NBT_REMAINING) : getMaxTicks();

        if (!tag.contains(NBT_REMAINING)) {
            tag.putInt(NBT_REMAINING, getMaxTicks());
        }

        if (active && remaining > 0) {
            remaining--;
            tag.putInt(NBT_REMAINING, remaining);

            if (remaining == 0) {
                explodeAndConsume(world, x, y, z);

                if (shrinkOnExplode) {
                    if (stack.getCount() > 0) stack.shrink(1);
                } else if (entityToRemove != null && !world.isClientSide) {
                    entityToRemove.remove();
                }
                return true;
            }
        }
        return false;
    }

    // ----- STACK SIZE -----
    @Override
    public int getItemStackLimit(ItemStack stack) {
        int v = ModConfigs.COMMON.TIMER_STACK.get();
        return (v < 1) ? 1 : Math.min(v, 64);
    }

    // ----- TOGGLE -----
    public static void toggleTimer(ItemStack stack) {
        CompoundNBT tag = stack.getOrCreateTag();
        boolean active     = tag.getBoolean(NBT_ACTIVE);
        boolean hasStarted = tag.getBoolean(NBT_HAS_STARTED);
        int remaining      = tag.contains(NBT_REMAINING) ? tag.getInt(NBT_REMAINING) : getMaxTicks();

        if (!hasStarted && !active) {
            tag.putBoolean(NBT_ACTIVE, true);
            tag.putBoolean(NBT_HAS_STARTED, true);
            tag.putInt(NBT_REMAINING, getMaxTicks());
        } else if (active) {
            tag.putBoolean(NBT_ACTIVE, false);
            tag.putBoolean(NBT_HAS_STARTED, true);
            tag.putInt(NBT_REMAINING, Math.max(0, remaining));
        } else {
            tag.putBoolean(NBT_ACTIVE, true);
            tag.putBoolean(NBT_HAS_STARTED, true);
            tag.putInt(NBT_REMAINING, Math.max(0, remaining));
        }
    }

    // ----- DISPLAY -----
    public static int getDisplaySeconds(ItemStack stack) {
        CompoundNBT tag = stack.getOrCreateTag();
        if (!tag.getBoolean(NBT_HAS_STARTED)) return -1;
        int ticks = tag.getInt(NBT_REMAINING);
        return Math.max(0, Math.min(getMaxSeconds(), (int) Math.ceil(ticks / 20.0)));
    }

    // ----- EXPLOSION -----
    public static void explodeAndConsume(World world, double x, double y, double z) {
        world.explode(null, x, y, z,
                ModConfigs.COMMON.TIMER_EXPLOSION_STRENGTH.get().floatValue(),
                ModConfigs.COMMON.TIMER_CAUSES_FIRE.get(),
                ModConfigs.COMMON.TIMER_BREAK_BLOCKS.get() ? Explosion.Mode.BREAK : Explosion.Mode.NONE);
    }


}
