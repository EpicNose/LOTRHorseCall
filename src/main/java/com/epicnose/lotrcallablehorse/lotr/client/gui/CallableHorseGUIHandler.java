package com.epicnose.lotrcallablehorse.lotr.client.gui;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import lotr.client.gui.LOTRGuiMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class CallableHorseGUIHandler {
    private static final int HORSE_BUTTON_ID = 0x4C4843;

    public CallableHorseGUIHandler() {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void postInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.gui;
        if (!(gui instanceof LOTRGuiMenu)) {
            return;
        }
        LOTRGuiMenu menu = (LOTRGuiMenu) gui;
        List buttons = event.buttonList;

        // 避免重复添加
        for (Object b : buttons) {
            if (b instanceof HorseButton || (b instanceof GuiButton && ((GuiButton) b).id == HORSE_BUTTON_ID)) {
                return;
            }
        }

        // 创建我们的载具按钮
        HorseButton horseBtn = new HorseButton(HORSE_BUTTON_ID, 0, 0, "LOTR-召唤载具");
        buttons.add(horseBtn);

        // 收集所有 32x32 菜单主按钮（包括魔戒原版按钮与我们新增的载具按钮）
        List<GuiButton> menuButtons = new ArrayList<GuiButton>();
        for (Object b : buttons) {
            if (b instanceof GuiButton) {
                GuiButton gb = (GuiButton) b;
                if (gb.width == 32 && gb.height == 32) {
                    menuButtons.add(gb);
                }
            }
        }

        // 应用魔戒原生双排网格公式，重新全量自适应居中排版！
        int midX = menu.width / 2;
        int midY = menu.height / 2;
        int buttonGap = 10;
        int buttonSize = 32;

        int numButtons = menuButtons.size(); // 传承版: 7+1=8; 重制版: 8+1=9
        int numTopRowButtons = (numButtons - 1) / 2 + 1; // 传承版: 4; 重制版: 5
        int numBtmRowButtons = numButtons - numTopRowButtons; // 传承版: 4; 重制版: 4

        int topRowLeft = midX - (numTopRowButtons * buttonSize + (numTopRowButtons - 1) * buttonGap) / 2;
        int btmRowLeft = midX - (numBtmRowButtons * buttonSize + (numBtmRowButtons - 1) * buttonGap) / 2;

        for (int i = 0; i < numButtons; i++) {
            GuiButton btn = menuButtons.get(i);
            if (i < numTopRowButtons) {
                btn.xPosition = topRowLeft + i * (buttonSize + buttonGap);
                btn.yPosition = midY - buttonGap / 2 - buttonSize;
            } else {
                btn.xPosition = btmRowLeft + (i - numTopRowButtons) * (buttonSize + buttonGap);
                btn.yPosition = midY + buttonGap / 2;
            }
        }
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event == null || !(event.gui instanceof LOTRGuiMenu) || event.button == null) {
            return;
        }
        if (event.button instanceof HorseButton || event.button.id == HORSE_BUTTON_ID) {
            openHorseGui();
        }
    }

    private static void openHorseGui() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(new HorseGui());
        try {
            Field lastScreenField = LOTRGuiMenu.class.getDeclaredField("lastMenuScreen");
            lastScreenField.setAccessible(true);
            lastScreenField.set(null, HorseGui.class);
        } catch (Throwable ignored) {
        }
    }
}
