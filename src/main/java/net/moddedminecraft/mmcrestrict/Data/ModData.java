package net.moddedminecraft.mmcrestrict.Data;


import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.List;

public class ModData extends ModDataUtil {

    public ModData(Boolean hidden, String mod, String modname, String banreason, Boolean usagebanned, Boolean breakingbanned, Boolean placingbanned, Boolean ownershipbanned, Boolean dropbanned, Boolean craftbanned) {
        super(hidden, mod, modname, banreason, usagebanned, breakingbanned, placingbanned, ownershipbanned, dropbanned, craftbanned);
    }

    public static class ModDataSerializer implements TypeSerializer<ModData> {
        @SuppressWarnings("serial")
        final public static TypeToken<List<ModData>> token = new TypeToken<List<ModData>>() {
        };

        @Override
        public ModData deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return new ModData(
                    node.node("hidden").getBoolean(),
                    node.node("mod").getString(),
                    node.node("modname").getString(),
                    node.node("banreason").getString(),
                    node.node("usagebanned").getBoolean(),
                    node.node("breakingbanned").getBoolean(),
                    node.node("placingbanned").getBoolean(),
                    node.node("ownershipbanned").getBoolean(),
                    node.node("dropbanned").getBoolean(),
                    node.node("craftbanned").getBoolean());
        }

        @Override
        public void serialize(Type type, @Nullable ModData itemData, ConfigurationNode node) throws SerializationException {
            if (itemData == null) {
                throw new SerializationException("ModData may not be null!");
            }
            node.node("hidden").set(itemData.hidden);
            node.node("mod").set(itemData.mod);
            node.node("modname").set(itemData.modname);
            node.node("banreason").set(itemData.banreason);
            node.node("usagebanned").set(itemData.usagebanned);
            node.node("breakingbanned").set(itemData.breakingbanned);
            node.node("placingbanned").set(itemData.placingbanned);
            node.node("ownershipbanned").set(itemData.ownershipbanned);
            node.node("dropbanned").set(itemData.dropbanned);
            node.node("craftbanned").set(itemData.craftbanned);
        }
    }


}
