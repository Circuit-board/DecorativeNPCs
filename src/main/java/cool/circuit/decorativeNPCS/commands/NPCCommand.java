package cool.circuit.decorativeNPCS.commands;

import cool.circuit.decorativeNPCS.Item;
import cool.circuit.decorativeNPCS.NPC;
import cool.circuit.decorativeNPCS.SkinFetchResponse;
import cool.circuit.decorativeNPCS.menusystem.ConfigurationMenu;
import cool.circuit.decorativeNPCS.menusystem.Menu;
import cool.circuit.decorativeNPCS.menusystem.NPCCreationMenu;
import cool.circuit.decorativeNPCS.obj.NPCInteractFunction;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

import static cool.circuit.decorativeNPCS.DecorativeNPCS.*;
import static cool.circuit.decorativeNPCS.managers.InputManager.input;
import static cool.circuit.decorativeNPCS.managers.MenuManager.*;

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

        if (getConfigObj().isRequirePerm()) {
            if (!sender.hasPermission("decorativenpcs.npc")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return false;
            }
        }


        // Handle the "create" subcommand
        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 4) {
                player.sendMessage("Usage: /npc create <npc_name> <npc_id> <npc_type>");
                return false;
            }

            String npcDisplayName = ChatColor.translateAlternateColorCodes('&', args[1]);

            if (npcDisplayName.length() >= 16) {
                player.sendMessage("Display name too long! Failed to create NPC.");
                return false;
            }

            String npcName = args[2];
            String npcType = args[3].toLowerCase();  // Convert to lowercase for case insensitivity

            if (getConfigObj().isDoBlacklist()) {
                for (String word : blacklist) {
                    if (npcName.toLowerCase().contains(word.toLowerCase()) ||
                            npcDisplayName.toLowerCase().contains(word.toLowerCase())) {
                        player.sendMessage("You cannot use this word!");
                        return false;
                    }
                }
            }
            if (npcs.containsKey(npcName)) {
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

                NPC.saveNPCs(getConfiguration());
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

            if (getConfigObj().isNPCOwnerLock()) {
                if (!player.getUniqueId().equals(npc.owner.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You cannot modify other peoples NPCs!");
                    return false;
                }
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
            NPC.saveNPCs(getConfiguration());
            return true;
        } else if (args[0].equalsIgnoreCase("remove")) {
            if (args.length != 2) {
                player.sendMessage("Usage: /npc remove <npc_id>");
                return false;
            }
            NPC npc = getNpc(args[1]);
            if (npc == null) {
                player.sendMessage("Invalid NPC ID: " + args[1]);
                return false;
            }
            if (npc.owner == null) {
                player.sendMessage("An error occurred while removing NPC: " + args[1] + " Please contact CircuitBoard on spigotmc.org.");
                return false;
            }
            if (getConfigObj().isNPCOwnerLock()) {
                if (!player.getUniqueId().equals(npc.owner.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You cannot modify other peoples NPCs!");
                    return false;
                }
            }
            player.sendMessage("Successfully removed NPC with ID: " + args[1]);
            npc.remove();
            getConfiguration().set(npc.name, null);
            return true;
        } else if (args[0].equalsIgnoreCase("list")) {

            Audience p = getInstance().adventure().player(player);

            NPC.loadNPCs(getConfiguration());

            if (npcs.isEmpty()) {
                Component msg = Component.text("No NPCs have been created!")
                        .color(TextColor.fromHexString("#FFD700"));

                p.sendMessage(msg);
                return false;
            }

            Component header = Component.text("Listing NPCs (" + npcs.size() + ")")
                    .color(TextColor.fromHexString("#FFD700"));

            p.sendMessage(header);

            for (NPC npc : npcs.values()) {
                Component msg = Component.text(npc.name)
                        .color(TextColor.fromHexString("#FFD700"))
                        .hoverEvent(HoverEvent.showText(Component.text("NPC Id: " + npc.name + " NPC Displayname: ")
                                .appendNewline()
                                .append(Component.text("NPC Coordinates: " + Math.round(npc.getLocation().getX()) + ", " + Math.round(npc.getLocation().getY()) + ", " + Math.round(npc.getLocation().getZ())).color(TextColor.fromHexString("#FFD700")))
                                .color(TextColor.fromHexString("#FFD700"))));
                p.sendMessage(msg);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("chat")) {
            if (args.length < 3) {
                player.sendMessage("Usage: /npc chat <npc_id> <message>");
                return false;
            }
            NPC npc = npcs.get(args[1]);
            if (npc == null) {
                player.sendMessage("Invalid NPC ID: " + args[1]);
                return false;
            }
            String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            if (getConfigObj().isDoBlacklist()) {
                for (String word : blacklist) {
                    if (message.toLowerCase().contains(word)) {
                        player.sendMessage("You cannot use that word!");
                        return false;
                    }
                }
            }
            npc.chat(message);
            return true;
        } else if (args[0].equalsIgnoreCase("removeall")) {
            int i = 0;

            for (NPC npc : npcs.values()) {
                if (npc.owner == null) {
                    player.sendMessage("An error occurred while removing NPC: " + npc.name + " Please contact CircuitBoard on spigotmc.org.");
                    return false;
                }
            }

            if (getConfigObj().isNPCOwnerLock()) {
                if (npcs.values().stream()
                        .noneMatch(npc -> npc.owner.getUniqueId().equals(player.getUniqueId()))
                ) {
                    player.sendMessage(ChatColor.RED + "You cannot modify other peoples NPCs!");
                    return false;
                }
            }

            for (NPC npc : npcs.values()) {
                npc.remove();
                getConfiguration().set(npc.name, null);
                i++;
            }

            player.sendMessage(i + " NPCs have been removed.");
            NPC.saveNPCs(getConfiguration());
            return true;
        } else if (args[0].equalsIgnoreCase("pose")) {
            NPC npc = npcs.get(args[1]);
            if (npc == null) {
                player.sendMessage("Invalid NPC ID: " + args[1]);
                return false;
            }

            if (getConfigObj().isNPCOwnerLock()) {
                if (!player.getUniqueId().equals(npc.owner.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You cannot modify other peoples NPCs!");
                    return false;
                }
            }

            if (args[2].equalsIgnoreCase("default")) {
                npc.setPose(Pose.SHOOTING);
                return true;
            }

            Pose pose = Pose.valueOf(args[2]);

            switch (npc.setPose(pose)) {
                case INVALID:
                    player.sendMessage("Invalid pose: " + pose);
                    break;
                case ONLY_WORKS_FOR_PLAYERS:
                    player.sendMessage("This pose only works for players.");
                    break;
                case WORKS_FOR_ALL_ENTITIES:
                    player.sendMessage("Pose applied successfully!");
                    break;
                case DOESNT_WORK_FOR_PLAYERS:
                    player.sendMessage("This pose does not work for players!");
                    break;
                default:
                    player.sendMessage("An unknown error occurred while applying the pose. Please contact CircuitBoard on spigotmc.org.");
                    break;
            }
            NPC.saveNPCs(getConfiguration());
            return true;
        } else if (args[0].equalsIgnoreCase("skin")) {
            NPC npc = npcs.get(args[1]);
            if (npc == null) {
                player.sendMessage("Invalid NPC ID: " + args[1]);
                return false;
            }
            if (args.length != 3) {
                player.sendMessage("Usage: /npc skin <npc_id> <skin_name>");
                return false;
            }

            if (getConfigObj().isNPCOwnerLock()) {
                if (!player.getUniqueId().equals(npc.owner.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You cannot modify other peoples NPCs!");
                    return false;
                }
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
            NPC.saveNPCs(getConfiguration());
            return true;
        } else if (args[0].equalsIgnoreCase("blacklist")) {
            if (args.length != 3) {
                player.sendMessage("Usage: /npc blacklist <add|remove> <word>");
                return false;
            }
            if (getConfigObj().isDoBlacklist()) {
                String action = args[1];
                if (!Objects.equals(action, "add") && !Objects.equals(action, "remove")) {
                    player.sendMessage("Usage: /npc blacklist <add|remove> <word>");
                    return false;
                }
                String word = args[2];
                switch (action) {
                    case "add":
                        blacklist.add(word);
                        break;
                    case "remove":
                        if (blacklist.contains(word)) {
                            blacklist.remove(word);
                        } else {
                            player.sendMessage("That word isnt in the blacklist.");
                        }
                        break;
                }
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
                return true;
            }

            player.sendMessage("Particles added to NPC " + args[1] + " with type " + args[2] + " and size " + particleSize);
        } else if (args[0].equalsIgnoreCase("help")) {
            if (args.length != 1) {
                player.sendMessage("Usage: /npc help");
                return false;
            }

            Audience p = getInstance().adventure().player(player);

            Component header = Component.text("Commands: ")
                    .color(TextColor.fromHexString("#FFD700"));

            Component createCommand = Component.text("/npc create <npc_name> <npc_id> <npc_type>")
                    .color(TextColor.fromHexString("#FFD700"))
                    .hoverEvent(HoverEvent.showText(Component.text("Create an NPC with this command!")
                            .color(TextColor.fromHexString("#FFD700"))));

            Component modifyCommand = Component.text("/npc modify <npc_id> <option> <value>")
                    .color(TextColor.fromHexString("#FFD700"))
                    .hoverEvent(HoverEvent.showText(Component.text("Modify an existing NPC's attributes!")
                            .color(TextColor.fromHexString("#FFD700"))));

            Component removeCommand = Component.text("/npc remove <npc_id>")
                    .color(TextColor.fromHexString("#FFD700"))
                    .hoverEvent(HoverEvent.showText(Component.text("Remove an NPC by its ID.")
                            .color(TextColor.fromHexString("#FFD700"))));

            Component listCommand = Component.text("/npc list")
                    .color(TextColor.fromHexString("#FFD700"))
                    .hoverEvent(HoverEvent.showText(Component.text("List all created NPCs.")
                            .color(TextColor.fromHexString("#FFD700"))));

            Component chatCommand = Component.text("/npc chat <npc_id>")
                    .color(TextColor.fromHexString("#FFD700"))
                    .hoverEvent(HoverEvent.showText(Component.text("Make an NPC send a chat message!")
                            .color(TextColor.fromHexString("#FFD700"))));

            Component removeAllCommand = Component.text("/npc removeall")
                    .color(TextColor.fromHexString("#FFD700"))
                    .hoverEvent(HoverEvent.showText(Component.text("Remove all NPCs at once!")
                            .color(TextColor.fromHexString("#FFD700"))));

            Component poseCommand = Component.text("/npc pose <npc_id> <pose>")
                    .color(TextColor.fromHexString("#FFD700"))
                    .hoverEvent(HoverEvent.showText(Component.text("Change the pose of an NPC.")
                            .color(TextColor.fromHexString("#FFD700"))));

            Component skinCommand = Component.text("/npc skin <npc_id> <player_name>")
                    .color(TextColor.fromHexString("#FFD700"))
                    .hoverEvent(HoverEvent.showText(Component.text("Change an NPC's skin to a player's skin.")
                            .color(TextColor.fromHexString("#FFD700"))));

            Component blacklistCommand = Component.text("/npc blacklist <add/remove> <word>")
                    .color(TextColor.fromHexString("#FFD700"))
                    .hoverEvent(HoverEvent.showText(Component.text("Add or remove a player from the NPC blacklist.")
                            .color(TextColor.fromHexString("#FFD700"))));

            Component particleCommand = Component.text("/npc particle <npc_id> <particle_type> <particle_size_multiplier")
                    .color(TextColor.fromHexString("#FFD700"))
                    .hoverEvent(HoverEvent.showText(Component.text("Attach a particle effect to an NPC.")
                            .color(TextColor.fromHexString("#FFD700"))));

            Component helpCommand = Component.text("/npc help")
                    .color(TextColor.fromHexString("#FFD700"))
                    .hoverEvent(HoverEvent.showText(Component.text("Show help for all NPC commands.")
                            .color(TextColor.fromHexString("#FFD700"))));

            Component configCommand = Component.text("/npc config")
                    .color(TextColor.fromHexString("#FFD700"))
                    .hoverEvent(HoverEvent.showText(Component.text("Opens the configuration menu!")
                            .color(TextColor.fromHexString("#FFD700"))));

            Component interactCommand = Component.text("/npc interact <npc_id> <add/remove> <type> <value>")
                    .color(TextColor.fromHexString("#FFD700"))
                    .hoverEvent(HoverEvent.showText(Component.text("Add/Removes an interact event from an NPC!")
                            .color(TextColor.fromHexString("#FFD700"))));

            Component menuCommand = Component.text("/npc menu <create|delete|setitem> [menuName] [title] [slots]")
                    .color(TextColor.fromHexString("#FFD700"))
                    .hoverEvent(HoverEvent.showText(Component.text("Creates a menu that can be linked to the interact function of an NPC!")
                            .color(TextColor.fromHexString("#FFD700"))));


            Component itemCommand = Component.text("/npc item <create|get> [itemname], [itemtype], [displayname] [glowing], [lore] ")
                    .color(TextColor.fromHexString("#FFD700"))
                    .hoverEvent(HoverEvent.showText(Component.text("Creates a custom item!")
                            .color(TextColor.fromHexString("#FFD700"))));

            Component creationMenuCommand = Component.text("/npc creationmenu ")
                    .color(TextColor.fromHexString("#FFD700"))
                    .hoverEvent(HoverEvent.showText(Component.text("Opens the creation menu!")
                            .color(TextColor.fromHexString("#FFD700"))));

            Component presetCommand = Component.text("/npc preset <preset> ")
                    .color(TextColor.fromHexString("#FFD700"))
                    .hoverEvent(HoverEvent.showText(Component.text("Spawns an NPC preset!")
                            .color(TextColor.fromHexString("#FFD700"))));


            p.sendMessage(header);
            p.sendMessage(createCommand);
            p.sendMessage(modifyCommand);
            p.sendMessage(removeCommand);
            p.sendMessage(listCommand);
            p.sendMessage(chatCommand);
            p.sendMessage(removeAllCommand);
            p.sendMessage(poseCommand);
            p.sendMessage(skinCommand);
            p.sendMessage(blacklistCommand);
            p.sendMessage(particleCommand);
            p.sendMessage(helpCommand);
            p.sendMessage(configCommand);
            p.sendMessage(interactCommand);
            p.sendMessage(menuCommand);
            p.sendMessage(itemCommand);
            p.sendMessage(creationMenuCommand);
            p.sendMessage(presetCommand);

            return true;
        } else if (args[0].equalsIgnoreCase("config")) {
            if (args.length != 1) {
                player.sendMessage("Usage: /npc config");
                return false;
            }

            ConfigurationMenu menu = new ConfigurationMenu("Config", 9 * 3, player);
            menu.open(player );
            Bukkit.getPluginManager().registerEvents(menu, getInstance());

            return true;
        } else if (args[0].equalsIgnoreCase("interact")) {
            if (args.length < 3) {
                player.sendMessage("Usage: /npc interact <npc_id> <add/remove> [type] [value]");
                return false;
            }

            NPC npc = getNpc(args[1]);
            if (npc == null) {
                player.sendMessage("Invalid NPC ID: " + args[1]);
                return false;
            }

            switch (args[2].toLowerCase()) {
                case "add" -> {
                    if (args.length < 5) {
                        player.sendMessage("Usage: /npc interact <npc_id> add <type> <value>");
                        return false;
                    }

                    switch (args[3].toLowerCase()) {
                        case "chat" -> {
                            String message = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                            Consumer<Player> func = playerSender -> npc.chat(message);

                            NPCInteractFunction interactFunc = new NPCInteractFunction(
                                    func, npc, CHAT, message
                            );

                            npc.addInteractFunction(interactFunc);
                            player.sendMessage("Added chat interaction to NPC.");
                        }
                        case "command" -> {
                            String cmd = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                            Consumer<Player> func = playerSender -> playerSender.performCommand(cmd);

                            NPCInteractFunction interactFunc = new NPCInteractFunction(
                                    func, npc, COMMAND, cmd
                            );

                            npc.addInteractFunction(interactFunc);
                            player.sendMessage("Added command interaction to NPC.");
                        }
                        case "menu" -> {
                            String menuName = args[4];
                            Menu menu;
                            if (menus.containsKey(menuName)) {
                                menu = menus.get(menuName);
                            } else {
                                player.sendMessage("Invalid menu name: " + args[4]);
                                return false;
                            }

                            Consumer<Player> func = menu::open;

                            NPCInteractFunction interactFunc = new NPCInteractFunction(
                                    func, npc, MENU, menuName
                            );

                            npc.addInteractFunction(interactFunc);
                            player.sendMessage("Added menu interaction to NPC.");
                        }
                        default -> {
                            player.sendMessage("Invalid interaction type: " + args[3]);
                            return false;
                        }
                    }
                }
                case "remove" -> {
                    if (args.length < 4) {
                        player.sendMessage("Usage: /npc interact <npc_id> remove <action_id>");
                        return false;
                    }

                    List<NPCInteractFunction> interactFunctions = npc.getInteractFunctions();
                    if (interactFunctions == null || interactFunctions.isEmpty()) {
                        player.sendMessage("Error: This NPC has no interact actions.");
                        return false;
                    }

                    try {
                        int actionId = Integer.parseInt(args[3]);
                        if (actionId < 0 || actionId >= interactFunctions.size()) {
                            player.sendMessage("Invalid action ID: " + actionId);
                            return false;
                        }

                        interactFunctions.remove(actionId);
                        player.sendMessage("Removed action: " + actionId);
                    } catch (NumberFormatException e) {
                        player.sendMessage("Error: Action ID must be a number.");
                        return false;
                    }
                }
                default -> {
                    player.sendMessage("Invalid action: " + args[2]);
                    return false;
                }
            }

            return true;
        } else if (args[0].equalsIgnoreCase("menu")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /npc menu <create|delete|setitem> [menuName] [title] [slots]");
                return true;
            }

            String subCommand = args[1].toLowerCase();
            String menuName = args[2];
            if(menuName == null) {
                player.sendMessage("Invalid menu name.");
                return false;
            }

            switch (subCommand) {
                case "create":
                    if (args.length < 5) {
                        sender.sendMessage(ChatColor.RED + "Usage: /npc menu create <menuName> <title> <slots>");
                        return true;
                    }
                    String title = args[3];
                    int slots;

                    // Validate the number of slots
                    try {
                        slots = Integer.parseInt(args[4]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid number for slots.");
                        return true;
                    }

                    // Create the menu
                    HashMap<Integer, ItemStack> items = new HashMap<>();
                    // No items set by default, you can later add logic to populate the menu

                    // Create the menu and save it
                    createMenu(title, items, slots, player, menuName);
                    sender.sendMessage(ChatColor.GREEN + "Menu '" + menuName + "' with title '" + title + "' and " + slots + " slots has been created.");
                    break;

                case "delete":
                    String deleteMenu = args[2];

                    // Delete menu
                    deleteMenu(deleteMenu);
                    sender.sendMessage(ChatColor.YELLOW + "Menu '" + deleteMenu + "' has been deleted.");
                    break;

                case "setitem":
                    if (args.length < 4) {
                        sender.sendMessage(ChatColor.RED + "Usage: /npc menu setitem <menuName> <slot> (item in mainhand will be set in that slot)");
                        return true;
                    }

                    Menu menuToEdit = getMenu(menuName); // Get the menu by name
                    if (menuToEdit == null) {
                        sender.sendMessage(ChatColor.RED + "Menu '" + menuName + "' does not exist.");
                        return true;
                    }

                    int slot;
                    try {
                        slot = Integer.parseInt(args[3]); // Get the slot from the args
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid slot number: " + args[3]);
                        return true;
                    }

                    // Ensure the player has a valid item in their main hand
                    if (player.getInventory().getItemInMainHand().getType().equals(Material.AIR)) {
                        player.sendMessage(ChatColor.RED + "Please hold a valid item in your main hand!");
                        return true;
                    }

                    // Set the item in the specified slot
                    setItemForMenu(menuName, slot, player.getInventory().getItemInMainHand());
                    menus.get(menuName).refresh();
                    player.sendMessage(ChatColor.GREEN + "Item set in slot " + slot + " for menu '" + menuName + "'.");

                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Invalid subcommand. Use /npc menu <create|delete|edit>.");
                    break;
            }
            return true;
        } else if(args[0].equalsIgnoreCase("item")) {
            if(args.length < 3) {
                player.sendMessage("Usage: /npc item <create|get> [itemname] [itemtype] [displayname] [glowing] [lore]");
                return false;
            }

            String action = args[1];
            if(!Objects.equals(action, "create") && !Objects.equals(action, "get")) {
                player.sendMessage("Usage: /npc item <create|get> [itemname] [itemtype] [displayname] [glowing] [lore]");
                return false;
            }

            switch (action) {
                case "create" -> {
                    if (args.length != 7) {
                        player.sendMessage("Usage: /npc item create [itemname] [itemtype] [displayName] [glowing] [lore]");
                        return false;
                    }

                    String itemName = args[2];
                    String displayName = ChatColor.translateAlternateColorCodes('&', args[4]);

                    if (!Arrays.stream(Material.values()).map(Material::name).toList().contains(args[3])) {
                        player.sendMessage("Invalid material type: " + args[3]);
                        return false;
                    }

                    Material itemType = Material.valueOf(args[3].toUpperCase());

                    boolean glowing;
                    try {
                        glowing = Boolean.parseBoolean(args[5]);
                    } catch (Exception e) {
                        player.sendMessage("Invalid glowing value: " + args[5] + ". Use true or false.");
                        return false;
                    }

                    List<String> lore = Arrays.asList(args[6].split("\\|")); // Lore separated by '|'

                    // Create and store the item
                    Item item = new Item(itemName, displayName, lore.stream().map(value -> ChatColor.translateAlternateColorCodes('&', value)).toList(), glowing, itemType);
                    items.put(itemName, item);

                    player.sendMessage("Item '" + itemName + "' created successfully!");
                }
                case "get" -> {
                    if(args.length != 3) {
                        player.sendMessage("Usage: /npc item get [itemname]");
                        return false;
                    }

                    String itemName = args[2];

                    if(!items.containsKey(itemName)) {
                        player.sendMessage("Invalid item name: " + itemName);
                        return false;
                    }

                    Item item = items.get(itemName);
                    item.give(player);
                }
            }
            return true;
        } else if (args[0].equalsIgnoreCase("moveTo")) {
            if (args.length != 5) {
                player.sendMessage("Usage: /npc moveTo <npc_id> <x> <y> <z>");
                return false;
            }

            NPC npc = getNpc(args[1]);
            if(npc == null) {
                player.sendMessage("Invalid NPC Id: " + args[1]);
                return false;
            }

            try {
                double x = Double.parseDouble(args[2]);
                double y = Double.parseDouble(args[3]);
                double z = Double.parseDouble(args[4]);

                // Teleport NPC to the specified coordinates
                Location location = new Location(player.getWorld(), x, y, z);

                npc.moveTo(location);

                player.sendMessage("NPC moved to the specified coordinates!");
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid coordinates!");
            }
            return true;
        } else if(args[0].equalsIgnoreCase("creationmenu")) {
            if(!(args.length == 1)) {
                player.sendMessage("Usage: /npc creationmenu");
                return false;
            }

            NPCCreationMenu menu = new NPCCreationMenu(9*3, "Npc creation");
            menu.open(player);

            return true;
        } /*else if(args[0].equalsIgnoreCase("glow")) {
            if(args.length != 2) {
                player.sendMessage("Usage: /npc glow <npc_id>");
                return false;
            }

            NPC npc = getNpc(args[1]);
            if (npc == null) {
                player.sendMessage("Invalid NPC ID: " + args[1]);
                return false;
            }

            npc.toggleGlowing();

            return true;
        }*/
        player.sendMessage("Invalid subcommand. Use /npc create, /npc modify, /npc remove, /npc list, /npc chat or another subcommand.");
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return List.of(
                    "create",
                    "modify",
                    "remove",
                    "list",
                    "chat",
                    "removeall",
                    "pose",
                    "skin",
                    "blacklist",
                    "particle",
                    "help",
                    "config",
                    "interact",
                    "menu",
                    "item",
                    "moveto",
                    "creationmenu"
            );
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
        } else if (args[0].equalsIgnoreCase("pose")) {
            switch (args.length) {
                case 2 -> {
                    return npcs.keySet().stream().toList();
                }
                case 3 -> {
                    return List.of("CROUCHING", "SLEEPING", "DEFAULT");
                }
            }
        } else if (args[0].equalsIgnoreCase("skin")) {
            if (args.length == 2) {
                return npcs.keySet().stream().toList();
            } else if (args.length == 3) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            }
        } else if (args[0].equalsIgnoreCase("blacklist") && args.length == 2) {
            return List.of("remove", "add");
        } else if (args[0].equalsIgnoreCase("particle")) {
            if (args.length == 2) {
                return npcs.keySet().stream().toList();
            } else if (args.length == 3) {
                return Arrays.stream(Particle.values()).map(Enum::name).toList();
            }
        } else if (args[0].equalsIgnoreCase("interact")) {
            if (args.length == 2) {
                return npcs.keySet().stream().toList();
            } else if (args.length == 3) {
                return List.of("add", "remove");
            } else if (args.length == 4 && args[2].equalsIgnoreCase("add")) {
                return List.of("chat", "command", "menu");
            } else if (args.length == 4 && args[2].equalsIgnoreCase("remove")) {
                List<Integer> thing = new ArrayList<>();
                if (getNpc(args[1]) == null) {
                    return List.of();
                }
                for (int i = 0; i < getNpc(args[1]).getInteractFunctions().size(); i++) {
                    thing.add(i);
                }
                return thing.stream().map(String::valueOf).toList();
            } else if (args.length == 5 && args[3].equalsIgnoreCase("menu")) {
                return menus.keySet().stream().toList();
            }
        } else if (args[0].equalsIgnoreCase("menu")) {
            if (args.length == 2) {
                return List.of("create", "delete", "setitem");
            } else if (args.length == 3) {
                if (args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("setitem")) {
                    return menus.keySet().stream().toList();
                }
            } else if (args.length == 4) {
                if (args[1].equalsIgnoreCase("setitem")) {
                    List<String> results = new ArrayList<>();
                    for (int i = 0; i < 53; i++) {
                        results.add("" + i);
                    }
                }
            } else if (args.length == 5) {
                return List.of("9", "18", "27", "36", "45", "54");
            }
        } else if (args[0].equalsIgnoreCase("item")) {
            switch(args[1]) {
                case "get" -> {
                    if (args.length == 3) {
                        return items.keySet().stream()
                                .toList();
                    }
                }
                case "create" -> {
                    if (args.length == 4) {
                        return Arrays.stream(Material.values())
                                .map(Material::name)
                                .toList();
                    } else if (args.length == 6) {
                        return List.of("true", "false");
                    } else if (args.length == 7) {
                        return List.of("| Creates a new line");
                    }
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("moveto")) {
            return npcs.keySet().stream()
                    .toList();
        }

        return List.of();
    }
    public static NPC getNpc(String arg) {
        return npcs.get(arg);
    }
}