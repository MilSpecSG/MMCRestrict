package net.moddedminecraft.mmcrestrict;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.moddedminecraft.mmcrestrict.Data.ItemData;
import net.moddedminecraft.mmcrestrict.Data.ModData;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.entity.carrier.chest.Chest;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.CraftItemEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.scheduler.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class EventListener {

    private MMCRestrict plugin;

    public EventListener(MMCRestrict instance) {
        plugin = instance;
    }

    /*@Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event, @Root Player player) {
        checkInventory(player);
    }*/

    @Listener
    public void onItemPickup(ChangeInventoryEvent.Pickup event, @Root ServerPlayer player) {
        if (player.hasPermission(Permissions.ITEM_BYPASS)) {
            return;
        }

        for (SlotTransaction transaction : event.transactions()) {
            ItemStack itemStack = transaction.finalReplacement().createStack();

            if (checkBanned(itemStack, "own", player)) {
                event.setCancelled(true);
            }
        }

    }

    @Listener
    public void onItemDrop(DropItemEvent.Pre event, @Root ServerPlayer player) {
        if (player.hasPermission(Permissions.ITEM_BYPASS)) {
            return;
        }
        List<ItemStackSnapshot> itemIDs = event.droppedItems();

        for (ItemStackSnapshot itemSnapshot : itemIDs) {
            ItemStack itemStack = itemSnapshot.createStack();
            if (checkBanned(itemStack, "drop", player)) {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void InteractItemEvent(InteractItemEvent.Secondary event, @Root ServerPlayer player) {
        if (player.hasPermission(Permissions.ITEM_BYPASS)) {
            return;
        }

        ItemStack itemStack = event.itemStack().createStack();

        if (checkBanned(itemStack, "use", player)) {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onBlockPlace(ChangeBlockEvent.All event, @Root ServerPlayer player) {
        if (event.transactions().get(0).finalReplacement().state().type().equals(BlockTypes.AIR)) {
            return;
        }
        if (player.hasPermission(Permissions.ITEM_BYPASS)) {
            return;
        }
        BlockSnapshot targetBlock = event.transactions().get(0).finalReplacement();

        if (checkBanned(targetBlock, "place", player)) {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onBlockBreak(InteractBlockEvent.Primary.Start event, @Root ServerPlayer player) {
        if (event.block().state().type().equals(BlockTypes.AIR)) {
            return;
        }
        if (player.hasPermission(Permissions.ITEM_BYPASS)) {
            return;
        }
        if (checkBanned(event.block(), "break", player)) {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onBlockModify(InteractBlockEvent.Secondary event, @Root ServerPlayer player) {
        if (event.block().state().type().equals(BlockTypes.AIR)) {
            return;
        }
        if (player.hasPermission(Permissions.ITEM_BYPASS)) {
            return;
        }
        if (checkBanned(event.block(), "use", player)) {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onEntityInteract(InteractEntityEvent.Secondary event, @Root ServerPlayer player) {
        if (player.hasPermission(Permissions.ITEM_BYPASS)) {
            return;
        }

        ItemStack mainHand = player.itemInHand(HandTypes.MAIN_HAND).copy();
        ItemStack offHand = player.itemInHand(HandTypes.OFF_HAND).copy();

        if (checkBanned(mainHand, "use", player) || checkBanned(offHand, "use", player)) {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onEntityInteract(InteractEntityEvent.Primary event, @Root ServerPlayer player) {
        if (player.hasPermission(Permissions.ITEM_BYPASS)) {
            return;
        }

        ItemStack mainHand = player.itemInHand(HandTypes.MAIN_HAND).copy();
        ItemStack offHand = player.itemInHand(HandTypes.OFF_HAND).copy();

        if (checkBanned(mainHand, "use", player) || checkBanned(offHand, "use", player)) {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onRightClick(InteractBlockEvent.Secondary event, @Root ServerPlayer player) {
        boolean sneaking = player.get(Keys.IS_SNEAKING).orElse(false);
        if (player.hasPermission(Permissions.ADD_BANNED_ITEM) && event.block().state().type().equals(BlockTypes.CHEST) && sneaking) {
            if (!(player.itemInHand(HandTypes.MAIN_HAND).isEmpty()) && player.itemInHand(HandTypes.MAIN_HAND).type().equals(ItemTypes.FLINT)) {

                Chest chest = (Chest) event.block().location().get().blockEntity().get();
                for (Inventory slot : chest.inventory().slots()) {
                    if (!slot.peek().isEmpty()) {
                        DataContainer container = slot.peek().toContainer();
                        DataQuery query = DataQuery.of('/', "UnsafeDamage");

                        int unsafeDamage = 0;
                        if (container.get(query).isPresent()) {
                            unsafeDamage = Integer.parseInt(container.get(query).get().toString());
                        }
                        String itemId = ItemTypes.registry().valueKey(slot.peek().type()).asString();
                        if (unsafeDamage != 0) {
                            itemId = itemId + ":" + unsafeDamage;
                        }

                        String itemID = itemId;
                        String itemName = slot.peek().getValue(Keys.CUSTOM_NAME).toString();
                        plugin.checkChestItem(itemID, itemName, player.name());
                    }
                }
                try {
                    plugin.saveData();
                } catch (Exception e) {
                    player.sendMessage(Component.text("Data was not saved correctly."));
                    e.printStackTrace();
                }
                player.sendMessage(Component.text("Chest items were added to the list."));
                return;
            }
        }

        if (player.hasPermission(Permissions.ITEM_BYPASS)) {
            return;
        }

        BlockSnapshot targetBlock = event.block();

        if (checkBanned(targetBlock, "use", player)) {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onLeftClick(InteractBlockEvent.Primary.Start event, @Root ServerPlayer player) {
        if (player.hasPermission(Permissions.ITEM_BYPASS)) {
            return;
        }

        BlockSnapshot targetBlock = event.block();

        if (checkBanned(targetBlock, "use", player)) {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onCraftItemEvent(CraftItemEvent.Preview event, @Root ServerPlayer player) {
        if (player.hasPermission(Permissions.ITEM_BYPASS)) {
            return;
        }
        if (!event.preview().finalReplacement().isEmpty()) {
            ItemStack itemStack = event.preview().finalReplacement().createStack();
            if (checkBanned(itemStack, "craft", player)) {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onChangeEquipment(ChangeEntityEquipmentEvent event, @Root ServerPlayer player) {
        if (player.hasPermission(Permissions.ITEM_BYPASS)) {
            return;
        }
        ItemStack itemStack = event.transaction().finalReplacement().createStack();
        if (itemStack.type().equals(ItemTypes.AIR)) {
            return;
        }
        if (checkBanned(itemStack, "own", player)) {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onChangeHeld(ChangeInventoryEvent.Held event, @Root ServerPlayer player) {
        if (player.hasPermission(Permissions.ITEM_BYPASS)) {
            return;
        }
        for (SlotTransaction transaction : event.transactions()) {
            ItemStack itemStack = transaction.finalReplacement().createStack();
            if (itemStack.type().equals(ItemTypes.AIR)) {
                continue;
            }

            if (checkBanned(itemStack, "own", player)) {
                event.setCancelled(true);
            }
        }
    }

    /*@Listener
    public void onClickInventoryEvent(ClickInventoryEvent event, @Root Player player) {
        if (player.hasPermission(Permissions.ITEM_BYPASS)) {
            return;
        }
        if (event instanceof ClickInventoryEvent.Shift) {
            for (SlotTransaction transaction : event.getTransactions()) {
                ItemStack itemStack = transaction.getFinal().createStack();
                if (checkBanned(itemStack, "own", player)) {
                    event.setCancelled(true);
                    Sponge.getScheduler().createTaskBuilder().execute(new Runnable() {
                        public void run() {
                            checkInventory(player);
                        }
                    }).delay(1, TimeUnit.SECONDS).name("mmcrestrict-s-onclickinventoryevent").submit(this.plugin);
                }
            }
        } else {
            ItemStack itemStack = event.getCursorTransaction().getFinal().createStack();
            if (checkBanned(itemStack, "own", player)) {
                event.setCancelled(true);
                Sponge.getScheduler().createTaskBuilder().execute(new Runnable() {
                    public void run() {
                        checkInventory(player);
                    }
                }).delay(1, TimeUnit.SECONDS).name("mmcrestrict-s-onclickinventoryevent").submit(this.plugin);
            }
        }
    }*/

    private boolean checkBanned(ItemStack itemStack, String banType, ServerPlayer player) {
        final List<ItemData> items = new ArrayList<ItemData>(plugin.getItemData());
        final List<ModData> mods = new ArrayList<ModData>(plugin.getModData());
        DataContainer container = itemStack.toContainer();
        DataQuery query = DataQuery.of('/', "UnsafeDamage");
        String itemID = ItemTypes.registry().valueKey(itemStack.type()).asString();

        int unsafeDamage = 0;
        if (container.get(query).isPresent()) {
            unsafeDamage = Integer.parseInt(container.get(query).get().toString());
        }
        if (unsafeDamage != 0) {
            itemID = itemID + ":" + unsafeDamage;
        }

        for (ModData mod : mods) {
            if (itemID.startsWith(mod.getMod())
                && ((banType.equalsIgnoreCase("craft") && mod.getCraftbanned())
                || (banType.equalsIgnoreCase("break") && mod.getBreakingbanned())
                || (banType.equalsIgnoreCase("drop") && mod.getDropbanned())
                || (banType.equalsIgnoreCase("own") && mod.getOwnershipbanned())
                || (banType.equalsIgnoreCase("place") && mod.getPlacingbanned())
                || (banType.equalsIgnoreCase("use") && mod.getUsagebanned()))) {
                if (plugin.checkPerm(player, banType.toLowerCase(), mod.getMod())) {
                    String reason = "";
                    if (!mod.getBanreason().isEmpty()) {
                        reason = " &3- &7" + mod.getBanreason();
                    }
                    plugin.logToFile("action-log", player.name() + " tried to " + banType.toLowerCase() + " " + mod.getModname());
                    if (!banType.equalsIgnoreCase("own")) {
                        plugin.notifyOnlineStaff(
                            Component.text()
                                .append(Component.text("[").color(NamedTextColor.DARK_GRAY))
                                .append(Component.text("MMCRestrict").color(NamedTextColor.GOLD))
                                .append(Component.text("]").color(NamedTextColor.DARK_GRAY))
                                .append(Component.text(player.name() + " tried to " + banType.toLowerCase() + " " + itemID).color(NamedTextColor.RED))
                                .build()
                        );
                    }
                    player.sendMessage(
                        Component.text()
                            .append(Component.text(mod.getModname() + " mod is banned " + reason).color(NamedTextColor.RED))
                            .build()
                    );
                    checkInventory(player);
                    return true;
                }
            }
        }

        for (ItemData item : items) {
            if (item.getItemId().equalsIgnoreCase(itemID)
                && ((banType.equalsIgnoreCase("craft") && item.getCraftBanned())
                || (banType.equalsIgnoreCase("break") && item.getBreakingBanned())
                || (banType.equalsIgnoreCase("drop") && item.getDropBanned())
                || (banType.equalsIgnoreCase("own") && item.getOwnershipBanned())
                || (banType.equalsIgnoreCase("place") && item.getPlacingBanned())
                || (banType.equalsIgnoreCase("use") && item.getUsageBanned()))) {
                if (plugin.checkPerm(player, banType.toLowerCase(), itemID)) {
                    String reason = "";
                    if (!item.getBanReason().isEmpty()) {
                        reason = " &3- &7" + item.getBanReason();
                    }
                    plugin.logToFile("action-log", player.name() + " tried to " + banType.toLowerCase() + " " + item.getItemId());
                    if (!banType.equalsIgnoreCase("own")) {
                        plugin.notifyOnlineStaff(
                            Component.text()
                                .append(Component.text("[").color(NamedTextColor.GRAY))
                                .append(Component.text("MMCRestrict").color(NamedTextColor.GOLD))
                                .append(Component.text("]").color(NamedTextColor.GRAY))
                                .append(Component.text(player.name() + " tried to " + banType.toLowerCase() + " " + item.getItemId()))
                                .build()
                        );
                    }
                    player.sendMessage(Component.text(item.getItemId() + " is banned " + reason).color(NamedTextColor.RED));
                    checkInventory(player);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkBanned(BlockSnapshot blockSnapshot, String banType, ServerPlayer player) {
        final List<ItemData> items = new ArrayList<>(plugin.getItemData());
        final List<ModData> mods = new ArrayList<>(plugin.getModData());
        Optional<ItemType> itemType = blockSnapshot.state().type().item();
        String itemID = "";
        if (!itemType.isPresent()) {
            BlockType type = blockSnapshot.state().type();
            itemID = BlockTypes.registry().valueKey(type).value();
        } else {
            itemID = ItemTypes.registry().valueKey(itemType.get()).value();
        }
        if (!blockSnapshot.state().type().item().isPresent()) {
            return false;
        }

        for (ModData mod : mods) {
            if (itemID.startsWith(mod.getMod())
                && ((banType.equalsIgnoreCase("craft") && mod.getCraftbanned())
                || (banType.equalsIgnoreCase("break") && mod.getBreakingbanned())
                || (banType.equalsIgnoreCase("drop") && mod.getDropbanned())
                || (banType.equalsIgnoreCase("own") && mod.getOwnershipbanned())
                || (banType.equalsIgnoreCase("place") && mod.getPlacingbanned())
                || (banType.equalsIgnoreCase("use") && mod.getUsagebanned()))) {
                if (plugin.checkPerm(player, banType.toLowerCase(), mod.getMod())) {
                    String reason = "";
                    if (!mod.getBanreason().isEmpty()) {
                        reason = " &3- &7" + mod.getBanreason();
                    }
                    plugin.logToFile("action-log", player.name() + " tried to " + banType.toLowerCase() + " " + mod.getModname());
                    if (!banType.equalsIgnoreCase("own")) {
                        plugin.notifyOnlineStaff(
                            Component.text()
                                .append(Component.text("[").color(NamedTextColor.GRAY))
                                .append(Component.text("MMCRestrict").color(NamedTextColor.GOLD))
                                .append(Component.text("]").color(NamedTextColor.GRAY))
                                .append(Component.text(player.name() + " tried to " + banType.toLowerCase() + " " + itemID).color(NamedTextColor.RED))
                                .build()
                        );
                    }
                    player.sendMessage(Component.text(mod.getModname() + " mod is banned " + reason).color(NamedTextColor.RED));
                    checkInventory(player);
                    return true;
                }
            }
        }

        ItemStack itemStack;
        try {
            itemStack = ItemStack.builder().fromBlockState(blockSnapshot.state()).build();
        } catch (Exception e) {
            return false;
        }

        DataContainer container = itemStack.toContainer();
        DataQuery query = DataQuery.of('/', "UnsafeDamage");

        int unsafeDamage = 0;
        if (container.get(query).isPresent()) {
            unsafeDamage = Integer.parseInt(container.get(query).get().toString());
        }
        if (unsafeDamage != 0) {
            itemID = itemID + ":" + unsafeDamage;
        }

        for (ItemData item : items) {
            if (item.getItemId().equalsIgnoreCase(itemID)
                && ((banType.equalsIgnoreCase("craft") && item.getCraftBanned())
                || (banType.equalsIgnoreCase("break") && item.getBreakingBanned())
                || (banType.equalsIgnoreCase("drop") && item.getDropBanned())
                || (banType.equalsIgnoreCase("own") && item.getOwnershipBanned())
                || (banType.equalsIgnoreCase("place") && item.getPlacingBanned())
                || (banType.equalsIgnoreCase("use") && item.getUsageBanned()))) {
                if (plugin.checkPerm(player, banType.toLowerCase(), itemID)) {
                    String reason = "";
                    if (!item.getBanReason().isEmpty()) {
                        reason = " &3- &7" + item.getBanReason();
                    }
                    plugin.logToFile("action-log", player.name() + " tried to " + banType.toLowerCase() + " " + item.getItemId());
                    if (!banType.equalsIgnoreCase("own")) {
                        plugin.notifyOnlineStaff(
                            Component.text()
                                .append(Component.text("[").color(NamedTextColor.GRAY))
                                .append(Component.text("MMCRestrict").color(NamedTextColor.GOLD))
                                .append(Component.text("]").color(NamedTextColor.GRAY))
                                .append(Component.text(player.name() + " tried to " + banType.toLowerCase() + " " + itemID))
                                .build()
                        );
                    }
                    player.sendMessage(
                        Component.text(item.getItemId() + " is banned " + reason).color(NamedTextColor.RED)
                    );
                    Sponge.server().scheduler().submit(
                        Task.builder().execute(runnable -> checkInventory(player)).delay(250, TimeUnit.MILLISECONDS).build(),
                        "mmcrestrict-s-checkInventory-isBanned"
                    );
                    return true;
                }
            }
        }
        return false;
    }

    private void checkInventory(ServerPlayer player) {
        if (player.hasPermission(Permissions.ITEM_BYPASS)) {
            return;
        }
        final List<ItemData> items = new ArrayList<ItemData>(plugin.getItemData());
        for (Inventory s : player.inventory().slots()) {
            if (!s.peek().isEmpty()) {
                ItemStack itemStack = s.peek().copy();
                String itemID = ItemTypes.registry().valueKey(itemStack.type()).asString();

                DataContainer container = s.peek().toContainer();
                DataQuery query = DataQuery.of('/', "UnsafeDamage");

                int unsafeDamage = 0;
                if (container.get(query).isPresent()) {
                    unsafeDamage = Integer.parseInt(container.get(query).get().toString());
                }
                if (unsafeDamage != 0) {
                    itemID = itemID + ":" + unsafeDamage;
                }
                for (ItemData item : items) {
                    if (item.getItemId().equalsIgnoreCase(itemID) && item.getOwnershipBanned()) {
                        if (plugin.checkPerm(player, "own", itemID)) {
                            s.clear();
                            String reason = "";
                            if (!item.getBanReason().isEmpty()) {
                                reason = " &3- &7" + item.getBanReason();
                            }
                            plugin.logToFile("action-log", item.getItemId() + " was removed from " + player.name() + "'s inventory");
                            player.sendMessage(Component.text(item.getItemId() + " is banned and has been removed from your inventory" + reason).color(NamedTextColor.RED));
                        }
                    }
                }
            }
        }
    }
}
