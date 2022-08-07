package net.moddedminecraft.mmcrestrict.Commands;

import com.google.inject.Inject;
import net.moddedminecraft.mmcrestrict.Config.Config;
import net.moddedminecraft.mmcrestrict.Data.ModData;
import net.moddedminecraft.mmcrestrict.MMCRestrict;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Mod implements CommandExecutor {

    @Inject
    private MMCRestrict plugin;

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Optional<String> modOp = args.getOne("Mod");
        if (!(src instanceof Player)) {
            throw new CommandException(Text.of("Console users cannot use this command"));
        }
        Player player = (Player) src;
        if (!modOp.isPresent()) {
            throw new CommandException(plugin.fromLegacy("&cUsage: /restrict mod (modid)"));
        }
        String modArg = modOp.get();
        final List<ModData> mods = new ArrayList(plugin.getModData());

        if (!mods.isEmpty()) {
            for (ModData mod : mods) {
                if (mod.getMod().equals(modArg)) {
                    throw new CommandException(Text.of("Mod already exists"));
                }
            }
        }

        plugin.addMod(new ModData(
            Config.defaultHidden,
            modArg,
            modArg,
            Config.defaultReason,
            Config.defaultUsage,
            Config.defaultBreaking,
            Config.defaultPlacing,
            Config.defaultOwnership,
            Config.defaultDrop,
            Config.defaultCraft
        ));

        try {
            plugin.saveData();
        } catch (Exception e) {
            player.sendMessage(Text.of("Data was not saved correctly."));
            e.printStackTrace();
        }
        plugin.logToFile("ban-list", player.getName() + " added the " + modArg + " mod to the ban list");
        player.sendMessage(Text.of(modArg + " mod was added to the list."));

        return CommandResult.success();
    }
}
