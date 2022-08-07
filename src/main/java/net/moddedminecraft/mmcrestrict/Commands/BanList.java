package net.moddedminecraft.mmcrestrict.Commands;

import com.google.inject.Inject;
import net.moddedminecraft.mmcrestrict.Config.Messages;
import net.moddedminecraft.mmcrestrict.Data.ItemData;
import net.moddedminecraft.mmcrestrict.Data.ModData;
import net.moddedminecraft.mmcrestrict.MMCRestrict;
import net.moddedminecraft.mmcrestrict.Permissions;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;

import java.util.*;
import java.util.function.Consumer;

public class BanList implements CommandExecutor {

    @Inject
    private MMCRestrict plugin;

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        final List<ItemData> items = new ArrayList(plugin.getItemData());
        final List<ModData> mods = new ArrayList(plugin.getModData());
        Optional<String> hidden = args.getOne("hidden");

        PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
        List<Text> contents = new ArrayList<>();

        if (hidden.isPresent()) {
            for (ModData mod : mods) {
                if (!mod.getHidden()) {
                    continue;
                }
                HashMap<String, String> arguments = new HashMap<>();
                arguments.put("banreason", mod.getBanreason());
                arguments.put("itemid", mod.getMod());
                arguments.put("usebanned", mod.getUsagebanned().toString());
                arguments.put("breakbanned", mod.getBreakingbanned().toString());
                arguments.put("placebanned", mod.getPlacingbanned().toString());
                arguments.put("ownbanned", mod.getOwnershipbanned().toString());
                arguments.put("craftbanned", mod.getCraftbanned().toString());
                arguments.put("worldbanned", "false");
                arguments.put("modname", mod.getModname());
                arguments.put("hidden", mod.getHidden() ? "show" : "hide");

                Text.Builder send = Text.builder();

                String banreason = "";
                if (!mod.getBanreason().isEmpty()) {
                    banreason = Messages.bannedItemReason;
                }

                if (src.hasPermission(Permissions.LIST_HIDDEN_EDIT)) {
                    send.append(Text.builder().append(Messages.parseMessage(Messages.bannedListHidden, arguments))
                        .onClick(TextActions.executeCallback(changeHiddenMod(mod.getMod())))
                        .onHover(TextActions.showText(Messages.parseMessage(Messages.bannedListHideHover, arguments))).build());
                    send.append(plugin.fromLegacy(" "));
                }

                String banInfo = Messages.bannedItemHover;
                if ((src.hasPermission(Permissions.LIST_EXTRA) && src.hasPermission(Permissions.EDIT_BANNED_ITEM))) {
                    banInfo = banInfo + "\n" + Messages.bannedItemExtraInfo + "\n" + Messages.bannedItemEdit;
                    send.append(Messages.parseMessage(Messages.bannedMod + banreason, arguments))
                        .onClick(TextActions.runCommand("/restrict edit " + mod.getMod()))
                        .onHover(TextActions.showText(Messages.parseMessage(banInfo, arguments)));
                } else if (src.hasPermission(Permissions.LIST_EXTRA)) {
                    banInfo = banInfo + "\n" + Messages.bannedItemExtraInfo;
                    send.append(Messages.parseMessage(Messages.bannedMod + banreason, arguments))
                        .onHover(TextActions.showText(Messages.parseMessage(banInfo, arguments)));
                } else if (src.hasPermission(Permissions.EDIT_BANNED_ITEM)) {
                    banInfo = banInfo + "\n" + Messages.bannedItemEdit;
                    send.append(Messages.parseMessage(Messages.bannedMod + banreason, arguments))
                        .onClick(TextActions.runCommand("/restrict edit " + mod.getMod()))
                        .onHover(TextActions.showText(Messages.parseMessage(banInfo, arguments)));
                } else {
                    send.append(Messages.parseMessage(Messages.bannedMod + banreason, arguments))
                        .onHover(TextActions.showText(Messages.parseMessage(banInfo, arguments)));
                }

                contents.add(send.build());
            }

            for (ItemData item : items) {
                if (!item.getHidden()) {
                    continue;
                }
                HashMap<String, String> arguments = new HashMap<>();
                arguments.put("banreason", item.getBanreason());
                arguments.put("itemid", item.getItemid());
                arguments.put("usebanned", item.getUsagebanned().toString());
                arguments.put("breakbanned", item.getBreakingbanned().toString());
                arguments.put("placebanned", item.getPlacingbanned().toString());
                arguments.put("ownbanned", item.getOwnershipbanned().toString());
                arguments.put("craftbanned", item.getCraftbanned().toString());
                arguments.put("worldbanned", item.getWorldbanned().toString());
                arguments.put("itemname", item.getItemname());
                arguments.put("hidden", item.getHidden() ? "show" : "hide");

                Text.Builder send = Text.builder();

                String banreason = "";
                if (!item.getBanreason().isEmpty()) {
                    banreason = Messages.bannedItemReason;
                }

                if (src.hasPermission(Permissions.LIST_HIDDEN_EDIT)) {
                    send.append(Text.builder().append(Messages.parseMessage(Messages.bannedListHidden, arguments))
                        .onClick(TextActions.executeCallback(changeHidden(item.getItemid())))
                        .onHover(TextActions.showText(Messages.parseMessage(Messages.bannedListHideHover, arguments))).build());
                    send.append(plugin.fromLegacy(" "));
                }

                String banInfo = Messages.bannedItemHover;
                if ((src.hasPermission(Permissions.LIST_EXTRA) && src.hasPermission(Permissions.EDIT_BANNED_ITEM))) {
                    banInfo = banInfo + "\n" + Messages.bannedItemExtraInfo + "\n" + Messages.bannedItemEdit;
                    send.append(Messages.parseMessage(Messages.bannedItem + banreason, arguments))
                        .onClick(TextActions.runCommand("/restrict edit " + item.getItemid()))
                        .onHover(TextActions.showText(Messages.parseMessage(banInfo, arguments)));
                } else if (src.hasPermission(Permissions.LIST_EXTRA)) {
                    banInfo = banInfo + "\n" + Messages.bannedItemExtraInfo;
                    send.append(Messages.parseMessage(Messages.bannedItem + banreason, arguments))
                        .onHover(TextActions.showText(Messages.parseMessage(banInfo, arguments)));
                } else if (src.hasPermission(Permissions.EDIT_BANNED_ITEM)) {
                    banInfo = banInfo + "\n" + Messages.bannedItemEdit;
                    send.append(Messages.parseMessage(Messages.bannedItem + banreason, arguments))
                        .onClick(TextActions.runCommand("/restrict edit " + item.getItemid()))
                        .onHover(TextActions.showText(Messages.parseMessage(banInfo, arguments)));
                } else {
                    send.append(Messages.parseMessage(Messages.bannedItem + banreason, arguments))
                        .onHover(TextActions.showText(Messages.parseMessage(banInfo, arguments)));
                }

                contents.add(send.build());
            }
        } else {
            for (ModData mod : mods) {
                if (mod.getHidden()) {
                    continue;
                }
                HashMap<String, String> arguments = new HashMap<>();
                arguments.put("banreason", mod.getBanreason());
                arguments.put("itemid", mod.getMod());
                arguments.put("usebanned", mod.getUsagebanned().toString());
                arguments.put("breakbanned", mod.getBreakingbanned().toString());
                arguments.put("placebanned", mod.getPlacingbanned().toString());
                arguments.put("ownbanned", mod.getOwnershipbanned().toString());
                arguments.put("craftbanned", mod.getCraftbanned().toString());
                arguments.put("worldbanned", "false");
                arguments.put("modname", mod.getModname());
                arguments.put("hidden", mod.getHidden() ? "show" : "hide");

                Text.Builder send = Text.builder();

                String banreason = "";
                if (!mod.getBanreason().isEmpty()) {
                    banreason = Messages.bannedItemReason;
                }

                if (src.hasPermission(Permissions.LIST_HIDDEN_EDIT)) {
                    send.append(Text.builder().append(Messages.parseMessage(Messages.bannedListHide, arguments))
                        .onClick(TextActions.executeCallback(changeHiddenMod(mod.getMod())))
                        .onHover(TextActions.showText(Messages.parseMessage(Messages.bannedListHideHover, arguments))).build());
                    send.append(plugin.fromLegacy(" "));
                }

                String banInfo = Messages.bannedItemHover;
                if ((src.hasPermission(Permissions.LIST_EXTRA) && src.hasPermission(Permissions.EDIT_BANNED_ITEM))) {
                    banInfo = banInfo + "\n" + Messages.bannedItemExtraInfo + "\n" + Messages.bannedItemEdit;
                    send.append(Messages.parseMessage(Messages.bannedMod + banreason, arguments))
                        .onClick(TextActions.runCommand("/restrict edit " + mod.getMod()))
                        .onHover(TextActions.showText(Messages.parseMessage(banInfo, arguments)));
                } else if (src.hasPermission(Permissions.LIST_EXTRA)) {
                    banInfo = banInfo + "\n" + Messages.bannedItemExtraInfo;
                    send.append(Messages.parseMessage(Messages.bannedMod + banreason, arguments))
                        .onHover(TextActions.showText(Messages.parseMessage(banInfo, arguments)));
                } else if (src.hasPermission(Permissions.EDIT_BANNED_ITEM)) {
                    banInfo = banInfo + "\n" + Messages.bannedItemEdit;
                    send.append(Messages.parseMessage(Messages.bannedMod + banreason, arguments))
                        .onClick(TextActions.runCommand("/restrict edit " + mod.getMod()))
                        .onHover(TextActions.showText(Messages.parseMessage(banInfo, arguments)));
                } else {
                    send.append(Messages.parseMessage(Messages.bannedMod + banreason, arguments))
                        .onHover(TextActions.showText(Messages.parseMessage(banInfo, arguments)));
                }

                contents.add(send.build());
            }

            for (ItemData item : items) {
                if (item.getHidden()) {
                    continue;
                }
                HashMap<String, String> arguments = new HashMap<>();
                arguments.put("banreason", item.getBanreason());
                arguments.put("itemid", item.getItemid());
                arguments.put("usebanned", item.getUsagebanned().toString());
                arguments.put("breakbanned", item.getBreakingbanned().toString());
                arguments.put("placebanned", item.getPlacingbanned().toString());
                arguments.put("ownbanned", item.getOwnershipbanned().toString());
                arguments.put("craftbanned", item.getCraftbanned().toString());
                arguments.put("worldbanned", item.getWorldbanned().toString());
                arguments.put("itemname", item.getItemname());
                arguments.put("hidden", item.getHidden() ? "show" : "hide");

                Text.Builder send = Text.builder();

                String banreason = "";
                if (!item.getBanreason().isEmpty()) {
                    banreason = Messages.bannedItemReason;
                }

                if (src.hasPermission(Permissions.LIST_HIDDEN_EDIT)) {
                    send.append(Text.builder().append(Messages.parseMessage(Messages.bannedListHide, arguments))
                        .onClick(TextActions.executeCallback(changeHidden(item.getItemid())))
                        .onHover(TextActions.showText(Messages.parseMessage(Messages.bannedListHideHover, arguments))).build());
                    send.append(plugin.fromLegacy(" "));
                }

                String banInfo = Messages.bannedItemHover;
                if ((src.hasPermission(Permissions.LIST_EXTRA) && src.hasPermission(Permissions.EDIT_BANNED_ITEM))) {
                    banInfo = banInfo + "\n" + Messages.bannedItemExtraInfo + "\n" + Messages.bannedItemEdit;
                    send.append(Messages.parseMessage(Messages.bannedItem + banreason, arguments))
                        .onClick(TextActions.runCommand("/restrict edit " + item.getItemid()))
                        .onHover(TextActions.showText(Messages.parseMessage(banInfo, arguments)));
                } else if (src.hasPermission(Permissions.LIST_EXTRA)) {
                    banInfo = banInfo + "\n" + Messages.bannedItemExtraInfo;
                    send.append(Messages.parseMessage(Messages.bannedItem + banreason, arguments))
                        .onHover(TextActions.showText(Messages.parseMessage(banInfo, arguments)));
                } else if (src.hasPermission(Permissions.EDIT_BANNED_ITEM)) {
                    banInfo = banInfo + "\n" + Messages.bannedItemEdit;
                    send.append(Messages.parseMessage(Messages.bannedItem + banreason, arguments))
                        .onClick(TextActions.runCommand("/restrict edit " + item.getItemid()))
                        .onHover(TextActions.showText(Messages.parseMessage(banInfo, arguments)));
                } else {
                    send.append(Messages.parseMessage(Messages.bannedItem + banreason, arguments))
                        .onHover(TextActions.showText(Messages.parseMessage(banInfo, arguments)));
                }

                contents.add(send.build());
            }
        }

