package org.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.NonNull;
import org.apache.commons.collections4.IteratorUtils;

import java.io.*;
import java.util.Collection;
import java.util.List;

/**
 * This class ...
 */

public final class JsonUtils {
    private static final String LINE = "\n";

    private JsonUtils() {

    }

    public static ObjectMapper getJsonMapper() {
        return getJsonMapper(false);
    }

    public static ObjectMapper getJsonMapper(boolean indentOutput) {
        ObjectMapper mapper = new ObjectMapper();
        if (indentOutput) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        } else {
            mapper.disable(SerializationFeature.INDENT_OUTPUT);
        }
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        return mapper;
    }

    public static <T> void writeList(@NonNull final OutputStream stream,
                                     @NonNull final Collection<T> list) throws IOException {
        stream.write(toString(list).getBytes(IOUtils.DEFAULT_ENCODING));
    }

    public static <T> void writeList(@NonNull final Writer writer,
                                     @NonNull final Collection<T> list) throws IOException {
        writer.write(toString(list));
    }

    public static <T> void writeList(@NonNull final File file,
                                     @NonNull final Collection<T> list) throws IOException {

        try (FileOutputStream outputStream = org.apache.commons.io.FileUtils.openOutputStream(file)) {
            writeList(outputStream, list);
        }
    }

    public static <T> String toString(@NonNull final Collection<T> list) {
        ObjectMapper mapper = getJsonMapper();
        StringBuilder sb = new StringBuilder();
        list.forEach(t -> {
            try {
                sb.append(mapper.writeValueAsString(t)).append(LINE);
            } catch (JsonProcessingException e) {
//                e.printStackTrace();
//                log.warn(e.getMessage());
            }
        });
        return sb.toString();
    }

    public static <T> T read(@NonNull final String file,
                             @NonNull final Class<? extends T> c) throws IOException {
        try (InputStream s = IOUtils.openStream(file)) {
            return getJsonMapper().readValue(s, c);
        }
    }

    public static <T> List<T> readList(@NonNull final Reader reader,
                                       @NonNull final Class<? extends T> c) throws IOException {
        MappingIterator<? extends T> it = getJsonMapper().readValues(new JsonFactory().createParser(reader), c);
        return IteratorUtils.toList(it);
    }

    public static <T> List<T> readList(@NonNull final InputStream stream,
                                       @NonNull final Class<? extends T> c) throws IOException {
        MappingIterator<? extends T> it = getJsonMapper().readValues(new JsonFactory().createParser(stream), c);
        return IteratorUtils.toList(it);
    }

    public static <T> List<T> readList(@NonNull final File file,
                                       @NonNull final Class<? extends T> c) throws IOException {
        try (FileInputStream stream = new FileInputStream(file)) {
            return readList(stream, c);
        }
    }

    public static <T> List<T> readList(@NonNull final String file,
                                       @NonNull final Class<? extends T> c) throws IOException {
        try (InputStream stream = IOUtils.openStream(file)) {
            return readList(stream, c);
        }
    }
}
