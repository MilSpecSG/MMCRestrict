package net.moddedminecraft.mmcrestrict.Data;

import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.List;

public class ItemData extends ItemDataUtil {

    public ItemData(Boolean hidden, String itemId, String itemname, String banreason, Boolean usagebanned, Boolean breakingbanned, Boolean placingbanned, Boolean ownershipbanned, Boolean dropbanned, Boolean craftbanned, Boolean worldbanned) {
        super(hidden, itemId, itemname, banreason, usagebanned, breakingbanned, placingbanned, ownershipbanned, dropbanned, craftbanned, worldbanned);
    }

    public static class ItemDataSerializer implements TypeSerializer<ItemData> {
        final public static TypeToken<List<ItemData>> token = new TypeToken<List<ItemData>>() {};

        @Override
        public ItemData deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return new ItemData(
                    node.node("hidden").getBoolean(),
                    node.node("itemId").getString(),
                    node.node("itemName").getString(),
                    node.node("banReason").getString(),
                    node.node("usageBanned").getBoolean(),
                    node.node("breakingBanned").getBoolean(),
                    node.node("placingBanned").getBoolean(),
                    node.node("ownershipBanned").getBoolean(),
                    node.node("dropBanned").getBoolean(),
                    node.node("craftBanned").getBoolean(),
                    node.node("worldBanned").getBoolean());
        }

        @Override
        public void serialize(Type type, @Nullable ItemData itemData, ConfigurationNode node) throws SerializationException {
            if (itemData == null) {
                throw new SerializationException("ItemData may not be null!");
            }
            node.node("hidden").set(itemData.hidden);
            node.node("itemId").set(itemData.itemId);
            node.node("itemName").set(itemData.itemName);
            node.node("banReason").set(itemData.banReason);
            node.node("usageBanned").set(itemData.usageBanned);
            node.node("breakingBanned").set(itemData.breakingBanned);
            node.node("placingBanned").set(itemData.placingBanned);
            node.node("ownershipBanned").set(itemData.ownershipBanned);
            node.node("dropBanned").set(itemData.dropBanned);
            node.node("craftBanned").set(itemData.craftBanned);
            node.node("worldBanned").set(itemData.worldBanned);
        }
    }


}
