package net.moddedminecraft.mmcrestrict;

import com.google.inject.Inject;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.moddedminecraft.mmcrestrict.Config.Config;
import net.moddedminecraft.mmcrestrict.Config.Messages;
import net.moddedminecraft.mmcrestrict.Data.ItemData;
import net.moddedminecraft.mmcrestrict.Data.ItemData.ItemDataSerializer;
import net.moddedminecraft.mmcrestrict.Data.ModData;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.LoadedGameEvent;
import org.spongepowered.api.event.lifecycle.RefreshGameEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.registry.RegistryEntry;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.chunk.WorldChunk;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.math.vector.Vector3i;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Plugin("mmcrestrict")
public class MMCRestrict {

    @Inject
    @DefaultConfig(sharedRoot = false)
    public Path defaultConf;

    @Inject
    @ConfigDir(sharedRoot = false)
    public Path ConfigDir;

    @Inject
    private PluginContainer plugin;

    @Inject
    private Logger logger;

    private Map<String, ItemData> items;
    private Map<String, ModData> modslist;

    private Task autoPurgeTask = null;

    @Listener
    public void Init(LoadedGameEvent event) throws IOException {
        new Config(this);
        new Messages(this);
        Sponge.eventManager().registerListeners(plugin, new EventListener(this));

        loadCommands();
        loadData();
        startAutoPurge();

        logger.info("Banned items loaded: " + items.size());
        logger.info("MMCRestrict Loaded");
    }

    public void startAutoPurge() {
        if (autoPurgeTask != null) {
            Sponge.asyncScheduler().tasks(plugin).remove(autoPurgeTask);
        }

        if (!Config.defaultAutoPurge) {
            return;
        }

        autoPurgeTask = Task.builder()
            .execute(() -> checkLoadedChunks())
            .interval(Config.defaultAutoPurgeInterval, TimeUnit.MINUTES)
            .delay(5, TimeUnit.MINUTES)
            .build();
        Sponge.asyncScheduler().submit(autoPurgeTask);

        logger.info("MMCRestrict World AutoPurge Started (check chunks every " + Config.defaultAutoPurgeInterval + " minutes )");

    }

    @Listener
    public void onPluginReload(RefreshGameEvent event) throws IOException {
        new Config(this);
        loadData();
        startAutoPurge();
    }


