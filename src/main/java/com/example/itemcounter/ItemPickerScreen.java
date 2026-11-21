package com.example.itemcounter;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A grid of every item / block / potion in the game with a search bar. Click one to choose it. */
public class ItemPickerScreen extends Screen {
    private static final int CELL = 20;

    /** One thing you can pick. */
    private record Entry(ItemStack stack, String itemId, String potionId, String searchText) {}

    // Built once, then reused
    private static List<Entry> allEntries = null;

    private List<Entry> filtered = new ArrayList<>();
    private TextFieldWidget searchField;
    private int scroll = 0;
    private boolean wasMouseDown = true;

    public ItemPickerScreen() {
        super(Text.literal("Choose an item"));
    }

    private static List<Entry> buildEntries() {
        List<Entry> list = new ArrayList<>();
        for (Item item : Registries.ITEM) {
            if (item == Items.AIR) continue;
            String itemId = Registries.ITEM.getId(item).toString();

            boolean isPotionType = item == Items.POTION || item == Items.SPLASH_POTION
                    || item == Items.LINGERING_POTION || item == Items.TIPPED_ARROW;
            if (isPotionType) {
                // One entry for every kind of potion (Healing, Swiftness, ...)
                for (var potion : Registries.POTION.streamEntries().toList()) {
                    ItemStack stack = PotionContentsComponent.createStack(item, potion);
                    String potionId = potion.registryKey().getValue().toString();
                    list.add(makeEntry(stack, itemId, potionId));
                }
            } else {
                list.add(makeEntry(new ItemStack(item), itemId, ""));
            }
        }
        return list;
    }

    private static Entry makeEntry(ItemStack stack, String itemId, String potionId) {
        String search = (stack.getName().getString() + " " + itemId + " " + potionId).toLowerCase(Locale.ROOT);
        return new Entry(stack, itemId, potionId, search);
    }

    @Override
    protected void init() {
        if (allEntries == null) allEntries = buildEntries();
        wasMouseDown = isMouseDown(); // ignore the click that opened this screen

        searchField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 28, 200, 20, Text.literal("Search"));
        searchField.setPlaceholder(Text.literal("Search (e.g. diamond, stone, healing)..."));
        searchField.setChangedListener(text -> {
            String q = text.toLowerCase(Locale.ROOT).trim();
            filtered = new ArrayList<>();
            for (Entry e : allEntries) {
                if (q.isEmpty() || e.searchText().contains(q)) filtered.add(e);
            }
            scroll = 0;
        });
        this.addDrawableChild(searchField);
        this.setInitialFocus(searchField);
        filtered = new ArrayList<>(allEntries);
    }

    private boolean isMouseDown() {
        long window = this.client.getWindow().getHandle();
        return GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, "Click an item to count it (scroll for more, ESC to go back)", this.width / 2, 12, 0xFFFFFFFF);

        // --- Grid layout ---
        int cols = Math.max(1, Math.min(14, (this.width - 40) / CELL));
        int gridX = (this.width - cols * CELL) / 2;
        int gridY = 58;
        int rowsVisible = Math.max(1, (this.height - gridY - 10) / CELL);
        int totalRows = (filtered.size() + cols - 1) / cols;
        int maxScroll = Math.max(0, totalRows - rowsVisible);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        ItemCounterConfig cfg = ItemCounterConfig.INSTANCE;
        Entry hovered = null;

        for (int row = 0; row < rowsVisible; row++) {
            for (int col = 0; col < cols; col++) {
                int index = (scroll + row) * cols + col;
                if (index >= filtered.size()) break;
                Entry entry = filtered.get(index);
                int cx = gridX + col * CELL;
                int cy = gridY + row * CELL;

                boolean isHover = mouseX >= cx && mouseX < cx + CELL && mouseY >= cy && mouseY < cy + CELL;
                boolean isSelected = entry.itemId().equals(cfg.item) && entry.potionId().equals(cfg.potion);

                context.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, (isHover ? 0xA0FFFFFF : 0x40FFFFFF));
                if (isSelected) context.drawBorder(cx, cy, CELL, CELL, 0xFFFFFF55);
                context.drawItem(entry.stack(), cx + 2, cy + 2);
                if (isHover) hovered = entry;
            }
        }

        if (filtered.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, "Nothing found", this.width / 2, gridY + 20, 0xFFAAAAAA);
        }

        // --- Click detection (left mouse button just got pressed) ---
        boolean down = isMouseDown();
        if (down && !wasMouseDown && hovered != null) {
            cfg.item = hovered.itemId();
            cfg.potion = hovered.potionId();
            ItemCounterConfig.save();
            this.client.setScreen(new HudEditScreen());
            return;
        }
        wasMouseDown = down;

        if (hovered != null) {
            context.drawItemTooltip(this.textRenderer, hovered.stack(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= (int) Math.signum(verticalAmount) * 2; // 2 rows per scroll tick
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        this.client.setScreen(new HudEditScreen()); // ESC goes back
    }
  }
              
