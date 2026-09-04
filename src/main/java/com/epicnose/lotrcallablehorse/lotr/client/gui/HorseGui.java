package com.epicnose.lotrcallablehorse.lotr.client.gui;

import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseLevelData;
import com.epicnose.lotrcallablehorse.lotr.common.SingleVehicle;
import com.epicnose.lotrcallablehorse.lotr.common.PlayerHorseData;
import com.epicnose.lotrcallablehorse.lotr.common.mount.IMountAdapter;
import com.epicnose.lotrcallablehorse.lotr.common.mount.MountAdapterRegistry;
import lotr.client.gui.LOTRGuiMenuBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.model.ModelHorse;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class HorseGui extends LOTRGuiMenuBase {
    public static ResourceLocation horseIconTexture = new ResourceLocation("lotrcallablehorse", "textures/gui/horseAndSelectHorse.png");
    public static boolean fullscreen = true;
    public static ModelHorse modelHorse = new ModelHorse();
    public float modelRotation;
    public float modelRotationPrev;

    public PlayerHorseData lpd;
    public int index = 0;
    public int playerlimit = 0;
    public GuiButton horseleft;
    public GuiButton horseright;

    public GuiButton horsecall;

    public GuiButton horserelease;
    public GuiButton horsecallback;
    public int modelX;
    public int modelY;
    //    public int isMouseDown;
    public int mouseX;
    public int mouseY;
    private EntityLivingBase previewEntity;
    private SingleVehicle previewVehicle;

    private long lastCallRequest;
//    public int prevMouseX;

    public HorseGui(){
//        modelRotationPrev = modelRotation = -140.0f;
    }

    @Override
    public void drawScreen(int i, int j, float f) {
        mouseX = i;
        mouseY = j;
        drawDefaultBackground();

        clampIndex();
        drawEntity();

        super.drawScreen(i, j, f);

        drawVehicleStats();

        // 界面左下角版权与声明信息，避免文字被屏幕左边缘截断
        int footerX = Math.max(8, guiLeft);
        int footerY = Math.min(height - 24, guiTop + 228);
        fontRendererObj.drawStringWithShadow("MadeBy Epic_Nose", footerX, footerY, 0x90FFFFFF);
        fontRendererObj.drawStringWithShadow("禁止将本模组功能用于商业用途！", footerX, footerY + 10, 0x90FFFFFF);

        // 按钮悬停提示信息置于顶层绘制
        for (Object obj : buttonList) {
            if (!(obj instanceof GuiButton)) {
                continue;
            }
            GuiButton button = (GuiButton) obj;
            if (!button.func_146115_a() || button.displayString == null || button.displayString.isEmpty()) {
                continue;
            }
            float z = zLevel;
            drawCreativeTabHoveringText(button.displayString, i, j);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            zLevel = z;
        }
    }

    public void drawEntity() {
        clampIndex();
        if (lpd != null && lpd.horseInfo != null && lpd.horseInfo.getVehicleCount() > 0) {
            if (index >= 0 && index < lpd.horseInfo.getVehicleCount()) {
                SingleVehicle sv = lpd.horseInfo.getSingleVehicleByIndex(index);
                if (sv != null) {
                    if (previewVehicle != sv) {
                        if (previewEntity != null) {
                            previewEntity.setDead();
                        }
                        previewVehicle = sv;
                        try {
                            previewEntity = sv.createPreviewEntity(Minecraft.getMinecraft().theWorld);
                        } catch (Throwable ignored) {
                            previewEntity = null;
                        }
                    }

                    IMountAdapter adapter = previewEntity != null
                            ? MountAdapterRegistry.getAdapter(previewEntity)
                            : MountAdapterRegistry.getFallback();

                    if (previewEntity != null) {
                        float bounds = Math.max(previewEntity.height, previewEntity.width);
                        float scaleMult = adapter.getRenderScaleMultiplier(previewEntity);
                        int renderScale = Math.max(10, Math.min(60, (int) ((70.0F / Math.max(bounds, 0.1F)) * scaleMult)));
                        int renderY = modelY + (int) adapter.getRenderYOffset(previewEntity);
                        GL11.glPushMatrix();
                        GuiInventory.func_147046_a(modelX, renderY, renderScale, modelX - mouseX, 0, previewEntity);
                        GL11.glPopMatrix();
                        GL11.glDisable(GL11.GL_LIGHTING);
                        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                    }
                }
            }
        } else {
            this.drawCenteredString("还没有登记载具", width / 2, guiTop + 50, 16777215);
            this.drawCenteredString("骑在载具上使用 /addhorse 登记吧", width / 2, guiTop + 65, 16777215);
        }
    }

    public void drawVehicleStats() {
        if (lpd == null || lpd.horseInfo == null || lpd.horseInfo.getVehicleCount() == 0) {
            return;
        }
        if (index < 0 || index >= lpd.horseInfo.getVehicleCount()) {
            return;
        }
        SingleVehicle sv = lpd.horseInfo.getSingleVehicleByIndex(index);
        if (sv == null) {
            return;
        }

        IMountAdapter adapter = previewEntity != null
                ? MountAdapterRegistry.getAdapter(previewEntity)
                : MountAdapterRegistry.getFallback();

        List<String> stats = adapter.getDisplayStats(sv);
        List<String> allLines = new ArrayList<String>();

        int totalCount = lpd.horseInfo.getVehicleCount();
        allLines.add(String.format("§6【 载具 %d / %d 】", index + 1, totalCount));
        allLines.add("当前状态: " + (sv.isUsing ? "§a出战中" : "§7待命中"));
        allLines.addAll(stats);

        int maxTextWidth = 0;
        for (String line : allLines) {
            maxTextWidth = Math.max(maxTextWidth, fontRendererObj.getStringWidth(line));
        }

        int padX = 8;
        int padY = 6;
        int lineHeight = 11;
        int cardWidth = maxTextWidth + (padX * 2);
        int cardHeight = (allLines.size() * lineHeight) + padY + 2;

        // 默认定位在右侧空旷区域，彻底避开翻页按钮 (右箭头结束于 modelX + 96)
        int cardX = modelX + 104;
        int cardY = guiTop + 28;

        // 屏幕自适应：若右侧被窗口边缘截断，自适应向左平移或换位
        if (cardX + cardWidth > width - 6) {
            cardX = width - cardWidth - 6;
            // 若平移后侵入右翻页按钮区域 (右翻页按钮结束于 modelX + 96)
            if (cardX < modelX + 98) {
                // 尝试挪至左翻页按钮左侧空白区域 (左翻页按钮起始于 modelX - 96)
                int leftCardX = (modelX - 96) - cardWidth - 8;
                if (leftCardX >= 6) {
                    cardX = leftCardX;
                } else {
                    // 超窄屏幕极限兜底：居中置于顶部空白区
                    cardX = Math.max(6, (width - cardWidth) / 2);
                    cardY = Math.max(2, guiTop - cardHeight - 4);
                }
            }
        }

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // 绘制魔戒风格半透明典雅边框属性卡片
        drawFancyRect(cardX, cardY, cardX + cardWidth, cardY + cardHeight);

        int curY = cardY + padY;
        for (String line : allLines) {
            fontRendererObj.drawStringWithShadow(line, cardX + padX, curY, 0xFFE082);
            curY += lineHeight;
        }
    }


    @Override
    public void initGui() {
        xSize = 256;
        ySize = 256;
        modelX = width / 2;
        modelY = guiTop + 125;

        lpd=mc.thePlayer == null ? null : CallableHorseLevelData.getData(mc.thePlayer.getUniqueID());
        previewEntity = null;
        previewVehicle = null;
//        if(lpd.isLord){
//            playerlimit=2;
//        }else if(lpd.isKing){
//            playerlimit=3;
//        }else {
            playerlimit=lpd == null || lpd.horseInfo == null ? 0 : lpd.horseInfo.getVehicleCount();
//        }

        super.initGui();
        modelX = width / 2;
        modelY = guiTop + 125;
        if (fullscreen) {
            int midX = width / 2;
            int d = 125;
            buttonMenuReturn.xPosition = midX - d - buttonMenuReturn.width;
            buttonMenuReturn.yPosition = 4;
        }
//        horseleft = new LOTRGuiButtonShieldsArrows(0, true, guiLeft + xSize / 2 - 64, guiTop + 207);
//        horseleft = new HorseButtonArrows(0, true, guiLeft + xSize / 2 - 64, guiTop + 100);
        horseleft = new HorseButtonArrows(0, true,  width / 2 - 96, guiTop + 30);
        buttonList.add(horseleft);
//        horseright = new LOTRGuiButtonShieldsArrows(2, false, guiLeft + xSize / 2 + 44, guiTop + 207);
        horseright = new HorseButtonArrows(1, false, width/ 2 + 48, guiTop + 30);
        buttonList.add(horseright);
        horsecall = new GuiButton(2, width / 2- 80, guiTop + 150, 60, 20, "召唤");
        buttonList.add(horsecall);
        horsecallback = new GuiButton(3, width / 2 + 20, guiTop + 150, 60, 20, "收回");
        buttonList.add(horsecallback);

        horserelease = new GuiButton(4, width - 66, guiTop + 200, 60, 20, "§c§l销毁");
        buttonList.add(horserelease);
        clampIndex();
        updateActionButtons();

//        horsecall.displayString="召唤";
//        lpd= LOTRLevelData.getData(Minecraft.getMinecraft().thePlayer);

    }



    @Override
    public void updateScreen() {
        super.updateScreen();
        clampIndex();
        updateActionButtons();
//        modelRotationPrev = modelRotation;
//        modelRotationPrev = MathHelper.wrapAngleTo180_float(modelRotationPrev);
//        modelRotation = MathHelper.wrapAngleTo180_float(modelRotation);
//        mouseWithinModel = Math.abs(mouseX - modelX) <= 200 && Math.abs(mouseY - modelY) <= 80;
//        if (Mouse.isButtonDown(0)) {
//            if (isMouseDown == 0 || isMouseDown == 1) {
//                if (isMouseDown == 0) {
//                    if (mouseWithinModel) {
//                        isMouseDown = 1;
//                    }
//                } else if (mouseX != prevMouseX) {
//                    float move = -(mouseX - prevMouseX) * 1.0f;
//                    modelRotation += move;
//                }
//                prevMouseX = mouseX;
//            }
//        } else {
//            isMouseDown = 0;
//        }
    }
    @Override
    public void actionPerformed(GuiButton button) {
        if (button != null && button.enabled) {
            if (button == horseleft) {
                if (canTurnLeft()) {
                    updateIndex(index);
                }
            } else if (button == horseright) {
                if (canTurnRight()) {
                    updateIndex(index);
                }
            } else if (button == horsecall) {
                if (lpd != null && lpd.horseInfo != null) {
                    if (hasSelectedVehicle()) {
                        if ((System.currentTimeMillis() - lastCallRequest) > 500L) {
                            lpd.horseInfo.sendCallHorseMessage2Server(index, lpd.getPlayerUUID());
                            lastCallRequest = System.currentTimeMillis();
                        } else {
                            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("[召之马来]请求过于频繁，请稍后再试"));
                        }
                    } else {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("[召之马来]请先使用/addhorse登记后再召唤"));
                    }
                }
            } else if (button == horsecallback) {
                if (lpd != null && lpd.horseInfo != null) {
                    if (hasSelectedVehicle()) {
                        lpd.horseInfo.sendCallBackHorseMessage2Server(index, lpd.getPlayerUUID());
                    }
                }
            } else if (button == horserelease) {
                if (lpd != null && lpd.horseInfo != null) {
                    if (hasSelectedVehicle()) {
                        lpd.horseInfo.sendReleaseHorseMessage2Server(index, lpd.getPlayerUUID());
                    }
                }
            } else {
                super.actionPerformed(button);
            }
        }
    }

    public void updateIndex(int i){  //用于切换生物

    }

    public boolean canTurnLeft(){
        if(index>0){
            index--;
            previewVehicle = null;
            previewEntity = null;
            return true;
        }else return false;
    }
    public boolean canTurnRight(){
        if(lpd != null && lpd.horseInfo != null && index+1<lpd.horseInfo.getVehicleCount()){
            index++;
            previewVehicle = null;
            previewEntity = null;
            return true;
        }else return false;
    }

    private boolean hasSelectedVehicle() {
        clampIndex();
        return lpd != null && lpd.horseInfo != null
                && lpd.horseInfo.getSingleVehicleByIndex(index) != null;
    }

    private void clampIndex() {
        int count = lpd == null || lpd.horseInfo == null ? 0 : lpd.horseInfo.getVehicleCount();
        int previous = index;
        index = count == 0 ? 0 : Math.max(0, Math.min(index, count - 1));
        if (previous != index) {
            previewVehicle = null;
            previewEntity = null;
        }
    }

    private void updateActionButtons() {
        boolean selected = hasSelectedVehicle();
        if (horseleft != null) {
            horseleft.enabled = selected && index > 0;
        }
        if (horseright != null) {
            horseright.enabled = selected && lpd.horseInfo.getVehicleCount() > index + 1;
        }
        if (horsecall != null) {
            horsecall.enabled = selected;
        }
        if (horsecallback != null) {
            horsecallback.enabled = selected;
        }
        if (horserelease != null) {
            horserelease.enabled = selected;
        }
    }

    @Override
    public void onGuiClosed() {
        if (previewEntity != null) {
            previewEntity.setDead();
            previewEntity = null;
        }
        previewVehicle = null;
        super.onGuiClosed();
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
    public void drawFancyRect(int x1, int y1, int x2, int y2) {
        Gui.drawRect(x1, y1, x2, y2, -1073741824);
        drawHorizontalLine(x1 - 1, x2, y1 - 1, -6156032);
        drawHorizontalLine(x1 - 1, x2, y2, -6156032);
        drawVerticalLine(x1 - 1, y1 - 1, y2, -6156032);
        drawVerticalLine(x2, y1 - 1, y2, -6156032);
    }


}
