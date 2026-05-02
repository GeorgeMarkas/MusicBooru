package com.example.musicbooru.track;

import java.util.List;

public record DeleteTracksRequest(
        List<String> publicTrackIds
) {
}
