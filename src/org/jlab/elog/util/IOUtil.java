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
     * file.  Be careful reading in large files because they may result in an
     * OutOfMemoryError.
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
    
    public static String streamToString(InputStream is, String encoding) {
        String str = "";
        
        Scanner scan = new Scanner(is, encoding).useDelimiter("\\A");
        
        if(scan.hasNext()) {
            str = scan.next();
        }
        
        return str;
    }
}
