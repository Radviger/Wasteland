package com.legacy.wasteland.common.command.wasteland;

import com.legacy.wasteland.Wasteland;
import com.legacy.wasteland.common.command.CommandBaseRecursive;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.io.File;

public class SubCommandReload extends CommandBaseRecursive {
    public SubCommandReload() {
        super(false);
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.wasteland.reload.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 4;
    }

    @Override
    protected boolean run(MinecraftServer server, ICommandSender sender, String[] args) {
        File structuresDir = new File(Wasteland.CONFIG_DIR, "structures");
        File lootTablesDir = new File(Wasteland.CONFIG_DIR, "loot_tables");
        if (!Wasteland.load(structuresDir, lootTablesDir)) {
            ITextComponent message = new TextComponentTranslation("commands.wasteland.reload.failed");
            message.getStyle().setColor(TextFormatting.RED);
            sender.sendMessage(message);
        } else {
            ITextComponent message = new TextComponentTranslation("commands.wasteland.reload.success");
            message.getStyle().setColor(TextFormatting.GREEN);
            sender.sendMessage(message);
        }
        return true;
    }
}
