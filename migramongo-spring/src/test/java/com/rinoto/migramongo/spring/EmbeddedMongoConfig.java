package com.rinoto.migramongo.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rinoto.migramongo.EmbeddedMongo;

@Configuration
public class EmbeddedMongoConfig {

    @Bean
    public EmbeddedMongo embeddedMongo() {
        return new EmbeddedMongo();
    }

}
