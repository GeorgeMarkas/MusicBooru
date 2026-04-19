package org.example.musicbooru.util;

import lombok.extern.slf4j.Slf4j;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.example.musicbooru.util.Common.ARTWORK_EXTENSION;

@Slf4j
public class MetadataUtils {

    private final AudioFile audioFile;
    private final Tag tag;

    public MetadataUtils(File file) {
        try {
            this.audioFile = AudioFileIO.read(file);
            this.tag = this.audioFile.getTag();
        } catch (Exception e) {
            throw new RuntimeException("Could not read the tag contained in the given file", e);
        }
    }

    /**
     * Extracts the embedded cover art from the given file.
     *
     * @return The path to a newly created temporary file to which the cover art was copied,
     * wrapped in an {@code Optional}.
     * @throws RuntimeException if extracting the embedded cover art fails.
     */
    public Optional<Path> extractArtwork() {
        Artwork artwork = this.tag.getFirstArtwork();
        if (artwork == null) return Optional.empty();

        try {
            byte[] image = artwork.getBinaryData();
            Path temp = Files.createTempFile(null, ARTWORK_EXTENSION);

            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(image));
            ImageIO.write(bufferedImage, "jpg", temp.toFile());

            // Delete the embedded artwork since we don't want it in the blob
            this.tag.deleteArtworkField();
            this.audioFile.commit();

            return Optional.of(temp);
        } catch (IOException | CannotWriteException e) {
            throw new RuntimeException("Failed to extract artwork", e);
        }
    }

    /**
     * Returns the specified metadata field from the given file.
     *
     * @param field The corresponding {@link FieldKey} constant of the field to get.
     * @return The field contents as a string or an empty string if the field does not exist.
     */
    public String getField(FieldKey field) {
        try {
            return this.tag.getFirst(field);
        } catch (KeyNotFoundException e) {
            log.warn("{} field does not exist", field.name(), e);
            return "";
        }
    }

    /**
     * Returns the audio file's duration.
     *
     * @return The duration in seconds.
     */
    public int getDuration() {
        return audioFile.getAudioHeader().getTrackLength();
    }
}
