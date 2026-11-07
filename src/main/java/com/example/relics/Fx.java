package com.example.relics;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** Shared helpers: targeting, expanding rings, area damage. */
public final class Fx {
    private Fx() {}

    /** Point the player is looking at on a block, or null if nothing is in range. */
    public static Vec3d rayTarget(ServerWorld world, PlayerEntity player, double range) {
        Vec3d start = player.getEyePos();
        Vec3d end = start.add(player.getRotationVec(1.0f).multiply(range));
        BlockHitResult hit = world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? null : hit.getPos();
    }

    /** An expanding ring on the ground that animates over several ticks. */
    public static void ring(ServerWorld world, Vec3d origin, double maxRadius, int ticks,
                            ParticleEffect main, ParticleEffect accent) {
        for (int i = 1; i <= ticks; i++) {
            final double r = maxRadius * i / ticks;
            Scheduler.later(i - 1, () -> {
                int points = (int) (r * 8) + 8;
                for (int k = 0; k < points; k++) {
                    double a = (Math.PI * 2 * k) / points;
                    double x = origin.x + Math.cos(a) * r;
                    double z = origin.z + Math.sin(a) * r;
                    world.spawnParticles(main, x, origin.y + 0.2, z, 1, 0, 0.02, 0, 0.02);
                    if (k % 3 == 0) {
                        world.spawnParticles(accent, x, origin.y + 0.1, z, 1, 0, 0.05, 0, 0.02);
                    }
                }
            });
        }
    }

    /** Damage and knock back every living thing within a horizontal radius. */
    public static void damageNearby(ServerWorld world, PlayerEntity owner, Vec3d c,
                                    double radius, float damage, double knock) {
        Box box = new Box(c.x - radius, c.y - 1, c.z - radius, c.x + radius, c.y + radius, c.z + radius);
        for (Entity e : world.getOtherEntities(owner, box, ent -> ent instanceof LivingEntity && ent.isAlive())) {
            double dx = e.getX() - c.x;
            double dz = e.getZ() - c.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > radius) continue;
            e.timeUntilRegen = 0;
            e.damage(world.getDamageSources().playerAttack(owner), damage);
            if (dist > 0.01 && knock > 0) {
                e.addVelocity(dx / dist * knock, 0.35, dz / dist * knock);
                e.velocityModified = true;
            }
        }
    }
}
