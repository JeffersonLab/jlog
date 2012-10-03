package org.jlab.elog.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * IO Utilities.
 *
 * @author ryans
 */
public final class IOUtil {

    private static final Logger logger = Logger.getLogger(
            IOUtil.class.getName());

    private IOUtil() {
        // Can't instantiate publicly
    }

    /**
     * Fully reads in a file and returns an array of the bytes representing the
     * file. Be careful reading in large files because they may result in an
     * OutOfMemoryError.
     *
     * This method uses the File length to efficiently allocate memory.
     * 
     * @param file The file to load into memory.
     * @return The bytes
     * @throws IOException If an error occurs reading in the file.
     */
    public static byte[] fileToBytes(final File file) throws IOException {
        final byte[] bytes = new byte[(int) file.length()];

        DataInputStream dis = null;

        try {
            dis = new DataInputStream(new FileInputStream(file));

            dis.readFully(bytes);
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                    // Supressed, but logged
                    logger.log(Level.WARNING, "Unable to close file stream.",
                            e);
                }
            }
        }

        return bytes;
    }

    /**
     * Reads in an InputStream fully and returns the result as a String.
     * 
     * @param is The InputStream
     * @param encoding The character encoding of the String
     * @return The String representation of the data
     */
    public static String streamToString(InputStream is, String encoding) {
        String str = "";

        Scanner scan = new Scanner(is, encoding).useDelimiter("\\A");

        if (scan.hasNext()) {
            str = scan.next();
        }

        return str;
    }

    /**
     * Converts an array of String values to a comma-separated-values String.
     * 
     * @param values The array
     * @return The comma-separated-values
     */
    public static String arrayToCSV(String[] values) {
        StringBuilder builder = new StringBuilder();

        if (values != null && values.length > 0) {
            for (String s : values) {
                builder.append(s);
                builder.append(",");
            }
            
            // Remove trailing comma
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }
    
    /**
     * Converts a comma-separated-values String to an array of String values.
     * 
     * @param values The comma-separated-values
     * @return The array of String values
     */
    public static String[] csvToArray(String values) {
        return values.split(",");
    }
}
