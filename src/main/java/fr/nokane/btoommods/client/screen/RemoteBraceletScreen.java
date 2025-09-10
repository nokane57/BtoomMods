package fr.nokane.btoommods.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import fr.nokane.btoommods.net.Net;
import fr.nokane.btoommods.net.RemoteQuerySlotsC2S;
import fr.nokane.btoommods.net.RemoteTriggerC2S;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class RemoteBraceletScreen extends Screen {
    private static final int GUI_W = 160;
    private static final int GUI_H = 180;
    private static final ResourceLocation BG =
            new ResourceLocation("btoommods", "textures/gui/remote_bracelet.png");

    // Palette (slots 1..8)
    private static final int[] SLOT_ACCENT = new int[]{
            0xFFFF6B6B, // 1
            0xFFFFD166, // 2
            0xFF06D6A0, // 3
            0xFF118AB2, // 4
            0xFF9B5DE5, // 5
            0xFFF15BB5, // 6
            0xFFEF476F, // 7
            0xFF1B9AAA  // 8
    };
    private static final int OFF_BG   = 0x40FFFFFF;
    private static final int OFF_HOVER= 0x60FFFFFF;
    private static final int OFF_TEXT = 0x90FFFFFF;

    private final Map<Integer, SlotButton> slotBtns = new HashMap<>();
    private int rescanTicker = 0;

    public RemoteBraceletScreen() { super(new StringTextComponent("Remote Bracelet")); }

    @Override
    protected void init() {
        super.init();
        if (this.minecraft != null) this.minecraft.keyboardHandler.setSendRepeatsToGui(true);

        int left = (this.width - GUI_W) / 2;
        int top  = (this.height - GUI_H) / 2;

        // face dans le PNG
        final int FACE_X = 55, FACE_Y = 40, FACE_W = 72, FACE_H = 136;

        int bw = 9, bh = 9, pad = 3; // tailles demandées
        int gridW = 2 * bw + pad;
        int gridH = 4 * bh + 3 * pad;

        int faceLeft = left + FACE_X;
        int faceTop  = top  + FACE_Y;

        int bx = faceLeft + (FACE_W - gridW) / 2;
        int by = faceTop  + (FACE_H - gridH) / 2;

        // 8 boutons
        int id = 1;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 2; col++) {
                final int slot = id++;
                int x = bx + col * (bw + pad);
                int y = by + row * (bh + pad);

                SlotButton btn = new SlotButton(x, y, bw, bh, slot);
                this.addButton(btn);
                slotBtns.put(slot, btn);
            }
        }

        // première requête serveur
        requestSlots();
    }

    /** Demande au serveur quels slots sont actifs. */
    private void requestSlots() {
        Net.CH.sendToServer(new RemoteQuerySlotsC2S());
    }

    /** Reçoit un bitmask (bits 0..7 pour slots 1..8) et met à jour le rendu. */
    public void applySlotMask(int mask) {
        for (int s = 1; s <= 8; s++) {
            SlotButton b = slotBtns.get(s);
            if (b != null) b.setLit(((mask >> (s - 1)) & 1) == 1);
        }
    }

    /** Bouton de slot colorable. */
    public class SlotButton extends Button {
        private final int slot;
        private boolean lit = false;

        public SlotButton(int x, int y, int w, int h, int slot) {
            super(x, y, w, h, new StringTextComponent(Integer.toString(slot)), b -> {
                Net.CH.sendToServer(new RemoteTriggerC2S(slot));
                if (RemoteBraceletScreen.this.minecraft != null)
                    RemoteBraceletScreen.this.minecraft.setScreen(null);
            });
            this.slot = slot;
        }

        public void setLit(boolean v) { this.lit = v; }

        @Override
        public void renderButton(MatrixStack ms, int mouseX, int mouseY, float partial) {
            boolean hover = this.isMouseOver(mouseX, mouseY);
            if (lit) {
                int accent = SLOT_ACCENT[(slot - 1) % SLOT_ACCENT.length] & 0x00FFFFFF;
                int bg     = ((hover ? 0x99 : 0x66) << 24) | accent; // alpha 0x66 / 0x99
                int text   = 0xFFFFFFFF;
                fill(ms, this.x, this.y, this.x + this.width, this.y + this.height, bg);
                drawCenteredString(ms, RemoteBraceletScreen.this.font,
                        this.getMessage().getString(),
                        this.x + this.width / 2,
                        this.y + (this.height - 8) / 2,
                        text);
            } else {
                int bg   = hover ? OFF_HOVER : OFF_BG;
                fill(ms, this.x, this.y, this.x + this.width, this.y + this.height, bg);
                drawCenteredString(ms, RemoteBraceletScreen.this.font,
                        this.getMessage().getString(),
                        this.x + this.width / 2,
                        this.y + (this.height - 8) / 2,
                        OFF_TEXT);
            }
        }
    }

    // 1..8 pour déclencher + ESC pour fermer
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int slot = -1;
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_8)
            slot = (keyCode - GLFW.GLFW_KEY_1) + 1;
        if (keyCode >= GLFW.GLFW_KEY_KP_1 && keyCode <= GLFW.GLFW_KEY_KP_8)
            slot = (keyCode - GLFW.GLFW_KEY_KP_1) + 1;

        if (slot >= 1 && slot <= 8) {
            Net.CH.sendToServer(new RemoteTriggerC2S(slot));
            if (this.minecraft != null) this.minecraft.setScreen(null);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (this.minecraft != null) this.minecraft.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // Miroir des touches + requête périodique (toutes les 10 ticks)
    @Override
    public void tick() {
        super.tick();
        if (this.minecraft == null) return;

        long win = this.minecraft.getWindow().getWindow();
        KeyBinding[] mv = new KeyBinding[]{
                this.minecraft.options.keyUp,
                this.minecraft.options.keyDown,
                this.minecraft.options.keyLeft,
                this.minecraft.options.keyRight,
                this.minecraft.options.keyJump,
                this.minecraft.options.keySprint,
                this.minecraft.options.keyShift
        };
        for (KeyBinding kb : mv) {
            if (kb == null) continue;
            InputMappings.Input key = kb.getKey();
            int glfwKey = (int) key.getValue();
            boolean pressed = GLFW.glfwGetKey(win, glfwKey) == GLFW.GLFW_PRESS
                    || GLFW.glfwGetKey(win, glfwKey) == GLFW.GLFW_REPEAT;
            KeyBinding.set(key, pressed);
        }

        if (++rescanTicker % 10 == 0) requestSlots();
    }

    @Override
    public void removed() {
        super.removed();
        if (this.minecraft != null) {
            this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
            KeyBinding[] mv = new KeyBinding[]{
                    this.minecraft.options.keyUp,
                    this.minecraft.options.keyDown,
                    this.minecraft.options.keyLeft,
                    this.minecraft.options.keyRight,
                    this.minecraft.options.keyJump,
                    this.minecraft.options.keySprint,
                    this.minecraft.options.keyShift
            };
            for (KeyBinding kb : mv) if (kb != null) KeyBinding.set(kb.getKey(), false);
        }
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(ms);
        int left = (this.width - GUI_W) / 2;
        int top  = (this.height - GUI_H) / 2;

        if (this.minecraft != null) this.minecraft.getTextureManager().bind(BG);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        blit(ms, left, top, 0, 0, GUI_W, GUI_H);

        super.render(ms, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