    @Listener
    public void onRegisterCommand(RegisterCommandEvent<Command.Parameterized> event) {
        Parameter.Value<String> modKey = Parameter.string().key("mod").optional().build();
        Parameter.Value<String> _itemId = Parameter.string().key("itemid").build();
        Parameter.Value<String> _option = generateParameter("option");
        Parameter.Value<String> _setting = generateParameter("setting");

        event.register(
            plugin,
            Command.builder()
                .executor(context -> {
                    PaginationService paginationService = Sponge.serviceProvider().paginationService();
                    List<Component> contents = new ArrayList<>();

                    if (context.hasPermission(Permissions.ADD_BANNED_ITEM)) {
                        contents.add(
                            Component.text()
                                .append(Component.text("/restrict ").color(NamedTextColor.AQUA))
                                .append(Component.text("add - ").color(NamedTextColor.BLUE))
                                .append(Component.text("Add an item to the ban list from your hand").color(NamedTextColor.GRAY))
                                .build()
                        );
                    }
                    if (context.hasPermission(Permissions.REMOVE_BANNED_ITEM)) {
                        contents.add(
                            Component.text()
                                .append(Component.text("/restrict ").color(NamedTextColor.AQUA))
                                .append(Component.text("remove [itemID] - ").color(NamedTextColor.BLUE))
                                .append(Component.text("Remove an item from the ban list").color(NamedTextColor.GRAY))
                                .build()
                        );
                    }
                    if (context.hasPermission(Permissions.EDIT_BANNED_ITEM)) {
                        contents.add(
                            Component.text()
                                .append(Component.text("/restrict ").color(NamedTextColor.AQUA))
                                .append(Component.text("edit (option) (value) - ").color(NamedTextColor.BLUE))
                                .append(Component.text("List options for a banned item or edit an option").color(NamedTextColor.GRAY))
                                .build()
                        );
                    }
                    if (context.hasPermission(Permissions.LIST_BANNED_ITEMS)) {
                        contents.add(
                            Component.text()
                                .append(Component.text("/restrict ").color(NamedTextColor.AQUA))
                                .append(Component.text("list ").color(NamedTextColor.BLUE))
                                .append(Component.text("List all current banned items").color(NamedTextColor.GRAY))
                                .build()
                        );
                    }

                    paginationService.builder()
                        .title(Component.text("MMCRestrict Help").color(NamedTextColor.GOLD))
                        .contents(contents)
                        .header(Component.text("[] = required  () = optional").color(NamedTextColor.AQUA))
                        .padding(Component.text("="))
                        .sendTo(context.cause().audience());

                    return CommandResult.success();
                })
                .addChild(
                    Command.builder()
                        .addParameter(modKey)
                        .executor(context -> {
                            Optional<String> mod = context.one(modKey);
                            Player player = (Player) context.subject();
                            if (!mod.isPresent()) {
                                throw new CommandException(Component.text("Usage: /restrict mod (modid)").color(NamedTextColor.RED));
                            }
                            String modArgument = mod.get();
                            final List<ModData> mods = new ArrayList<>(getModData());
                            if (!mods.isEmpty()) {
                                for (ModData modData : mods) {
                                    if (modData.getMod().equals(modArgument)) {
                                        throw new CommandException(Component.text("Mod already exists"));
                                    }
                                }
                            }

                            this.addMod(new ModData(
                                Config.defaultHidden,
                                modArgument,
                                modArgument,
                                Config.defaultReason,
                                Config.defaultUsage,
                                Config.defaultBreaking,
                                Config.defaultPlacing,
                                Config.defaultOwnership,
                                Config.defaultDrop,
                                Config.defaultCraft
                            ));

                            try {
                                saveData();
                            } catch (Exception e) {
                                player.sendMessage(Component.text("Data was not saved correctly."));
                                e.printStackTrace();
                            }
                            logToFile("ban-list", player.name() + " added the " + modArgument + " mod to the ban list");
                            player.sendMessage(Component.text(modArgument + " mod was added to the list."));
                            return CommandResult.success();
                        })
                        .permission(Permissions.ADD_BANNED_ITEM)
                        .build(),
                    "mod"
                ).addChild(
                    Command.builder()
                        .executor(context -> {
                            if (!(context.subject() instanceof Player)) {
                                return CommandResult.error(Component.text("Player only command"));
                            }
                            Player player = (Player) context.subject();
                            final List<ItemData> items = new ArrayList<>(getItemData());

                            if (!player.itemInHand(HandTypes.MAIN_HAND).isEmpty()) {
                                ItemStackSnapshot mainHandItem = player.itemInHand(HandTypes.MAIN_HAND).createSnapshot();
                                ResourceKey resource = Sponge.game().registry(RegistryTypes.ITEM_TYPE).valueKey(mainHandItem.type());

                                logger.info(resource.namespace() + ":" + resource.value());

                                if (!items.isEmpty()) {
                                    for (ItemData item : items) {
                                        ItemType type = ItemTypes.registry().value(ResourceKey.resolve(item.getItemId()));
                                        if (type.equals(mainHandItem.type())) {
                                            throw new CommandException(Component.text("Item already exists!"));
                                        }
                                    }
                                }
                                addItem(new ItemData(
                                    Config.defaultHidden,
                                    resource.namespace() + ":" + resource.value(),
                                    resource.value(),
                                    Config.defaultReason,
                                    Config.defaultUsage,
                                    Config.defaultBreaking,
                                    Config.defaultPlacing,
                                    Config.defaultOwnership,
                                    Config.defaultDrop,
                                    Config.defaultCraft,
                                    Config.defaultWorld
                                ));

                                try {
                                    saveData();
                                } catch (Exception e) {
                                    CommandResult.error(Component.text("Data was not saved correctly"));
                                    e.printStackTrace();
                                }
                                logToFile("ban-list", player.name() + " added " + LegacyComponentSerializer.legacyAmpersand().serialize(mainHandItem.get(Keys.DISPLAY_NAME).orElse(Component.text("INVALID"))) + " to the ban list");
                            }
                            return CommandResult.success();
                        })
                        .permission(Permissions.ADD_BANNED_ITEM)
                        .build(),
                    "add"
                ).addChild(
                    Command.builder()
                        .addParameter(_itemId)
                        .executor(context -> {
                            Optional<String> id = context.one(_itemId);
                            final List<ItemData> items = new ArrayList<>(getItemData());
                            final List<ModData> mods = new ArrayList<>(getModData());
                            String itemName = null;

                            if (!id.isPresent()) {
                                return CommandResult.error(Component.text("item id is required!"));
                            }

                            for (ModData mod : mods) {
                                if (mod.getMod().equals(id.get())) {
                                    itemName = mod.getMod();
                                    if (removeMod(id.get()) != null) {
                                        try {
                                            saveData();
                                        } catch (Exception e) {
                                            context.cause().sendMessage(Identity.nil(), Component.text("Data was not saved correctly"));
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }

                            for (ItemData item : items) {
                                ItemType itemType = ItemTypes.registry().findEntry(ResourceKey.resolve(id.get())).get().value();
                                ItemType type = ItemTypes.registry().findEntry(ResourceKey.resolve(item.getItemId())).get().value();
                                if (type.equals(itemType)) {
                                    itemName = itemType.get(Keys.CUSTOM_NAME).get().examinableName();
                                    if (removeItem(id.get()) != null) {
                                        try {
                                            saveData();
                                        } catch (Exception e) {
                                            context.cause().sendMessage(Identity.nil(), Component.text("Data was not saved correctly"));
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }


                            if (itemName == null) {
                                logToFile("ban-list", context.identifier() + " removed an item from the ban list");
                                context.cause().sendMessage(Identity.nil(), Component.text("Item was removed from the list."));
                            } else {
                                logToFile("ban-list", context.identifier() + " removed " + itemName + " from the ban list.");
                                context.cause().sendMessage(Identity.nil(), Component.text(itemName + " was removed from the list."));
                            }
                            return CommandResult.success();
                        })
                        .permission(Permissions.REMOVE_BANNED_ITEM)
                        .build(),
                    "remove"
                )/*.addChild(
                    Command.builder()
                        .permission(Permissions.SEARCH_BANNED_ITEM)
                        .build(),
                    "search"
                )*/.addChild(
                    Command.builder()
                        .addParameters(
                            _itemId,
                            _option,
                            _setting
                        )
                        .permission(Permissions.EDIT_BANNED_ITEM)
                        .executor(context -> {
                            Optional<String> itemID = context.one(_itemId);
                            Optional<String> option = context.one(_option);
                            Optional<String> value = context.one(_setting);

                            final List<ItemData> items = new ArrayList<>(getItemData());
                            final List<ModData> mods = new ArrayList<>(getModData());
                            PaginationService paginationService = Sponge.serviceProvider().paginationService();
                            List<Component> contents = new ArrayList<>();

                            if (!itemID.isPresent()) {
                                throw new CommandException(Component.text("Invalid usage: /restrict edit ItemId [option] [value]"));
                            }

                            String itemType = itemID.get();

                            if (option.isPresent()) {
                                if (!value.isPresent()) {
                                    throw new CommandException(Component.text("Invalid usage: /restrict edit ItemId [option] [value]"));
                                }
                                if (!mods.isEmpty()) {
                                    for (ModData mod : mods) {
                                        if (mod.getMod().equals(itemType)) {
                                            switch (option.get()) {
                                                case "name":
                                                    mod.setItemname(value.get());
                                                    logToFile("ban-list", "World for " + mod.getModname() + " was changed to " + value.get());
                                                    context.cause().sendMessage(Identity.nil(), Component.text("Name set to: " + value.get()));
                                                    break;
                                                case "reason":
                                                    mod.setBanreason(value.get());
                                                    logToFile("ban-list", "Reason for " + mod.getModname() + " was changed to " + value.get());
                                                    context.cause().sendMessage(Identity.nil(), Component.text("Reason set to: " + value.get()));
                                                    break;
                                                case "use":
                                                    if (value.get().equalsIgnoreCase("true") || value.get().equalsIgnoreCase("false")) {
                                                        mod.setUsagebanned(Boolean.parseBoolean(value.get()));
                                                        logToFile("ban-list", "Usage for " + mod.getModname() + " was changed to " + value.get());
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Usage set to: " + value.get()));
                                                    } else {
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Invalid value: " + value.get()));
                                                    }
                                                    break;
                                                case "own":
                                                    if (value.get().equalsIgnoreCase("true") || value.get().equalsIgnoreCase("false")) {
                                                        mod.setOwnershipbanned(Boolean.parseBoolean(value.get()));
                                                        logToFile("ban-list", "Ownership for " + mod.getModname() + " was changed to " + value.get());
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Ownership set to: " + value.get()));
                                                    } else {
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Invalid value: " + value.get()));
                                                    }
                                                    break;
                                                case "place":
                                                    if (value.get().equalsIgnoreCase("true") || value.get().equalsIgnoreCase("false")) {
                                                        mod.setPlacingbanned(Boolean.parseBoolean(value.get()));
                                                        logToFile("ban-list", "Placing for " + mod.getModname() + " was changed to " + value.get());
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Placing set to: " + value.get()));
                                                    } else {
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Invalid value: " + value.get()));
                                                    }
                                                    break;
                                                case "drop":
                                                    if (value.get().equalsIgnoreCase("true") || value.get().equalsIgnoreCase("false")) {
                                                        mod.setDropbanned(Boolean.parseBoolean(value.get()));
                                                        logToFile("ban-list", "Drop for " + mod.getModname() + " was changed to " + value.get());
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Drop set to: " + value.get()));
                                                    } else {
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Invalid value: " + value.get()));
                                                    }
                                                    break;
                                                case "craft":
                                                    if (value.get().equalsIgnoreCase("true") || value.get().equalsIgnoreCase("false")) {
                                                        mod.setCraftbanned(Boolean.parseBoolean(value.get()));
                                                        logToFile("ban-list", "Crafting for " + mod.getModname() + " was changed to " + value.get());
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Crafting set to: " + value.get()));
                                                    } else {
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Invalid value: " + value.get()));
                                                    }
                                                    break;
                                                default:
                                                    throw new CommandException(Component.text("Invalid usage: /restrict edit ItemId [option] [value]"));
                                            }
                                            try {
                                                saveData();
                                            } catch (Exception e) {
                                                context.cause().sendMessage(Identity.nil(), Component.text("Data was not saved correctly."));
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                                if (!items.isEmpty()) {
                                    ItemType type0 = ItemTypes.registry().value(ResourceKey.resolve(itemType));
                                    for (ItemData item : items) {
                                        ItemType type = ItemTypes.registry().value(ResourceKey.resolve(item.getItemId()));
                                        if (type.equals(type0)) {
                                            switch (option.get()) {
                                                case "name":
                                                    item.setItemName(value.get());
                                                    logToFile("ban-list", "World for " + type.get(Keys.CUSTOM_NAME) + " was changed to " + value.get());
                                                    context.cause().sendMessage(Identity.nil(), Component.text("Name set to: " + value.get()));
                                                    break;
                                                case "reason":
                                                    item.setBanReason(value.get());
                                                    logToFile("ban-list", "Reason for " + type.get(Keys.CUSTOM_NAME) + " was changed to " + value.get());
                                                    context.cause().sendMessage(Identity.nil(), Component.text("Reason set to: " + value.get()));
                                                    break;
                                                case "use":
                                                    if (value.get().equalsIgnoreCase("true") || value.get().equalsIgnoreCase("false")) {
                                                        item.setUsageBanned(Boolean.parseBoolean(value.get()));
                                                        logToFile("ban-list", "Usage for " + type.get(Keys.CUSTOM_NAME) + " was changed to " + value.get());
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Usage set to: " + value.get()));
                                                    } else {
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Invalid value: " + value.get()));
                                                    }
                                                    break;
                                                case "own":
                                                    if (value.get().equalsIgnoreCase("true") || value.get().equalsIgnoreCase("false")) {
                                                        item.setOwnershipBanned(Boolean.parseBoolean(value.get()));
                                                        logToFile("ban-list", "Ownership for " + type.get(Keys.CUSTOM_NAME) + " was changed to " + value.get());
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Ownership set to: " + value.get()));
                                                    } else {
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Invalid value: " + value.get()));
                                                    }
                                                    break;
                                                case "break":
                                                    if (value.get().equalsIgnoreCase("true") || value.get().equalsIgnoreCase("false")) {
                                                        item.setBreakingBanned(Boolean.parseBoolean(value.get()));
                                                        logToFile("ban-list", "Breaking for " + type.get(Keys.CUSTOM_NAME) + " was changed to " + value.get());
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Breaking set to: " + value.get()));
                                                    } else {
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Invalid value: " + value.get()));
                                                    }
                                                    break;
                                                case "place":
                                                    if (value.get().equalsIgnoreCase("true") || value.get().equalsIgnoreCase("false")) {
                                                        item.setPlacingBanned(Boolean.parseBoolean(value.get()));
                                                        logToFile("ban-list", "Placing for " + type.get(Keys.CUSTOM_NAME) + " was changed to " + value.get());
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Placing set to: " + value.get()));
                                                    } else {
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Invalid value: " + value.get()));
                                                    }
                                                    break;
                                                case "world":
                                                    if (value.get().equalsIgnoreCase("true") || value.get().equalsIgnoreCase("false")) {
                                                        item.setWorldBanned(Boolean.parseBoolean(value.get()));
                                                        logToFile("ban-list", "World for " + type.get(Keys.CUSTOM_NAME) + " was changed to " + value.get());
                                                        context.cause().sendMessage(Identity.nil(), Component.text("World set to: " + value.get()));
                                                    } else {
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Invalid value: " + value.get()));
                                                    }
                                                    break;
                                                case "drop":
                                                    if (value.get().equalsIgnoreCase("true") || value.get().equalsIgnoreCase("false")) {
                                                        item.setDropBanned(Boolean.parseBoolean(value.get()));
                                                        logToFile("ban-list", "Drop for " + type.get(Keys.CUSTOM_NAME) + " was changed to " + value.get());
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Drop set to: " + value.get()));
                                                    } else {
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Invalid value: " + value.get()));
                                                    }
                                                    break;
                                                case "craft":
                                                    if (value.get().equalsIgnoreCase("true") || value.get().equalsIgnoreCase("false")) {
                                                        item.setCraftBanned(Boolean.parseBoolean(value.get()));
                                                        logToFile("ban-list", "Crafting for " + type.get(Keys.CUSTOM_NAME) + " was changed to " + value.get());
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Crafting set to: " + value.get()));
                                                    } else {
                                                        context.cause().sendMessage(Identity.nil(), Component.text("Invalid value: " + value.get()));
                                                    }
                                                    break;
                                                default:
                                                    ;
                                                    throw new CommandException(Component.text("Invalid usage: /restrict edit ItemId [option] [value]"));
                                            }
                                            try {
                                                saveData();
                                            } catch (Exception e) {
                                                context.cause().sendMessage(Identity.nil(), Component.text("Data was not saved correctly."));
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            } else {
                                boolean valid = false;
                                if (!mods.isEmpty()) {
                                    for (ModData mod : mods) {
                                        if (mod.getMod().equals(itemType)) {
                                            valid = true;
                                            if (context.hasPermission(Permissions.REMOVE_BANNED_ITEM)) {
                                                contents.add(
                                                    Component.text()
                                                        .append(Component.text("[").color(NamedTextColor.AQUA))
                                                        .append(Component.text("Remove").color(NamedTextColor.GOLD))
                                                        .append(Component.text("]").color(NamedTextColor.AQUA))
                                                        .hoverEvent(HoverEvent.showText(
                                                            Component.text("Click here to remove this item from the ban list")
                                                        ))
                                                        .clickEvent(ClickEvent.runCommand("/restrict remove " + mod.getMod()))
                                                        .build()
                                                );
                                                contents.add(
                                                    Component.text()
                                                        .append(Component.text("ID: ").color(NamedTextColor.GOLD))
                                                        .append(Component.text(mod.getMod()).color(NamedTextColor.GRAY))
                                                        .build()
                                                );
                                                contents.add(
                                                    Component.text()
                                                        .append(Component.text("Name: ").color(NamedTextColor.AQUA))
                                                        .append(Component.text(mod.getModname()).color(NamedTextColor.GRAY))
                                                        .hoverEvent(
                                                            HoverEvent.showText(
                                                                Component.text()
                                                                    .append(Component.text("Click here to change the value of ").color(NamedTextColor.AQUA))
                                                                    .append(Component.text("Name").color(NamedTextColor.GRAY))
                                                                    .build()
                                                            )
                                                        )
                                                        .clickEvent(ClickEvent.suggestCommand("/restrict edit " + mod.getMod() + " name " + mod.getModname()))
                                                        .build()
                                                );
                                                contents.add(
                                                    Component.text()
                                                        .append(Component.text("Ban Reason: ").color(NamedTextColor.AQUA))
                                                        .append(Component.text(mod.getBanreason()).color(NamedTextColor.GRAY))
                                                        .hoverEvent(HoverEvent.showText(
                                                            Component.text()
                                                                .append(Component.text("Click here to change the value of ").color(NamedTextColor.AQUA))
                                                                .append(Component.text("reason").color(NamedTextColor.GOLD))
                                                                .build()
                                                        ))
                                                        .clickEvent(ClickEvent.suggestCommand("/restrict edit " + mod.getMod() + " reason [MESSAGE]"))
                                                        .build()
                                                );
                                                contents.add(
                                                    Component.text()
                                                        .append(Component.text("Usage Banned: ").color(NamedTextColor.AQUA))
                                                        .append(Component.text(mod.getUsagebanned()).color(NamedTextColor.GRAY))
                                                        .hoverEvent(HoverEvent.showText(
                                                            Component.text()
                                                                .append(Component.text("Click here to change the value of ").color(NamedTextColor.AQUA))
                                                                .append(Component.text("Usage").color(NamedTextColor.GOLD))
                                                                .build()
                                                        ))
                                                        .clickEvent(ClickEvent.suggestCommand("/restrict edit " + mod.getMod() + " usage [setting]"))
                                                        .build()
                                                );
                                                contents.add(
                                                    Component.text()
                                                        .append(Component.text("Breaking Banned: ").color(NamedTextColor.AQUA))
                                                        .append(Component.text(mod.getBreakingbanned()).color(NamedTextColor.GRAY))
                                                        .hoverEvent(HoverEvent.showText(
                                                            Component.text()
                                                                .append(Component.text("Click here to change the value of ").color(NamedTextColor.AQUA))
                                                                .append(Component.text("Breaking").color(NamedTextColor.GOLD))
                                                                .build()
                                                        ))
                                                        .clickEvent(ClickEvent.suggestCommand("/restrict edit " + mod.getMod() + " break [setting]"))
                                                        .build()
                                                );
                                                contents.add(
                                                    Component.text()
                                                        .append(Component.text("Placing Banned: ").color(NamedTextColor.AQUA))
                                                        .append(Component.text(mod.getPlacingbanned()).color(NamedTextColor.GRAY))
                                                        .hoverEvent(HoverEvent.showText(
                                                            Component.text()
                                                                .append(Component.text("Click here to change the value of ").color(NamedTextColor.AQUA))
                                                                .append(Component.text("Placing").color(NamedTextColor.GOLD))
                                                                .build()
                                                        ))
                                                        .clickEvent(ClickEvent.suggestCommand("/restrict edit " + mod.getMod() + " place [setting]"))
                                                        .build()
                                                );
                                                contents.add(
                                                    Component.text()
                                                        .append(Component.text("Ownership Banned: ").color(NamedTextColor.AQUA))
                                                        .append(Component.text(mod.getOwnershipbanned()).color(NamedTextColor.GRAY))
                                                        .hoverEvent(HoverEvent.showText(
                                                            Component.text()
                                                                .append(Component.text("Click here to change the value of ").color(NamedTextColor.AQUA))
                                                                .append(Component.text("Ownership").color(NamedTextColor.GOLD))
                                                                .build()
                                                        ))
                                                        .clickEvent(ClickEvent.suggestCommand("/restrict edit " + mod.getMod() + " ownership [setting]"))
                                                        .build()
                                                );
                                                contents.add(
                                                    Component.text()
                                                        .append(Component.text("Drop banned: ").color(NamedTextColor.AQUA))
                                                        .append(Component.text(mod.getDropbanned()).color(NamedTextColor.GRAY))
                                                        .hoverEvent(HoverEvent.showText(
                                                            Component.text()
                                                                .append(Component.text("Click here to change the value of ").color(NamedTextColor.AQUA))
                                                                .append(Component.text("Drop").color(NamedTextColor.GOLD))
                                                                .build()
                                                        ))
                                                        .clickEvent(ClickEvent.suggestCommand("/restrict edit " + mod.getMod() + " drop [setting]"))
                                                        .build()
                                                );
                                                contents.add(
                                                    Component.text()
                                                        .append(Component.text("Craft banned: ").color(NamedTextColor.AQUA))
                                                        .append(Component.text(mod.getCraftbanned()).color(NamedTextColor.GRAY))
                                                        .hoverEvent(HoverEvent.showText(
                                                            Component.text()
                                                                .append(Component.text("Click here to change the value of ").color(NamedTextColor.AQUA))
                                                                .append(Component.text("Craft").color(NamedTextColor.GOLD))
                                                                .build()
                                                        ))
                                                        .clickEvent(ClickEvent.suggestCommand("/restrict edit " + mod.getMod() + " craft [setting]"))
                                                        .build()
                                                );
                                            }
                                        }
                                    }
                                }
                                if (!items.isEmpty()) {
                                    valid = true;
                                    for (ItemData item : items) {
                                        ResourceKey entry = ResourceKey.resolve(itemID.get());
                                        Optional<RegistryEntry<ItemType>> itemName = ItemTypes.registry().findEntry(entry);
                                        if (!itemName.isPresent()) {
                                            valid =false;
                                            continue;
                                        }
                                        Component itemNameComponent = itemName.get().value().get(Keys.DISPLAY_NAME).orElse(Component.text("INVALID"));
                                        ItemType type = ItemTypes.registry().value(ResourceKey.resolve(item.getItemId()));
                                        if (type.equals(ItemTypes.registry().findEntry(ResourceKey.resolve(itemID.get())).get())) {
                                            if (context.hasPermission(Permissions.REMOVE_BANNED_ITEM)) {
                                                contents.add(
                                                    Component.text()
                                                        .append(Component.text("[").color(NamedTextColor.AQUA))
                                                        .append(Component.text("Remove").color(NamedTextColor.GOLD))
                                                        .append(Component.text("]").color(NamedTextColor.AQUA))
                                                        .hoverEvent(HoverEvent.showText(
                                                            Component.text("Click here to remove this item from the ban list")
                                                        ))
                                                        .clickEvent(ClickEvent.runCommand("/restrict remove " + type.get(Keys.CUSTOM_NAME).get()))
                                                        .build()
                                                );
                                            }
                                            contents.add(
                                                Component.text()
                                                    .append(Component.text("ID: ").color(NamedTextColor.AQUA))
                                                    .append(itemNameComponent).color(NamedTextColor.GOLD)
                                                    .build()
                                            );
                                            contents.add(
                                                Component.text()
                                                    .append(Component.text("Name: ").color(NamedTextColor.AQUA))
                                                    .append(itemNameComponent.color(NamedTextColor.GRAY))
                                                    .hoverEvent(HoverEvent.showText(
                                                        Component.text()
                                                            .append(Component.text("Click here to change the value of ").color(NamedTextColor.AQUA))
                                                            .append(Component.text("Name").color(NamedTextColor.GOLD))
                                                            .build()
                                                    ))
                                                    .clickEvent(ClickEvent.suggestCommand("/restrict edit " + item.getItemId() + " name [setting]"))
                                                    .build()
                                            );
                                            contents.add(
                                                Component.text()
                                                    .append(Component.text("Ban Reason: ").color(NamedTextColor.AQUA))
                                                    .append(itemNameComponent.color(NamedTextColor.GRAY))
                                                    .hoverEvent(HoverEvent.showText(
                                                        Component.text()
                                                            .append(Component.text("Click here to change the value of ").color(NamedTextColor.AQUA))
                                                            .append(Component.text("Ban Reason").color(NamedTextColor.GOLD))
                                                            .build()
                                                    ))
                                                    .clickEvent(ClickEvent.suggestCommand("/restrict edit " + item.getItemId() + " reason [setting]"))
                                                    .build()
                                            );
                                            contents.add(
                                                Component.text()
                                                    .append(Component.text("Usage Banned: ").color(NamedTextColor.AQUA))
                                                    .append(itemNameComponent.color(NamedTextColor.GRAY))
                                                    .hoverEvent(HoverEvent.showText(
                                                        Component.text()
                                                            .append(Component.text("Click here to change the value of ").color(NamedTextColor.AQUA))
                                                            .append(Component.text("Usage").color(NamedTextColor.GOLD))
                                                            .build()
                                                    ))
                                                    .clickEvent(ClickEvent.suggestCommand("/restrict edit " + item.getItemId() + " usage [setting]"))
                                                    .build()
                                            );
                                            contents.add(
                                                Component.text()
                                                    .append(Component.text("Breaking Banned: ").color(NamedTextColor.AQUA))
                                                    .append(Component.text().color(NamedTextColor.GRAY))
                                                    .hoverEvent(HoverEvent.showText(
                                                        Component.text()
                                                            .append(Component.text("Click here to change the value of ").color(NamedTextColor.AQUA))
                                                            .append(Component.text("Breaking").color(NamedTextColor.GOLD))
                                                            .build()
                                                    ))
                                                    .clickEvent(ClickEvent.suggestCommand("/restrict edit " + itemName + " break [setting]"))
                                                    .build()
                                            );
                                            contents.add(
                                                Component.text()
                                                    .append(Component.text("Placing Banned: ").color(NamedTextColor.AQUA))
                                                    .append(Component.text().color(NamedTextColor.GRAY))
                                                    .hoverEvent(HoverEvent.showText(
                                                        Component.text()
                                                            .append(Component.text("Click here to change the value of ").color(NamedTextColor.AQUA))
                                                            .append(Component.text("Placing").color(NamedTextColor.GOLD))
                                                            .build()
                                                    ))
                                                    .clickEvent(ClickEvent.suggestCommand("/restrict edit " + itemName + " place [setting]"))
                                                    .build()
                                            );
                                            contents.add(
                                                Component.text()
                                                    .append(Component.text("Ownership Banned: ").color(NamedTextColor.AQUA))
                                                    .append(Component.text().color(NamedTextColor.GRAY))
                                                    .hoverEvent(HoverEvent.showText(
                                                        Component.text()
                                                            .append(Component.text("Click here to change the value of ").color(NamedTextColor.AQUA))
                                                            .append(Component.text("Ownership").color(NamedTextColor.GOLD))
                                                            .build()
                                                    ))
                                                    .clickEvent(ClickEvent.suggestCommand("/restrict edit " + itemName + " own [setting]"))
                                                    .build()
                                            );
                                            contents.add(
                                                Component.text()
                                                    .append(Component.text("Drop Banned: ").color(NamedTextColor.AQUA))
                                                    .append(Component.text().color(NamedTextColor.GRAY))
                                                    .hoverEvent(HoverEvent.showText(
                                                        Component.text()
                                                            .append(Component.text("Click here to change the value of ").color(NamedTextColor.AQUA))
                                                            .append(Component.text("Drop").color(NamedTextColor.GOLD))
                                                            .build()
                                                    ))
                                                    .clickEvent(ClickEvent.suggestCommand("/restrict edit " + itemName + " drop [setting]"))
                                                    .build()
                                            );
                                            contents.add(
                                                Component.text()
                                                    .append(Component.text("Craft Banned: ").color(NamedTextColor.AQUA))
                                                    .append(Component.text().color(NamedTextColor.GRAY))
                                                    .hoverEvent(HoverEvent.showText(
                                                        Component.text()
                                                            .append(Component.text("Click here to change the value of ").color(NamedTextColor.AQUA))
                                                            .append(Component.text("Craft").color(NamedTextColor.GOLD))
                                                            .build()
                                                    ))
                                                    .clickEvent(ClickEvent.suggestCommand("/restrict edit " + itemName + " craft [setting]"))
                                                    .build()
                                            );
                                            contents.add(
                                                Component.text()
                                                    .append(Component.text("World Banned: ").color(NamedTextColor.AQUA))
                                                    .append(Component.text().color(NamedTextColor.GRAY))
                                                    .hoverEvent(HoverEvent.showText(
                                                        Component.text()
                                                            .append(Component.text("Click here to change the value of ").color(NamedTextColor.AQUA))
                                                            .append(Component.text("World").color(NamedTextColor.GOLD))
                                                            .build()
                                                    ))
                                                    .clickEvent(ClickEvent.suggestCommand("/restrict edit " + "" + " world [setting]"))
                                                    .build()
                                            );

                                        }
                                    }
                                }
                                if (!valid) {
                                    throw new CommandException(Component.text("Item specified is not currently banned."));
                                }

                                paginationService.builder()
                                    .title(Component.text("Edit").color(NamedTextColor.AQUA))
                                    .contents(contents)
                                    .padding(Component.text("-"))
                                    .sendTo(context.cause().audience());
                            }
                            return CommandResult.success();
                        })
                        .build(),
                    "edit"
                ).addChild(
                    Command.builder()
                        .permission(Permissions.LIST_BANNED_ITEMS)
                        .addParameters(generateOptionalParameter("hidden"))
                        .executor(context -> {
                            List<ItemData> items = new ArrayList<>(getItemData());
                            System.out.println(items.size());
                            List<ModData> mods = new ArrayList<>(getModData());
                            Optional<String> hidden = context.one(generateOptionalParameter("hidden"));

                            PaginationService paginationService = Sponge.serviceProvider().provide(PaginationService.class).get();
                            List<Component> contents = new ArrayList<>();

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

                                    TextComponent.Builder send = Component.text();

                                    String banReason = "";
                                    if (!mod.getBanreason().isEmpty()) {
                                        banReason = Messages.bannedItemReason;
                                    }

                                    if (context.cause().hasPermission(Permissions.LIST_HIDDEN_EDIT)) {
                                        send.append(Messages.parseMessage(Messages.bannedListHidden, arguments));
                                        send.append(Component.text(" "));
                                    }

                                    String banInfo = Messages.bannedItemHover;
                                    if ((context.hasPermission(Permissions.LIST_EXTRA) && context.hasPermission(Permissions.EDIT_BANNED_ITEM))) {
                                        banInfo = banInfo + "\n" + Messages.bannedItemExtraInfo + "\n" + Messages.bannedItemEdit;
                                        send.append(Messages.parseMessage(Messages.bannedMod + banReason, arguments))
                                            .clickEvent(ClickEvent.runCommand("/restrict edit " + mod.getMod()))
                                            .hoverEvent(HoverEvent.showText(Messages.parseMessage(banInfo, arguments)));
                                    } else if (context.hasPermission(Permissions.LIST_EXTRA)) {
                                        banInfo = banInfo + "\n" + Messages.bannedItemExtraInfo;
                                        send.append(Messages.parseMessage(Messages.bannedMod + banReason, arguments))
                                            .hoverEvent(HoverEvent.showText(Messages.parseMessage(banInfo, arguments)));
                                    } else if (context.hasPermission(Permissions.EDIT_BANNED_ITEM)) {
                                        banInfo = banInfo + "\n" + Messages.bannedItemEdit;
                                        send.append(Messages.parseMessage(Messages.bannedMod + banReason, arguments))
                                            .clickEvent(ClickEvent.runCommand("/restrict edit " + mod.getMod()))
                                            .hoverEvent(HoverEvent.showText(Messages.parseMessage(banInfo, arguments)));
                                    } else {
                                        send.append(Messages.parseMessage(Messages.bannedMod + banReason, arguments))
                                            .hoverEvent(HoverEvent.showText(Messages.parseMessage(banInfo, arguments)));
                                    }
                                    contents.add(send.build());
                                }

                                for (ItemData item : items) {
                                    if (!item.getHidden()) {
                                        continue;
                                    }
                                    HashMap<String, String> arguments = new HashMap<>();
                                    arguments.put("banreason", item.getBanReason());
                                    arguments.put("itemid", item.getItemId());
                                    arguments.put("usebanned", item.getUsageBanned().toString());
                                    arguments.put("breakbanned", item.getBreakingBanned().toString());
                                    arguments.put("placebanned", item.getPlacingBanned().toString());
                                    arguments.put("ownbanned", item.getOwnershipBanned().toString());
                                    arguments.put("craftbanned", item.getCraftBanned().toString());
                                    arguments.put("worldbanned", item.getWorldBanned().toString());
                                    arguments.put("itemname", item.getItemId());
                                    arguments.put("hidden", item.getHidden() ? "show" : "hide");

                                    TextComponent.Builder send = Component.text();

                                    String banReason = "";
                                    if (!item.getBanReason().isEmpty()) {
                                        banReason = Messages.bannedItemReason;
                                    }

                                    if (context.hasPermission(Permissions.LIST_HIDDEN_EDIT)) {
                                        send.append(Messages.parseMessage(Messages.bannedListHidden, arguments))
                                            .hoverEvent(HoverEvent.showText(Messages.parseMessage(Messages.bannedListHideHover, arguments)));
                                        send.append(Component.text(" "));
                                    }

                                    String banInfo = Messages.bannedItemHover;
                                    ItemType type = ItemTypes.registry().value(ResourceKey.resolve(item.getItemId()));
                                    if ((context.hasPermission(Permissions.LIST_EXTRA)) && context.hasPermission(Permissions.EDIT_BANNED_ITEM)) {
                                        banInfo = banInfo + "\n" + Messages.bannedItemExtraInfo + "\n" + Messages.bannedItemEdit;
                                        send.append(Messages.parseMessage(Messages.bannedItem + banReason, arguments))
                                            .clickEvent(ClickEvent.runCommand("/restrict edit " + ItemTypes.registry().valueKey(type).asString()))
                                            .hoverEvent(HoverEvent.showText(Messages.parseMessage(banInfo, arguments)));
                                    } else if (context.hasPermission(Permissions.LIST_EXTRA)) {
                                        banInfo = banInfo + "\n" + Messages.bannedItemExtraInfo;
                                        send.append(Messages.parseMessage(Messages.bannedItem + banReason, arguments))
                                            .hoverEvent(HoverEvent.showText(Messages.parseMessage(banInfo, arguments)));
                                    } else if (context.hasPermission(Permissions.EDIT_BANNED_ITEM)) {
                                        banInfo = banInfo + "\n" + Messages.bannedItemEdit;
                                        send.append(Messages.parseMessage(Messages.bannedItem + banReason, arguments))
                                            .clickEvent(ClickEvent.runCommand("/restrict edit " + ItemTypes.registry().valueKey(type).asString()))
                                            .hoverEvent(HoverEvent.showText(Messages.parseMessage(banInfo, arguments)));
                                    } else {
                                        send.append(Messages.parseMessage(Messages.bannedItem + banReason, arguments))
                                            .hoverEvent(HoverEvent.showText(Messages.parseMessage(banInfo, arguments)));
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

                                    TextComponent.Builder send = Component.text();

                                    String banReason = "";
                                    if (!mod.getBanreason().isEmpty()) {
                                        banReason = Messages.bannedItemReason;
                                    }

                                    if (context.hasPermission(Permissions.LIST_HIDDEN_EDIT)) {
                                        send.append(Messages.parseMessage(Messages.bannedListHide, arguments))
                                            .hoverEvent(HoverEvent.showText(Messages.parseMessage(Messages.bannedListHideHover, arguments)));
                                        send.append(Component.text(" "));
                                    }

                                    String banInfo = Messages.bannedItemHover;
                                    if ((context.hasPermission(Permissions.LIST_EXTRA) && context.hasPermission(Permissions.EDIT_BANNED_ITEM))) {
                                        banInfo = banInfo + "\n" + Messages.bannedItemExtraInfo + "\n" + Messages.bannedItemEdit;
                                        send.append(Messages.parseMessage(Messages.bannedMod + banReason, arguments))
                                            .clickEvent(ClickEvent.runCommand("/restrict edit " + mod.getMod()))
                                            .hoverEvent(HoverEvent.showText(Messages.parseMessage(banInfo, arguments)));
                                    } else if (context.hasPermission(Permissions.LIST_EXTRA)) {
                                        banInfo = banInfo + "\n" + Messages.bannedItemExtraInfo;
                                        send.append(Messages.parseMessage(Messages.bannedMod + banReason, arguments))
                                            .hoverEvent(HoverEvent.showText(Messages.parseMessage(banInfo, arguments)));
                                    } else if (context.hasPermission(Permissions.EDIT_BANNED_ITEM)) {
                                        banInfo = banInfo + "\n" + Messages.bannedItemEdit;
                                        send.append(Messages.parseMessage(Messages.bannedMod + banReason, arguments))
                                            .clickEvent(ClickEvent.runCommand("/restrict edit " + mod.getMod()))
                                            .hoverEvent(HoverEvent.showText(Messages.parseMessage(banInfo, arguments)));
                                    } else {
                                        send.append(Messages.parseMessage(Messages.bannedMod + banReason, arguments))
                                            .hoverEvent(HoverEvent.showText(Messages.parseMessage(banInfo, arguments)));
                                    }
                                    contents.add(send.build());
                                }

                                for (ItemData item : items) {
                                    System.out.println(item.getItemId());
                                    if (item.getHidden()) {
                                        continue;
                                    }
                                    HashMap<String, String> arguments = new HashMap<>();
                                    arguments.put("banreason", item.getBanReason());
                                    arguments.put("itemid", item.getItemId());
                                    arguments.put("usebanned", item.getUsageBanned().toString());
                                    arguments.put("breakbanned", item.getBreakingBanned().toString());
                                    arguments.put("placebanned", item.getPlacingBanned().toString());
                                    arguments.put("ownbanned", item.getOwnershipBanned().toString());
                                    arguments.put("craftbanned", item.getCraftBanned().toString());
                                    arguments.put("worldbanned", item.getWorldBanned().toString());
                                    arguments.put("itemname", item.getItemId());
                                    arguments.put("hidden", item.getHidden() ? "show" : "hide");

                                    TextComponent.Builder send = Component.text();

                                    String banReason = "";
                                    if (!item.getBanReason().isEmpty()) {
                                        banReason = Messages.bannedItemReason;
                                    }

                                    if (context.hasPermission(Permissions.LIST_HIDDEN_EDIT)) {
                                        send.append(Messages.parseMessage(Messages.bannedListHidden, arguments))
                                            .hoverEvent(HoverEvent.showText(Messages.parseMessage(Messages.bannedListHideHover, arguments)));
                                        send.append(Component.text(" "));
                                    }

                                    String banInfo = Messages.bannedItemHover;
                                    ItemType type = ItemTypes.registry().value(ResourceKey.resolve(item.getItemId()));
                                    if ((context.hasPermission(Permissions.LIST_EXTRA)) && context.hasPermission(Permissions.EDIT_BANNED_ITEM)) {
                                        banInfo = banInfo + "\n" + Messages.bannedItemExtraInfo + "\n" + Messages.bannedItemEdit;
                                        send.append(Messages.parseMessage(Messages.bannedItem + banReason, arguments))
                                            .clickEvent(ClickEvent.runCommand("/restrict edit " + ItemTypes.registry().valueKey(type).asString()))
                                            .hoverEvent(HoverEvent.showText(Messages.parseMessage(banInfo, arguments)));
                                    } else if (context.hasPermission(Permissions.LIST_EXTRA)) {
                                        banInfo = banInfo + "\n" + Messages.bannedItemExtraInfo;
                                        send.append(Messages.parseMessage(Messages.bannedItem + banReason, arguments))
                                            .hoverEvent(HoverEvent.showText(Messages.parseMessage(banInfo, arguments)));
                                    } else if (context.hasPermission(Permissions.EDIT_BANNED_ITEM)) {
                                        banInfo = banInfo + "\n" + Messages.bannedItemEdit;
                                        send.append(Messages.parseMessage(Messages.bannedItem + banReason, arguments))
                                            .clickEvent(ClickEvent.runCommand("/restrict edit " + ItemTypes.registry().valueKey(type).asString()))
                                            .hoverEvent(HoverEvent.showText(Messages.parseMessage(banInfo, arguments)));
                                    } else {
                                        System.out.println("adding message");
                                        send.append(Messages.parseMessage(Messages.bannedItem + banReason, arguments))
                                            .hoverEvent(HoverEvent.showText(Messages.parseMessage(banInfo, arguments)));
                                    }
                                    contents.add(send.build());
                                }
                            }

                            Component title = LegacyComponentSerializer.legacyAmpersand().deserialize(Messages.bannedListTitle);
                            if (contents.isEmpty()) {
                                if (hidden.isPresent()) {
                                    contents.add(LegacyComponentSerializer.legacyAmpersand().deserialize(Messages.bannedItemNonHidden));
                                    title = LegacyComponentSerializer.legacySection().deserialize(Messages.bannedListHiddenTitle);
                                } else {
                                    contents.add(LegacyComponentSerializer.legacyAmpersand().deserialize(Messages.bannedItemNonSet));
                                }
                            }

                            paginationService.builder()
                                .title(title)
                                .contents(contents)
                                .padding(LegacyComponentSerializer.legacyAmpersand().deserialize(Messages.bannedListPadding))
                                .sendTo(context.cause().audience());
                            return CommandResult.success();
                        })
                        .build(),
                    "list"
                )
                .build(),
            "restrict"
        );
    }

    private Parameter.Value<String> generateParameter(String key) {
        return Parameter.string().key(key).build();
    }

    private Parameter.Value<String> generateOptionalParameter(String key) {
        return Parameter.string().key(key).optional().build();
    }

    private void loadCommands() {

/*

        // /restrict search
        CommandSpec itemSearch = CommandSpec.builder()
            .description(Text.of("Search for an item from the banned items list within the world"))
            .executor(new Search(this))
            .arguments(GenericArguments.catalogedElement(Text.of("ItemID"), ItemType.class))
            .permission(Permissions.SEARCH_BANNED_ITEM)
            .build();

        // /restrict whatsthis | /whatsthis
        CommandSpec whatsThis = CommandSpec.builder()
            .description(Text.of("Get the id of an item in hand"))
            .executor(new Whatsthis(this))
            .permission(Permissions.WHATS_THIS)
            .build();

        // /restrict checkchunks
        CommandSpec checkChunks = CommandSpec.builder()
            .description(Text.of("Search loaded chunks for banned blocks"))
            .executor(new CheckChunks(this))
            .permission(Permissions.CHECK_CHUNKS)
            .build();

        // /restrict sendtochest
        CommandSpec sendToChest = CommandSpec.builder()
            .description(Text.of("Search loaded chunks for a block and put it in a chest"))
            .executor(new Sendtochest(this))
            .arguments(GenericArguments.catalogedElement(Text.of("ItemID"), ItemType.class))
            .permission(Permissions.SEND_TO_CHEST)
            .build();

        // /restrict
        CommandSpec restrict = CommandSpec.builder()
            .description(Text.of("Base restrict command"))
            .executor(new Help(this))
            .child(modAdd, "mod")
            .child(itemAddHand, "add")
            .child(itemRemove, "remove")
            .child(itemEdit, "edit")
            .child(bannedList, "list")
            .child(itemSearch, "search")
            .child(whatsThis, "whatsthis")
            .child(checkChunks, "checkchunks")
            .child(sendToChest, "sendtochest")
            .build();

        cmdManager.register(this, bannedList, "banneditems");
        cmdManager.register(this, whatsThis, "whatsthis");
        cmdManager.register(this, restrict, "restrict");
        
 */
    }

    public HoconConfigurationLoader getItemDataLoader() {
        return HoconConfigurationLoader.builder()
            .path(this.ConfigDir.resolve("Banneditems.conf"))
            .defaultOptions(opts -> opts.serializers(build ->
                build.register(ItemData.class, new ItemDataSerializer())
                    .register(ModData.class, new ModData.ModDataSerializer())))
            .build();
    }

    private void loadData() throws IOException {
        HoconConfigurationLoader loader = getItemDataLoader();
        ConfigurationNode rootNode = loader.load();

        List<ItemData> itemList = rootNode.node("Items").getList(TypeToken.get(ItemData.class));
        this.items = new HashMap<>();
        for (ItemData item : itemList) {
            ItemType type = ItemTypes.registry().value(ResourceKey.resolve(item.getItemId()));
            ResourceKey itemId = ItemTypes.registry().valueKey(type);
            this.items.put(itemId.asString(), item);
        }
        List<ModData> modList = rootNode.node("Mods").getList(TypeToken.get(ModData.class));
        this.modslist = new HashMap<>();
        if (modList != null && modList.isEmpty()) {
            return;
        }
        for (ModData mod : modList) {
            this.modslist.put(mod.getMod(), mod);
        }
    }

    public void saveData() throws IOException {
        HoconConfigurationLoader loader = getItemDataLoader();
        ConfigurationNode rootNode = loader.load();
        rootNode.node("Items").set(ItemDataSerializer.token, new ArrayList<>(this.items.values()));
        rootNode.node("Mods").set(ModData.ModDataSerializer.token, new ArrayList<>(this.modslist.values()));
        loader.save(rootNode);
    }

    public void checkChestItem(String itemID, String itemName, String playerName) {
        boolean itemExist = false;
        final List<ItemData> items = new ArrayList<>(getItemData());
        Optional<ResourceKey> type = ItemTypes.registry().findValueKey(ItemTypes.registry().value(ResourceKey.resolve(itemID)));
        if (!type.isPresent()) {
            return;
        }
        for (ItemData item : items) {
            ResourceKey itemId = ItemTypes.registry().valueKey(ItemTypes.registry().value(ResourceKey.resolve(item.getItemId())));
            if (itemId.asString().equals(itemID)) {
                itemExist = true;
            }
        }
        if (!itemExist) {
            addItem(new ItemData(
                Config.defaultHidden,
                type.get().namespace() + ":" + type.get().value(),
                itemName,
                Config.defaultReason,
                Config.defaultUsage,
                Config.defaultBreaking,
                Config.defaultPlacing,
                Config.defaultOwnership,
                Config.defaultDrop,
                Config.defaultCraft,
                Config.defaultWorld
            ));
            logToFile("ban-list", playerName + " added " + itemName + " to the ban list");
        }
    }

    public void checkLoadedChunks() {
        Collection<ServerWorld> loadedWorlds = Sponge.server().worldManager().worlds();
        final List<ItemData> items = new ArrayList<ItemData>(getItemData());
        Sponge.game().asyncScheduler().executor(plugin).execute(() -> loadedWorlds.forEach(world -> {
            Iterable<WorldChunk> loadedChunks = world.loadedChunks();
            loadedChunks.forEach(chunk -> {
                Vector3i min = chunk.min();
                Vector3i max = chunk.max();
                for (int x = min.x(); x <= max.x(); x++) {
                    for (int y = min.y(); y <= max.y(); y++) {
                        for (int z = min.z(); z <= max.z(); z++) {
                            BlockState block = chunk.block(x, y, z);
                            Location blockLoc = chunk.world().location(x, y, z);
                            for (ItemData item : items) {
                                ItemType type = ItemTypes.registry().value(ResourceKey.resolve(item.getItemId()));
                                ResourceKey itemId = Sponge.game().registry(RegistryTypes.ITEM_TYPE).valueKey(type);
                                ResourceKey blockId = Sponge.game().registry(RegistryTypes.BLOCK_TYPE).valueKey(block.type());
                                if (itemId.asString().equals(blockId.asString()) && item.getWorldBanned()) {
                                    int finalX = x;
                                    int finalY = y;
                                    int finalZ = z;
                                    Sponge.asyncScheduler().executor(plugin).execute(() -> {
                                        blockLoc.setBlock(BlockTypes.AIR.get().defaultState());
                                        logToFile("action-log", "Removed banned block:" + itemId + " at x:" + finalX + " y:" + finalY + " z:" + finalZ);
                                    });
                                }
                            }
                        }
                    }
                }
            });
        }));
    }

    public Collection<ItemData> getItemData() {
        return Collections.unmodifiableCollection(this.items.values());
    }

    public ItemData addItem(ItemData item) {
        return this.items.put(item.getItemId(), item);
    }

    public ItemData removeItem(String item) {
        return this.items.remove(item);
    }

    public Collection<ModData> getModData() {
        return Collections.unmodifiableCollection(this.modslist.values());
    }

    public ModData addMod(ModData mod) {
        return this.modslist.put(mod.getMod(), mod);
    }

    public ModData removeMod(String mod) {
        return this.modslist.remove(mod);
    }

    public boolean checkPerm(ServerPlayer cause, String banType, String ID) {
        if (cause.hasPermission(Permissions.ITEM_BYPASS + ".all." + ID.replace(":", "."))) {
            return false;
        } else {
            return !cause.hasPermission(Permissions.ITEM_BYPASS + "." + banType + "." + ID.replace(":", "."));
        }
    }

    public void logToFile(String filename, String message) {
        if (Config.logToFile) {
            try {
                if (!Files.exists(ConfigDir.resolve("logs"))) {
                    Files.createDirectory(ConfigDir.resolve("logs"));
                }

                Path saveTo = ConfigDir.resolve("logs/" + filename + ".txt");

                if (!Files.exists(saveTo)) {
                    Files.createFile(saveTo);
                }

                FileWriter fw = new FileWriter(saveTo.toFile(), true);
                PrintWriter pw = new PrintWriter(fw);

                pw.println(message);
                pw.flush();
                pw.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void notifyOnlineStaff(Component message) {
        if (Config.notifyStaff) {
            for (ServerPlayer player : Sponge.server().onlinePlayers()) {
                if (player.hasPermission(Permissions.NOTIFY)) {
                    player.sendMessage(message);
                }
            }
        }
    }

}
