package com.nutriamod.entity.ai;

import com.nutriamod.entity.OtterEntity;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** La nutria va al agua, nada de un lado a otro, da vueltas y salta chapoteando. */
public class OtterPlayInWaterGoal extends Goal {
    private final OtterEntity otter;
    private BlockPos target;
    private int playTicks;
    private int moveTimer;
    private int leapTimer;

    public OtterPlayInWaterGoal(OtterEntity otter) {
        this.otter = otter;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (otter.isOrderedToSit() || otter.isLeashed() || otter.isPassenger() || otter.getTarget() != null) {
            return false;
        }
        if (otter.getRandom().nextInt(otter.isInWater() ? 80 : 160) != 0) {
            return false;
        }
        LivingEntity owner = otter.getOwner();
        if (otter.isTame() && owner != null && otter.distanceToSqr(owner) > 100.0D) {
            return false;
        }
        target = findWater(otter.isInWater() ? 6 : 10);
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return playTicks > 0 && !otter.isOrderedToSit() && !otter.isLeashed() && otter.getTarget() == null;
    }

    @Override
    public void start() {
        playTicks = 120 + otter.getRandom().nextInt(120);
        moveTimer = 0;
        leapTimer = 20 + otter.getRandom().nextInt(30);
        if (target != null) {
            otter.getNavigation().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 1.0D);
        }
    }

    @Override
    public void stop() {
        otter.setPlayingInWater(false);
        otter.getNavigation().stop();
        target = null;
    }

    @Override
    public void tick() {
        playTicks--;
        if (otter.isInWater()) {
            otter.setPlayingInWater(true);
            if (--moveTimer <= 0 || otter.getNavigation().isDone()) {
                moveTimer = 20 + otter.getRandom().nextInt(20);
                BlockPos next = findWater(6);
                if (next != null) {
                    otter.getNavigation().moveTo(next.getX() + 0.5D, next.getY() + 0.5D, next.getZ() + 0.5D, 1.0D);
                }
            }
            if (--leapTimer <= 0 && otter.isEyeInFluid(FluidTags.WATER)) {
                leapTimer = 30 + otter.getRandom().nextInt(40);
                leap();
            }
        } else {
            otter.setPlayingInWater(false);
            if (otter.getNavigation().isDone()) {
                if (target != null) {
                    otter.getNavigation().moveTo(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 1.0D);
                    target = null;
                } else {
                    playTicks = 0;
                }
            }
        }
    }

    private void leap() {
        Vec3 motion = otter.getDeltaMovement();
        otter.setDeltaMovement(motion.x, 0.42D, motion.z);
        if (otter.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SPLASH, otter.getX(), otter.getY() + 0.3D, otter.getZ(),
                    12, 0.3D, 0.1D, 0.3D, 0.1D);
        }
        otter.playSound(SoundEvents.PLAYER_SPLASH, 0.4F, 1.4F);
    }

    private BlockPos findWater(int radius) {
        Level level = otter.level();
        BlockPos origin = otter.blockPosition();
        RandomSource random = otter.getRandom();
        for (int i = 0; i < 20; i++) {
            BlockPos pos = origin.offset(
                    random.nextInt(radius * 2 + 1) - radius,
                    random.nextInt(5) - 2,
                    random.nextInt(radius * 2 + 1) - radius);
            if (level.getFluidState(pos).is(FluidTags.WATER)) {
                return pos;
            }
        }
        return null;
    }
}
