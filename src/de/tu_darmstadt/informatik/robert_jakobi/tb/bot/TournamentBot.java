package de.tu_darmstadt.informatik.robert_jakobi.tb.bot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.security.auth.login.LoginException;

import de.tu_darmstadt.informatik.robert_jakobi.tb.util.FileManager;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Simplistic Discord bot for accessing and displaying specific files.
 *
 * @author Robert Jakobi
 * @since 17/09/18
 * @version 1.2
 *
 */
public class TournamentBot {
    private final JDABuilder builder = new JDABuilder(AccountType.BOT);
    private final JDA jda;
    private final String location = new File("").getAbsolutePath() + File.separator + "transfermarket" + File.separator;

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

        this.builder.addEventListener(new ListenerAdapter() {
            @Override
            public void onPrivateMessageReceived(final PrivateMessageReceivedEvent event) {
                // Prevents recursive call
                if (event.getAuthor().getId().equals(TournamentBot.this.jda.getSelfUser().getId())) return;

                final String name = event.getAuthor().getId();
                final String message = event.getMessage().getContentStripped().toLowerCase();

                String fileName = "";
                boolean exists = false;
                if (message.startsWith("!sr") || message.startsWith("!role")) {
                    final String playerFolder = TournamentBot.this.location + "player";

                    // Search for user
                    for (final String file : FileManager.getInstance().files(playerFolder)) {
                        if (file.startsWith(name)) {
                            fileName = file;
                            exists = true;
                            break;
                        }
                    }

                    final String[] temp;
                    if (exists) {
                        temp = fileName.split(" ");
                    } else {
                        temp = new String[] { name, "0", "none" };
                    }

                    final String value = message.replaceAll("\\s+", " ").split(" ")[1];

                    if (message.startsWith("!sr")) {
                        try {
                            temp[1] = Integer.valueOf(value).toString();
                        } catch (final NumberFormatException e) {
                            // TODO ?
                        }
                    } else {
                        switch (value) {
                            case "tank":
                            case "dps":
                            case "support":
                            case "flex":
                                temp[2] = value;
                                break;
                            default:
                                // TODO ?
                        }
                    }

                    if (exists) {
                        FileManager.getInstance().renameFile(playerFolder, fileName, String.format("%s %s %s", temp));
                    } else {
                        FileManager.getInstance().writeFile(playerFolder, String.format("%s %s %s", temp), " ");
                    }
                    TournamentBot.this.onMarketUpdate(temp, "Player");
                    event.getChannel().sendMessage("Profile updated:\n" + TournamentBot.this.getPlayerProfile(temp)).queue();
                } else if (message.startsWith("!search")) {
                    final String[] args = message.replaceAll("\\s+", " ").split(" ");
                    if (args.length % 2 != 1 || args.length < 3) {
                        event.getChannel().sendMessage("Unknown format").queue(); // TODO:
                                                                                  // DO
                                                                                  // TO
                        return;
                    }

                    int sr = 0;
                    String role = "none";
                    final int range = 300;

                    for (int i = 1; i < args.length; i += 2) {
                        switch (args[i]) {
                            case "-sr":
                            case "sr":
                            case "-rank":
                            case "rank":
                                try {
                                    sr = Integer.parseInt(args[i + 1]);
                                } catch (final NumberFormatException e) {
                                    // TODO ?
                                }
                                break;
                            case "-role":
                            case "role":
                            case "-r":
                            case "r":
                                role = args[i + 1].toLowerCase();
                                break;
                            case "-range":
                            case "range":
                            case "-d":
                            case "d":
                                try {
                                    sr = Integer.parseInt(args[i + 1]);
                                } catch (final NumberFormatException e) {
                                    // TODO ?
                                }
                            default:
                                event.getChannel().sendMessage("Unknown flag: " + args[i]).queue(); // TODO
                                                                                                    // DO
                                                                                                    // TO
                                return;
                        }

                        //
                    }
                    final String[] data = new String[] { name, "" + sr, role, "" + (range == 300 && sr == 0 ? 5000 : range) };
                    FileManager.getInstance().writeFile(TournamentBot.this.location + "trainer",
                            String.format("%s %s %s %s", data), " ");
                    TournamentBot.this.onMarketUpdate(data, "Trainer");
                } else if (message.startsWith("!list")) {

                } else {
                    event.getChannel().sendMessage("Unknown command: \nTry:" //
                            + "\n\t!sr [number]\t\tSet your SR" //
                            + "\n\t!rank [Tank|Support|DPS|Flex]\t\tSets your prefered role" //
                            + "\n\t!search <-sr [number]> <-role [Tank|Support|DPS|Flex]\t Search for fitting player") //
                            .queue();
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

    protected String getPlayerProfile(final String[] temp) {
        return String.format("Name: %s\nSR: %s\nRole: %s", this.jda.getUserById(temp[0]).getAsMention(), //
                temp[1], //
                temp[2].toUpperCase());
    }

    protected void onMarketUpdate(String[] data, final String type) {
        final boolean search = type.equals("Trainer");

        String idPlayer = null;
        int srPlayer = 0;
        String rolePlayer = null;

        String idTrainer = null;
        int srTrainer = 0;
        String roleTrainer = null;
        int rangeTrainer = 0;

        if (search) {
            idTrainer = data[0];
            srTrainer = Integer.parseInt(data[1]);
            roleTrainer = data[2];
            rangeTrainer = Integer.parseInt(data[3]);
        } else {
            idPlayer = data[0];
            srPlayer = Integer.parseInt(data[1]);
            rolePlayer = data[2];
        }

        final List<String[]> poi = new ArrayList<>();

        for (final String file : FileManager.getInstance().files(this.location + (search ? "player" : "trainer"))) {
            data = file.split(" ");
            if (!search) {
                idTrainer = data[0];
                srTrainer = Integer.parseInt(data[1]);
                roleTrainer = data[2];
                rangeTrainer = Integer.parseInt(data[3]);
            } else {
                idPlayer = data[0];
                srPlayer = Integer.parseInt(data[1]);
                rolePlayer = data[2];
            }

            if (srPlayer >= srTrainer - rangeTrainer && srPlayer <= srTrainer + rangeTrainer
                    && (rolePlayer.equals(roleTrainer) || roleTrainer.equals("any") || rolePlayer.equals("flex"))) {
                poi.add(data);
            }
        }
        //
        if (search) {
            this.jda.getUserById(idTrainer).openPrivateChannel().complete().sendMessage("Following Players could be of intrest:\n" //
                    + poi.stream().map(this::getPlayerProfile).collect(Collectors.joining("\n"))).queue();
        } else {
            poi.stream() //
                    .map(d -> this.jda.getUserById(d[0])) //
                    .map(u -> u.openPrivateChannel().complete()) //
                    .filter(c -> !c.getMessageById(c.getLatestMessageId()).complete().getContentRaw()
                            .contains("players available")) //
                    .forEach(c -> c.sendMessage("New players available. Please request update.").queue());
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