        Text title = plugin.fromLegacy(Messages.bannedListTitle);
        if (contents.isEmpty()) {
            if (hidden.isPresent()) {
                contents.add(plugin.fromLegacy(Messages.bannedItemNonHidden));
                title = plugin.fromLegacy(Messages.bannedListHiddenTitle);
            } else {
                contents.add(plugin.fromLegacy(Messages.bannedItemNonSet));
            }
        }

        Collections.sort(contents);

        paginationService.builder()
            .title(title)
            .contents(contents)
            .padding(plugin.fromLegacy(Messages.bannedListPadding))
            .sendTo(src);
        return CommandResult.success();

    }

    private Consumer<CommandSource> changeHidden(String itemID) {
        return consumer -> {
            final java.util.List<ItemData> items = new ArrayList<ItemData>(plugin.getItemData());
            for (ItemData item : items) {
                if (item.getItemid().equals(itemID)) {
                    item.setHidden(!item.getHidden());
                    String hidden = item.getHidden() ? "true" : "false";
                    consumer.sendMessage(plugin.fromLegacy("&6" + itemID + " &2hidden set to: &6" + hidden));
                }
            }
        };
    }

    private Consumer<CommandSource> changeHiddenMod(String modID) {
        return consumer -> {
            final java.util.List<ModData> mods = new ArrayList<ModData>(plugin.getModData());
            for (ModData mod : mods) {
                if (mod.getMod().equals(modID)) {
                    mod.setHidden(!mod.getHidden());
                    String hidden = mod.getHidden() ? "true" : "false";
                    consumer.sendMessage(plugin.fromLegacy("&6" + modID + " &2hidden set to: &6" + hidden));
                }
            }
        };
    }
}
