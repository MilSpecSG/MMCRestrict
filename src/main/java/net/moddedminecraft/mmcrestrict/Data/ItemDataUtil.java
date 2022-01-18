package net.moddedminecraft.mmcrestrict.Data;

public class ItemDataUtil {

    protected String itemName, itemId, banReason;
    protected Boolean hidden, ownershipBanned, usageBanned, breakingBanned, placingBanned, dropBanned, craftBanned, worldBanned;

    public ItemDataUtil(
        Boolean hidden,
        String itemId,
        String itemName,
        String banReason,
        Boolean usageBanned,
        Boolean breakingBanned,
        Boolean placingBanned,
        Boolean ownershipBanned,
        Boolean dropBanned,
        Boolean craftBanned,
        Boolean worldBanned
    ) {
        this.hidden = hidden;
        this.itemId = itemId;
        this.itemName = itemName;
        this.banReason = banReason;
        this.ownershipBanned = ownershipBanned;
        this.usageBanned = usageBanned;
        this.breakingBanned = breakingBanned;
        this.placingBanned = placingBanned;
        this.dropBanned = dropBanned;
        this.craftBanned = craftBanned;
        this.worldBanned = worldBanned;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public String getItemId() {
        return itemId;
    }

    public String getBanReason() {
        return banReason;
    }

    public Boolean getOwnershipBanned() {
        return ownershipBanned;
    }

    public Boolean getUsageBanned() {
        return usageBanned;
    }

    public Boolean getBreakingBanned() {
        return breakingBanned;
    }

    public Boolean getPlacingBanned() {
        return placingBanned;
    }

    public Boolean getDropBanned() {
        return dropBanned;
    }

    public Boolean getWorldBanned() {
        return worldBanned;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setBanReason(String banReason) {
        this.banReason = banReason;
    }

    public void setOwnershipBanned(Boolean ownershipBanned) {
        this.ownershipBanned = ownershipBanned;
    }

    public void setUsageBanned(Boolean usageBanned) {
        this.usageBanned = usageBanned;
    }

    public void setBreakingBanned(Boolean breakingBanned) {
        this.breakingBanned = breakingBanned;
    }

    public void setPlacingBanned(Boolean placingBanned) {
        this.placingBanned = placingBanned;
    }

    public void setDropBanned(Boolean dropBanned) {
        this.dropBanned = dropBanned;
    }

    public void setWorldBanned(Boolean worldBanned) {
        this.worldBanned = worldBanned;
    }

    public Boolean getCraftBanned() {
        return craftBanned;
    }

    public void setCraftBanned(Boolean craftBanned) {
        this.craftBanned = craftBanned;
    }
}
