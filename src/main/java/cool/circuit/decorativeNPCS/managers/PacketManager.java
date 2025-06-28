package cool.circuit.decorativeNPCS.managers;

import cool.circuit.decorativeNPCS.DecorativeNPCS;
import net.minecraft.network.protocol.Packet;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R5.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class PacketManager {
    public static void sendPacket(Packet<?> packet, Player player) {
        // Ensure the player has a valid connection before sending the packet
        if (((CraftPlayer) player).getHandle().connection == null) {
            Bukkit.getScheduler().runTaskLater(DecorativeNPCS.getInstance(), () -> {
                // Double-check if the player's connection is available before sending the packet
                if (((CraftPlayer) player).getHandle().connection != null) {
                    ((CraftPlayer) player).getHandle().connection.sendPacket(packet);
                }
            }, 25L);
        } else {
            // If the connection is already available, send the packet immediately
            ((CraftPlayer) player).getHandle().connection.sendPacket(packet);
        }
    }

    public static void setValue(Object packet, String fieldName, Object value) {
        try {
            Field field = packet.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(packet, value);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}