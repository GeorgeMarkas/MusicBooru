package com.example.musicbooru.track;

import com.example.musicbooru.exception.GenericException;
import com.example.musicbooru.exception.ResourceNotFoundException;
import com.example.musicbooru.outbox.OutboxEvent;
import com.example.musicbooru.outbox.OutboxEventRepository;
import com.example.musicbooru.outbox.OutboxStatus;
import com.example.musicbooru.util.ContentUtils;
import com.example.musicbooru.util.MetadataUtils;
import com.example.musicbooru.util.PublicIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static com.example.musicbooru.util.Constants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrackService {

    private final TrackRepository trackRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final S3Client s3Client;

    @Value("${garage.bucket-artwork}")
    private String artworkBucket;

    @Value("${garage.bucket-library}")
    private String libraryBucket;

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

    @Transactional
    public void removeTrack(String trackPublicId) {
        Track track = trackRepository.findByPublicId(trackPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Track",  trackPublicId));

        if (track.getStatus() == TrackStatus.PENDING) {
            throw new GenericException("Track is still being processed", HttpStatus.CONFLICT);
        }

        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(artworkBucket)
                        .key(trackPublicId)
                        .build()
        );

        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(libraryBucket)
                        .key(trackPublicId)
                        .build()
        );

        outboxEventRepository.deleteByTrackId(track.getId());
        trackRepository.delete(track);
        log.info("Track '{}' removed", track.getPublicId());
    }
}
