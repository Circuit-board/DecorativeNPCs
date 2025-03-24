package cool.circuit.decorativeNPCS.menusystem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;

public abstract class Menu implements InventoryHolder, Listener {

    protected final String title;
    protected final int slots;
    protected final Player player;
    protected final HashMap<Integer, ItemStack> items = new HashMap<>();

    protected Inventory inv;

    public Menu(String title, int slots, Player player) {
        this.title = title;
        this.slots = slots;
        this.player = player;

        inv = Bukkit.createInventory(this, slots, title);
    }

    public Menu(int slots, String title) {
        this.title = title;
        this.slots = slots;
        this.player = null;

        inv = Bukkit.createInventory(this, slots, title);
    }

    public abstract void setMenuItems();

    @EventHandler
    public abstract void handleClicks(InventoryClickEvent event);

    public void open(Player p) {
        inv = Bukkit.createInventory(this, slots, title);

        setMenuItems(); // Set items before opening the inventory
        p.openInventory(inv); // Open the inventory to the player
    }


    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    protected ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);
        meta.setLore(Arrays.stream(lore).toList());
        item.setItemMeta(meta);

        return item;
    }

    public String getTitle() {
        return title;
    }

    public int getSlots() {
        return slots;
    }

    public void setItem(ItemStack item, int slot) {
        if (inv != null && slot >= 0 && slot < inv.getSize()) {
            items.put(slot, item);
            refresh();
        }
    }

    public Player getPlayer() {
        return player;
    }

    public HashMap<Integer, ItemStack> getItems() {
        return items;
    }

    public void refresh() {
        inv.clear();

        if(items.isEmpty()) {
            return;
        }

        for(int slot : items.keySet()) {
            inv.setItem(slot, items.get(slot));
        }
    }
}
