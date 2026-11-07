package com.example.relics;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/** Netherite pickaxe that mines a 3x3x3 cube. Sneak to mine normally. */
public class TitanPickaxeItem extends PickaxeItem {
    public TitanPickaxeItem(Settings settings) {
        super(ToolMaterials.NETHERITE, settings);
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient && miner instanceof PlayerEntity player && !player.isSneaking()) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos p = pos.add(dx, dy, dz);
                        BlockState s = world.getBlockState(p);
                        if (s.isAir()) continue;
                        if (s.getHardness(world, p) < 0) continue; // bedrock etc.
                        if (!s.isIn(BlockTags.PICKAXE_MINEABLE)) continue;
                        world.breakBlock(p, true, miner);
                    }
                }
            }
        }
        return super.postMine(stack, world, state, pos, miner);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Mines a 3x3x3 area").formatted(Formatting.AQUA));
        tooltip.add(Text.literal("Sneak to mine one block").formatted(Formatting.GRAY));
    }
}
