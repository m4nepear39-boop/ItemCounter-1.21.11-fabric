package com.example.itemcounter;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** The screen where you drag the counter and open the item picker. */
public class HudEditScreen extends Screen {
    private boolean dragging = false;
    private boolean wasMouseDown = false;
    private int grabOffsetX, grabOffsetY;

    public HudEditScreen() {
        super(Text.literal("Edit Item Counter"));
    }

    @Override
    protected void init() {
        wasMouseDown = isMouseDown(); // so a click that opened this screen doesn't start a drag

        ItemStack target = ItemCounterHud.getTargetStack();
        String name = target.isEmpty() ? "none" : target.getName().getString();
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Item: " + name + "  (click to change)"),
                button -> this.client.setScreen(new ItemPickerScreen())
        ).dimensions(this.width / 2 - 110, 35, 220, 20).build());
    }

    private boolean isMouseDown() {
        long window = this.client.getWindow().getHandle();
        return GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Keep the game visible (no blur), just a light dark tint
        context.fill(0, 0, this.width, this.height, 0x60000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta); // draws background + button

        context.drawCenteredTextWithShadow(this.textRenderer, "Drag the counter with your mouse. Press ESC when done.", this.width / 2, 15, 0xFFFFFFFF);

        ItemCounterConfig cfg = ItemCounterConfig.INSTANCE;
        int w = ItemCounterHud.getWidth();
        int h = ItemCounterHud.HEIGHT;

        // --- Dragging (we just check if the left mouse button is held down) ---
        boolean mouseDown = isMouseDown();
        boolean overBox = mouseX >= cfg.x && mouseX <= cfg.x + w && mouseY >= cfg.y && mouseY <= cfg.y + h;

        if (mouseDown && !wasMouseDown && overBox) {
            dragging = true;
            grabOffsetX = mouseX - cfg.x;
            grabOffsetY = mouseY - cfg.y;
        }
        if (!mouseDown && dragging) {
            dragging = false;
            ItemCounterConfig.save();
        }
        if (dragging) {
            cfg.x = Math.max(0, Math.min(mouseX - grabOffsetX, this.width - w));
            cfg.y = Math.max(0, Math.min(mouseY - grabOffsetY, this.height - h));
        }
        wasMouseDown = mouseDown;

        // --- Draw the counter + an outline (yellow while hovering/dragging) ---
        ItemCounterHud.draw(context);
        int outline = (dragging || overBox) ? 0xFFFFFF55 : 0xFFFFFFFF;
        context.fill(cfg.x - 1, cfg.y - 1, cfg.x + w + 1, cfg.y, outline);
context.fill(cfg.x - 1, cfg.y + h, cfg.x + w + 1, cfg.y + h + 1, outline);
context.fill(cfg.x - 1, cfg.y, cfg.x, cfg.y + h, outline);
context.fill(cfg.x + w, cfg.y, cfg.x + w + 1, cfg.y + h, outline);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        ItemCounterConfig.save();
        super.close();
    }
}
