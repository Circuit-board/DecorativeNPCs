package cool.circuit.decorativeNPCS.managers;

import cool.circuit.decorativeNPCS.menusystem.Menu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.*;

import static cool.circuit.decorativeNPCS.DecorativeNPCS.*;
import static org.bukkit.Bukkit.getLogger;

public class MenuManager {

    public static void createMenu(String title, HashMap<Integer, ItemStack> items, int slots, Player player, String name) {
        Menu menu = new Menu(title, slots, player) {
            @Override
            public void setMenuItems() {
                for(int slot : items.keySet()) {
                    setItem(items.get(slot), slot);
                }
            }

            @Override
            public void handleClicks(InventoryClickEvent event) {
                // Will implement next update.
                if(event.getView().getTitle().equalsIgnoreCase(title)) {
                    event.setCancelled(true);
                }
            }
        };

        // Ensure the items are set before opening the menu
        menu.setMenuItems();
        menus.put(name, menu);
        menu.open(player);  // Open the inventory after the items are set
    }


    public static void deleteMenu(String menuName) {
        if (menus.containsKey(menuName)) {
            menus.remove(menuName);
            System.out.println("Menu '" + menuName + "' deleted.");
        } else {
            System.out.println("Menu '" + menuName + "' not found.");
        }
    }

    public static void setItemForMenu(String menuName, int slot, ItemStack item) {
        Menu menu = menus.get(menuName);

        menu.setItem(item, slot);
    }

    public static Menu getMenu(String menuName) {
        return menus.get(menuName);
    }

    public static void saveMenus() {
        ConfigurationSection menusSection = menusFileConfig.getConfigurationSection("menus");

        if (menusSection == null) {
            menusSection = menusFileConfig.createSection("menus");
        }

        if (menus.keySet().isEmpty()) {
            return;
        }

        for (String menuName : menus.keySet()) {
            ConfigurationSection section = menusSection.createSection(menuName);
            Menu menu = menus.get(menuName);

            section.set("slots", menu.getSlots());
            section.set("title", menu.getTitle());
            section.set("playerName", menu.getPlayer());

            Inventory inv = menu.getInventory();
            ItemStack[] contents = inv.getContents();
            Map<String, Object> contentsMap = new HashMap<>();

            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item != null) {
                    // Save item by its material name and possibly meta data
                    String key = String.valueOf(i);
                    Map<String, Object> itemData = new HashMap<>();
                    itemData.put("material", item.getType().toString());
                    if (item.getAmount() > 1) {
                        itemData.put("amount", item.getAmount());
                    }
                    itemData.put("displayName", item.getItemMeta().getDisplayName());
                    itemData.put("lore", item.getItemMeta().getLore());
                    contentsMap.put(key, itemData);
                }
            }
            section.set("contents", contentsMap);
        }

        try {
            menusFileConfig.save(menusFile);
            getLogger().info("Menus saved successfully!");
        } catch (IOException e) {
            getLogger().severe("Failed to save menus: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static void loadMenus() {
        ConfigurationSection menusSection = menusFileConfig.getConfigurationSection("menus");

        if (menusSection == null) {
            return; // No menus saved, so return early
        }

        for (String menuName : menusSection.getKeys(false)) {
            // Retrieve the menu title and slots
            String title = menusSection.getString(menuName + ".title");
            int slots = menusSection.getInt(menuName + ".slots");

            // Initialize a HashMap to hold the items for the menu
            HashMap<Integer, ItemStack> items = new HashMap<>();

            // Retrieve the map of items (material and other properties)
            ConfigurationSection contentsSection = menusSection.getConfigurationSection(menuName + ".contents");

            // Loop through the map and recreate the items
            if (contentsSection != null) {
                for (String key : contentsSection.getKeys(false)) {
                    ConfigurationSection itemDataSection = contentsSection.getConfigurationSection(key);

                    if (itemDataSection != null) {
                        String materialName = itemDataSection.getString("material");
                        Material material = Material.getMaterial(materialName);

                        if (material != null) {
                            ItemStack item = new ItemStack(material);
                            ItemMeta meta = item.getItemMeta();
                            if (itemDataSection.contains("amount")) {
                                item.setAmount(itemDataSection.getInt("amount"));
                            }

                            meta.setDisplayName(itemDataSection.getString("displayName"));
                            meta.setLore(itemDataSection.getStringList("lore"));

                            item.setItemMeta(meta);

                            // Put the item in the corresponding slot (parse the slot key as an integer)
                            int slot = Integer.parseInt(key);
                            items.put(slot, item);
                        }
                    }
                }
            }

            // Create the menu and store it in the 'menus' map
            Menu menu = new Menu(slots, title) {
                @Override
                public void setMenuItems() {
                    for (int slot : items.keySet()) {
                        inv.setItem(slot, items.get(slot));
                    }
                }

                @Override
                public void handleClicks(InventoryClickEvent event) {
                    if(event.getView().getTitle().equalsIgnoreCase(title)) {
                        event.setCancelled(true);
                    }
                }
            };

            for(int slot : items.keySet()) {
                menu.setItem(items.get(slot), slot);
            }

            menus.put(menuName, menu); // Store the menu in the map
        }
    }

}
