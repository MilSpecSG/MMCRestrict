package net.moddedminecraft.mmcrestrict.Data;

public class ItemDataUtil {

    protected String itemname, itemid, banreason;
    protected Boolean usagebanned, ownershipbanned, worldbanned;

    public ItemDataUtil(String itemid, String itemname, String banreason, Boolean usagebanned, Boolean ownershipbanned, Boolean worldbanned) {
        this.itemid = itemid;
        this.itemname = itemname;
        this.banreason = banreason;
        this.usagebanned = usagebanned;
        this.ownershipbanned = ownershipbanned;
        //this.craftingbanned = craftingbanned;
        this.worldbanned = worldbanned;
    }

    public String getItemname() {
        return itemname;
    }

    public String getItemid() {
        return itemid;
    }

    public String getBanreason() {
        return banreason;
    }

    public Boolean getOwnershipbanned() {
        return ownershipbanned;
    }

    public Boolean getUsagebanned() {
        return usagebanned;
    }

    public Boolean getWorldbanned() {
        return worldbanned;
    }

    //public Boolean getCraftingbanned() {
    //    return craftingbanned;
    //}

    //public void setCraftingbanned(Boolean craftingbanned) {
    //    this.craftingbanned = craftingbanned;
    //}


    public void setItemname(String itemname) {
        this.itemname = itemname;
    }

    public void setBanreason(String banreason) {
        this.banreason = banreason;
    }

    public void setOwnershipbanned(Boolean ownershipbanned) {
        this.ownershipbanned = ownershipbanned;
    }

    public void setUsagebanned(Boolean usagebanned) {
        this.usagebanned = usagebanned;
    }

    public void setWorldbanned(Boolean worldbanned) {
        this.worldbanned = worldbanned;
    }
}