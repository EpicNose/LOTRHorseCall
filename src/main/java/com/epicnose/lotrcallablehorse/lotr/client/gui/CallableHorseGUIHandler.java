package com.epicnose.lotrcallablehorse.lotr.client.gui;

import com.epicnose.lotrcallablehorse.lotr.client.gui.ReworkVer.HorseButtonRework;
import com.epicnose.lotrcallablehorse.lotr.client.gui.ReworkVer.HorseGuiRework;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import lotr.client.gui.LOTRGuiButtonMenu;
import lotr.client.gui.LOTRGuiMenu;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;

public class CallableHorseGUIHandler {

    public CallableHorseGUIHandler() {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }
    @SubscribeEvent
    public void postInitGui(GuiScreenEvent.InitGuiEvent.Post event) { //GuiScreenEvent.InitGuiEvent.Post
        GuiButton buttonDifficulty;
        GuiScreen gui = event.gui;
        List buttons = event.buttonList;
        if(gui instanceof LOTRGuiMenu){
            int buttonGap = 10;
            int buttonSize = 32;
            int midX = gui.width / 2;
            int midY = gui.height / 2;
            int numButtons = buttons.size();
            int numTopRowButtons = 4;
            int numBtmRowButtons = numButtons - numTopRowButtons;
            int topRowLeft = midX - (numTopRowButtons * buttonSize + (numTopRowButtons - 1) * buttonGap) / 2;
            int btmRowLeft = midX - (numBtmRowButtons * buttonSize + (numBtmRowButtons - 1) * buttonGap) / 2;

//            buttonList.add(new LOTRGuiButtonMenu(gui, 7, 0, 0, LOTRGuiTitles.class, StatCollector.translateToLocal("lotr.gui.titles"), 20));
//            int xpos=((LOTRGuiButtonMenu)buttons.get(7)).xPosition;
//            int ypos=((LOTRGuiButtonMenu)buttons.get(7)).yPosition;

//            int btmRowLeft = midX - (numBtmRowButtons * buttonSize + (numBtmRowButtons - 1) * buttonGap) / 2;

//            HorseButton Horse;

            try {
                if(Class.forName("lotr.client.gui.button.LOTRGuiButtonMenu") != null){
                    HorseButtonRework Horse = new HorseButtonRework((LOTRGuiMenu) gui,8,0,0, HorseGuiRework.class,"LOTR-召唤载具",45);
                    Horse.xPosition = btmRowLeft + (9 - numTopRowButtons) * (buttonSize + buttonGap);
                    Horse.yPosition = midY + buttonGap / 2;
                    Horse.visible =true;
                    buttons.add(Horse);
//                    Horse = (lotr.client.gui.button.LOTRGuiButtonMenu)(Object)Horse;
                }else{
                    HorseButton Horse = new HorseButton((LOTRGuiMenu) gui,8,0,0, HorseGui.class,"LOTR-召唤载具",45);
                    Horse.xPosition = btmRowLeft + (9 - numTopRowButtons) * (buttonSize + buttonGap);
                    Horse.yPosition = midY + buttonGap / 2;
                    Horse.visible =true;
                    buttons.add(Horse);
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }


        }

    }

}
