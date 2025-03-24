package cool.circuit.decorativeNPCS.managers;

import cool.circuit.decorativeNPCS.Item;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static cool.circuit.decorativeNPCS.DecorativeNPCS.*;
import static org.bukkit.Bukkit.getLogger;

public class ItemManager {
    public static void saveItems() {
        ConfigurationSection section = itemsFileConfig.createSection("items");
        for(String name : items.keySet()) {
            Item item = items.get(name);

            ConfigurationSection itemSection = section.createSection(name);

            itemSection.set("displayName", item.getDisplayName());
            itemSection.set("lore", item.getLore());
            itemSection.set("material", item.getMaterial().name());
            itemSection.set("glowing", item.isGlowing());
        }
        try {
            itemsFileConfig.save(itemsFile);
            getLogger().info("Items saved successfully!");
        } catch (IOException e) {
            getLogger().severe("Failed to save items: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void loadItems() {
        ConfigurationSection section = itemsFileConfig.getConfigurationSection("items");
        if (section == null) {
            getLogger().warning("No items section found in config.");
            return;
        }

        for (String name : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(name);
            if (itemSection != null) {
                String displayName = itemSection.getString("displayName");
                List<String> lore = itemSection.getStringList("lore");
                Material material = Material.getMaterial(itemSection.getString("material").toUpperCase());
                boolean glowing = itemSection.getBoolean("glowing");

                if (displayName != null && material != null) {
                    Item item = new Item(name, displayName, lore, glowing, material);
                    items.put(name, item);
                } else {
                    getLogger().warning("Invalid item data for item: " + name);
                }
            }
        }
        getLogger().info("Items loaded successfully!");
    }

}
