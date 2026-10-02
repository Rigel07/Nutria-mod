package com.nutriamod.entity;

import com.nutriamod.ModEntities;
import com.nutriamod.entity.ai.OtterFollowOwnerGoal;
import com.nutriamod.entity.ai.OtterPlayInWaterGoal;
import com.nutriamod.entity.ai.OtterStayGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

public class OtterEntity extends TamableAnimal {

    public static final int MAX_LEVEL = 10;
    /** Puntos de amistad necesarios para alcanzar cada nivel (nivel 1 = 0 puntos). */
    private static final int[] LEVEL_THRESHOLDS = {0, 15, 40, 75, 120, 175, 240, 315, 400, 500};
    /** Una espada de diamante hace 7 de daño (3,5 corazones). */
    private static final double MIN_DAMAGE = 1.0D;
    private static final double MAX_DAMAGE = 7.0D;

    private static final int FEED_COOLDOWN_TICKS = 100;
    private static final int PET_COOLDOWN_TICKS = 40;
    private static final int PET_POINTS = 4;

    private static final EntityDataAccessor<Integer> DATA_FRIENDSHIP =
            SynchedEntityData.defineId(OtterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PLAYING =
            SynchedEntityData.defineId(OtterEntity.class, EntityDataSerializers.BOOLEAN);

    private int feedCooldown;
    private int petCooldown;

    public OtterEntity(EntityType<? extends OtterEntity> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.35F, 1.0F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 20);
        this.setMaxUpStep(1.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, MIN_DAMAGE);
    }

    public static boolean checkOtterSpawnRules(EntityType<OtterEntity> type, ServerLevelAccessor level,
                                               MobSpawnType reason, BlockPos pos, RandomSource random) {
        boolean inWater = level.getFluidState(pos).is(FluidTags.WATER)
                || level.getFluidState(pos.below()).is(FluidTags.WATER);
        if (inWater) {
            return true;
        }
        BlockState below = level.getBlockState(pos.below());
        boolean ground = below.is(BlockTags.DIRT) || below.is(BlockTags.SAND)
                || below.is(Blocks.GRAVEL) || below.is(Blocks.CLAY);
        boolean waterNearby = BlockPos.betweenClosedStream(pos.offset(-4, -2, -4), pos.offset(4, 1, 4))
                .anyMatch(p -> level.getFluidState(p).is(FluidTags.WATER));
        return ground && waterNearby;
    }

