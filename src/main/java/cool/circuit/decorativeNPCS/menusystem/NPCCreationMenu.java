package cool.circuit.decorativeNPCS.menusystem;

import cool.circuit.decorativeNPCS.NPC;
import net.minecraft.world.entity.EntityType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import static cool.circuit.decorativeNPCS.managers.InputManager.input;

public class NPCCreationMenu extends Menu implements Listener {
    public NPCCreationMenu(int slots, String title) {
        super(slots, title);
    }

    private String id;
    private String displayName;
    private String skinOwnerName;
    private Location location;
    private EntityType<?> type;

    @Override
    public void setMenuItems() {
        ItemStack createItem = createItem(
                Material.LIME_WOOL,
                ChatColor.GREEN + "Create",
                ChatColor.GREEN + "Click to create!"
        );
        ItemStack idItem = createItem(
                Material.NAME_TAG,
                ChatColor.YELLOW + "Set NPC ID",
                ChatColor.GRAY + (id == null ? "Not set" : id)
        );
        ItemStack displayNameItem = createItem(
                Material.PAPER,
                ChatColor.YELLOW + "Set Display Name",
                ChatColor.GRAY + (displayName == null ? "Not set" : displayName)
        );
        ItemStack skinOwnerItem = createItem(
                Material.PLAYER_HEAD,
                ChatColor.YELLOW + "Set Skin Owner",
                ChatColor.GRAY + (skinOwnerName == null ? "Not set" : skinOwnerName)
        );
        ItemStack locationItem = createItem(
                Material.COMPASS,
                ChatColor.YELLOW + "Set Location",
                location == null ? ChatColor.GRAY + "Not set" :
                        ChatColor.GRAY + "X: " + location.getBlockX() +
                                " Y: " + location.getBlockY() +
                                " Z: " + location.getBlockZ()
        );
        ItemStack typeItem = createItem(
                Material.ZOMBIE_HEAD,
                ChatColor.YELLOW + "Set Type",
                ChatColor.GRAY + (type == null ? "Not set" : type.toString())
        );

        setItem(idItem, 10);
        setItem(displayNameItem, 11);
        setItem(skinOwnerItem, 12);
        setItem(locationItem, 13);
        setItem(typeItem, 14);
        setItem(createItem, 17);
    }

    @Override
    @EventHandler
    public void handleClicks(InventoryClickEvent event) {
        if (!event.getView().getTitle().equalsIgnoreCase(title)) {
            return;
        }

        event.setCancelled(true); // Prevent item movement
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        switch (slot) {
            case 10 -> {
                player.closeInventory();
                input(player, "Enter the NPC ID in chat.", (str) -> {
                    id = str;
                    player.sendMessage(ChatColor.GREEN + "NPC ID set to: " + str);
                    player.openInventory(inv);
                    setMenuItems();
                });
            }
            case 11 -> {
                player.closeInventory();
                input(player, "Enter the NPC Display Name in chat.", (str) -> {
                    displayName = ChatColor.translateAlternateColorCodes('&', str);
                    player.sendMessage(ChatColor.GREEN + "Display Name set to: " + displayName);
                    player.openInventory(inv);
                    setMenuItems();
                });
            }
            case 12 -> {
                player.closeInventory();
                input(player, "Enter the skin owner name in chat.", (str) -> {
                    skinOwnerName = str;
                    player.sendMessage(ChatColor.GREEN + "Skin Owner set to: " + str);
                    player.openInventory(inv);
                    setMenuItems();
                });

            }
            case 13 -> {
                player.closeInventory();
                location = player.getLocation();
                player.sendMessage(ChatColor.GREEN + "Location set to your current position!");
                player.openInventory(inv);
                setMenuItems();
            }
            case 14 -> {
                player.closeInventory();
                input(player, "Enter the NPC Type in chat (e.g., ZOMBIE, VILLAGER).", (str) -> {
                    EntityType<?> entityType = EntityType.byString(str.toLowerCase()).orElse(null);

                    if (entityType == null) {
                        player.sendMessage(ChatColor.RED + "Invalid entity type. Try again.");
                        player.openInventory(inv); // Reopen the inventory for retry
                        return;
                    }

                    type = entityType;
                    player.sendMessage(ChatColor.GREEN + "NPC Type set to: " + type);
                    setMenuItems(); // Update inventory items
                    player.openInventory(inv);
                });
            }
            case 17 -> {
                if (displayName == null || id == null || type == null || location == null) {
                    player.sendMessage(ChatColor.RED + "NPC creation failed! Missing information.");
                    return;
                }

                NPC npc = new NPC(displayName, id, type, player, location);
                npc.spawn();
                player.sendMessage(ChatColor.GREEN + "NPC created successfully!");
                player.closeInventory();
            }
        }
    }
}
