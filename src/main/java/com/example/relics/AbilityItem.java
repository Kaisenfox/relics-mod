package com.example.relics;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** A right-click item whose behavior is a function. */
public class AbilityItem extends Item {

    @FunctionalInterface
    public interface Ability {
        /** @return true if the ability fired (and the cooldown should start). */
        boolean activate(ServerWorld world, PlayerEntity player);
    }

    private final Ability ability;
    private final int cooldownTicks;
    private final String[] lines;

    public AbilityItem(Settings settings, int cooldownTicks, Ability ability, String... lines) {
        super(settings);
        this.ability = ability;
        this.cooldownTicks = cooldownTicks;
        this.lines = lines;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.pass(stack);
        }
        if (!(world instanceof ServerWorld serverWorld)) {
            return TypedActionResult.success(stack, true);
        }

        boolean fired = ability.activate(serverWorld, player);
        if (!fired) {
            return TypedActionResult.fail(stack);
        }
        player.getItemCooldownManager().set(this, cooldownTicks);
        return TypedActionResult.success(stack, false);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        for (String line : lines) {
            tooltip.add(Text.literal(line).formatted(Formatting.GRAY));
        }
    }
}
