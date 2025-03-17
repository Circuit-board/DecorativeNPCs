package cool.circuit.decorativeNPCS.commands;

import cool.circuit.decorativeNPCS.NPC;
import cool.circuit.decorativeNPCS.SkinFetchResponse;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static cool.circuit.decorativeNPCS.DecorativeNPCS.*;

public class NPCCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return false;
        }


        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /npc subcommand");
            return false;
        }

        if(!sender.hasPermission("decorativenpcs.npc")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return false;
        }


        // Handle the "create" subcommand
        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 4) {
                player.sendMessage("Usage: /npc create <npc_name> <npc_id> <npc_type>");
                return false;
            }

            String npcDisplayName = ChatColor.translateAlternateColorCodes('&', args[1]);
            String npcName = args[2];
            String npcType = args[3].toLowerCase();  // Convert to lowercase for case insensitivity

            for(String word : blacklist) {
                if(npcName.toLowerCase().contains(word.toLowerCase()) ||
                        npcDisplayName.toLowerCase().contains(word.toLowerCase())) {
                    player.sendMessage("You cannot use this word!");
                    return false;
                }
            }

            if(npcs.containsKey(npcName)) {
                player.sendMessage("This npc already exists.");
                return false;
            }

            // Manually map Bukkit EntityType to NMS EntityType
            Optional<EntityType<?>> optionalEntityType = net.minecraft.world.entity.EntityType.byString(npcType);

            if (optionalEntityType.isPresent()) {
                // Create and spawn the NPC if valid
                NPC npc = new NPC(
                        npcDisplayName,
                        npcName,
                        optionalEntityType.get(), // Get the NMS EntityType
                        player,
                        player.getLocation()
                );
                npc.spawn(); // Spawn the NPC at the player's location


                npcs.put(npcName, npc);

                player.sendMessage("NPC created with name: " + npcDisplayName + " and type: " + npcType);
            } else {
                player.sendMessage("Error: Invalid NPC type " + npcType + ". Please try again.");
            }
            return true;

        } else if (args[0].equalsIgnoreCase("modify")) {
            // Handle the "modify" subcommand
            if (args.length < 3) {
                player.sendMessage("Usage: /npc modify <npc_id> <slot>");
                return false;
            }


            NPC npc = npcs.get(args[1]);
            if (npc == null) {
                player.sendMessage("Invalid NPC ID: " + args[1]);
                return false;
            }

            if(!player.getUniqueId().equals(npc.owner.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You cannot modify other peoples NPCs!");
                return false;
            }

            String slot = args[2].toLowerCase();
            org.bukkit.inventory.ItemStack item = player.getInventory().getItemInMainHand();
            Material material = item.getType();

            if (material == Material.AIR) {
                player.sendMessage("You must be holding an item to equip it.");
                return false;
            }

            // Get current equipment of the NPC before modification
            org.bukkit.inventory.ItemStack currentHelmet = npc.getHelmet();
            org.bukkit.inventory.ItemStack currentChestplate = npc.getChestplate();
            org.bukkit.inventory.ItemStack currentLeggings = npc.getLeggings();
            ItemStack currentBoots = npc.getBoots();

            // Handle different slots and equip items without overriding other slots
            switch (slot) {
                case "helmet" -> {
                    if (material.name().endsWith("_HELMET")) {
                        npc.setEquipment(item, currentChestplate, currentLeggings, currentBoots);
                        player.sendMessage("Set NPC's helmet to " + material);
                    } else {
                        player.sendMessage("You must hold a helmet to set it.");
                    }
                }
                case "chestplate" -> {
                    if (material.name().endsWith("_CHESTPLATE")) {
                        npc.setEquipment(currentHelmet, item, currentLeggings, currentBoots);
                        player.sendMessage("Set NPC's chestplate to " + material);
                    } else {
                        player.sendMessage("You must hold a chestplate to set it.");
                    }
                }
                case "leggings" -> {
                    if (material.name().endsWith("_LEGGINGS")) {
                        npc.setEquipment(currentHelmet, currentChestplate, item, currentBoots);
                        player.sendMessage("Set NPC's leggings to " + material);
                    } else {
                        player.sendMessage("You must hold leggings to set them.");
                    }
                }
                case "boots" -> {
                    if (material.name().endsWith("_BOOTS")) {
                        npc.setEquipment(currentHelmet, currentChestplate, currentLeggings, item);
                        player.sendMessage("Set NPC's boots to " + material);
                    } else {
                        player.sendMessage("You must hold boots to set them.");
                    }
                }
                case "mainhand" -> {
                    npc.setItemInMainHand(item);
                    player.sendMessage("Set NPC's mainhand to " + material);
                }
                default -> {
                    player.sendMessage("Invalid slot: " + slot);
                    return false;
                }
            }
            return true;
        } else if (args[0].equalsIgnoreCase("remove")) {
            if(args.length != 2) {
                player.sendMessage("Usage: /npc remove <npc_id>");
                return false;
            }
            NPC npc = getNpc(args[1]);
            if (npc == null) {
                player.sendMessage("Invalid NPC ID: " + args[1]);
                return false;
            }
            if(!player.getUniqueId().equals(npc.owner.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You cannot modify other peoples NPCs!");
                return false;
            }
            player.sendMessage("Successfully removed NPC with ID: " + args[1]);
            npc.remove();
            getConfiguration().set(npc.name, null);
            return true;
        } else if (args[0].equalsIgnoreCase("list")) {
            for(NPC npc : npcs.values()) {
                player.sendMessage(npc.name);
            }
            return true;
        } else if(args[0].equalsIgnoreCase("chat")) {
            if(args.length < 3) {
                player.sendMessage("Usage: /npc chat <npc_id> <message>");
                return false;
            }
            NPC npc = npcs.get(args[1]);
            if (npc == null) {
                player.sendMessage("Invalid NPC ID: " + args[1]);
                return false;
            }
            String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            for(String word : blacklist) {
                if(message.toLowerCase().contains(word)) {
                    player.sendMessage("You cannot use that word!");
                    return false;
                }
            }
            npc.chat(message);
            return true;
        } else if(args[0].equalsIgnoreCase("removeall")) {
            int i = 0;

            if(npcs.values().stream()
                    .noneMatch(npc -> npc.owner.getUniqueId().equals(player.getUniqueId()))
            ) {
                player.sendMessage(ChatColor.RED + "You cannot modify other peoples NPCs!");
                return false;
            }

            for(NPC npc : npcs.values()) {
                npc.remove();
                getConfiguration().set(npc.name, null);
                i++;
            }

            player.sendMessage(i + " NPCs have been removed.");
            return true;
        } else if(args[0].equalsIgnoreCase("pose")) {
            NPC npc = npcs.get(args[1]);
            if (npc == null) {
                player.sendMessage("Invalid NPC ID: " + args[1]);
                return false;
            }
            Pose pose = Pose.valueOf(args[2]);

            npc.setPose(pose);

            return true;
        } else if(args[0].equalsIgnoreCase("skin")) {
            NPC npc = npcs.get(args[1]);
            if (npc == null) {
                player.sendMessage("Invalid NPC ID: " + args[1]);
                return false;
            }
            if(args.length != 3) {
                player.sendMessage("Usage: /npc skin <npc_id> <skin_name>");
                return false;
            }
            if(!player.getUniqueId().equals(npc.owner.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You cannot modify other peoples NPCs!");
                return false;
            }
            SkinFetchResponse response = npc.setSkin(args[2]); // args[2] is the player name for the skin
            switch (response) {
                case SUCCESS:
                    player.sendMessage(ChatColor.GREEN + "Successfully changed the skin of the NPC!");
                    npcs.put(args[1], npc);
                    break;
                case FAILED_TO_FETCH_SKIN:
                    player.sendMessage(ChatColor.RED + "Failed to fetch the skin for player: " + args[2] + ". Please check if the player name is correct.");
                    break;
                case DATA_IS_NULL:
                    player.sendMessage(ChatColor.RED + "No skin data found for player: " + args[2] + ". Make sure the player has a valid skin.");
                    break;
                case SKIN_VALUE_OR_SIGNATURE_IS_NULL:
                    player.sendMessage(ChatColor.RED + "The skin value or signature for player: " + args[2] + " is invalid.");
                    break;
                case NO_SERVER_PLAYER:
                    player.sendMessage(ChatColor.RED + "Unable to find the server player associated with the NPC. This may be a server-side issue.");
                    break;
                default:
                    player.sendMessage(ChatColor.RED + "An unexpected error occurred while changing the NPC skin.");
                    break;
            }
            return true;
        } else if(args[0].equalsIgnoreCase("blacklist")) {
            if(args.length != 3) {
                player.sendMessage("Usage: /npc blacklist <add|remove> <word>");
                return false;
            }
            String action = args[1];
            if(!Objects.equals(action, "add") && !Objects.equals(action, "remove")) {
                player.sendMessage("Usage: /npc blacklist <add|remove> <word>");
                return false;
            }
            String word = args[2];
            switch(action) {
                case "add":
                    blacklist.add(word);
                    break;
                case "remove":
                    if(blacklist.contains(word)) {
                        blacklist.remove(word);
                    } else {
                        player.sendMessage("That word isnt in the blacklist.");
                    }
                    break;
            }
            return true;
        }
        player.sendMessage("Invalid subcommand. Use /npc create, /npc modify, /npc remove, /npc list, /npc chat or another subcommand.");
        return false;
    }



    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return List.of("create", "modify", "remove", "list", "chat", "removeall", "pose", "skin", "blacklist");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("modify")) {
            return npcs.keySet().stream().toList();
        } else if (args.length == 3 && args[0].equalsIgnoreCase("modify")) {
            return List.of("helmet", "chestplate", "leggings", "boots", "mainhand");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            return Arrays.stream(org.bukkit.entity.EntityType.values())
                    .map(Enum::name)
                    .toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return npcs.keySet().stream().toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("chat")) {
            return npcs.keySet().stream().toList();
        } else if(args[0].equalsIgnoreCase("pose")) {
            switch (args.length) {
                case 2 -> {
                    return npcs.keySet().stream().toList();
                }
                case 3 -> {
                    return Arrays.stream(Pose.values()).map(Enum::name).toList();
                }
            }
        } else if(args[0].equalsIgnoreCase("skin")) {
            if(args.length == 2) {
                return npcs.keySet().stream().toList();
            } else if(args.length == 3) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            }
        } else if(args[0].equalsIgnoreCase("blacklist") && args.length == 2) {
            return List.of("remove","add");
        }

        return List.of();
    }
    private NPC getNpc(String arg) {
        return npcs.get(arg);
    }
}