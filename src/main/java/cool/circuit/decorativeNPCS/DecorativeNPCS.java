package cool.circuit.decorativeNPCS;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.EnumWrappers;
import cool.circuit.decorativeNPCS.commands.NPCCommand;
import cool.circuit.decorativeNPCS.menusystem.Menu;
import cool.circuit.decorativeNPCS.obj.Config;
import cool.circuit.decorativeNPCS.obj.NPCInteractFunction;
import cool.circuit.decorativeNPCS.obj.NPCInteractType;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static cool.circuit.decorativeNPCS.managers.ItemManager.loadItems;
import static cool.circuit.decorativeNPCS.managers.ItemManager.saveItems;
import static cool.circuit.decorativeNPCS.managers.MenuManager.loadMenus;
import static cool.circuit.decorativeNPCS.managers.MenuManager.saveMenus;

public final class DecorativeNPCS extends JavaPlugin implements Listener {

    private static DecorativeNPCS instance;

    public static final HashMap<String, Menu> menus = new HashMap<>();

    public static final HashMap<String, Item> items = new HashMap<>();

    public static final Map<String, NPC> npcs = new ConcurrentHashMap<>();

    public static final List<String> blacklist = new ArrayList<>();

    public static final NPCInteractType CHAT = new NPCInteractType("Chat");
    public static final NPCInteractType COMMAND = new NPCInteractType("Command");
    public static final NPCInteractType MENU = new NPCInteractType("Menu");

    public static File menusFile;
    public static FileConfiguration menusFileConfig;

    public static File itemsFile;
    public static FileConfiguration itemsFileConfig;

    public static Config cfg;

    public static NamespacedKey KEY;

    private BukkitAudiences adventure;

    public @NonNull BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic

        menusFile = new File(this.getDataFolder(), "menus.yml");

        if (!menusFile.exists()) {
            try {
                if (menusFile.createNewFile()) {
                    getLogger().info("menus.yml created successfully!");
                }
            } catch (IOException e) {
                getLogger().severe("Failed to create menus.yml: " + e.getMessage());
            }
        }

        menusFileConfig = YamlConfiguration.loadConfiguration(menusFile);

        itemsFile = new File(this.getDataFolder(), "items.yml");

        if (!itemsFile.exists()) {
            try {
                if (itemsFile.createNewFile()) {
                    getLogger().info("menus.yml created successfully!");
                }
            } catch (IOException e) {
                getLogger().severe("Failed to create items.yml: " + e.getMessage());
            }
        }

        itemsFileConfig = YamlConfiguration.loadConfiguration(itemsFile);

        // Load menus from the config
        loadMenus();  // Make sure this works if menusFileConfig is valid
        loadItems();

        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        protocolManager.addPacketListener(new PacketAdapter(
                this,
                PacketType.Play.Client.USE_ENTITY
        ) {
            @Override
            public void onPacketReceiving(PacketEvent packetEvent) {
                int entityId = packetEvent.getPacket().getIntegers().read(0);
                EnumWrappers.EntityUseAction action = packetEvent.getPacket().getEnumEntityUseActions().read(0).getAction();

                if (action == EnumWrappers.EntityUseAction.ATTACK) {
                    npcs.values().stream()
                            .filter(n -> n.getId() == entityId)
                            .findFirst().ifPresent(npc -> Bukkit.getScheduler().runTask(instance, () -> npc.interact(packetEvent.getPlayer())));
                }
            }
        });


        loadConfigObj();

        if (cfg == null) {
            cfg = new Config();
        }

        this.adventure = BukkitAudiences.create(this);

        Bukkit.getScheduler().runTask(this, () -> KEY = new NamespacedKey(DecorativeNPCS.getInstance(), "stand"));

        instance = this;
        // Plugin startup logic
        getCommand("npc").setExecutor(new NPCCommand());

        NPC.loadNPCs(getConfig());

        Bukkit.getPluginManager().registerEvents(this, this);

        Bukkit.getScheduler().runTask(this, () -> {
            for (NPC npc : npcs.values()) {
                npc.spawn();
            }
        });

