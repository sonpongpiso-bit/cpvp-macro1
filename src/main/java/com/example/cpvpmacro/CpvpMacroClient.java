package com.example.cpvpmacro;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

public class CpvpMacroClient implements ClientModInitializer {
    private static boolean enabled = false;
    private static int cooldown = 0;
    private static final int DELAY_TICKS = 2; // raise if you get kicked / desync

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("cpvpmacro", "main"));

        KeyMapping toggle = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.cpvpmacro.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (toggle.consumeClick()) {
                enabled = !enabled;
                if (mc.player != null)
                    mc.player.sendSystemMessage(Component.literal("CPvP Macro: " + (enabled ? "ON" : "OFF")));
            }
            if (!enabled || mc.player == null || mc.level == null || mc.gameMode == null) return;
            if (mc.screen != null) return;
            if (cooldown > 0) { cooldown--; return; }
            tick(mc);
        });
    }

    private void tick(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit == null) return;

        // Break the crystal you're aiming at
        if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof EndCrystal crystal) {
            mc.gameMode.attack(mc.player, crystal);
            mc.player.swing(InteractionHand.MAIN_HAND);
            cooldown = DELAY_TICKS;
            return;
        }

        // Place a crystal on obsidian/bedrock while holding one
        if (hit instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK
                && mc.player.getMainHandItem().is(Items.END_CRYSTAL)) {
            var state = mc.level.getBlockState(bhr.getBlockPos());
            if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK)) {
                InteractionResult r = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, bhr);
                if (r.consumesAction()) mc.player.swing(InteractionHand.MAIN_HAND);
                cooldown = DELAY_TICKS;
            }
        }
    }
}
