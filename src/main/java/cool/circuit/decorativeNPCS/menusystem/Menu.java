package cool.circuit.decorativeNPCS.menusystem;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;

public interface Menu {

    Inventory getInventory(); // Returns the inventory

    String getTitle(); // Returns the menu title

    int getSlots(); // Returns the number of slots

    HashMap<Integer, ItemStack> getItems(); // Returns items in the menu

    void setItems(HashMap<Integer, ItemStack> items);

    void open(Player player); // Opens the menu for a player

    void close(Player player); // Closes the menu for a player

    void setMenuItems(List<ItemStack> items); // Sets multiple items in the menu

    void setMenuTitle(String title); // Updates menu title
}
