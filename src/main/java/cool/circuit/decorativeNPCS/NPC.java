package cool.circuit.decorativeNPCS;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.datafixers.util.Pair;
import cool.circuit.decorativeNPCS.utils.NPCSkinFetcher;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_21_R3.CraftServer;
import org.bukkit.craftbukkit.v1_21_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static cool.circuit.decorativeNPCS.DecorativeNPCS.getInstance;
import static cool.circuit.decorativeNPCS.DecorativeNPCS.npcs;
import static cool.circuit.decorativeNPCS.managers.PacketManager.sendPacket;
import static cool.circuit.decorativeNPCS.managers.PacketManager.setValue;

public class NPC {
    public final String displayName;
    public final String name;
    private final EntityType type;

    public Player owner;

    private final Location location;
    private Entity entity;
    private ServerPlayer serverPlayer;

    private ItemStack helmet = null;
    private ItemStack chestplate = null;
    private ItemStack leggings = null;
    private ItemStack boots = null;
    private ItemStack mainhand = null;

    private NPCSkinFetcher.SkinData data = null;

    private String skinOwnerName = null;

    private Pose pose;

    public Location getLocation() {
        return location;
    }

    public NPC(String displayName, String name, EntityType type, Player player, Location location) {
        this.displayName = displayName;
        this.name = name;
        this.type = type;
        this.owner = player;
        this.location = location;
        this.pose = Pose.STANDING;
    }

