package cool.circuit.decorativeNPCS.managers;

import cool.circuit.decorativeNPCS.DecorativeNPCS;
import org.bukkit.ChatColor;
import org.bukkit.conversations.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class InputManager {

    public static void input(Player player, String prompt, Consumer<String> callback) {
        ConversationFactory factory = new ConversationFactory(DecorativeNPCS.getInstance())
                .withModality(true)
                .withLocalEcho(false)
                .withPrefix(context -> ChatColor.YELLOW + "" + ChatColor.BOLD +  "[Input] ")
                .withFirstPrompt(new StringPrompt() {
                    @Override
                    public @NotNull String getPromptText(@NotNull ConversationContext context) {
                        return ChatColor.YELLOW + prompt + ChatColor.GRAY + " (Type 'cancel' to exit)";
                    }

                    @Override
                    public @Nullable Prompt acceptInput(@NotNull ConversationContext context, @Nullable String input) {
                        if (input == null || input.equalsIgnoreCase("cancel")) {
                            context.getForWhom().sendRawMessage(ChatColor.RED + "Input cancelled.");
                            return END_OF_CONVERSATION;
                        }

                        if (context.getForWhom() instanceof Player) {
                            Player p = (Player) context.getForWhom();

                            // Ensure callback runs on the main thread
                            DecorativeNPCS.getInstance().getServer().getScheduler().runTask(DecorativeNPCS.getInstance(),
                                    () -> callback.accept(input));
                        }

                        return END_OF_CONVERSATION;
                    }
                })
                .withEscapeSequence("cancel")
                .thatExcludesNonPlayersWithMessage(ChatColor.RED + "Only players can provide input!")
                .addConversationAbandonedListener(event -> {
                    if (!event.gracefulExit()) {
                        player.sendMessage(ChatColor.RED + "Input was abandoned.");
                    }
                });

        factory.buildConversation(player).begin();
    }
}