        if (getConfig().contains("blacklist")) {
            blacklist.clear();
            blacklist.addAll(getConfig().getStringList("blacklist"));
        } else {
            getConfig().set("blacklist", blacklist);
        }
        loadInteractFunctions();
    }

    @Override
    public void onDisable() {
        saveMenus();
        saveItems();
        NPC.saveNPCs(getConfig());
        saveInteractFunctions();
        Iterator<NPC> iterator = npcs.values().iterator();
        while (iterator.hasNext()) {
            NPC npc = iterator.next();
            // Perform cleanup tasks if needed
            npc.remove();
            iterator.remove(); // Safely remove the NPC from the collection
        }
        npcs.clear();

        getConfig().set("blacklist", blacklist);
        saveConfig();
        saveConfigObj();
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
        saveInteractFunctions();
    }

    public static DecorativeNPCS getInstance() {
        return instance;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        NPC.loadNPCs(getConfig());
        for (NPC npc : npcs.values()) {
            new BukkitRunnable() {

                @Override
                public void run() {
                    npc.spawnForPlayer(player);
                    npc.setSkin(npc.skinOwnerName);
                    loadInteractFunctions();
                }
            }.runTaskLater(this, 20L);
        }
    }

    public static void saveConfiguration() {
        instance.saveConfig();
    }

    public static FileConfiguration getConfiguration() {
        return instance.getConfig();
    }

    private void saveConfigObj() {
        getConfig().set("config.require-permissions", cfg.isRequirePerm());
        getConfig().set("config.do-blacklist", cfg.isDoBlacklist());
        getConfig().set("config.npc-owner-lock", cfg.isNPCOwnerLock());

        saveConfig();
    }


    private void loadConfigObj() {
        cfg = new Config();

        boolean requirePerms = getConfig().getBoolean("config.require-permissions", true);
        boolean doBlacklist = getConfig().getBoolean("config.do-blacklist", true);
        boolean NPCOwnerLock = getConfig().getBoolean("config.npc-owner-lock", true);

        cfg.setDoBlacklist(doBlacklist);
        cfg.setRequirePerm(requirePerms);
        cfg.setNPCOwnerLock(NPCOwnerLock);
    }

    private void saveInteractFunctions() {
        ConfigurationSection section = getConfig().createSection("interact_funcs");
        for(NPC npc : npcs.values()) {
            for(NPCInteractFunction func : npc.getInteractFunctions()) {
                ConfigurationSection npcSection = section.createSection(npc.getName());
                npcSection.set("type", func.getType().name());
                npcSection.set("value", func.getValue());
                npcSection.set("npc_name", func.getNpc().getName());

                // Debugging log
                getLogger().info("Saving interact function for NPC: " + npc.getName() + " Type: " + func.getType().name());
            }
        }
    }


    private void loadInteractFunctions() {
        ConfigurationSection section = getConfig().getConfigurationSection("interact_funcs");
        if (section == null) return; // No data to load

        for (String npcName : section.getKeys(false)) {
            ConfigurationSection npcSection = section.getConfigurationSection(npcName);
            if (npcSection == null) continue;

            String typeString = npcSection.getString("type", "chat");
            String value = npcSection.getString("value");
            String npcNameFromConfig = npcSection.getString("npc_name");

            NPCInteractType type; // Default to chat
            if (typeString.equalsIgnoreCase("chat")) {
                type = CHAT;
            } else if (typeString.equalsIgnoreCase("command")) {
                type = COMMAND;
            } else if(typeString.equalsIgnoreCase("menu")) {
                type = MENU;
            } else {
                type = CHAT;
                getLogger().warning("Unknown interact function type: " + typeString);
            }

            NPC npc = npcs.get(npcNameFromConfig); // Retrieve NPC by name
            if (npc != null) {
                NPCInteractFunction func = new NPCInteractFunction((player) -> {
                    if (type == CHAT) {
                        npc.chat(value); // Chat interaction
                    } else if (type == COMMAND) {
                        if (value != null) {
                            player.performCommand(value); // Command interaction
                        }
                    } else {
                        if (menus.containsKey(value)) {
                            Menu menu = menus.get(value);
                            menu.open(player);
                        }
                    }
                }, npc, type, value);

                npc.addInteractFunction(func);

                // Debugging log to check what gets loaded
                getLogger().info("Loaded interact function for NPC: " + npcName + " Type: " + typeString);
            }
        }
    }

    public static Config getConfigObj() {
        return cfg;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if(event.getInventory().getHolder() instanceof Menu menu) {
            menu.handleClicks(event);
        }
    }
}
