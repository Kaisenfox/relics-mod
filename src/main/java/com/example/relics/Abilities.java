package com.example.relics;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;

/** The right-click abilities. Each returns true if it fired. */
public final class Abilities {
    private Abilities() {}

    private static boolean noTarget(PlayerEntity player) {
        player.sendMessage(Text.literal("No target in range"), true);
        return false;
    }

    // ============ 1. Thunder Scepter: a barrage of lightning ============
    public static boolean thunder(ServerWorld world, PlayerEntity player) {
        Vec3d t = Fx.rayTarget(world, player, 64);
        if (t == null) return noTarget(player);

        for (int i = 0; i < 5; i++) {
            final int idx = i;
            Scheduler.later(i * 3, () -> {
                double ox = idx == 0 ? 0 : (world.random.nextDouble() - 0.5) * 8;
                double oz = idx == 0 ? 0 : (world.random.nextDouble() - 0.5) * 8;
                strike(world, t.x + ox, t.y, t.z + oz);
            });
        }
        Fx.ring(world, t, 6, 6, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.END_ROD);
        return true;
    }

    private static void strike(ServerWorld world, double x, double y, double z) {
        LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
        if (bolt != null) {
            bolt.refreshPositionAfterTeleport(x, y, z);
            world.spawnEntity(bolt);
        }
    }