    // ------------------------------------------------------------------ IA

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D) {
            @Override
            public boolean canUse() {
                return !OtterEntity.this.isTame() && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new OtterStayGoal(this));
        this.goalSelector.addGoal(3, new OtterFollowOwnerGoal(this, 1.2D, 6.0F, 2.0F));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.3D, true));
        this.goalSelector.addGoal(5, new OtterPlayInWaterGoal(this));
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.8D, 120));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return level.getFluidState(pos).is(FluidTags.WATER) ? 10.0F : super.getWalkTargetValue(pos, level);
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof Creeper || target instanceof Ghast) {
            return false;
        }
        if (target instanceof TamableAnimal pet) {
            return !pet.isTame() || pet.getOwner() != owner;
        }
        if (target instanceof Player targetPlayer && owner instanceof Player ownerPlayer
                && !ownerPlayer.canHarmPlayer(targetPlayer)) {
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------ Datos

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FRIENDSHIP, 0);
        this.entityData.define(DATA_PLAYING, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FriendshipPoints", getFriendshipPoints());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_FRIENDSHIP, tag.getInt("FriendshipPoints"));
        applyFriendshipStats();
    }

    public int getFriendshipPoints() {
        return this.entityData.get(DATA_FRIENDSHIP);
    }

    public int getFriendshipLevel() {
        int points = getFriendshipPoints();
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (points >= LEVEL_THRESHOLDS[i]) {
                return i + 1;
            }
        }
        return 1;
    }

    public static double getAttackDamageForLevel(int level) {
        return MIN_DAMAGE + (level - 1) * ((MAX_DAMAGE - MIN_DAMAGE) / (MAX_LEVEL - 1));
    }

    public boolean isPlayingInWater() {
        return this.entityData.get(DATA_PLAYING);
    }

    public void setPlayingInWater(boolean playing) {
        this.entityData.set(DATA_PLAYING, playing);
    }

    private void applyFriendshipStats() {
        AttributeInstance attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(isTame() ? getAttackDamageForLevel(getFriendshipLevel()) : MIN_DAMAGE);
        }
    }

    @Override
    public void setTame(boolean tame) {
        super.setTame(tame);
        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(tame ? 20.0D : 10.0D);
        }
        if (tame) {
            setHealth(20.0F);
        }
        applyFriendshipStats();
    }

    // ----------------------------------------------------- Amistad / comida

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SALMON) || stack.is(Items.COOKED_SALMON)
                || stack.is(Items.COD) || stack.is(Items.COOKED_COD);
    }

    private int foodPoints(ItemStack stack) {
        if (stack.is(Items.SALMON)) return 10;
        if (stack.is(Items.COOKED_SALMON)) return 15;
        if (stack.is(Items.COOKED_COD)) return 8;
        return 5;
    }

    public void addFriendship(int amount, Player player) {
        if (this.level().isClientSide) {
            return;
        }
        int before = getFriendshipLevel();
        int max = LEVEL_THRESHOLDS[LEVEL_THRESHOLDS.length - 1];
        this.entityData.set(DATA_FRIENDSHIP, Math.min(max, getFriendshipPoints() + amount));
        int after = getFriendshipLevel();
        applyFriendshipStats();
        if (after > before) {
            onLevelUp(after, player);
        }
    }

    private void onLevelUp(int newLevel, Player player) {
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.5D, getZ(), 12, 0.3D, 0.3D, 0.3D, 0.0D);
        }
        this.playSound(SoundEvents.PLAYER_LEVELUP, 0.6F, 1.2F);
        if (player != null) {
            Component message = newLevel >= MAX_LEVEL
                    ? Component.translatable("message.nutriamod.max_level")
                    : Component.translatable("message.nutriamod.level_up", newLevel);
            player.displayClientMessage(message, false);
        }
    }

    private void showFriendship(Player player) {
        int level = getFriendshipLevel();
        double damage = Math.round(getAttackDamageForLevel(level) * 10.0D) / 10.0D;
        player.displayClientMessage(
                Component.translatable("message.nutriamod.friendship", level, MAX_LEVEL, damage), true);
    }

    private void spawnHearts() {
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.6D, getZ(), 4, 0.3D, 0.2D, 0.3D, 0.0D);
        }
    }

    private void feed(Player player, ItemStack stack) {
        if (feedCooldown > 0) {
            player.displayClientMessage(Component.translatable("message.nutriamod.not_hungry"), true);
            return;
        }
        int points = foodPoints(stack);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        feedCooldown = FEED_COOLDOWN_TICKS;
        heal(4.0F);
        this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F);
        spawnHearts();
        addFriendship(points, player);
        showFriendship(player);
    }

    private void pet(Player player) {
        if (petCooldown > 0) {
            return;
        }
        petCooldown = PET_COOLDOWN_TICKS;
        spawnHearts();
        this.playSound(SoundEvents.OCELOT_AMBIENT, 0.8F, 1.4F);
        addFriendship(PET_POINTS, player);
        showFriendship(player);
    }

    private void toggleSit(Player player) {
        boolean sit = !isOrderedToSit();
        setOrderedToSit(sit);
        this.navigation.stop();
        setTarget(null);
        player.displayClientMessage(
                Component.translatable(sit ? "message.nutriamod.stay" : "message.nutriamod.follow"), true);
    }

    // ------------------------------------------------------- Interacción

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean client = this.level().isClientSide;

        if (!isTame()) {
            if (stack.is(Items.SALMON)) {
                if (!client) {
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    if (this.random.nextInt(3) == 0) {
                        tame(player);
                        this.navigation.stop();
                        setTarget(null);
                        setOrderedToSit(false);
                        setInSittingPose(false);
                        this.level().broadcastEntityEvent(this, (byte) 7);
                    } else {
                        this.level().broadcastEntityEvent(this, (byte) 6);
                    }
                }
                return InteractionResult.sidedSuccess(client);
            }
            return InteractionResult.PASS;
        }

        if (!isOwnedBy(player)) {
            return InteractionResult.PASS;
        }

        if (isFood(stack)) {
            if (!client) {
                feed(player, stack);
            }
            return InteractionResult.sidedSuccess(client);
        }

        if (hand == InteractionHand.MAIN_HAND) {
            if (player.isShiftKeyDown()) {
                if (!client) {
                    toggleSit(player);
                }
                return InteractionResult.sidedSuccess(client);
            }
            if (stack.isEmpty()) {
                if (!client) {
                    pet(player);
                }
                return InteractionResult.sidedSuccess(client);
            }
        }
        return InteractionResult.PASS;
    }

    // ------------------------------------------------ Inmortalidad y agua

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (isTame() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.7F;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        if (feedCooldown > 0) feedCooldown--;
        if (petCooldown > 0) petCooldown--;

        if (isTame()) {
            if (isOnFire()) {
                clearFire();
            }
            if (getY() < this.level().getMinBuildHeight() - 8) {
                rescueFromVoid();
            }
        }

        // Si está quieta en el agua, flota hasta la superficie.
        if (isEyeInFluid(FluidTags.WATER) && this.navigation.isDone()) {
            setDeltaMovement(getDeltaMovement().add(0.0D, 0.012D, 0.0D));
        }
    }

    private void rescueFromVoid() {
        LivingEntity owner = getOwner();
        if (owner != null) {
            teleportTo(owner.getX(), owner.getY(), owner.getZ());
            setDeltaMovement(Vec3.ZERO);
            this.fallDistance = 0.0F;
            this.navigation.stop();
        }
    }

    // ------------------------------------------------------------ Sonidos

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.OCELOT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.OCELOT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.OCELOT_DEATH;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mate) {
        return ModEntities.OTTER.get().create(level);
    }
}
