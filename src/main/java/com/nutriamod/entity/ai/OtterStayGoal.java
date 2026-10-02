package com.nutriamod.entity.ai;

import com.nutriamod.entity.OtterEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

/** La nutria se queda quieta cuando su dueño se lo ordena (Shift + clic derecho). */
public class OtterStayGoal extends Goal {
    private final OtterEntity otter;

    public OtterStayGoal(OtterEntity otter) {
        this.otter = otter;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return otter.isTame() && otter.isOrderedToSit();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        otter.getNavigation().stop();
        otter.setPlayingInWater(false);
        otter.setInSittingPose(true);
    }

    @Override
    public void stop() {
        otter.setInSittingPose(false);
    }
}
