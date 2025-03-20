package cool.circuit.decorativeNPCS.commands;

import cool.circuit.decorativeNPCS.NPC;
import cool.circuit.decorativeNPCS.SkinFetchResponse;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

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

            if(npcDisplayName.length() >= 16) {
                player.sendMessage("Display name too long! Failed to create NPC.");
                return false;
            }

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
            Optional<EntityType<?>> optionalEntityType = EntityType.byString(npcType);

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
            ItemStack item = player.getInventory().getItemInMainHand();
            Material material = item.getType();

            if (material == Material.AIR) {
                player.sendMessage("You must be holding an item to equip it.");
                return false;
            }

            // Get current equipment of the NPC before modification
            ItemStack currentHelmet = npc.getHelmet();
            ItemStack currentChestplate = npc.getChestplate();
            ItemStack currentLeggings = npc.getLeggings();
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
        } else if (args[0].equalsIgnoreCase("particle")) {
            if (args.length < 4) {
                player.sendMessage("Usage: /npc particle <npc_id> <particle_type> <particle_size_multiplier> [extra_data]");
                return false;
            }

            NPC npc = getNpc(args[1]);
            if (npc == null) {
                player.sendMessage("Invalid NPC ID: " + args[1]);
                return false;
            }

            Particle particle;
            try {
                particle = Particle.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage("Invalid particle type: " + args[2]);
                return false;
            }

            int particleSize;
            try {
                particleSize = Integer.parseInt(args[3]);
                if (particleSize <= 0) {
                    player.sendMessage("Particle size multiplier must be greater than 0.");
                    return false;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid number for particle size multiplier.");
                return false;
            }

            Location npcLocation = npc.getLocation();

            for (int i = 0; i < particleSize; i++) {
                Location spawnLoc = npcLocation.clone().add(Math.random() - 0.5, Math.random() * 2, Math.random() - 0.5);

                try {
                    if (particle == Particle.DUST) {
                        // Requires DustOptions (color and size)
                        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.RED, 1.0f);
                        npcLocation.getWorld().spawnParticle(particle, spawnLoc, 1, dustOptions);
                } else if (particle == Particle.BLOCK || particle == Particle.BLOCK_MARKER || particle == Particle.FALLING_DUST) {
                        // Requires a block material
                        if (args.length < 5) {
                            player.sendMessage("This particle requires a block type! Usage: /npc particle <npc_id> BLOCK_CRACK <particle_size> <block_type>");
                            return false;
                        }
                        Material blockMaterial = Material.matchMaterial(args[4]);
                        if (blockMaterial == null || !blockMaterial.isBlock()) {
                            player.sendMessage("Invalid block type: " + args[4]);
                            return false;
                        }
                        npcLocation.getWorld().spawnParticle(particle, spawnLoc, 1, blockMaterial.createBlockData());
                    } else if (particle == Particle.ITEM) {
                        // Requires an item material
                        if (args.length < 5) {
                            player.sendMessage("This particle requires an item type! Usage: /npc particle <npc_id> ITEM_CRACK <particle_size> <item_type>");
                            return false;
                        }
                        Material itemMaterial = Material.matchMaterial(args[4]);
                        if (itemMaterial == null || itemMaterial.isAir()) {
                            player.sendMessage("Invalid item type: " + args[4]);
                            return false;
                        }
                        npcLocation.getWorld().spawnParticle(particle, spawnLoc, 1, new ItemStack(itemMaterial));
                    } else {
                        // Normal particles without extra data
                        npcLocation.getWorld().spawnParticle(particle, spawnLoc, 1);
                    }
                } catch (Exception e) {
                    player.sendMessage("Error spawning particle: " + e.getMessage());
                }
            }

            player.sendMessage("Particles added to NPC " + args[1] + " with type " + args[2] + " and size " + particleSize);
        }

/*else if(args[0].equalsIgnoreCase("glow")) {
            if(args.length != 2) {
                player.sendMessage("Usage: /npc glow <npc_id>");
                return false;
            }

            NPC npc = getNpc(args[1]);
            if(npc == null) {
                player.sendMessage("Invalid NPC ID: " + args[1]);
                return false;
            }

            npc.toggleGlowing();

            return true;
        }
        /*else if(args[0].equalsIgnoreCase("menu")) {
            if(args.length != 1) {
                player.sendMessage("Usage: /npc menu");
                return false;
            }

            MainTab menu = new MainTab("NPC : Main", 9 * 5, player);
            menu.open();
            Bukkit.getPluginManager().registerEvents(menu, getInstance());
            return true;
        }*/
        player.sendMessage("Invalid subcommand. Use /npc create, /npc modify, /npc remove, /npc list, /npc chat or another subcommand.");
        return false;
    }



    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return List.of("create", "modify", "remove", "list", "chat", "removeall", "pose", "skin", "blacklist", "particle");
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
        } else if(args[0].equalsIgnoreCase("particle")) {
            if(args.length == 2) {
                return npcs.keySet().stream().toList();
            } else if(args.length == 3) {
                return Arrays.stream(Particle.values()).map(Enum::name).toList();
            }
        }

        return List.of();
    }
    public static NPC getNpc(String arg) {
        return npcs.get(arg);
    }
}