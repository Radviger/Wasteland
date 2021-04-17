package com.legacy.wasteland.common.command;

import com.legacy.wasteland.common.command.wasteland.SubCommandFind;
import com.legacy.wasteland.common.command.wasteland.SubCommandReload;
import com.legacy.wasteland.common.command.wasteland.SubCommandWeather;
import net.minecraft.command.ICommandSender;

public class CommandWasteland extends CommandBaseRecursive {
    public CommandWasteland() {
        super(true,
            new SubCommandReload(),
            new SubCommandFind(),
            new SubCommandWeather()
        );
    }

    @Override
    public String getName() {
        return "wasteland";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.wasteland.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }
}
