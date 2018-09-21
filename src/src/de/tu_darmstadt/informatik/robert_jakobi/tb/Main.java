package de.tu_darmstadt.informatik.robert_jakobi.tb;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import de.tu_darmstadt.informatik.robert_jakobi.tb.bot.TournamentBot;
import de.tu_darmstadt.informatik.robert_jakobi.tb.util.FileManager;

/**
 * Main class of this Discord application.
 *
 * @author Robert Jakobi
 * @since 17/09/18
 * @version 1.1
 *
 */
public class Main {
    /**
     * Initiates essential parts of program. <br>
     * Expects Discord Bot-Token. Expects location of team name file.
     *
     * @param args
     *            Has to contains a Discord Bot-token at first place and
     *            optional a string pointing at the team name file (creates new
     *            groups).
     */
    public static void main(final String args[]) {
        try (Scanner sc = new Scanner(System.in)) {
            TournamentBot tb;
            if (args.length < 1)
                throw new Error("No bot token given!");
            else {
                if (args.length > 1) {
                    createGroups(args[1]);
                }
                tb = new TournamentBot(args[0]);
            }
            do {
                if (sc.next().equalsIgnoreCase("exit")) {
                    break;
                } else {
                    System.out.println("Write >exit< to exit programm.");
                }
            } while (true);
            tb.shutdown();
        }
    }

    /**
     * Parses given file for teams and groups them randomly within one of four
     * groups.
     *
     * @param fileName
     *            Name of the file, who holds the team names
     */
    private static void createGroups(final String fileName) {
        final List<List<String>> groups = new LinkedList<>();
        for (int i = 0; i < 4; i++) {
            groups.add(new LinkedList<>());
        }

        // Read all team names
        final List<String> teams = Arrays.asList(FileManager.getInstance().readFile(fileName).split(System.lineSeparator()));
        // Shuffle team names
        Collections.shuffle(teams);

        // Dissect into groups
        int temp = 0;
        for (final String team : teams) {
            groups.get(temp++ % 4).add(team);
        }

        char group = 'a';
        // Write group of teams into files
        for (final List<String> team : groups) {
            FileManager.getInstance().writeFile("groups", String.valueOf(group++),
                    team.stream().collect(Collectors.joining(System.lineSeparator())));
        }

    }
}
