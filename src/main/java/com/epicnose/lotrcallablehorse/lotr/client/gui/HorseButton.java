package com.epicnose.lotrcallablehorse.lotr.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;

/**
 * 魔戒菜单原生风格载具按钮。
 * 直接继承原版 GuiButton，零依赖魔戒特定版本内部类，彻底规避双版本 ClassNotFoundException。
 */
public class HorseButton extends GuiButton {
    public int menuKeyCode;

    public HorseButton(int id, int x, int y, String label) {
        this(id, x, y, label, 45); // 默认按键码
    }

    public HorseButton(int id, int x, int y, String label, int key) {
        super(id, x, y, 32, 32, label);
        this.menuKeyCode = key;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }
        mc.getTextureManager().bindTexture(HorseGui.horseIconTexture);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        field_146123_n = mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width && mouseY < yPosition + height;

        float u = 0.0F + (enabled ? 0 : width * 2) + (field_146123_n ? width : 0);
        drawModalRectWithCustomSizedTexture(xPosition, yPosition, u, 0.0F, 32, 32, 64.0F, 32.0F);
        mouseDragged(mc, mouseX, mouseY);

        // 鼠标悬停时绘制原汁原味的 Tooltip 悬浮标签
        if (field_146123_n && displayString != null && mc.currentScreen != null) {
            drawHoveringLabel(mc.fontRenderer, displayString, mouseX, mouseY);
        }
    }

    private void drawHoveringLabel(FontRenderer font, String text, int mouseX, int mouseY) {
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, 0.0F, 300.0F);
        int stringWidth = font.getStringWidth(text);
        int x = mouseX - stringWidth / 2;
        int y = mouseY - 14;
        // 绘制半透明黑色圆角边框背景
        drawRect(x - 3, y - 3, x + stringWidth + 3, y + 8 + 3, 0xCC000000);
        font.drawStringWithShadow(text, x, y, 0xFFE082); // 魔戒经典金黄色
        GL11.glPopMatrix();
    }

    public static void drawModalRectWithCustomSizedTexture(int x, int y, float u, float v, int width, int height, float textureWidth, float textureHeight) {
        float f = 1.0F / textureWidth;
        float f1 = 1.0F / textureHeight;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV((double) x, (double) (y + height), 0.0D, (double) (u * f), (double) ((v + (float) height) * f1));
        tessellator.addVertexWithUV((double) (x + width), (double) (y + height), 0.0D, (double) ((u + (float) width) * f), (double) ((v + (float) height) * f1));
        tessellator.addVertexWithUV((double) (x + width), (double) y, 0.0D, (double) ((u + (float) width) * f), (double) (v * f1));
        tessellator.addVertexWithUV((double) x, (double) y, 0.0D, (double) (u * f), (double) (v * f1));
        tessellator.draw();
    }
}
