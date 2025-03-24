package cool.circuit.decorativeNPCS.menusystem;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import static cool.circuit.decorativeNPCS.DecorativeNPCS.getConfigObj;
import static cool.circuit.decorativeNPCS.DecorativeNPCS.saveConfiguration;

public class ConfigurationMenu extends Menu {

    private ItemStack permissionItem;
    private ItemStack blacklistItem;
    private ItemStack NPCOwnerItem;

    public ConfigurationMenu(String title, int slots, Player player) {
        super(title, slots, player);
    }

    @Override
    public void setMenuItems() {
        // Set gray glass panes for the borders
        for (int i = 0; i < 9; i++) {
            setItem(createItem(
                    Material.GRAY_STAINED_GLASS_PANE,
                    " "
            ), i);
        }

        // Set permission item
        permissionItem = createItem(
                (getConfigObj().isRequirePerm() ? Material.LIME_DYE : Material.GRAY_DYE),
                (getConfigObj().isRequirePerm() ? ChatColor.GREEN : ChatColor.GRAY) + "Require permission?",
                ChatColor.GRAY + "Should users need a permission to use the /npc command?",
                (getConfigObj().isRequirePerm() ? ChatColor.GREEN : ChatColor.GRAY) + "Click to toggle off!"
        );

        // Set blacklist item
        blacklistItem = createItem(
                (getConfigObj().isDoBlacklist() ? Material.LIME_DYE : Material.GRAY_DYE),
                (getConfigObj().isDoBlacklist() ? ChatColor.GREEN : ChatColor.GRAY) + "Do blacklist?",
                ChatColor.GRAY + "Should certain words be blacklisted?",
                (getConfigObj().isDoBlacklist() ? ChatColor.GREEN : ChatColor.GRAY) + "Click to toggle off!"
        );

        NPCOwnerItem = createItem(
                (getConfigObj().isNPCOwnerLock() ? Material.LIME_DYE : Material.GRAY_DYE),
                (getConfigObj().isNPCOwnerLock() ? ChatColor.GREEN : ChatColor.GRAY) + "Only allow players to modify their NPCs?",
                ChatColor.GRAY + "Should players only be able to modify their own NPCs?",
                (getConfigObj().isNPCOwnerLock() ? ChatColor.GREEN : ChatColor.GRAY) + "Click to toggle off!"
        );

        // Place items in the menu
        setItem(permissionItem, 12);
        setItem(NPCOwnerItem, 13);
        setItem(blacklistItem, 14);
    }

    @Override
    @EventHandler
    public void handleClicks(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(title)) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) {
                return;
            }

            boolean settingChanged = false;  // Track if the setting has changed

            if (event.getSlot() == 12) {
                switch (event.getCurrentItem().getType()) {
                    case LIME_DYE -> {
                        permissionItem = createItem(
                                Material.GRAY_DYE,
                                ChatColor.GRAY + "Require permission?",
                                ChatColor.GRAY + "Should users need a permission to use the /npc command?",
                                ChatColor.GRAY + "Click to toggle on!"
                        );

                        getConfigObj().setRequirePerm(false);
                        settingChanged = true;
                    }
                    case GRAY_DYE -> {
                        permissionItem = createItem(
                                Material.LIME_DYE,
                                ChatColor.GREEN + "Require permission?",
                                ChatColor.GRAY + "Should users need a permission to use the /npc command?",
                                ChatColor.GREEN + "Click to toggle off!"
                        );

                        getConfigObj().setRequirePerm(true);
                        settingChanged = true;
                    }
                }
                setItem(permissionItem, 12);
            } else if (event.getSlot() == 14) {
                switch (event.getCurrentItem().getType()) {
                    case LIME_DYE -> {
                        blacklistItem = createItem(
                                Material.GRAY_DYE,
                                ChatColor.GRAY + "Do blacklist?",
                                ChatColor.GRAY + "Should certain words be blacklisted?",
                                ChatColor.GRAY + "Click to toggle on!"
                        );

                        getConfigObj().setDoBlacklist(false);
                        settingChanged = true;
                    }
                    case GRAY_DYE -> {
                        blacklistItem = createItem(
                                Material.LIME_DYE,
                                ChatColor.GREEN + "Do blacklist?",
                                ChatColor.GRAY + "Should certain words be blacklisted?",
                                ChatColor.GREEN + "Click to toggle off!"
                        );

                        getConfigObj().setDoBlacklist(true);
                        settingChanged = true;
                    }
                }
                setItem(blacklistItem, 14);
            } else if(event.getSlot() == 13) {
                switch (event.getCurrentItem().getType()) {
                    case LIME_DYE -> {
                        NPCOwnerItem = createItem(
                                Material.GRAY_DYE,
                                ChatColor.GRAY + "Only allow players to modify their NPCs?",
                                ChatColor.GRAY + "Should players only be able to modify their own NPCs?",
                                ChatColor.GRAY + "Click to toggle on!"
                        );

                        getConfigObj().setNPCOwnerLock(false);
                        settingChanged = true;
                    }
                    case GRAY_DYE -> {
                        NPCOwnerItem = createItem(
                                (getConfigObj().isNPCOwnerLock() ? Material.LIME_DYE : Material.GRAY_DYE),
                                (getConfigObj().isNPCOwnerLock() ? ChatColor.GREEN : ChatColor.GRAY) + "Only allow players to modify their NPCs?",
                                ChatColor.GRAY + "Should players only be able to modify their own NPCs?",
                                (getConfigObj().isNPCOwnerLock() ? ChatColor.GREEN : ChatColor.GRAY) + "Click to toggle off!"
                        );

                        getConfigObj().setNPCOwnerLock(true);
                        settingChanged = true;
                    }
                }
                setItem(NPCOwnerItem, 13);
            }

            // Save the config if any setting changed
            if (settingChanged) {
                saveConfiguration();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            }
        }
    }
}
