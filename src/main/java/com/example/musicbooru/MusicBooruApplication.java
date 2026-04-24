package com.example.musicbooru;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MusicBooruApplication {

    public static void main(String[] args) {
        SpringApplication.run(MusicBooruApplication.class, args);
    }
}
