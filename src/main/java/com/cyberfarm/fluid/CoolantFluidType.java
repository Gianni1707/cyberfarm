package com.cyberfarm.fluid;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * Coolant and Cryo Coolant. Living entities move through them like through water, only slower:
 * NeoForge leaves the movement in a modded fluid to its type, and without it entities stand still.
 */
public class CoolantFluidType extends FluidType {
    private static final float WATER_ACCELERATION = 0.02F;
    private static final float WATER_DRAG = 0.8F;
    private static final float SPRINT_DRAG = 0.9F;

    /** Fraction of the water speed an entity reaches in this coolant. */
    private final float thickness;

    public CoolantFluidType(Properties properties, float thickness) {
        super(properties);
        this.thickness = thickness;
    }

    @Override
    public boolean move(FluidState state, LivingEntity entity, Vec3 input, double gravity) {
        return move(entity, input, gravity);
    }

    /** The vanilla water movement (accelerate, move, drag, sink, climb out at the edges), with less push. */
    @Override
    public boolean move(LivingEntity entity, Vec3 input, double gravity) {
        boolean falling = entity.getDeltaMovement().y <= 0.0;
        double startY = entity.getY();
        entity.moveRelative(WATER_ACCELERATION * thickness, input);
        entity.move(MoverType.SELF, entity.getDeltaMovement());
        float drag = entity.isSprinting() ? SPRINT_DRAG : WATER_DRAG;
        Vec3 motion = entity.getDeltaMovement().multiply(drag, WATER_DRAG, drag);
        motion = entity.getFluidFallingAdjustedMovement(gravity, falling, motion);
        entity.setDeltaMovement(motion);
        if (entity.horizontalCollision && entity.isFree(motion.x, motion.y + 0.6 - entity.getY() + startY, motion.z)) {
            entity.setDeltaMovement(motion.x, 0.3, motion.z);
        }
        return true;
    }
}
