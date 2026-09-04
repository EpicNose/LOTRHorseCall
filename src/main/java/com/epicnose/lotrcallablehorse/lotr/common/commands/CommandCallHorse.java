package com.epicnose.lotrcallablehorse.lotr.common.commands;

import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseConfig;
import com.epicnose.lotrcallablehorse.lotr.common.CallableHorseLevelData;
import com.epicnose.lotrcallablehorse.lotr.common.HorseInfo;
import com.epicnose.lotrcallablehorse.lotr.common.PlayerHorseData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

/** Server command counterpart of the menu summon action. */
public class CommandCallHorse extends CommandBase {
    @Override
    public String getCommandName() {
        return "callhorse";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/callhorse <序号>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return sender instanceof EntityPlayer;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayer)) {
            throw new WrongUsageException("[召之马来]该命令只能由玩家执行");
        }
        if (args == null || args.length != 1) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        int index;
        try {
            index = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            throw new WrongUsageException("[召之马来]序号必须是整数，例如 /callhorse 0");
        }
        if (!CallableHorseConfig.isValidStoredSlotIndex(index)) {
            throw new WrongUsageException("[召之马来]序号范围为 0-"
                    + (CallableHorseConfig.MAX_STORED_VEHICLES - 1));
        }

        EntityPlayer player = (EntityPlayer) sender;
        PlayerHorseData data = CallableHorseLevelData.getData(player);
        HorseInfo info = data == null ? null : data.horseInfo;
        if (info == null || info.getSingleVehicleByIndex(index) == null) {
            player.addChatMessage(new ChatComponentText("[召之马来]没有找到该序号的载具"));
            return;
        }
        if (info.spawnSpecificVehicleByIndex(index, player)) {
            player.addChatMessage(new ChatComponentText("[召之马来]载具召唤成功"));
        }
    }
}
