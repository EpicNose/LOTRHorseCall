package com.epicnose.lotrcallablehorse.lotr.client.gui;

import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseLevelData;
import com.epicnose.lotrcallablehorse.lotr.common.SingleVehicle;
import com.epicnose.lotrcallablehorse.lotr.common.PlayerHorseData;
import lotr.client.gui.LOTRGuiButtonMenu;
import lotr.client.gui.LOTRGuiMenuBase;
import lotr.common.entity.animal.LOTREntityHorse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.model.ModelHorse;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class HorseGui extends LOTRGuiMenuBase {
    public static ResourceLocation horseIconTexture = new ResourceLocation("lotrcallablehorse","textures/gui/horseAndSelectHorse.png");
    public static boolean fullscreen = true;
    //    public static final ResourceLocation HORSE_TEXTURES = new ResourceLocation("textures/entity/horse/horse_white.png");
    public static ModelHorse modelHorse = new ModelHorse();
    public float modelRotation;
    public float modelRotationPrev;

    public PlayerHorseData lpd= CallableHorseLevelData.getData(Minecraft.getMinecraft().thePlayer.getUniqueID());
    //    public static ModelBiped playerModel = new ModelBiped();
    static {
        modelHorse.isChild=false;
//        playerModel.isChild = false;
    }
    public int index=0;

    public int playerlimit=0;
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

    public static long prevCallTime=0;
//    public int prevMouseX;

    public HorseGui(){
//        modelRotationPrev = modelRotation = -140.0f;
    }

    @Override
    public void drawScreen(int i, int j, float f) {

        mouseX = i;
        mouseY = j;
        drawDefaultBackground();
        for (Object obj : buttonList) {
            LOTRGuiButtonMenu button;
            if (!(obj instanceof LOTRGuiButtonMenu) || !(button = (LOTRGuiButtonMenu) obj).func_146115_a() || button.displayString == null) {
                continue;
            }
            float z = zLevel;
            drawCreativeTabHoveringText(button.displayString, i, j);
            GL11.glDisable(2896);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            zLevel = z;
        }

//        if(!(lpd.horseInfo.vehicles.size()>0)){
//            this.drawCenteredString("没有载具"+lpd.horseInfo.vehicles.size(),width/2,guiTop+20,16777215);
//        }else{
//            this.drawCenteredString("载具数量"+lpd.horseInfo.vehicles.size(),width/2,guiTop+20,16777215);
//        }

//        GL11.glPushMatrix();
//        LOTREntityHorse horsehere=new LOTREntityHorse(Minecraft.getMinecraft().theWorld);
//        GuiInventory.func_147046_a(modelX , modelY , 50, modelX-mouseX, 0, horsehere);
//        GL11.glPopMatrix();
        drawEntity();
//        this.mc.getTextureManager().bindTexture(HORSE_TEXTURES);
//
//        this.modelHorse.render(null, 0, 0, 0, 0, 0, 0.0625F);
        this.drawCenteredString("MadeBy Epic_Nose",guiLeft,guiTop+230,16777215);
        this.drawCenteredString("禁止将本模组功能用于商业用途！",guiLeft,guiTop+238,16777215);
        super.drawScreen(i, j, f);
    }
    public void drawEntity(){
        if(lpd.horseInfo.vehicles.size()>0){
            if(index<lpd.horseInfo.vehicles.size()){
//            SingleVehicle sv=lpd.horseInfo.
                SingleVehicle sv=lpd.horseInfo.getSingleVehicleByIndex(index);
                if(sv!=null){
                    GL11.glPushMatrix();
                    LOTREntityHorse horsehere=sv.spawnVehicle(Minecraft.getMinecraft().thePlayer);
                    GuiInventory.func_147046_a(modelX , modelY , 50, modelX-mouseX, 0, horsehere);
                    GL11.glPopMatrix();
                    this.drawCenteredString("载具名称:"+sv.horseName,modelX+140,guiTop+20,16777215);
                    this.drawCenteredString("载具生命上限:"+sv.health,modelX+140,guiTop+28,16777215);
                    this.drawCenteredString("载具速度:"+sv.horseSpeed,modelX+140,guiTop+36,16777215);
                    this.drawCenteredString("载具跳跃:"+sv.horseJump,modelX+140,guiTop+44,16777215);
                    this.drawCenteredString("变种值:"+sv.variant,modelX+140,guiTop+52,16777215);
                }

            }
        }else{
            this.drawCenteredString("还没有登记载具",width/2,guiTop+20,16777215);
            this.drawCenteredString("骑在马上使用/addhorse吧",width/2,guiTop+28,16777215);
        }



    }
    @Override
    public void initGui() {
        xSize = 256;
        ySize = 256;
        modelX = width / 2 ;
        modelY = guiTop + 125;

        lpd=CallableHorseLevelData.getData(mc.thePlayer.getUniqueID());
//        if(lpd.isLord){
//            playerlimit=2;
//        }else if(lpd.isKing){
//            playerlimit=3;
//        }else {
            playerlimit=3;
//        }

        super.initGui();
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

        horserelease = new GuiButton(4, width   -60, guiTop + 200, 60, 20, "§c§l销毁");
        buttonList.add(horserelease);

//        horsecall.displayString="召唤";
//        lpd= LOTRLevelData.getData(Minecraft.getMinecraft().thePlayer);

    }



    @Override
    public void updateScreen() {
        boolean mouseWithinModel;
        super.updateScreen();
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
        if(button.enabled){
            if(button==horseleft){
                if(canTurnLeft()){
                    updateIndex(index);
                }
            }else if(button==horseright){
                if(canTurnRight()){
                    updateIndex(index);
                }
            }else if(button==horsecall){
                if(lpd.horseInfo!=null){
                    if(lpd.horseInfo.vehicles.size()>0 & index <lpd.horseInfo.vehicles.size()){
                        if((System.currentTimeMillis()-prevCallTime)>60*1000){
                            lpd.horseInfo.sendCallHorseMessage2Server(index,lpd.getPlayerUUID());
                            prevCallTime=System.currentTimeMillis();
                        }else{
//                    prevCallTime=System.currentTimeMillis();
                            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("[召之马来]请等1分钟后再点击"));
                        }
                    }else{

                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("[召之马来]请先使用/addhorse登记后再召唤"));
                    }
                }


            }else if(button==horsecallback){
                if(lpd.horseInfo!=null){
                    if(lpd.horseInfo.vehicles.size()>0 & index<lpd.horseInfo.vehicles.size()){

                        lpd.horseInfo.sendCallBackHorseMessage2Server(index,lpd.getPlayerUUID());
                        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("[召之马来]召回"+index+"号载具成功！"));
                    }
                }





            }else if(button==horserelease){
                if(lpd.horseInfo!=null){
                    if(lpd.horseInfo.vehicles.size()>0 & index<lpd.horseInfo.vehicles.size()){
                        lpd.horseInfo.sendReleaseHorseMessage2Server(index,lpd.getPlayerUUID());
//                        lpd.horseInfo.deleteVehicle(lpd.horseInfo.vehicles.get(index));
//                        this.initGui();
//                        this.lpd=CallableHorseLevelData.getData(lpd.getPlayerUUID());
//                        lpd.horseInfo.sendBasicData(Minecraft.getMinecraft().thePlayer);
//                        lpd.horseInfo.sendBasicData((EntityPlayerMP) lpd.getPlayer());
//                        this.lpd=LOTRLevelData.getData(Minecraft.getMinecraft().thePlayer.getUniqueID());



                    }
                }

            }else{
                super.actionPerformed(button);
            }



        }

    }
    public void updateIndex(int i){  //用于切换生物

    }

    public boolean canTurnLeft(){
        if(index>0){
            index--;
            return true;
        }else return false;
    }
    public boolean canTurnRight(){
        if(index+1<playerlimit){
            index++;
            return true;
        }else return false;
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
