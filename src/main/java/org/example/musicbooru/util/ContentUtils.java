package org.example.musicbooru.util;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.mime.MimeTypes;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ContentUtils {

    private static final Tika tika = new Tika();

    private static final Map<String, String> PREFERRED = Map.of(
            "audio/mp4", ".m4a",
            "audio/mpeg", ".mp3",
            "audio/x-aiff", ".aiff"
    );

    /**
     * Detects the extension of the given file.
     *
     * @param file The file to parse.
     * @return The detected file extension.
     * @throws RuntimeException if the file could not be read, or if the media type name is invalid.
     */
    public static String detectFileExtension(File file) {
        try {
            String mimeType = tika.detect(file);

            return PREFERRED.getOrDefault(
                    mimeType,
                    MimeTypes
                            .getDefaultMimeTypes()
                            .forName(mimeType)
                            .getExtension()
            );
        } catch (IOException e) {
            throw new RuntimeException("Could not read file", e);
        } catch (TikaException e) {
            throw new RuntimeException("Invalid media type name", e);
        }
    }
}
