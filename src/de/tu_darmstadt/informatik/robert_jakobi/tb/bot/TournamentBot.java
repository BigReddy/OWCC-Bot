package de.tu_darmstadt.informatik.robert_jakobi.tb.bot;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.security.auth.login.LoginException;

import de.tu_darmstadt.informatik.robert_jakobi.tb.util.FileManager;
import de.tu_darmstadt.informatik.robert_jakobi.tb.util.MessageContend;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Simplistic Discord bot for accessing and displaying specific files.
 *
 * @author Robert Jakobi
 * @since 17/09/18
 * @version 1.2.1
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

        // match making function
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

        // player market function
        this.builder.addEventListener(new ListenerAdapter() {
            @Override
            public void onPrivateMessageReceived(final PrivateMessageReceivedEvent event) {
                // Prevents recursive call
                if (event.getAuthor().getId().equals(TournamentBot.this.jda.getSelfUser().getId())) return;
                TournamentBot.this.onPrivateMessage(event);
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
     * React to private messages. Core to player market function.
     *
     * @param event
     *            Private message event
     */
    protected void onPrivateMessage(final PrivateMessageReceivedEvent event) {
        final FileManager fileManager = FileManager.getInstance();
        final PrivateChannel channel = event.getChannel();

        final String name = event.getAuthor().getId();
        final String message = event.getMessage().getContentStripped().toLowerCase();

        // Switching
        if (message.startsWith("!sr") || message.startsWith("!role")) {
            String fileName = "";
            boolean exists = false;
            final String playerFolder = TournamentBot.this.location + "player";

            // Search for user
            for (final String file : fileManager.files(playerFolder)) {
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

            final String value = message.split("\\s+")[1];

            if (message.startsWith("!sr")) {
                // !sr
                try {
                    final Integer tempValue = Integer.valueOf(value);
                    if (tempValue > 5000 || tempValue < 0) throw new NumberFormatException();
                    temp[1] = tempValue.toString();
                } catch (final NumberFormatException e) {
                    channel.sendMessage("Expected a number [0-5000]: " + value).queue();
                    return;
                }
            } else {
                // !role
                switch (value) {
                    case "tank":
                    case "dps":
                    case "support":
                    case "flex":
                        temp[2] = value;
                        break;
                    default:
                        channel.sendMessage("Unknown role: " + value).queue();
                        return;
                }
            }

            if (exists) {
                fileManager.renameFile(playerFolder, fileName, String.format("%s %s %s", temp));
            } else {
                fileManager.writeFile(playerFolder, String.format("%s %s %s", temp), " ");
            }

            channel.sendMessage("__**Profile updated:\n**__" + TournamentBot.this.formatPlayerProfile(temp)).queue();
            TournamentBot.this.onMarketUpdate(temp, "Player");
        } else if (message.startsWith("!search")) {
            // Search request

            final String[] args = message.split("\\s+");
            if (message.contains("help")) {
                channel.sendMessage(MessageContend.MARKET_SEARCH_HELP).queue();
                return;
            } else if (args.length % 2 != 1) {
                channel.sendMessage("Unknown format").queue();
                return;
            }

            // Initializing parameters
            int sr = 0;
            String role = "any";
            int range = 300;
            char remove = ' ';
            boolean notify = false;

            // Parse flag values
            for (int i = 1; i < args.length; i += 2) {
                switch (args[i]) {
                    case "-sr":
                    case "sr":
                    case "-rank":
                    case "rank":
                        try {
                            sr = Integer.parseInt(args[i + 1]);
                        } catch (final NumberFormatException e) {
                            channel.sendMessage("Unknown flag-value: " + args[i + 1]).queue();
                            return;
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
                            range = Integer.parseInt(args[i + 1]);
                        } catch (final NumberFormatException e) {
                            channel.sendMessage("Unknown flag-value: " + args[i + 1]).queue();
                            return;
                        }
                        break;
                    case "-remove":
                    case "remove":
                        remove = args[i + 1].charAt(0);
                        if (!(remove == 'a' || remove == 'r')) {
                            channel.sendMessage("Unknown flag-value: " + args[i + 1]).queue();
                            return;
                        }
                        break;
                    case "-notify":
                    case "notify":
                    case "-n":
                    case "n":
                        notify = true;
                    default:
                        channel.sendMessage("Unknown flag: " + args[i]).queue();
                        return;
                }
            }

            // Set data-set
            final String[] data = new String[] { name, "" + sr, role, "" + (range == 300 && sr == 0 ? 5000 : range) };

            if (remove != ' ') {
                final char fRemove = remove;
                final String fRole = role;
                // Delete all files with given parameters
                Arrays.stream(fileManager.files(TournamentBot.this.location + "trainer")) //
                        .filter(f -> f.startsWith(name)) //
                        .filter(f -> f.contains((fRemove == 'r' ? fRole : ""))) //
                        .forEach(f -> fileManager.deleteFile(TournamentBot.this.location + "trainer", f));

                // List of current active search requests
                String reply = TournamentBot.this.trainerSearchList(name);
                if (reply.isEmpty()) {
                    reply = "No requests currently active";
                } else {
                    reply = "__**Current requests:\n**__" + reply;
                }
                channel.sendMessage(reply).queue();
            } else if (notify) {
                fileManager.writeFile(TournamentBot.this.location + "trainer", String.format("%s %s %s %s", data), " ");
            }

            // Send updates
            TournamentBot.this.onMarketUpdate(data, "Trainer");
        } else if (message.startsWith("!list")) {
            // List of current active search requests
            String reply = TournamentBot.this.trainerSearchList(name);
            if (reply.isEmpty()) {
                reply = "No requests currently active";
            } else {
                reply = "__**Current requests:\n**__" + reply;
            }
            channel.sendMessage(reply).queue();
        } else if (message.startsWith("!help")) {
            // Help command
            channel.sendMessage(MessageContend.MARKET_HELP).queue();
        } else {
            // Unknown Command
            channel.sendMessage("__**Unknown command: \n**__Try:\n" + MessageContend.MARKET_HELP).queue();
        }
    }

    /**
     * Returns a list of all registered search requests with mark-up.
     *
     * @param id
     *            ID of trainer requesting
     * @return A list of all registered search requests
     */
    private String trainerSearchList(final String id) {
        return Arrays.stream(FileManager.getInstance().files(this.location + "trainer")) //
                .filter(f -> f.startsWith(id)) //
                .map(f -> f.split(" ")) //
                .map(this::formatTrainerRequest) //
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * Formats given data into a mark-up player profile
     *
     * @param data
     *            Data to format
     * @return Formated player profile
     */
    private String formatPlayerProfile(final String[] data) {
        return String.format("*Name: %s\nSR: %s\nRole: %s*", //
                this.jda.getUserById(data[0]).getAsMention(), //
                data[1], //
                data[2].toUpperCase());
    }

    /**
     * Formats given data into a mark-up trainer request
     *
     * @param data
     *            Data to format
     * @return Formated trainer request
     */
    private String formatTrainerRequest(final String[] data) {
        return String.format("*SR: %s\nRole: %s\nRange: %s*", //
                data[1], data[2].toUpperCase(), data[3]) //
                .replaceAll("SR: 0", "SR: Any") //
                .replaceAll("\nRange: 5000", "");
    }

    /**
     * Handles updates on data on the market. Notifies trainers on interesting
     * updates.
     *
     * @param data
     *            Changed data-set
     * @param type
     *            By whom the update is triggered ("trainer" or "player")
     */
    private void onMarketUpdate(String[] data, final String type) {
        final boolean search = type.equals("Trainer");

        int srPlayer = 0;
        String rolePlayer = null;

        String idTrainer = null;
        int srTrainer = 0;
        String roleTrainer = null;
        int rangeTrainer = 0;

        // Setting of data into its local values
        if (search) {
            idTrainer = data[0];
            srTrainer = Integer.parseInt(data[1]);
            roleTrainer = data[2];
            rangeTrainer = Integer.parseInt(data[3]);
        } else {
            srPlayer = Integer.parseInt(data[1]);
            rolePlayer = data[2];
        }

        // List of persons of interest
        final List<String[]> poi = new ArrayList<>();

        // Search
        for (final String file : FileManager.getInstance().files(this.location + (search ? "player" : "trainer"))) {
            data = file.split(" ");
            // Setting of current data written on file
            if (!search) {
                idTrainer = data[0];
                srTrainer = Integer.parseInt(data[1]);
                roleTrainer = data[2];
                rangeTrainer = Integer.parseInt(data[3]);
            } else {
                srPlayer = Integer.parseInt(data[1]);
                rolePlayer = data[2];
            }

            // Check if current file is a poi
            if (srPlayer >= srTrainer - rangeTrainer && srPlayer <= srTrainer + rangeTrainer
                    && (rolePlayer.equals(roleTrainer) || roleTrainer.equals("any") || rolePlayer.equals("flex"))) {
                poi.add(data);
            }
        }

        if (search) {
            // Send trainer all interesting players
            this.jda.getUserById(idTrainer).openPrivateChannel().complete()
                    .sendMessage("__**Following Players could be of intrest:**__\n" //
                            + poi.stream().map(this::formatPlayerProfile).collect(Collectors.joining("\n\n")))
                    .queue();
        } else {
            // Send all interested trainers an update notification, if not
            // already send
            poi.stream() //
                    .map(d -> this.jda.getUserById(d[0])) //
                    .map(u -> u.openPrivateChannel().complete()) //
                    .filter(c -> !c.getMessageById(c.getLatestMessageId()).complete().getContentRaw()
                            .contains("players available")) //
                    .forEach(c -> c.sendMessage("*New players available. Please request update.*").queue());
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
