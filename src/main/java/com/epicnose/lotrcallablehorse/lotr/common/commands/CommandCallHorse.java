package com.epicnose.lotrcallablehorse.lotr.common.commands;

import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseLevelData;
import com.epicnose.lotrcallablehorse.lotr.common.PlayerHorseData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

public class CommandCallHorse extends CommandBase {


    @Override
    public String getCommandName() {
        return "callhorse";
    }

    @Override
    public String getCommandUsage(ICommandSender p_71518_1_) {
        return "/callhorse index";
    }
    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if(sender instanceof EntityPlayer){
            EntityPlayer p =(EntityPlayer) sender;

            if(args.length==1){
                String a=args[0];
                if(isNumeric(a)) {
                    PlayerHorseData lpd= CallableHorseLevelData.getData(p);
                    if(lpd.horseInfo.vehicles.size()>Integer.valueOf(a)) {


                        lpd.horseInfo.spawnSpecificVehicleByIndex(Integer.valueOf(a), p);
//								sv.spawnVehicle(forgep);
//								Bukkit.broadcastMessage("uuid为"+sv.entityuuid);
//								Bukkit.broadcastMessage("速度为"+sv.horseSpeed);
//								Bukkit.broadcastMessage("uuid为"+sv.entityuuid);
                        p.addChatMessage(new ChatComponentText("[召之马来]载具召唤成功"));
//                        p.sendMessage("");
                        return;
                    }
                }

            }else{
                throw new WrongUsageException("/callhorse 序号");
            }
        }
    }

    public static boolean isNumeric(final String str) {
        // null or empty
        if (str == null || str.length() == 0) {
            return false;
        }

        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            try {
                Double.parseDouble(str);
                return true;
            } catch (NumberFormatException ex) {
                try {
                    Float.parseFloat(str);
                    return true;
                } catch (NumberFormatException exx) {
                    return false;
                }
            }
        }
    }






}
