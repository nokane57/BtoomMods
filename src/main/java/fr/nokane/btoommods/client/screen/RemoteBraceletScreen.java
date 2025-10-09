package fr.nokane.btoommods.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import fr.nokane.btoommods.net.Net;
import fr.nokane.btoommods.net.RemoteQuerySlotsC2S;
import fr.nokane.btoommods.net.RemoteTriggerC2S;
import fr.nokane.btoommods.sound.SoundUtils;
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

    // 🎨 Palette des boutons 1..8 (identique au code RemoteBimEntity)
    public static final int[] SLOT_ACCENT = {
            0xFFFF6B6B, // 1 rouge
            0xFFFFD166, // 2 jaune
            0xFF06D6A0, // 3 vert menthe
            0xFF118AB2, // 4 bleu clair
            0xFF9B5DE5, // 5 violet
            0xFFF15BB5, // 6 rose
            0xFFEF476F, // 7 magenta
            0xFF1B9AAA  // 8 turquoise
    };
    private static final int OFF_BG = 0x40FFFFFF;
    private static final int OFF_HOVER = 0x60FFFFFF;
    private static final int OFF_TEXT = 0x90FFFFFF;

    private final Map<Integer, SlotButton> slotBtns = new HashMap<>();
    private int rescanTicker = 0;

    public RemoteBraceletScreen() {
        super(new StringTextComponent("Remote Bracelet"));
    }

    @Override
    protected void init() {
        super.init();
        if (minecraft != null)
            minecraft.keyboardHandler.setSendRepeatsToGui(true);

        int left = (this.width - GUI_W) / 2;
        int top = (this.height - GUI_H) / 2;

        // zone visible dans la texture
        final int FACE_X = 55, FACE_Y = 40, FACE_W = 72, FACE_H = 136;
        final int bw = 9, bh = 9, pad = 3;

        int gridW = 2 * bw + pad;
        int gridH = 4 * bh + 3 * pad;

        int faceLeft = left + FACE_X;
        int faceTop = top + FACE_Y;

        int bx = faceLeft + (FACE_W - gridW) / 2;
        int by = faceTop + (FACE_H - gridH) / 2;

        // 8 boutons (2 colonnes × 4 lignes)
        int id = 1;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 2; col++) {
                final int slot = id++;
                int x = bx + col * (bw + pad);
                int y = by + row * (bh + pad);
                SlotButton btn = new SlotButton(x, y, bw, bh, slot);
                this.addButton(btn); // ✅ version 1.16
                slotBtns.put(slot, btn);
            }
        }

        requestSlots();
    }

    /** 🔄 Demande au serveur les Remote BIM actives */
    private void requestSlots() {
        Net.CH.sendToServer(new RemoteQuerySlotsC2S());
    }

    /** 🔔 Mise à jour visuelle via le masque reçu */
    public void applySlotMask(int mask) {
        for (int s = 1; s <= 8; s++) {
            SlotButton b = slotBtns.get(s);
            if (b != null)
                b.setLit(((mask >> (s - 1)) & 1) == 1);
        }
    }

    /** Bouton individuel */
    public class SlotButton extends Button {
        private final int slot;
        private boolean lit = false;

        public SlotButton(int x, int y, int w, int h, int slot) {
            super(x, y, w, h, new StringTextComponent(Integer.toString(slot)), b -> {
                SoundUtils.playClac();
                Net.CH.sendToServer(new RemoteTriggerC2S(slot));
            });
            this.slot = slot;
        }

        public void setLit(boolean v) {
            this.lit = v;
        }

        @Override
        public void renderButton(MatrixStack ms, int mouseX, int mouseY, float partial) {
            boolean hover = this.isMouseOver(mouseX, mouseY);
            int accent = SLOT_ACCENT[(slot - 1) % SLOT_ACCENT.length] & 0x00FFFFFF;

            if (lit) {
                int bg = ((hover ? 0x99 : 0x66) << 24) | accent;
                fill(ms, this.x, this.y, this.x + this.width, this.y + this.height, bg);
                drawCenteredString(ms, RemoteBraceletScreen.this.font, this.getMessage().getString(),
                        this.x + this.width / 2, this.y + (this.height - 8) / 2, 0xFFFFFFFF);
            } else {
                int bg = hover ? OFF_HOVER : OFF_BG;
                fill(ms, this.x, this.y, this.x + this.width, this.y + this.height, bg);
                drawCenteredString(ms, RemoteBraceletScreen.this.font, this.getMessage().getString(),
                        this.x + this.width / 2, this.y + (this.height - 8) / 2, OFF_TEXT);
            }
        }
    }

    /** 🎹 Gestion des touches 1–8 + ESC */
    /** 🎹 Gestion des touches 1–8 + fermeture sur toute autre touche */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        // ✅ Gestion des touches de slot 1–8
        int slot = -1;
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_8)
            slot = (keyCode - GLFW.GLFW_KEY_1) + 1;
        else if (keyCode >= GLFW.GLFW_KEY_KP_1 && keyCode <= GLFW.GLFW_KEY_KP_8)
            slot = (keyCode - GLFW.GLFW_KEY_KP_1) + 1;

        // ✅ Active le Remote correspondant
        if (slot >= 1 && slot <= 8) {
            SoundUtils.playClac();
            Net.CH.sendToServer(new RemoteTriggerC2S(slot));
            return true;
        }

        // ✅ Échap ferme aussi
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (minecraft != null)
                minecraft.setScreen(null);
            return true;
        }

        // 🔁 Pour toute autre touche : ferme le GUI
        if (minecraft != null) {
            minecraft.setScreen(null);
            SoundUtils.playClac();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }



    /** 🔁 Synchronisation périodique */
    @Override
    public void tick() {
        super.tick();
        if (minecraft == null)
            return;

        long win = minecraft.getWindow().getWindow();
        KeyBinding[] keys = {
                minecraft.options.keyUp,
                minecraft.options.keyDown,
                minecraft.options.keyLeft,
                minecraft.options.keyRight,
                minecraft.options.keyJump,
                minecraft.options.keySprint,
                minecraft.options.keyShift
        };
        for (KeyBinding kb : keys) {
            if (kb == null) continue;
            InputMappings.Input key = kb.getKey();
            int glfwKey = (int) key.getValue();
            boolean pressed = GLFW.glfwGetKey(win, glfwKey) == GLFW.GLFW_PRESS
                    || GLFW.glfwGetKey(win, glfwKey) == GLFW.GLFW_REPEAT;
            KeyBinding.set(key, pressed);
        }

        if (++rescanTicker % 10 == 0)
            requestSlots();
    }

    @Override
    public void removed() {
        super.removed();
        if (minecraft == null) return;
        minecraft.keyboardHandler.setSendRepeatsToGui(false);

        KeyBinding[] keys = {
                minecraft.options.keyUp,
                minecraft.options.keyDown,
                minecraft.options.keyLeft,
                minecraft.options.keyRight,
                minecraft.options.keyJump,
                minecraft.options.keySprint,
                minecraft.options.keyShift
        };
        for (KeyBinding kb : keys)
            if (kb != null)
                KeyBinding.set(kb.getKey(), false);
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(ms);

        int left = (this.width - GUI_W) / 2;
        int top = (this.height - GUI_H) / 2;

        if (minecraft != null)
            minecraft.getTextureManager().bind(BG);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        blit(ms, left, top, 0, 0, GUI_W, GUI_H);

        super.render(ms, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
