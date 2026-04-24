package com.example.musicbooru.track;

import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.outbox.OutboxEvent;
import com.example.musicbooru.outbox.OutboxEventRepository;
import com.example.musicbooru.outbox.OutboxStatus;
import com.example.musicbooru.util.ContentUtils;
import com.example.musicbooru.util.MetadataUtils;
import com.example.musicbooru.util.PublicIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static com.example.musicbooru.util.Constants.*;

@Service
@RequiredArgsConstructor
public class TrackService {

    private final TrackRepository trackRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public Track addTrack(MultipartFile file) {
        // Copy the user uploaded content to a temporary file for processing
        Path userUpload;
        try {
            userUpload = Files.createTempFile(null, ContentUtils.detectFileExtension(file));
            file.transferTo(userUpload);
        } catch (IOException e) {
            throw new GenericException("Failed to copy user upload to temporary file", e);
        }

        // Create a unique public ID for API use
        String publicId;
        try {
            publicId = PublicIdGenerator.generate(PUBLIC_ID_LENGTH, 3, trackRepository::existsByPublicId);
        } catch (RuntimeException e) {
            throw new GenericException(e.getMessage(), e);
        }

        // Create track entity instance
        MetadataUtils metadataUtils = new MetadataUtils(userUpload.toFile());

        Track track = Track.builder()
                .publicId(publicId)
                .artist(metadataUtils.getArtist())
                .title(metadataUtils.getTitle())
                .album(metadataUtils.getAlbum())
                .year(metadataUtils.getYear())
                .genre(metadataUtils.getGenre())
                .duration(metadataUtils.getDuration())
                .status(TrackStatus.PENDING)
                .build();

        trackRepository.save(track);

        // If embedded artwork is present, extract it
        String artworkPath;
        try {
            Optional<Path> artwork = metadataUtils.extractArtwork();
            artworkPath = artwork.map(Path::toString).orElse(null);
        } catch (RuntimeException e) {
            throw new GenericException(e.getMessage(), e);
        }

        // TODO: Attempt transcoding, probably here or offloaded to another worker
        // Pretend to transcode
        String audioPath = userUpload.toString();

        // Create outbox event
        OutboxEvent event = OutboxEvent.builder()
                .trackId(track.getId())
                .trackPublicId(publicId)
                .audioPath(audioPath)
                .artworkPath(artworkPath)
                .status(OutboxStatus.PENDING)
                .attempts(0)
                .createdAt(Instant.now())
                .build();

        outboxEventRepository.save(event);

        return track;
    }
}