    // ============ 2. Blink Staff: teleport where you look ============
    public static boolean blink(ServerWorld world, PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity sp)) return false;

        Vec3d start = player.getEyePos();
        Vec3d dir = player.getRotationVec(1.0f);
        Vec3d end = start.add(dir.multiply(32));
        BlockHitResult hit = world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        if (hit.getType() != HitResult.Type.MISS) end = hit.getPos();

        Vec3d delta = end.subtract(start);
        boolean fits = false;
        for (int i = 0; i < 70 && delta.length() > 1.0; i++) {
            if (world.isSpaceEmpty(player, player.getBoundingBox().offset(delta))) {
                fits = true;
                break;
            }
            delta = delta.subtract(dir.multiply(0.5));
        }
        if (!fits) {
            player.sendMessage(Text.literal("Something is in the way"), true);
            return false;
        }

        Vec3d from = player.getPos();
        Vec3d to = from.add(delta);
        double len = delta.length();
        Vec3d step = delta.normalize();
        for (double d = 0; d < len; d += 0.6) {
            Vec3d p = from.add(0, 1, 0).add(step.multiply(d));
            world.spawnParticles(ParticleTypes.PORTAL, p.x, p.y, p.z, 4, 0.2, 0.3, 0.2, 0.1);
        }
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1, from.z, 40, 0.3, 0.6, 0.3, 0.2);
        world.playSound(null, from.x, from.y, from.z,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);

        sp.networkHandler.requestTeleport(to.x, to.y, to.z, sp.getYaw(), sp.getPitch());
        sp.fallDistance = 0;

        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, to.x, to.y + 1, to.z, 40, 0.3, 0.6, 0.3, 0.2);
        world.playSound(null, to.x, to.y, to.z,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.4f);
        return true;
    }

    // ============ 3. Meteor Staff: five meteors fall from the sky ============
    public static boolean meteor(ServerWorld world, PlayerEntity player) {
        Vec3d t = Fx.rayTarget(world, player, 80);
        if (t == null) return noTarget(player);

        for (int i = 0; i < 5; i++) {
            final double ox = i == 0 ? 0 : (world.random.nextDouble() - 0.5) * 10;
            final double oz = i == 0 ? 0 : (world.random.nextDouble() - 0.5) * 10;
            Scheduler.later(i * 5, () -> dropMeteor(world, player, new Vec3d(t.x + ox, t.y, t.z + oz)));
        }
        return true;
    }

    private static void dropMeteor(ServerWorld world, PlayerEntity player, Vec3d impact) {
        final int fall = 14;
        final double height = 36;
        world.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 2.0f, 0.5f);
        for (int i = 0; i <= fall; i++) {
            final int tick = i;
            Scheduler.later(i, () -> {
                if (tick < fall) {
                    double f = 1.0 - tick / (double) fall;
                    Vec3d p = impact.add(f * 8, f * height, f * 3);
                    world.spawnParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 8, 0.3, 0.3, 0.3, 0.05);
                    world.spawnParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 3, 0.3, 0.3, 0.3, 0.02);
                    world.spawnParticles(ParticleTypes.LAVA, p.x, p.y, p.z, 1, 0.2, 0.2, 0.2, 0.0);
                } else {
                    world.createExplosion(player, impact.x, impact.y, impact.z, 3.5f, true,
                            World.ExplosionSourceType.TNT);
                    world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, impact.x, impact.y + 0.5, impact.z, 1, 0, 0, 0, 0);
                    Fx.ring(world, impact, 5, 5, ParticleTypes.FLAME, ParticleTypes.LAVA);
                }
            });
        }
    }

    // ============ 4. Lifebloom Staff: heal + feed + shield nearby players ============
    public static boolean lifebloom(ServerWorld world, PlayerEntity player) {
        List<ServerPlayerEntity> allies = world.getPlayers(p -> p.isAlive() && p.squaredDistanceTo(player) <= 100.0);
        for (ServerPlayerEntity p : allies) {
            p.heal(20.0f);
            p.getHungerManager().add(10, 1.0f);
            p.removeStatusEffect(StatusEffects.POISON);
            p.removeStatusEffect(StatusEffects.WITHER);
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 2));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 600, 3));
            world.spawnParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.0, p.getZ(), 12, 0.5, 0.6, 0.5, 0.1);
        }

        final Vec3d c = player.getPos();
        for (int t = 0; t < 12; t++) {
            final int tick = t;
            Scheduler.later(t, () -> {
                double y = c.y + tick * 0.18;
                for (int k = 0; k < 16; k++) {
                    double a = (Math.PI * 2 * k) / 16 + tick * 0.2;
                    world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                            c.x + Math.cos(a) * 3.0, y, c.z + Math.sin(a) * 3.0, 1, 0, 0, 0, 0);
                }
            });
        }
        world.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.2f);
        return true;
    }

    // ============ 5. Gale Fan: launch yourself, blast enemies away ============
    public static boolean gale(ServerWorld world, PlayerEntity player) {
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d origin = player.getPos();

        Box box = player.getBoundingBox().expand(9);
        for (Entity e : world.getOtherEntities(player, box, ent -> ent instanceof LivingEntity)) {
            Vec3d rel = e.getPos().subtract(origin);
            double d = rel.length();
            if (d < 0.1 || d > 9) continue;
            if (rel.normalize().dotProduct(look) < 0.4) continue;
            e.setVelocity(rel.normalize().multiply(2.2).add(0, 0.6, 0));
            e.velocityModified = true;
        }

        player.setVelocity(look.x * 1.6, Math.max(look.y * 1.6, 0.0) + 0.9, look.z * 1.6);
        player.velocityModified = true;
        player.fallDistance = 0;
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 160, 0));

        for (int i = 0; i < 70; i++) {
            double dist = world.random.nextDouble() * 8;
            Vec3d p = origin.add(0, 1, 0).add(look.multiply(dist))
                    .add((world.random.nextDouble() - 0.5) * dist * 0.5,
                         (world.random.nextDouble() - 0.5) * dist * 0.5,
                         (world.random.nextDouble() - 0.5) * dist * 0.5);
            world.spawnParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 1, 0, 0, 0, 0.05);
        }
        Fx.ring(world, origin, 5, 5, ParticleTypes.CLOUD, ParticleTypes.POOF);
        world.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1.5f, 1.4f);
        return true;
    }

    // ============ 6. Black Hole Orb: 5 seconds of crushing gravity ============
    public static boolean blackHole(ServerWorld world, PlayerEntity player) {
        Vec3d t = Fx.rayTarget(world, player, 40);
        if (t == null) return noTarget(player);

        final Vec3d c = t.add(0, 2.0, 0);
        final int duration = 100;
        world.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.6f, 0.5f);

        for (int i = 0; i < duration; i++) {
            final int tick = i;
            Scheduler.later(i, () -> {
                // Three arms spiraling inward.
                for (int s = 0; s < 3; s++) {
                    double ang = tick * 0.4 + s * (Math.PI * 2 / 3);
                    double rad = 4.5 - (tick % 25) / 25.0 * 4.0;
                    double x = c.x + Math.cos(ang) * rad;
                    double z = c.z + Math.sin(ang) * rad;
                    double y = c.y + Math.sin(ang * 0.5) * rad * 0.3;
                    world.spawnParticles(ParticleTypes.PORTAL, x, y, z, 2, 0, 0, 0, 0.0);
                    world.spawnParticles(ParticleTypes.SMOKE, x, y, z, 1, 0, 0, 0, 0.0);
                }
                world.spawnParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 3, 0.2, 0.2, 0.2, 0.05);

                // Pull everything in.
                Box box = new Box(c.x - 10, c.y - 10, c.z - 10, c.x + 10, c.y + 10, c.z + 10);
                for (Entity e : world.getOtherEntities(player, box, ent -> ent instanceof LivingEntity && ent.isAlive())) {
                    Vec3d pull = c.subtract(e.getPos().add(0, e.getHeight() / 2, 0));
                    double d = pull.length();
                    if (d > 10 || d < 0.3) continue;
                    e.setVelocity(pull.normalize().multiply(Math.min(0.9, 0.3 + 4.0 / (d + 2))));
                    e.velocityModified = true;
                    if (tick % 10 == 0 && d < 4.0) {
                        e.timeUntilRegen = 0;
                        e.damage(world.getDamageSources().playerAttack(player), 4.0f);
                    }
                }

                // Collapse.
                if (tick == duration - 1) {
                    world.createExplosion(player, c.x, c.y, c.z, 4.0f, World.ExplosionSourceType.NONE);
                    world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
                    world.spawnParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
                    Fx.ring(world, new Vec3d(c.x, t.y, c.z), 8, 8, ParticleTypes.PORTAL, ParticleTypes.END_ROD);
                }
            });
        }
        return true;
    }

    // ============ 7. Chrono Watch: freeze everything nearby, you move at 4x ============
    public static boolean chrono(ServerWorld world, PlayerEntity player) {
        final double r = 14;
        Box box = player.getBoundingBox().expand(r);
        for (Entity e : world.getOtherEntities(player, box, ent -> ent instanceof LivingEntity && ent.isAlive())) {
            if (e.squaredDistanceTo(player) > r * r) continue;
            LivingEntity le = (LivingEntity) e;
            le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 160, 10));
            le.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 160, 4));
            le.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 160, 0));
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + e.getHeight() / 2, e.getZ(),
                    15, 0.4, 0.5, 0.4, 0.2);
        }
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 160, 3));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 160, 2));

        final Vec3d c = player.getPos();
        Fx.ring(world, c, r, 10, ParticleTypes.END_ROD, ParticleTypes.ELECTRIC_SPARK);
        for (int i = 0; i < 4; i++) {
            Scheduler.later(i * 8, () -> world.playSound(null, c.x, c.y, c.z,
                    SoundEvents.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1.5f, 0.5f));
        }
        Scheduler.later(32, () -> world.playSound(null, c.x, c.y, c.z,
                SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 1.5f, 0.6f));
        return true;
    }

    // ============ 8. Phoenix Feather: passive resurrection ============
    public static void phoenixRevive(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        final Vec3d c = player.getPos();

        player.setHealth(player.getMaxHealth());
        player.clearStatusEffects();
        player.extinguish();
        player.timeUntilRegen = 60;
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 600, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 2));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 600, 4));
        // The death-cancel hook can reset health to 1, so heal again next tick.
        Scheduler.later(1, () -> player.setHealth(player.getMaxHealth()));

        world.sendEntityStatus(player, (byte) 35); // totem pop animation

        Fx.damageNearby(world, player, c, 7.0, 10.0f, 1.2);
        for (Entity e : world.getOtherEntities(player, new Box(c, c).expand(7), ent -> ent instanceof LivingEntity)) {
            e.setOnFireFor(8);
        }

        Fx.ring(world, c, 7, 8, ParticleTypes.FLAME, ParticleTypes.LAVA);
        for (int t = 0; t < 16; t++) {
            final int tick = t;
            Scheduler.later(t, () -> {
                for (int s = 0; s < 4; s++) {
                    double a = tick * 0.5 + s * (Math.PI / 2);
                    double rad = 2.5 - tick * 0.1;
                    world.spawnParticles(ParticleTypes.FLAME,
                            c.x + Math.cos(a) * rad, c.y + tick * 0.2, c.z + Math.sin(a) * rad, 2, 0.05, 0.05, 0.05, 0.02);
                }
            });
        }
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1, c.z, 1, 0, 0, 0, 0);
        world.playSound(null, c.x, c.y, c.z, SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }
}
