package cool.circuit.decorativeNPCS.menusystem;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;

public abstract class SimpleMenu implements Menu, InventoryHolder {

    private Inventory inv;
    private final int slots;
    private String title;
    private final HashMap<Integer, ItemStack> items = new HashMap<>();

    public SimpleMenu(String title, int slots) {
        this.title = title;
        this.slots = slots;
        this.inv = Bukkit.createInventory(null, slots, title);
    }

    @Override
    public void open(Player player) {
        inv.clear();
        inv.setContents(items.values().toArray(new ItemStack[0])); // More efficient
        player.openInventory(inv);
    }

    @Override
    public void close(Player player) {
        player.closeInventory();
    }

    @Override
    public void setMenuItems(List<ItemStack> newItems) {
        items.clear();
        for (int i = 0; i < Math.min(newItems.size(), slots); i++) { // Prevent overflow
            items.put(i, newItems.get(i));
        }

    }

    @Override
    public void setMenuTitle(String title) {
        this.title = title;
        this.inv = Bukkit.createInventory(null, slots, title); // Ensure inventory updates
    }
    @Override
    public HashMap<Integer, ItemStack> getItems() {
        return items;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    @Override
    public int getSlots() {
        return slots;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setItems(HashMap<Integer, ItemStack> items) {
        this.items.clear();
        this.items.putAll(items);
    }
}
