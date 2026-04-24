package com.example.musicbooru.outbox;

import com.example.musicbooru.track.TrackRepository;
import com.example.musicbooru.track.TrackStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxWorker {

    private static final int MAX_ATTEMPTS = 3;

    private final OutboxEventRepository outboxEventRepository;
    private final TrackRepository trackRepository;
    private final S3Client s3Client;

    @Value("${garage.bucket-artwork}")
    private String artworkBucket;

    @Value("${garage.bucket-library}")
    private String libraryBucket;

    @Scheduled(fixedDelay = 10_000) // 10 seconds
    @Transactional
    public void processPending() {
        List<OutboxEvent> events = outboxEventRepository
                .findByStatusAndAttemptsLessThan(OutboxStatus.PENDING, MAX_ATTEMPTS);

        for (OutboxEvent event : events) {
            try {
                uploadToS3(event);
                markTrackReady(event.getTrackId());
                event.setStatus(OutboxStatus.DONE);
            } catch (RuntimeException e) {
                log.error("Outbox processing failed for event '{}'", event.getId(), e);
                event.updateAttempts();
                if (event.getAttempts() >= MAX_ATTEMPTS) {
                    event.setStatus(OutboxStatus.FAILED);
                    markTrackFailed(event.getTrackId());
                }
            }

            outboxEventRepository.save(event);
        }
    }

    private void uploadToS3(OutboxEvent event) {
        if (event.getArtworkPath() != null) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(artworkBucket)
                            .key(event.getTrackPublicId())
                            .build(),
                    RequestBody.fromFile(Path.of(event.getArtworkPath()))
            );
        }

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(libraryBucket)
                        .key(event.getTrackPublicId())
                        .build(),
                RequestBody.fromFile(Path.of(event.getAudioPath()))
        );

        deleteTempFile(event.getAudioPath());
        if (event.getArtworkPath() != null) deleteTempFile(event.getArtworkPath());
    }

    private void markTrackReady(Long trackId) {
        trackRepository.findById(trackId).ifPresent(track -> {
            track.setStatus(TrackStatus.READY);
            trackRepository.save(track);
        });
    }

    private void markTrackFailed(Long trackId) {
        trackRepository.findById(trackId).ifPresent(track -> {
            track.setStatus(TrackStatus.FAILED);
            trackRepository.save(track);
        });
    }

    private void deleteTempFile(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            log.warn("Could not clean up temporary file '{}'", path, e);
        }
    }
}
