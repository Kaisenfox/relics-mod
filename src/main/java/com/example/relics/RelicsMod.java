package com.example.relics;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class RelicsMod implements ModInitializer {
    public static final String MOD_ID = "relics";

    public static Item THUNDER_SCEPTER;
    public static Item BLINK_STAFF;
    public static Item METEOR_STAFF;
    public static Item LIFEBLOOM_STAFF;
    public static Item BLOOD_FANG;
    public static Item TITAN_PICKAXE;
    public static Item GALE_FAN;
    public static Item BLACK_HOLE_ORB;
    public static Item CHRONO_WATCH;
    public static Item PHOENIX_FEATHER;

    private static Item.Settings relic() {
        return new Item.Settings().maxCount(1).rarity(Rarity.EPIC).fireproof();
    }

    private static Item register(String id, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(MOD_ID, id), item);
    }

    @Override
    public void onInitialize() {
        Scheduler.init();

        THUNDER_SCEPTER = register("thunder_scepter", new AbilityItem(relic(), 40, Abilities::thunder,
                "Right-click: call down a barrage of lightning", "Range: 64 blocks"));

        BLINK_STAFF = register("blink_staff", new AbilityItem(relic(), 30, Abilities::blink,
                "Right-click: teleport up to 32 blocks", "No fall damage on arrival"));

        METEOR_STAFF = register("meteor_staff", new AbilityItem(relic(), 200, Abilities::meteor,
                "Right-click: rain 5 meteors on your target", "Range: 80 blocks"));

        LIFEBLOOM_STAFF = register("lifebloom_staff", new AbilityItem(relic(), 160, Abilities::lifebloom,
                "Right-click: heal, feed and shield", "yourself and nearby players"));

        BLOOD_FANG = register("blood_fang", new BloodFangItem(relic()
                .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.DIAMOND, 4, -1.4f))));

        TITAN_PICKAXE = register("titan_pickaxe", new TitanPickaxeItem(relic()
                .attributeModifiers(PickaxeItem.createAttributeModifiers(ToolMaterials.NETHERITE, 1.0f, -2.8f))));

        GALE_FAN = register("gale_fan", new AbilityItem(relic(), 40, Abilities::gale,
                "Right-click: launch yourself skyward", "and blast enemies away"));

        BLACK_HOLE_ORB = register("black_hole_orb", new AbilityItem(relic(), 400, Abilities::blackHole,
                "Right-click: create a black hole for 5 seconds", "Pulls in and crushes everything nearby"));

        CHRONO_WATCH = register("chrono_watch", new AbilityItem(relic(), 600, Abilities::chrono,
                "Right-click: freeze everything within 14 blocks", "You move at high speed for 8 seconds"));

        PHOENIX_FEATHER = register("phoenix_feather", new Item(new Item.Settings()
                .maxCount(4).rarity(Rarity.EPIC).fireproof()) {
            @Override
            public boolean hasGlint(ItemStack stack) {
                return true;
            }

            @Override
            public void appendTooltip(ItemStack stack, TooltipContext context,
                                      java.util.List<Text> tooltip, net.minecraft.item.tooltip.TooltipType type) {
                tooltip.add(Text.literal("Keep in your inventory").formatted(net.minecraft.util.Formatting.GOLD));
                tooltip.add(Text.literal("Consumed to revive you from death").formatted(net.minecraft.util.Formatting.GRAY));
            }
        });

        // Phoenix Feather: cancel death if the player is carrying one.
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                PlayerInventory inv = player.getInventory();
                for (int i = 0; i < inv.size(); i++) {
                    ItemStack s = inv.getStack(i);
                    if (s.isOf(PHOENIX_FEATHER)) {
                        s.decrement(1);
                        Abilities.phoenixRevive(player);
                        return false;
                    }
                }
            }
            return true;
        });

        // Creative tab with everything in it.
        ItemGroup group = FabricItemGroup.builder()
                .icon(() -> new ItemStack(THUNDER_SCEPTER))
                .displayName(Text.translatable("itemGroup.relics.main"))
                .entries((context, entries) -> {
                    entries.add(THUNDER_SCEPTER);
                    entries.add(BLINK_STAFF);
                    entries.add(METEOR_STAFF);
                    entries.add(LIFEBLOOM_STAFF);
                    entries.add(BLOOD_FANG);
                    entries.add(TITAN_PICKAXE);
                    entries.add(GALE_FAN);
                    entries.add(BLACK_HOLE_ORB);
                    entries.add(CHRONO_WATCH);
                    entries.add(PHOENIX_FEATHER);
                })
                .build();
        Registry.register(Registries.ITEM_GROUP, Identifier.of(MOD_ID, "main"), group);
    }
}
