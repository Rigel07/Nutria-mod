package com.nutriamod.entity.ai;

import com.nutriamod.entity.OtterEntity;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

/**
 * Sigue al dueño por tierra y por agua. Se escribe aparte porque el FollowOwnerGoal de vanilla
 * solo admite navegación terrestre o voladora y la nutria usa navegación anfibia.
 */
public class OtterFollowOwnerGoal extends Goal {
    private final OtterEntity otter;
    private final double speed;
    private final float startDistance;
    private final float stopDistance;
    private LivingEntity owner;
    private int timeToRecalcPath;

    public OtterFollowOwnerGoal(OtterEntity otter, double speed, float startDistance, float stopDistance) {
        this.otter = otter;
        this.speed = speed;
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean cannotMove() {
        return otter.isOrderedToSit() || otter.isPassenger() || otter.isLeashed();
    }

    @Override
    public boolean canUse() {
        if (!otter.isTame() || cannotMove()) {
            return false;
        }
        LivingEntity candidate = otter.getOwner();
        if (candidate == null || candidate.isSpectator()) {
            return false;
        }
        if (otter.distanceToSqr(candidate) < (double) (startDistance * startDistance)) {
            return false;
        }
        this.owner = candidate;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (owner == null || cannotMove() || otter.getNavigation().isDone()) {
            return false;
        }
        return otter.distanceToSqr(owner) > (double) (stopDistance * stopDistance);
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        otter.setPlayingInWater(false);
    }

    @Override
    public void stop() {
        this.owner = null;
        otter.getNavigation().stop();
    }

    @Override
    public void tick() {
        otter.getLookControl().setLookAt(owner, 10.0F, (float) otter.getMaxHeadXRot());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.adjustedTickDelay(10);
            if (otter.distanceToSqr(owner) >= 144.0D) {
                teleportToOwner();
            } else {
                otter.getNavigation().moveTo(owner, speed);
            }
        }
    }

    private void teleportToOwner() {
        BlockPos ownerPos = owner.blockPosition();
        for (int i = 0; i < 10; i++) {
            int dx = otter.getRandom().nextInt(7) - 3;
            int dy = otter.getRandom().nextInt(3) - 1;
            int dz = otter.getRandom().nextInt(7) - 3;
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
                continue;
            }
            BlockPos pos = ownerPos.offset(dx, dy, dz);
            if (canTeleportTo(pos)) {
                otter.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, otter.getYRot(), otter.getXRot());
                otter.getNavigation().stop();
                return;
            }
        }
    }

    private boolean canTeleportTo(BlockPos pos) {
        Level level = otter.level();
        boolean water = level.getFluidState(pos).is(FluidTags.WATER);
        boolean solidBelow = level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        if (!water && !solidBelow) {
            return false;
        }
        return level.noCollision(otter, otter.getBoundingBox().move(
                pos.getX() + 0.5D - otter.getX(),
                pos.getY() - otter.getY(),
                pos.getZ() + 0.5D - otter.getZ()));
    }
}
