package cool.circuit.decorativeNPCS.managers;

import cool.circuit.decorativeNPCS.DecorativeNPCS;
import net.minecraft.network.protocol.Packet;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class PacketManager {
    public static void sendPacket(Packet<?> packet, Player player) {
        if(((CraftPlayer) player).getHandle().connection == null) {
            Bukkit.getScheduler().runTaskLater(DecorativeNPCS.getInstance(), () -> ((CraftPlayer) player).getHandle().connection.sendPacket(packet), 25L);
        }
        ((CraftPlayer) player).getHandle().connection.sendPacket(packet);
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