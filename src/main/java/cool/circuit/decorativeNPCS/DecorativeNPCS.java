package cool.circuit.decorativeNPCS;

import cool.circuit.decorativeNPCS.commands.NPCCommand;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DecorativeNPCS extends JavaPlugin implements Listener {

    private static DecorativeNPCS instance;

    public static final Map<String, NPC> npcs = new ConcurrentHashMap<>();

    public static final List<String> blacklist = new ArrayList<>();

    @Override
    public void onEnable() {
        // Plugin startup logic

        instance = this;
        // Plugin startup logic
        getCommand("npc").setExecutor(new NPCCommand());

        NPC.loadNPCs(getConfig());

        Bukkit.getPluginManager().registerEvents(this, this);

        for(Player player : Bukkit.getOnlinePlayers()) {
            for(NPC npc : npcs.values()) {
                npc.spawnForPlayer(player);
            }
        }

        if (getConfig().contains("blacklist")) {
            blacklist.clear();
            blacklist.addAll(getConfig().getStringList("blacklist"));
        } else {
            getConfig().set("blacklist", blacklist);
        }
    }

    @Override
    public void onDisable() {
        NPC.saveNPCs(getConfig());
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

    }

    public static DecorativeNPCS getInstance() {
        return instance;
    }

    @EventHandler
    public void onPlayerLogin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for(NPC npc : npcs.values()) {
            new BukkitRunnable() {

                @Override
                public void run() {
                    npc.spawnForPlayer(player);
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
}
