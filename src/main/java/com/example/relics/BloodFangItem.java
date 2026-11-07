package com.example.relics;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Fast dagger: heals you on every hit and withers the target. */
public class BloodFangItem extends SwordItem {
    public BloodFangItem(Settings settings) {
        super(ToolMaterials.DIAMOND, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld() instanceof ServerWorld world) {
            attacker.heal(3.0f);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 80, 1));
            world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR,
                    target.getX(), target.getY() + target.getHeight() * 0.6, target.getZ(), 8, 0.3, 0.3, 0.3, 0.2);
            world.spawnParticles(ParticleTypes.HEART,
                    attacker.getX(), attacker.getY() + 1.5, attacker.getZ(), 3, 0.3, 0.3, 0.3, 0.0);
            world.spawnParticles(ParticleTypes.CRIMSON_SPORE,
                    target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.4, 0.5, 0.4, 0.05);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Hits heal you and inflict Wither").formatted(Formatting.RED));
    }
}
