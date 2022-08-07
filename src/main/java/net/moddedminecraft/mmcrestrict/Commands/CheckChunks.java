package net.moddedminecraft.mmcrestrict.Commands;

import com.google.inject.Inject;
import net.moddedminecraft.mmcrestrict.Config.Messages;
import net.moddedminecraft.mmcrestrict.MMCRestrict;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;

public class CheckChunks implements CommandExecutor {

    @Inject
    private MMCRestrict plugin;

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        plugin.checkLoadedChunks();
        src.sendMessage(plugin.fromLegacy(Messages.checkStarted));
        return CommandResult.success();
    }
}
