package com.example.musicbooru.track;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/track")
public class TrackController {

    private final TrackService trackService;

    @PostMapping
    public ResponseEntity<Track> uploadTrack(@RequestPart("file") MultipartFile file) {
        Track track = trackService.addTrack(file);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(track);
    }

    @DeleteMapping("/{trackPublicId}")
    public ResponseEntity<?> deleteTrack(@PathVariable String trackPublicId) {
        trackService.removeTrack(trackPublicId);
        return ResponseEntity.noContent().build();
    }
}
