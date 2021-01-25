package org.utils;

import lombok.NonNull;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides some IO utility methods: (a) methods to write from and to streams;
 * (b) convenience methods for closing {@link Closeable}s
 */

public final class IOUtils {

 public static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * locates file either in the CLASSPATH or in the file system. The CLASSPATH
     * takes priority.
     *
     * @param file
     * @return the file input stream
     * @throws IOException
     */
    public static InputStream openStream(@NonNull final String file)
            throws IOException {
        InputStream is;
        return (is = IOUtils.class.getClassLoader().getResourceAsStream(file)) == null ?
                org.apache.commons.io.FileUtils.openInputStream(new File(file)) : is;
    }
}
