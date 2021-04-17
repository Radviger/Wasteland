package com.legacy.wasteland.common.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.*;

public abstract class CommandBaseRecursive extends CommandBase {
    private final boolean allowDirectCalls;
    private Map<String, CommandBaseRecursive> subCommands = new HashMap<>();

    public CommandBaseRecursive(boolean allowDirectCalls, CommandBaseRecursive... subCommands) {
        this.allowDirectCalls = allowDirectCalls;
        for (CommandBaseRecursive command : subCommands) {
            this.subCommands.put(command.getName(), command);
        }
    }

    public CommandBaseRecursive(boolean allowDirectCalls, Collection<CommandBaseRecursive> subCommands) {
        this.allowDirectCalls = allowDirectCalls;
        for (CommandBaseRecursive command : subCommands) {
            this.subCommands.put(command.getName(), command);
        }
    }

    @Override
    public final void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!this.allowDirectCalls) {
            throw new WrongUsageException(this.getUsage(sender));
        }
        this.run(server, sender, args);
    }

    protected boolean run(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!sender.canUseCommand(this.getRequiredPermissionLevel(), "")) {
            throw new CommandException("commands.generic.permission");
        }
        if (args.length > 0) {
            CommandBaseRecursive subCommand = this.subCommands.get(args[0]);
            if (subCommand != null) {
                return subCommand.run(server, sender, Arrays.copyOfRange(args, 1, args.length));
            } else {
                throw new WrongUsageException(this.getUsage(sender));
            }
        }
        return false;
    }

    @Override
    public final List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos lookingSpot) {
        if (!sender.canUseCommand(this.getRequiredPermissionLevel(), "")) {
            return Collections.emptyList();
        }
        try {
            return this.complete(server, sender, args, lookingSpot);
        } catch (CommandException exc) {
            TextComponentTranslation message = new TextComponentTranslation(exc.getMessage(), exc.getErrorObjects());
            message.getStyle().setColor(TextFormatting.RED);
            sender.sendMessage(message);
        }
        return Collections.emptyList();
    }

    protected List<String> complete(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos lookingSpot) throws CommandException {
        if (!this.subCommands.isEmpty()) {
            List<String> result = new ArrayList<>();
            switch (args.length) {
                case 0:
                    for (CommandBaseRecursive cmd : this.subCommands.values()) {
                        if (sender.canUseCommand(cmd.getRequiredPermissionLevel(), "")) {
                            result.add(cmd.getName());
                        }
                    }
                    break;
                case 1:
                    String partial = args[0].toLowerCase();
                    for (CommandBaseRecursive cmd : this.subCommands.values()) {
                        if (sender.canUseCommand(cmd.getRequiredPermissionLevel(), "")) {
                            if (cmd.getName().startsWith(partial)) {
                                result.add(cmd.getName());
                            }
                            for (String a : cmd.getAliases()) {
                                if (a.startsWith(partial)) {
                                    result.add(a);
                                }
                            }
                        }
                    }
                    break;
                default: {
                    CommandBaseRecursive c = this.subCommands.get(args[0]);
                    if (c != null) {
                        return c.complete(server, sender, Arrays.copyOfRange(args, 1, args.length), lookingSpot);
                    }
                    break;
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}
