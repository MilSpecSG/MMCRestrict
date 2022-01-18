package net.moddedminecraft.mmcrestrict.Config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.moddedminecraft.mmcrestrict.MMCRestrict;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Messages {

    private static MMCRestrict plugin;
    public Path defaultMessage;

    private static final Pattern URL_PATTERN = Pattern.compile("((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)", Pattern.CASE_INSENSITIVE);


    public static ConfigurationLoader<CommentedConfigurationNode> messageLoader;
    public static CommentedConfigurationNode messages;

    public Messages(MMCRestrict main) throws IOException {
        plugin = main;
        defaultMessage = plugin.ConfigDir.resolve("messages.conf");
        messageLoader = HoconConfigurationLoader.builder().path(defaultMessage).build();
        messages = messageLoader.load();
        messageCheck();
    }

    public static boolean logToFile;

    public static List<String> sendToChestWhitelist;

    //banlist
    public static String bannedItemHover = "&cBanned methods &7- &6Use&7: {usebanned} &6Break&7: {breakbanned} &6Place&7: {placebanned} &6Own&7: {ownbanned} &6Craft&7: {craftbanned} &6World&7: {worldbanned}";
    public static String bannedItemExtraInfo = "&7Id: {itemid}";
    public static String bannedItemEdit = "&3Click to edit this item";
    public static String bannedItemReason = " &3- &7{banreason}";
    public static String bannedMod = "&3- [Mod] &6{modname}";
    public static String bannedItem = "&3- &6{itemname}";
    public static String bannedItemNonSet = "&eNo banned items have been set";
    public static String bannedItemNonHidden = "&eNo banned items have been hidden";
    public static String bannedListTitle = "&6Banned List";
    public static String bannedListHiddenTitle = "&6Hidden Banned List";
    public static String bannedListPadding = "-";

    public static String bannedListHideHover = "&3Click to {hidden} this item";
    public static String bannedListHidden = "&8[&7H&8]";
    public static String bannedListHide = "&8[&6H&8]";

    //checkchunks
    public static String checkStarted = "Chunk searching has been initiated. All world banned items will be removed if found.";

    public void messageCheck() throws IOException {

        if (!Files.exists(defaultMessage)) {
            Files.createFile(defaultMessage);
        }


        //banlist
        bannedListTitle = check(messages.node("list", "title"), bannedListTitle).getString();
        bannedListPadding = check(messages.node("list", "padding"), bannedListPadding).getString();
        bannedItemNonHidden = check(messages.node("list", "error", "non-hidden"), bannedItemNonHidden).getString();
        bannedItemNonSet = check(messages.node("list", "error", "non-set"), bannedItemNonSet).getString();
        bannedMod = check(messages.node("list", "formatting", "mod-name"), bannedMod).getString();
        bannedItem = check(messages.node("list", "formatting", "name"), bannedItem).getString();
        bannedItemReason = check(messages.node("list", "formatting", "reason"), bannedItemReason).getString();
        bannedItemHover = check(messages.node("list", "hover", "info"), bannedItemHover).getString();
        bannedItemEdit = check(messages.node("list", "hover", "edit"), bannedItemEdit).getString();
        bannedItemExtraInfo = check(messages.node("list", "hover", "extra"), bannedItemExtraInfo).getString();

        //checkchunks
        checkStarted = check(messages.node("commands", "checkchunks", "check-started"), checkStarted).getString();

        //hidden banlist
        bannedListHiddenTitle = check(messages.node("list", "hidden", "title"), bannedListHiddenTitle).getString();
        bannedListHideHover = check(messages.node("list", "hidden", "hover"), bannedListHideHover).getString();
        bannedListHidden = check(messages.node("list", "hidden", "hidden-prefix"), bannedListHidden).getString();
        bannedListHide = check(messages.node("list", "hidden", "hide-prefix"), bannedListHide).getString();

        messageLoader.save(messages);
    }

    private CommentedConfigurationNode check(CommentedConfigurationNode node, Object defaultValue) throws SerializationException {
        if (node.virtual()) {
            node.set(defaultValue);
        }
        return node;
    }

    public static Component parseMessage(String message, HashMap<String, String> args) {
        for (Map.Entry<String, String> arg : args.entrySet()) {
            message = message.replace("{" + arg.getKey() + "}", arg.getValue());
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

}
