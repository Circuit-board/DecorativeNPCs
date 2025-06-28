package cool.circuit.decorativeNPCS;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.datafixers.util.Pair;
import cool.circuit.decorativeNPCS.obj.NPCInteractFunction;
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
import org.bukkit.craftbukkit.v1_21_R5.CraftServer;
import org.bukkit.craftbukkit.v1_21_R5.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R5.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R5.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static cool.circuit.decorativeNPCS.DecorativeNPCS.*;
import static cool.circuit.decorativeNPCS.managers.PacketManager.sendPacket;
import static cool.circuit.decorativeNPCS.managers.PacketManager.setValue;

public class NPC {
    public final String displayName;
    public final String name;

    private final EntityType<?> type;

    public Player owner;

    private boolean isGlowing = false;

    private final Location location;
    private Entity entity;
    private ServerPlayer serverPlayer;

    private ItemStack helmet = null;
    private ItemStack chestplate = null;
    private ItemStack leggings = null;
    private ItemStack boots = null;
    private ItemStack mainhand = null;

    private NPCSkinFetcher.SkinData data = null;

    public String skinOwnerName = null;

    private Pose pose;

    private final List<NPCInteractFunction> interactFunctions = new ArrayList<>();

    public List<NPCInteractFunction> getInteractFunctions() {
        return interactFunctions;
    }

    public void addInteractFunction(NPCInteractFunction function) {
        this.interactFunctions.add(function);
    }

    public Location getLocation() {
        return location;
    }

    public NPC(String displayName, String name, EntityType<?> type, Player player, Location location) {
        this.displayName = displayName;
        this.name = name;
        this.type = type;
        this.owner = player;
        this.location = location;
        this.pose = Pose.STANDING;
    }

