package de.tu_darmstadt.informatik.robert_jakobi.tb.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Utility class for outsourcing file system access.
 *
 * @author Robert Jakobi
 * @since 17/09/18
 * @version 1.0
 *
 */
public class FileManager {
    /**
     * Instance of {@link FileManager}.
     */
    private static FileManager inst = new FileManager();

    /**
     * Constructor of {@link FileManager}.
     */
    private FileManager() {}

    /**
     * Returns instance of {@link FileManager}.
     *
     * @return Instance of FileManager
     */
    public static FileManager getInstance() {
        return inst;
    }

    /**
     * Returns contend of given file.
     *
     * @param file
     *            Path to file
     * @return Contend of given file
     */
    public String readFile(final String file) {
        try (final BufferedReader reader = new BufferedReader(new FileReader(new File(file)))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (final FileNotFoundException e) {
            return "no found";
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns contend of given file located in given folder.
     *
     * @param folder
     *            Location of file to read
     * @param file
     *            Name of file to read
     * @return
     */
    public String readFile(final String folder, final String file) {
        return this.readFile(folder + File.separator + file);
    }

    /**
     * Writes given contend into given file located in given folder.
     *
     * @param folder
     *            Location of file to write in
     * @param file
     *            Name of file to write in
     * @param contend
     *            Contend to write
     */
    public void writeFile(final String folder, final String file, final String contend) {
        new File(folder).mkdirs();
        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(new File(folder + File.separator + file)))) {
            writer.write(contend);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }
}
