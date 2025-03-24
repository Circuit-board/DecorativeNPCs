package cool.circuit.decorativeNPCS;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;

public final class Item {
    private final String name;
    private final String displayName;
    private final List<String> lore;
    private final boolean glowing;
    private final Material material;
    private final ItemStack item;
    private final ItemMeta meta;

    public Item(String name, String displayName, List<String> lore, boolean glowing, Material material) {
        this.name = name;
        this.displayName = displayName;
        this.lore = lore;
        this.glowing = glowing;
        this.material = material;
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
        this.meta.setLore(lore);
        this.meta.setDisplayName(displayName);
        this.meta.setEnchantmentGlintOverride(glowing);

        this.item.setItemMeta(meta);
    }

    public Material getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void give(Player player) {
        player.getInventory().addItem(item);
    }}