    public void spawn() {
        Location location = owner.getLocation();
        ServerLevel serverLevel = ((CraftWorld) location.getWorld()).getHandle();

        if (type != EntityType.PLAYER) {
            entity = type.create(serverLevel, EntitySpawnReason.COMMAND);
            if (entity == null) {
                owner.sendMessage("Could not create entity: " + type);
                return;
            }

            entity.setPos(location.getX(), location.getY(), location.getZ());
            entity.setYHeadRot(location.getYaw());
            entity.setYBodyRot(location.getYaw());
            entity.setYRot(location.getYaw());
            entity.setXRot(location.getPitch());
            entity.setCustomName(Component.literal(displayName));

            SynchedEntityData entityData = entity.getEntityData();
            entityData.set(EntityDataSerializers.OPTIONAL_COMPONENT.createAccessor(2), Optional.of(Component.literal(displayName)));

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                sendPacket(new ClientboundAddEntityPacket(
                        entity.getId(),
                        entity.getUUID(),
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        location.getYaw(),
                        location.getPitch(),
                        type,
                        0,
                        Vec3.ZERO,
                        entity.getYRot()
                ), onlinePlayer);

                sendPacket(new ClientboundSetEntityDataPacket(
                        entity.getId(),
                        entityData.getNonDefaultValues()
                ), onlinePlayer);
            }
        } else {
            MinecraftServer minecraftServer = ((CraftServer) Bukkit.getServer()).getServer();
            GameProfile gameProfile = new GameProfile(UUID.randomUUID(), name);
            if(skinOwnerName != null) {
                setSkin(skinOwnerName);
            }
            serverPlayer = new ServerPlayer(minecraftServer, serverLevel, gameProfile, ClientInformation.createDefault());
            serverPlayer.setPos(location.getX(), location.getY(), location.getZ());
            serverPlayer.setYHeadRot(location.getYaw());
            serverPlayer.setYBodyRot(location.getYaw());
            serverPlayer.setYRot(location.getYaw());
            serverPlayer.setXRot(location.getPitch());



            SynchedEntityData synchedEntityData = serverPlayer.getEntityData();
            synchedEntityData.set(new EntityDataAccessor<>(17, EntityDataSerializers.BYTE), (byte) 127);

            setValue(serverPlayer, "f", ((CraftPlayer) owner).getHandle().connection);

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                sendPacket(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, serverPlayer), onlinePlayer);
                ServerEntity se = new ServerEntity(serverPlayer.serverLevel(), serverPlayer, 0, false, packet -> {
                }, Set.of());
                Packet<?> packet = serverPlayer.getAddEntityPacket(se);
                sendPacket(packet, onlinePlayer);
                sendPacket(new ClientboundSetEntityDataPacket(serverPlayer.getId(), synchedEntityData.getNonDefaultValues()), onlinePlayer);
            }
        }
        this.setPose(pose);
    }


    public void remove() {
        if (entity != null) {
            entity.discard();

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                sendPacket(new ClientboundRemoveEntitiesPacket(entity.getId()), onlinePlayer);
            }

            entity = null;
        } else if(serverPlayer != null) {
            serverPlayer.discard();
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                sendPacket(new ClientboundPlayerInfoRemovePacket(List.of(serverPlayer.getUUID())), onlinePlayer);
                sendPacket(new ClientboundRemoveEntitiesPacket(serverPlayer.getId()), onlinePlayer);
            }

            serverPlayer = null;

        }
        npcs.remove(name);
    }


    public ItemStack getHelmet() {
        return helmet;
    }

    public ItemStack getChestplate() {
        return chestplate;
    }

    public ItemStack getLeggings() {
        return leggings;
    }

    public ItemStack getBoots() {
        return boots;
    }

    public void setEquipment(ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots) {
        if (helmet != null) this.helmet = helmet;
        if (chestplate != null) this.chestplate = chestplate;
        if (leggings != null) this.leggings = leggings;
        if (boots != null) this.boots = boots;

        net.minecraft.world.item.ItemStack nmsHelmet = CraftItemStack.asNMSCopy(helmet);
        net.minecraft.world.item.ItemStack nmsChestplate = CraftItemStack.asNMSCopy(chestplate);
        net.minecraft.world.item.ItemStack nmsLeggings = CraftItemStack.asNMSCopy(leggings);
        net.minecraft.world.item.ItemStack nmsBoots = CraftItemStack.asNMSCopy(boots);
        net.minecraft.world.item.ItemStack nmsMainhand = CraftItemStack.asNMSCopy(mainhand);

        if (entity instanceof Mob mob) {
            mob.setItemSlot(EquipmentSlot.HEAD, nmsHelmet);
            mob.setItemSlot(EquipmentSlot.CHEST, nmsChestplate);
            mob.setItemSlot(EquipmentSlot.LEGS, nmsLeggings);
            mob.setItemSlot(EquipmentSlot.FEET, nmsBoots);
            mob.setItemSlot(EquipmentSlot.MAINHAND, nmsMainhand);

            List<@NotNull Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> equipmentList = List.of(
                    Pair.of(EquipmentSlot.HEAD, nmsHelmet),
                    Pair.of(EquipmentSlot.CHEST, nmsChestplate),
                    Pair.of(EquipmentSlot.LEGS, nmsLeggings),
                    Pair.of(EquipmentSlot.FEET, nmsBoots),
                    Pair.of(EquipmentSlot.MAINHAND, nmsMainhand)
            ).reversed();

            // Send to all online players
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                sendPacket(new ClientboundSetEquipmentPacket(mob.getId(), equipmentList), onlinePlayer);
            }
        } else if (serverPlayer != null) {
            serverPlayer.setItemSlot(EquipmentSlot.HEAD, nmsHelmet);
            serverPlayer.setItemSlot(EquipmentSlot.CHEST, nmsChestplate);
            serverPlayer.setItemSlot(EquipmentSlot.LEGS, nmsLeggings);
            serverPlayer.setItemSlot(EquipmentSlot.FEET, nmsBoots);
            serverPlayer.setItemSlot(EquipmentSlot.MAINHAND, nmsMainhand);

            List<@NotNull Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> equipmentList = List.of(
                    Pair.of(EquipmentSlot.HEAD, nmsHelmet),
                    Pair.of(EquipmentSlot.CHEST, nmsChestplate),
                    Pair.of(EquipmentSlot.LEGS, nmsLeggings),
                    Pair.of(EquipmentSlot.FEET, nmsBoots),
                    Pair.of(EquipmentSlot.MAINHAND, nmsMainhand)
            );

            // Send to all online players
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                sendPacket(new ClientboundSetEquipmentPacket(serverPlayer.getId(), equipmentList), onlinePlayer);
            }
        }
    }


    public void spawnForPlayer(Player p) {
        Location location = getLocation();
        ServerLevel serverLevel = ((CraftWorld) location.getWorld()).getHandle();

        if (type != EntityType.PLAYER) {
            // Create and spawn an NPC as a regular entity
            entity = type.create(serverLevel, EntitySpawnReason.COMMAND);
            if (entity == null) {
                p.sendMessage("Could not create entity: " + type);
                return;
            }

            entity.setPos(location.getX(), location.getY(), location.getZ());
            // Set the NPC's yaw (side-to-side direction)
            entity.setYHeadRot(location.getYaw());
            entity.setYBodyRot(location.getYaw());
            entity.setYRot(location.getYaw());
            entity.setXRot(location.getPitch());
            entity.setCustomName(Component.literal(displayName));

            SynchedEntityData entityData = entity.getEntityData();
            entityData.set(EntityDataSerializers.OPTIONAL_COMPONENT.createAccessor(2), Optional.of(Component.literal(displayName)));

            // Send spawn packets for the entity
            sendPacket(new ClientboundAddEntityPacket(
                    entity.getId(),
                    entity.getUUID(),
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    location.getYaw(),
                    location.getPitch(),
                    type,
                    0,
                    Vec3.ZERO,
                    entity.getYRot()
            ), p);

            sendPacket(new ClientboundSetEntityDataPacket(
                    entity.getId(),
                    entityData.getNonDefaultValues()
            ), p);

            // Send equipment packet for the entity
            sendEquipmentPacket(entity.getId(), p);
        } else {
            // Create and spawn an NPC as a player
            MinecraftServer minecraftServer = ((CraftServer) Bukkit.getServer()).getServer();
            GameProfile gameProfile = new GameProfile(UUID.randomUUID(), name);
            if(skinOwnerName != null) {
                setSkin(skinOwnerName);
            }


            serverPlayer = new ServerPlayer(minecraftServer, serverLevel, gameProfile, ClientInformation.createDefault());
            serverPlayer.setPos(location.getX(), location.getY(), location.getZ());
            serverPlayer.setYHeadRot(location.getYaw());
            serverPlayer.setYBodyRot(location.getYaw());
            serverPlayer.setYRot(location.getYaw());
            serverPlayer.setXRot(location.getPitch());

            SynchedEntityData synchedEntityData = serverPlayer.getEntityData();
            synchedEntityData.set(new EntityDataAccessor<>(17, EntityDataSerializers.BYTE), (byte) 127);

            setValue(serverPlayer, "f", ((CraftPlayer) p).getHandle().connection);

            // Send player info update and spawn packet for the player NPC
            sendPacket(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, serverPlayer), p);

            ServerEntity se = new ServerEntity(serverPlayer.serverLevel(), serverPlayer, 0, false, packet -> {
            }, Set.of());

            Packet<?> packet = serverPlayer.getAddEntityPacket(se);
            sendPacket(packet, p);
            sendPacket(new ClientboundSetEntityDataPacket(serverPlayer.getId(), synchedEntityData.getNonDefaultValues()), p);

            // Send equipment packet for the player NPC
            sendEquipmentPacket(serverPlayer.getId(), p);
        }
        setPose(pose);
    }

    // Helper method to send the equipment packet for the entity/player
    private void sendEquipmentPacket(int entityId, Player p) {
        net.minecraft.world.item.ItemStack nmsHelmet = CraftItemStack.asNMSCopy(helmet);
        net.minecraft.world.item.ItemStack nmsChestplate = CraftItemStack.asNMSCopy(chestplate);
        net.minecraft.world.item.ItemStack nmsLeggings = CraftItemStack.asNMSCopy(leggings);
        net.minecraft.world.item.ItemStack nmsBoots = CraftItemStack.asNMSCopy(boots);
        net.minecraft.world.item.ItemStack nmsMainhand = CraftItemStack.asNMSCopy(mainhand);

        List<@NotNull Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> equipmentList = List.of(
                Pair.of(EquipmentSlot.HEAD, nmsHelmet),
                Pair.of(EquipmentSlot.CHEST, nmsChestplate),
                Pair.of(EquipmentSlot.LEGS, nmsLeggings),
                Pair.of(EquipmentSlot.FEET, nmsBoots),
                Pair.of(EquipmentSlot.MAINHAND, nmsMainhand)
        );

        sendPacket(new ClientboundSetEquipmentPacket(entityId, equipmentList), p);
    }

    public static void saveNPCs(FileConfiguration config) {
        for (NPC npc : npcs.values()) {
            ConfigurationSection npcSection = config.createSection(npc.name);

            npcSection.set("displayName", npc.displayName);
            npcSection.set("pose", npc.pose.name());
            npcSection.set("name", npc.name);
            npcSection.set("type", EntityType.getKey(npc.type).toString().toUpperCase().replace("MINECRAFT:", "")); // Ensures correct format
            npcSection.set("location.world", npc.location.getWorld().getName());
            npcSection.set("location.x", npc.location.getX());
            npcSection.set("location.y", npc.location.getY());
            npcSection.set("location.z", npc.location.getZ());
            npcSection.set("location.yaw", npc.location.getYaw());
            npcSection.set("location.pitch", npc.location.getPitch());

            // Prevent NullPointerException if owner is missing
            if (npc.owner != null) {
                npcSection.set("ownerName", npc.owner.getName());
            }

            if(npc.skinOwnerName != null) {
                npcSection.set("skin.ownerName", npc.skinOwnerName);
            }

            // Save equipment if available
            if (npc.helmet != null) npcSection.set("equipment.helmet", npc.helmet);
            if (npc.chestplate != null) npcSection.set("equipment.chestplate", npc.chestplate);
            if (npc.leggings != null) npcSection.set("equipment.leggings", npc.leggings);
            if (npc.boots != null) npcSection.set("equipment.boots", npc.boots);
            if (npc.mainhand != null) npcSection.set("equipment.mainhand", npc.mainhand);
        }

        // Ensure configuration is saved properly
        DecorativeNPCS.saveConfiguration();
    }


    // Load NPCs from the file
    public static void loadNPCs(FileConfiguration config) {
        for (String key : config.getKeys(false)) {
            ConfigurationSection npcSection = config.getConfigurationSection(key);
            if (npcSection == null) continue; // Prevent null errors

            String displayName = npcSection.getString("displayName");
            String name = npcSection.getString("name");
            String ownerName = npcSection.getString("ownerName");
            String typeString = npcSection.getString("type");
            String poseString = npcSection.getString("pose");

            if (typeString == null) {
                continue;
            }

            if (poseString == null) {
                continue;
            }

            Pose pose = Pose.valueOf(poseString);
            // Ensure the entity type string is in the correct format
            Optional<EntityType<?>> optionalType = EntityType.byString(typeString.toLowerCase(Locale.ROOT));
            if (optionalType.isEmpty()) {
                continue;
            }

            EntityType<?> type = optionalType.get();

            World world;
            world = Bukkit.getWorld(npcSection.getString("location.world", Bukkit.getWorld("world").getName()));
            if (world == null) {
                continue;
            }

            Location location = new Location(world,
                    npcSection.getDouble("location.x"),
                    npcSection.getDouble("location.y"),
                    npcSection.getDouble("location.z"),
                    (float) npcSection.getDouble("location.yaw"),
                    (float) npcSection.getDouble("location.pitch"));

            Player player = null; // Adjust this based on how you handle NPCs

            NPC npc = new NPC(
                    displayName,
                    name,
                    type,
                    player,
                    location
            );
            npc.pose = pose;
            if (ownerName != null) {
                npc.setOwner(Bukkit.getPlayerExact(ownerName)); // Ensure your NPC class has this setter if needed
            }


            Bukkit.getScheduler().runTaskLater(getInstance(), () -> {
                String skinOwnerNameString = npcSection.getString("skin.ownerName");
                if (skinOwnerNameString != null) {
                    npc.data = NPCSkinFetcher.fetchSkin(skinOwnerNameString); // Fetch skin data based on owner name
                    SkinFetchResponse response = npc.setSkin(skinOwnerNameString); // Set skin for the NPC

                    // Handle different responses and provide feedback in the console
                    switch (response) {
                        case SUCCESS -> System.out.println("Skin loaded successfully");
                        case SKIN_VALUE_OR_SIGNATURE_IS_NULL -> System.out.println("Skin value or signature is null");
                        case NO_SERVER_PLAYER -> System.out.println("Server player is null");
                        case FAILED_TO_FETCH_SKIN -> System.out.println("Skin fetch failed");
                        default -> System.out.println("Unknown failure");
                    }
                }
            }, 20L); // 20L for a delay of 1 tick (adjust the delay as needed)



            // Load equipment
            if (npcSection.contains("equipment.helmet")) npc.helmet = (ItemStack) npcSection.get("equipment.helmet");
            if (npcSection.contains("equipment.chestplate"))
                npc.chestplate = (ItemStack) npcSection.get("equipment.chestplate");
            if (npcSection.contains("equipment.leggings"))
                npc.leggings = (ItemStack) npcSection.get("equipment.leggings");
            if (npcSection.contains("equipment.boots")) npc.boots = (ItemStack) npcSection.get("equipment.boots");
            if (npcSection.contains("equipment.mainhand"))
                npc.mainhand = (ItemStack) npcSection.get("equipment.mainhand");

            npcs.put(name, npc);
        }
    }


    public void setOwner(Player player) {
        this.owner = player;
    }

    public void setItemInMainHand(ItemStack item) {
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);

        List<@NotNull Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> equipmentList = List.of(
                Pair.of(EquipmentSlot.MAINHAND, nmsItem)
        );

        // Send to all online players

        if (serverPlayer != null) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                sendPacket(new ClientboundSetEquipmentPacket(serverPlayer.getId(), equipmentList), onlinePlayer);
            }
        } else {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                sendPacket(new ClientboundSetEquipmentPacket(entity.getId(), equipmentList), onlinePlayer);
            }
        }

        mainhand = item;
    }

    public void chat(String message) {
        ClientboundSystemChatPacket packet = new ClientboundSystemChatPacket(
                Component.literal(ChatColor.GOLD + "[" + displayName + "] " + message),
                false
        );

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            sendPacket(packet, onlinePlayer);
        }
    }

    public void setPose(Pose pose) {
        if (pose == null) return;
        if (serverPlayer != null && pose == Pose.CROUCHING) {
            serverPlayer.setPose(pose);
            SynchedEntityData dataWatcher = serverPlayer.getEntityData();
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                sendPacket(new ClientboundSetEntityDataPacket(serverPlayer.getId(), dataWatcher.getNonDefaultValues()), onlinePlayer);
            }
        } else if (pose != Pose.CROUCHING && entity != null) {
            entity.setPose(pose);
            SynchedEntityData dataWatcher = entity.getEntityData();
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                sendPacket(new ClientboundSetEntityDataPacket(entity.getId(), dataWatcher.getNonDefaultValues()), onlinePlayer);
            }
        } else {
            return;
        }
        this.pose = pose;
    }

    public SkinFetchResponse setSkin(String playerName) {
        if (serverPlayer != null) {
            data = NPCSkinFetcher.fetchSkin(playerName);
            if(data == null) return SkinFetchResponse.DATA_IS_NULL;
            String skinValue = data.value;
            String skinSignature = data.signature;

            if (skinValue == null || skinSignature == null) return SkinFetchResponse.SKIN_VALUE_OR_SIGNATURE_IS_NULL;

            remove();

            ServerLevel serverLevel = ((CraftWorld) location.getWorld()).getHandle();

            MinecraftServer minecraftServer = ((CraftServer) Bukkit.getServer()).getServer();
            GameProfile gameProfile = new GameProfile(UUID.randomUUID(), name);
            gameProfile.getProperties().remove("textures", null);
            gameProfile.getProperties().put("textures", new Property("textures", skinValue, skinSignature));


            serverPlayer = new ServerPlayer(minecraftServer, serverLevel, gameProfile, ClientInformation.createDefault());
            serverPlayer.setPos(location.getX(), location.getY(), location.getZ());
            serverPlayer.setYHeadRot(location.getYaw());
            serverPlayer.setYBodyRot(location.getYaw());
            serverPlayer.setYRot(location.getYaw());
            serverPlayer.setXRot(location.getPitch());

            SynchedEntityData synchedEntityData = serverPlayer.getEntityData();
            synchedEntityData.set(new EntityDataAccessor<>(17, EntityDataSerializers.BYTE), (byte) 127);

            if(((CraftPlayer) owner).getHandle().connection != null) {
                setValue(serverPlayer, "f", ((CraftPlayer) owner).getHandle().connection);
            } else {
                System.out.println("An error occurred. Please contact CircuitBoard on spigotmc.org.");
                return null;
            }

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                sendPacket(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, serverPlayer), onlinePlayer);
                ServerEntity se = new ServerEntity(serverPlayer.serverLevel(), serverPlayer, 0, false, packet -> {
                }, Set.of());
                Packet<?> packet = serverPlayer.getAddEntityPacket(se);
                sendPacket(packet, onlinePlayer);
                sendPacket(new ClientboundSetEntityDataPacket(serverPlayer.getId(), synchedEntityData.getNonDefaultValues()), onlinePlayer);
            }

            skinOwnerName = playerName;

            return SkinFetchResponse.SUCCESS;
        }
        return SkinFetchResponse.NO_SERVER_PLAYER;
    }
}