    public void spawn() {
        ServerLevel serverLevel = ((CraftWorld) location.getWorld()).getHandle();

        if (type != EntityType.PLAYER) {
            entity = type.create(serverLevel, EntitySpawnReason.COMMAND);
            if (entity == null) {
                owner.sendMessage("Could not create entity: " + type);
                return;
            }

            entity.setPos(location.getX(), location.getY(), location.getZ());
            entity.setYRot(location.getYaw());
            entity.setXRot(location.getPitch());
            entity.setYHeadRot(location.getYaw());
            entity.setYBodyRot(location.getYaw());

            entity.teleportTo(location.getX(), location.getY(), location.getZ());

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

                sendPacket(new ClientboundSetEntityDataPacket(
                        entity.getId(),
                        entity.getEntityData().getNonDefaultValues()
                ), onlinePlayer);

                // Force teleport packet to sync rotation
                ClientboundTeleportEntityPacket teleportPacket = getClientboundTeleportEntityPacket();

                sendPacket(teleportPacket, onlinePlayer);
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
                ServerEntity se = new ServerEntity(serverPlayer.level(), serverPlayer, 0, false, packet -> {
                }, (a, b) -> {}, Set.of());
                Packet<?> packet = serverPlayer.getAddEntityPacket(se);
                sendPacket(packet, onlinePlayer);
                sendPacket(new ClientboundSetEntityDataPacket(serverPlayer.getId(), synchedEntityData.getNonDefaultValues()), onlinePlayer);
            }
        }
        this.setPose(pose);
    }

    private @NotNull ClientboundTeleportEntityPacket getClientboundTeleportEntityPacket() {
        Vec3 position = new Vec3((serverPlayer != null ? serverPlayer : entity).getX(), (serverPlayer != null ? serverPlayer : entity).getY(), (serverPlayer != null ? serverPlayer : entity).getZ());

        // Create Vec3 for the velocity (you can set it to 0, 0, 0 if you're not updating velocity)
        Vec3 velocity = new Vec3(0, 0, 0);  // Assuming no velocity change

        // Get the yaw (horizontal rotation) and pitch (vertical rotation) of the entity
        float yaw = location.getYaw();
        float pitch = location.getPitch();

        // Create the PositionMoveRotation object
        PositionMoveRotation positionMoveRotation = new PositionMoveRotation(position, velocity, yaw, pitch);

        // Create an empty set of Relatives (you can modify this set if needed)
        Set<Relative> relativeSet = new HashSet<>();

        // Set the flag to true or false based on the teleportation context
        boolean flag = true; // You can change this depending on your use case

        // Send the packet
        ClientboundTeleportEntityPacket teleportPacket = new ClientboundTeleportEntityPacket(
                entity.getId(),
                positionMoveRotation,
                relativeSet,
                flag
        );
        return teleportPacket;
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


    public void spawnForPlayer(Player player) {
        ServerLevel serverLevel = ((CraftWorld) location.getWorld()).getHandle();

        if (type != EntityType.PLAYER) {
            entity = type.create(serverLevel, EntitySpawnReason.COMMAND);
            if (entity == null) {
                owner.sendMessage("Could not create entity: " + type);
                return;
            }

            entity.setPos(location.getX(), location.getY(), location.getZ());
            entity.setYRot(location.getYaw());
            entity.setXRot(location.getPitch());
            entity.setYHeadRot(location.getYaw());
            entity.setYBodyRot(location.getYaw());

            entity.teleportTo(location.getX(), location.getY(), location.getZ());

            SynchedEntityData entityData = entity.getEntityData();
            entityData.set(EntityDataSerializers.OPTIONAL_COMPONENT.createAccessor(2), Optional.of(Component.literal(displayName)));

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
                ), player);

                sendPacket(new ClientboundSetEntityDataPacket(
                        entity.getId(),
                        entityData.getNonDefaultValues()
                ), player);

                sendPacket(new ClientboundSetEntityDataPacket(
                        entity.getId(),
                        entity.getEntityData().getNonDefaultValues()
                ), player);

                // Force teleport packet to sync rotation
                Vec3 position = new Vec3((serverPlayer != null ? serverPlayer : entity).getX(), (serverPlayer != null ? serverPlayer : entity).getY(), (serverPlayer != null ? serverPlayer : entity).getZ());

                // Create Vec3 for the velocity (you can set it to 0, 0, 0 if you're not updating velocity)
                Vec3 velocity = new Vec3(0, 0, 0);  // Assuming no velocity change

                // Get the yaw (horizontal rotation) and pitch (vertical rotation) of the entity
                float yaw = location.getYaw();
                float pitch = location.getPitch();

                // Create the PositionMoveRotation object
                PositionMoveRotation positionMoveRotation = new PositionMoveRotation(position, velocity, yaw, pitch);

                // Create an empty set of Relatives (you can modify this set if needed)
                Set<Relative> relativeSet = new HashSet<>();

                // Set the flag to true or false based on the teleportation context
                boolean flag = true; // You can change this depending on your use case

                // Send the packet
                ClientboundTeleportEntityPacket teleportPacket = new ClientboundTeleportEntityPacket(
                        entity.getId(),
                        positionMoveRotation,
                        relativeSet,
                        flag
                );

                sendPacket(teleportPacket, player);
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

            if(((CraftPlayer) owner).getHandle().connection == null) {
                Bukkit.getScheduler().runTaskLater(getInstance(), () -> setValue(serverPlayer, "f", ((CraftPlayer) owner).getHandle().connection),30L);
            } else {
                setValue(serverPlayer, "f", ((CraftPlayer) owner).getHandle().connection);
            }

                sendPacket(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, serverPlayer), player);
                ServerEntity se = new ServerEntity(serverPlayer.level(), serverPlayer, 0, false, packet -> {
                }, (a, b) -> {}, Set.of());
                Packet<?> packet = serverPlayer.getAddEntityPacket(se);
                sendPacket(packet, player);
                sendPacket(new ClientboundSetEntityDataPacket(serverPlayer.getId(), synchedEntityData.getNonDefaultValues()), player);
        }
        this.setPose(pose);
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

            if(ownerName == null) {
                continue;
            }

            EntityType<?> type = optionalType.get();

            World world;
            world = Bukkit.getWorld(npcSection.getString("location.world", Bukkit.getWorld("world").getName()));
            if (world == null) {
                continue;
            }

            Location loc = new Location(world,
                    npcSection.getDouble("location.x"),
                    npcSection.getDouble("location.y"),
                    npcSection.getDouble("location.z"),
                    (float) npcSection.getDouble("location.yaw"),
                    (float) npcSection.getDouble("location.pitch"));

            Player player = Bukkit.getPlayer(ownerName); // Adjust this based on how you handle NPCs
            if(player == null) {
                continue;
            }

            NPC npc = new NPC(
                    displayName,
                    name,
                    type,
                    player,
                    loc
            );

            npc.setPose(pose);
            npc.setOwner(Bukkit.getPlayerExact(ownerName)); // Ensure your NPC class has this setter if needed


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
        } else if (entity != null ){
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

    public PoseResponse setPose(Pose pose) {
        if (pose == null) return PoseResponse.INVALID;
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
        } else if(pose == Pose.CROUCHING && entity != null) {
            return PoseResponse.ONLY_WORKS_FOR_PLAYERS;
        } if(!(pose == Pose.CROUCHING) && !(pose == Pose.SLEEPING)) {
            return PoseResponse.INVALID;
        }

        if(pose == Pose.SLEEPING && serverPlayer != null) {
            return PoseResponse.DOESNT_WORK_FOR_PLAYERS;
        }
        this.pose = pose;
        return PoseResponse.WORKS_FOR_ALL_ENTITIES;
    }

    public SkinFetchResponse setSkin(String playerName) {
        if (serverPlayer != null) {
            data = NPCSkinFetcher.fetchSkin(playerName);
            if(data == null) return SkinFetchResponse.DATA_IS_NULL;
            String skinValue = data.value;
            String skinSignature = data.signature;

            if (skinValue == null || skinSignature == null) return SkinFetchResponse.SKIN_VALUE_OR_SIGNATURE_IS_NULL;

            serverPlayer.discard();
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                sendPacket(new ClientboundPlayerInfoRemovePacket(List.of(serverPlayer.getUUID())), onlinePlayer);
                sendPacket(new ClientboundRemoveEntitiesPacket(serverPlayer.getId()), onlinePlayer);
            }


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
                ServerEntity se = new ServerEntity(serverPlayer.level(), serverPlayer, 0, false, packet -> {
                }, (a, b) -> {}, Set.of());
                Packet<?> packet = serverPlayer.getAddEntityPacket(se);
                sendPacket(packet, onlinePlayer);
                sendPacket(new ClientboundSetEntityDataPacket(serverPlayer.getId(), synchedEntityData.getNonDefaultValues()), onlinePlayer);
                sendEquipmentPacket(serverPlayer.getId(), onlinePlayer);
            }

            skinOwnerName = playerName;

            return SkinFetchResponse.SUCCESS;
        }
        return SkinFetchResponse.NO_SERVER_PLAYER;
    }
    public int getId() {
        if(serverPlayer != null) return serverPlayer.getId();
        else if(entity != null) return entity.getId();
        return 0;
    }
    public void interact(Player p) {
        for(NPCInteractFunction i : interactFunctions) {
            i.execute(p);
        }
    }

    public String getName() {
        return name;
    }

    public void moveTo(Location whereToGo) {
        Vec3 position = new Vec3(whereToGo.getX(), whereToGo.getY(), whereToGo.getZ());

        Vec3 velocity = new Vec3(0, 0, 0);  // Assuming no velocity change

        float yaw = location.getYaw();
        float pitch = location.getPitch();

        PositionMoveRotation positionMoveRotation = new PositionMoveRotation(position, velocity, yaw, pitch);

        Set<Relative> relativeSet = new HashSet<>();


        ClientboundTeleportEntityPacket packet = new ClientboundTeleportEntityPacket(
                (serverPlayer != null ? serverPlayer : entity).getId(),
                positionMoveRotation,
                relativeSet,
                true
        );

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            sendPacket(packet, onlinePlayer);
        }
    }

    public void toggleGlowing() {
        isGlowing = !isGlowing;
    }

}