package de.tu_darmstadt.informatik.robert_jakobi.tb.bot;

import javax.security.auth.login.LoginException;

import de.tu_darmstadt.informatik.robert_jakobi.tb.util.FileManager;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Simplistic Discord bot for accessing and displaying specific files.
 *
 * @author Robert Jakobi
 * @since 17/09/18
 * @version 1.1
 *
 */
public class TournamentBot {
    private final JDABuilder builder = new JDABuilder(AccountType.BOT);
    private final JDA jda;

    /**
     * Constructor of {@link TournamentBot}. <br>
     * Initializes vital bot api and my fail, if not possible.
     *
     * @param botToken
     *            Token of application this bot shall connect to
     */
    public TournamentBot(final String botToken) {
        this.builder.setToken(botToken);
        this.builder.setAutoReconnect(true);
        this.builder.setStatus(OnlineStatus.ONLINE);

        this.builder.addEventListener(new ListenerAdapter() {
            @Override
            public void onMessageReceived(final MessageReceivedEvent event) {
                // Specific pre-filters (channel | isCommand | permission)
                if (event.getChannel().getName().equalsIgnoreCase("auslosung")) {
                    if (event.getMessage().getContentStripped().startsWith("!")) {
                        if (event.getGuild().getMember(event.getAuthor()).getRoles()
                                .contains(event.getGuild().getRolesByName("ｃｕｐ   ａｄｍｉｎ", true).get(0))) {
                            TournamentBot.this.onCommand(event);
                        }
                    }
                }
            }
        });

        // Login attempt: if it fails end program
        try {
            this.jda = this.builder.buildBlocking();
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    /**
     * Handles input commands by authorized user.
     *
     * @param event
     *            Message event
     */
    private void onCommand(final MessageReceivedEvent event) {
        String message = event.getMessage().getContentStripped();
        message = message.replaceAll(".*: ", "");
        if (message.startsWith("!group ")) {
            final String answer = FileManager.getInstance().readFile("groups",
                    message.split(" ")[1].replaceAll("[\\\\] | [/]", ""));
            if (answer == null)
                return;
            else if (answer.equals("not found")) {
                event.getChannel() //
                        .sendMessage("Gruppe \"%s\" wurde nicht gefunden!") //
                        .complete();
            } else {
                event.getChannel() //
                        .sendMessage(String.format("__**Gruppe %s:**__\n*%s*", message.split(" ")[1].toUpperCase(), answer)) //
                        .complete();
            }
        } else {
            event.getChannel().sendMessage("Unknown command: " + message).complete();
        }
    }

    /**
     * Shuts down the bot.
     */
    public void shutdown() {
        this.builder.setStatus(OnlineStatus.OFFLINE);
        this.jda.shutdown();
    }
}
