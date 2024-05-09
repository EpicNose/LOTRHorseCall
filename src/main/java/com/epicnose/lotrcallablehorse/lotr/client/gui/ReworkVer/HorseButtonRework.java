package com.epicnose.lotrcallablehorse.lotr.client.gui.ReworkVer;


import lotr.client.gui.LOTRGuiMap;
import lotr.client.gui.LOTRGuiMenu;
import lotr.client.gui.LOTRGuiMenuBase;
import lotr.client.gui.button.LOTRGuiButtonMenu;
import lotr.common.LOTRMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;

public class HorseButtonRework  extends LOTRGuiButtonMenu {
    public Class<? extends LOTRGuiMenuBase> menuScreenClass;
    public int menuKeyCode;

    public HorseButtonRework(LOTRGuiMenu gui, int i, int x, int y, Class<? extends LOTRGuiMenuBase> cls, String s, int key) {
//        super(i, x, y, 32, 32, s);
        super(gui,8,0,0, HorseGuiRework.class,"LOTR-召之马来",45);
        menuScreenClass = cls;

        menuKeyCode = key;
    }

    public boolean canDisplayMenu() {
        if (menuScreenClass == LOTRGuiMap.class) {
            WorldClient world = Minecraft.getMinecraft().theWorld;
            return world != null && world.getWorldInfo().getTerrainType() != LOTRMod.worldTypeMiddleEarthClassic;
        }
        return true;
    }

    @Override
    public void drawButton(Minecraft mc, int i, int j) {
        if (visible) {
            mc.getTextureManager().bindTexture(HorseGuiRework.horseIconTexture);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            field_146123_n = i >= xPosition && j >= yPosition && i < xPosition + width && j < yPosition + height;

            drawModalRectWithCustomSizedTexture(xPosition,yPosition,0+ (enabled ? 0 : width * 2) + (field_146123_n ? width : 0),0,32,32,64,32);


            mouseDragged(mc, i, j);

        }
    }

    public LOTRGuiMenuBase openMenu() {
        try {
            return menuScreenClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
}
