package com.legacy.wasteland.common.command.wasteland;

import com.legacy.wasteland.Wasteland;
import com.legacy.wasteland.common.command.CommandBaseRecursive;
import com.legacy.wasteland.world.WastelandWorldData;
import com.legacy.wasteland.world.biome.decorations.template.WastelandStructure;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

import static java.util.Collections.emptyList;

public class SubCommandFind extends CommandBaseRecursive {
    public SubCommandFind() {
        super(false);
    }

    @Override
    public String getName() {
        return "find";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.wasteland.find.usage";
    }

    @Override
    protected boolean run(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer)) {
            throw new CommandException("commands.generic.player.unspecified");
        }
        if (args.length == 1) {
            String id = args[0];
            WastelandStructure structure = Wasteland.STRUCTURES.get(id);
            if (structure != null) {
                World world = sender.getEntityWorld();
                WastelandWorldData data = Wasteland.PROXY.getData(world);
                if (data != null) {
                    WastelandStructure.SavedStructure str = data.getNearestStructure(id, sender.getPosition());
                    if (str != null) {
                        ITextComponent location = Wasteland.formatCoordinates(str.pos);
                        ITextComponent message = new TextComponentTranslation("commands.wasteland.find.nearest", id, location);
                        sender.sendMessage(message);
                    } else {
                        throw new CommandException("commands.wasteland.find.not_found", id);
                    }
                } else {
                    throw new CommandException("commands.wasteland.generic.other_world_type");
                }
            } else {
                throw new CommandException("commands.wasteland.find.unknown_structure", id);
            }
            return true;
        } else {
            throw new WrongUsageException(this.getUsage(sender));
        }
    }

    @Override
    protected List<String> complete(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos lookingSpot) {
        return args.length == 1 ? CommandBase.getListOfStringsMatchingLastWord(args, Wasteland.STRUCTURES.keySet()) : emptyList();
    }
}
