package cool.circuit.decorativeNPCS.obj;

import cool.circuit.decorativeNPCS.NPC;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class NPCInteractFunction {
    private final Consumer<Player> func;
    private final NPC npc;
    private final NPCInteractType type;
    private final Object value;

    public NPCInteractType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public NPCInteractFunction(Consumer<Player> func, NPC npc, NPCInteractType type, Object value) {
        this.func = func;
        this.npc = npc;
        this.type = type;
        this.value = value;
    }

    public NPC getNpc() {
        return npc;
    }

    public void execute(Player player) {
        func.accept(player);
    }
}
