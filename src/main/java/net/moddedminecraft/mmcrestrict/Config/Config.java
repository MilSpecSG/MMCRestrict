package net.moddedminecraft.mmcrestrict.Config;

import io.leangen.geantyref.TypeToken;
import net.moddedminecraft.mmcrestrict.MMCRestrict;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

public class Config {

    private final MMCRestrict plugin;

    private static ConfigurationLoader<CommentedConfigurationNode> loader;
    public static CommentedConfigurationNode config;

    public Config(MMCRestrict main) throws IOException {
        plugin = main;
        loader = HoconConfigurationLoader.builder().path(plugin.defaultConf).build();
        config = loader.load();
        configCheck();
    }

    public static boolean logToFile;

    public static List<String> sendToChestWhitelist;

    public static String defaultReason = "";
    public static Boolean defaultHidden = false;
    public static Boolean defaultUsage = true;
    public static Boolean defaultBreaking = true;
    public static Boolean defaultPlacing = true;
    public static Boolean defaultOwnership = true;
    public static Boolean defaultDrop = false;
    public static Boolean defaultCraft = true;
    public static Boolean defaultWorld = false;

    //Auto Purge
    public static Boolean defaultAutoPurge = false;
    public static Integer defaultAutoPurgeInterval = 20;


    public static Boolean notifyStaff = true;

    public void configCheck() throws IOException {

        if (!Files.exists(plugin.defaultConf)) {
            Files.createFile(plugin.defaultConf);
        }

        if (!config.node("send-to-chest", "whitelist").childrenList().isEmpty()) {
            sendToChestWhitelist = check(config.node("send-to-chest", "whitelist"), Collections.emptyList()).getList(TypeToken.get(String.class));
        } else {
            sendToChestWhitelist = config.node("send-to-chest", "whitelist").set(Collections.emptyList()).getList(TypeToken.get(String.class));
        }
        config.node("defaults").comment("Default values for any newly added banned item.");
        defaultReason = check(config.node("defaults", "reason"), defaultReason).getString();
        defaultHidden = check(config.node("defaults", "hidden"), defaultHidden).getBoolean();
        defaultUsage = check(config.node("defaults", "usage"), defaultUsage).getBoolean();
        defaultBreaking = check(config.node("defaults", "breaking"), defaultBreaking).getBoolean();
        defaultPlacing = check(config.node("defaults", "placing"), defaultPlacing).getBoolean();
        defaultOwnership = check(config.node("defaults", "ownership"), defaultOwnership).getBoolean();
        defaultDrop = check(config.node("defaults", "drop"), defaultDrop).getBoolean();
        defaultCraft = check(config.node("defaults", "craft"), defaultCraft).getBoolean();
        defaultWorld = check(config.node("defaults", "world"), defaultWorld).getBoolean();

        config.node("world-auto-purge").comment("Check all Loaded Chunks for banned items and remove them. If you experience lag, disable this and just use /restrict checkchunks manually");
        defaultAutoPurge = check(config.node("world-auto-purge", "is-enabled"), defaultAutoPurge).getBoolean();

        defaultAutoPurgeInterval = check(config.node("world-auto-purge", "interval-in-minutes"), defaultAutoPurgeInterval, "Check for banned items around the World... interval should be more than 5").getInt();
        if (defaultAutoPurgeInterval < 5){
            defaultAutoPurgeInterval = 5;
        }

        notifyStaff = check(config.node("notify-staff"), notifyStaff, "If enabled, will notify staff if someone attempts to use a banned item.").getBoolean();

        logToFile = check(config.node("log-to-file"), true, "Log any banned action or banned item change to a file.").getBoolean();
        loader.save(config);
    }

    private CommentedConfigurationNode check(CommentedConfigurationNode node, Object defaultValue, String comment) throws SerializationException {
        if (node.virtual()) {
            node.set(defaultValue).comment(comment);
        }
        return node;
    }

    private CommentedConfigurationNode check(CommentedConfigurationNode node, Object defaultValue) throws SerializationException {
        if (node.virtual()) {
            node.set(defaultValue);
        }
        return node;
    }
}
