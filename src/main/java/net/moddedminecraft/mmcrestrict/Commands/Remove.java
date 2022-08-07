package net.moddedminecraft.mmcrestrict.Commands;

import com.google.inject.Inject;
import net.moddedminecraft.mmcrestrict.Data.ItemData;
import net.moddedminecraft.mmcrestrict.Data.ModData;
import net.moddedminecraft.mmcrestrict.MMCRestrict;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.List;

public class Remove implements CommandExecutor {

    @Inject
    private MMCRestrict plugin;

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        String itemType = args.<String>getOne("ItemID").get();
        final List<ItemData> items = new ArrayList(plugin.getItemData());
        final List<ModData> mods = new ArrayList(plugin.getModData());
        String itemName = null;

        for (ModData mod : mods) {
            if (mod.getMod().equals(itemType)) {
                itemName = mod.getMod();
                if (plugin.removeMod(itemType) != null) {
                    try {
                        plugin.saveData();
                    } catch (Exception e) {
                        src.sendMessage(Text.of("Data was not saved correctly."));
                        e.printStackTrace();
                    }
                }
            }
        }

        for (ItemData item : items) {
            if (item.getItemid().equals(itemType)) {
                itemName = item.getItemname();
                if (plugin.removeItem(itemType) != null) {
                    try {
                        plugin.saveData();
                    } catch (Exception e) {
                        src.sendMessage(Text.of("Data was not saved correctly."));
                        e.printStackTrace();
                    }
                }
            }
        }

        if (itemName == null) {
            plugin.logToFile("ban-list", src.getName() + " removed an item from the ban list");
            src.sendMessage(Text.of("Item was removed the list."));
        } else {
            plugin.logToFile("ban-list", src.getName() + " removed " + itemName + " from the ban list");
            src.sendMessage(Text.of(itemName + " was removed the list."));

        }
        return CommandResult.success();
    }
}
