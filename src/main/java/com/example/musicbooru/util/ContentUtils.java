package com.example.musicbooru.util;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.web.multipart.MultipartFile;

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
     * Detects the proper extension for the given file.
     *
     * @param file The file to parse.
     * @return The detected file extension.
     * @throws RuntimeException if the file could not be read, or if the media type name is invalid.
     */
    public static String detectFileExtension(MultipartFile file) {
        try {
            String mimeType = tika.detect(file.getBytes());

            return PREFERRED.getOrDefault(
                    mimeType,
                    MimeTypes
                            .getDefaultMimeTypes()
                            .forName(mimeType)
                            .getExtension()
            );
        } catch (IOException | TikaException e) {
            throw new RuntimeException("Could not detect file extension", e);
        }
    }
}
