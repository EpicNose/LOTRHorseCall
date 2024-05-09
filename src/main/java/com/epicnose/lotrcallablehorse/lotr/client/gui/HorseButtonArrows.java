package com.epicnose.lotrcallablehorse.lotr.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class HorseButtonArrows extends GuiButton {
    public static ResourceLocation texture = new ResourceLocation("lotr:gui/widgets.png");
    public static ResourceLocation onlyArrow=new ResourceLocation("lotrcallablehorse","textures/gui/onlyarrow.png");
    public boolean leftOrRight;

    public HorseButtonArrows(int i, boolean flag, int j, int k) {
        super(i, j, k, 48, 96, "");
        leftOrRight = flag;
    }

    @Override
    public void drawButton(Minecraft mc, int i, int j) {
        if (visible) {
//            mc.getTextureManager().bindTexture(texture);
//            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
//            field_146123_n = i >= xPosition && j >= yPosition && i < xPosition + width && j < yPosition + height;
//            int k = getHoverState(field_146123_n);
////            drawTexturedModalRect(xPosition, yPosition, leftOrRight ? 0 : 20, 60 + k * 20, width, height);
//            drawModalRectWithCustomSizedTexture(xPosition,yPosition,leftOrRight ? 0 : 20, 60 + k * 20,20,20,width, height);

            mc.getTextureManager().bindTexture(onlyArrow);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            field_146123_n = i >= xPosition && j >= yPosition && i < xPosition + width && j < yPosition + height;
            int k = getHoverState(field_146123_n);
//            drawModalRectWithCustomSizedTexture(xPosition,yPosition,leftOrRight ? 0 : 20,  k * 20,20,20,40, 60);
            drawScaledCustomSizeModalRect(xPosition,yPosition,leftOrRight ? 0 : 20,  k * 20,20,20,48,96,40, 60);

        }
    }

    public static void drawModalRectWithCustomSizedTexture(int x, int y, float u, float v, int width, int height, float textureWidth, float textureHeight) {
        float f = 1.0F / textureWidth;
        float f1 = 1.0F / textureHeight;
        Tessellator tessellator = Tessellator.instance;
        //		WorldRenderer worldrenderer = tessellator.wo;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV((double) x, (double) (y + height), 0.0D, (double) (u * f), (double) ((v + (float) height) * f1));
        tessellator.addVertexWithUV((double) (x + width), (double) (y + height), 0.0D, (double) ((u + (float) width) * f), (double) ((v + (float) height) * f1));
        tessellator.addVertexWithUV((double) (x + width), (double) y, 0.0D, (double) ((u + (float) width) * f), (double) (v * f1));
        tessellator.addVertexWithUV((double) x, (double) y, 0.0D, (double) (u * f), (double) (v * f1));
        tessellator.draw();
    }

    public static void drawScaledCustomSizeModalRect(int x, int y, float u, float v, int width, int height, int lastWidth, int lastHeight, float textureWidth, float textureHeight) {
        float f = 1.0F / textureWidth;
        float f1 = 1.0F / textureHeight;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + lastHeight, 0, u * f, (v + height) * f1);
        tessellator.addVertexWithUV(x + lastWidth, y + lastHeight, 0, (u + width) * f, (v + height) * f1);
        tessellator.addVertexWithUV(x + lastWidth, y, 0, (u + width) * f, v * f1);
        tessellator.addVertexWithUV(x, y, 0, u * f, v * f1);
        tessellator.draw();
    }

}
