package com.example.itemcounter;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.Optional;

/** Everything about counting and drawing. */
public class ItemCounterHud {
    public static final int HEIGHT = 16;

    /** The chosen Item (diamond, stone, potion...). AIR means "nothing / invalid". */
    public static Item getItem() {
        Identifier id = Identifier.tryParse(ItemCounterConfig.INSTANCE.item.trim().toLowerCase());
        if (id == null || !Registries.ITEM.containsId(id)) return Items.AIR;
        return Registries.ITEM.get(id);
    }

    /** Finds a potion type like "minecraft:healing". */
    public static RegistryEntry<Potion> findPotion(String potionId) {
        Identifier id = Identifier.tryParse(potionId);
        if (id == null) return null;
        for (var entry : Registries.POTION.streamEntries().toList()) {
            if (entry.registryKey().getValue().equals(id)) return entry;
        }
        return null;
    }

    /** The stack used for drawing the icon (a potion gets its correct color/type). */
    public static ItemStack getTargetStack() {
        Item item = getItem();
        if (item == Items.AIR) return ItemStack.EMPTY;
        String potion = ItemCounterConfig.INSTANCE.potion;
        if (!potion.isEmpty()) {
            RegistryEntry<Potion> entry = findPotion(potion);
            if (entry != null) return PotionContentsComponent.createStack(item, entry);
        }
        return new ItemStack(item);
    }

    /** Does this inventory stack count as the chosen item? */
    private static boolean matches(ItemStack stack, Item item, String potion) {
        if (!stack.isOf(item)) return false;
        if (potion.isEmpty()) return true;
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) return false;
        Optional<RegistryEntry<Potion>> p = contents.potion();
        return p.isPresent() && p.get().getKey().map(k -> k.getValue().toString()).orElse("").equals(potion);
    }

    /** Counts the item in the whole inventory (hotbar, main, armor, offhand). */
    public static int countItem() {
        MinecraftClient client = MinecraftClient.getInstance();
        Item item = getItem();
        if (client.player == null || item == Items.AIR) return 0;
        String potion = ItemCounterConfig.INSTANCE.potion;
        PlayerInventory inv = client.player.getInventory();
        int total = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (matches(stack, item, potion)) total += stack.getCount();
        }
        return total;
    }

    /** Width of the draggable area. */
    public static int getWidth() {
        if (getItem() == Items.AIR) {
            return MinecraftClient.getInstance().textRenderer.getWidth("No item");
        }
        return 16;
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        // The edit/picker screens draw the counter themselves
        if (client.currentScreen instanceof HudEditScreen || client.currentScreen instanceof ItemPickerScreen) return;
        draw(context);
    }

    /** Draws the item icon with the count in the corner, like a normal inventory slot. */
    public static void draw(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        int x = ItemCounterConfig.INSTANCE.x;
        int y = ItemCounterConfig.INSTANCE.y;

        ItemStack stack = getTargetStack();
        if (stack.isEmpty()) {
            context.drawTextWithShadow(client.textRenderer, "No item", x, y + 4, 0xFFFF5555);
            return;
        }
        context.drawItem(stack, x, y);
        context.drawStackOverlay(client.textRenderer, stack, x, y, String.valueOf(countItem()));
    }
}
