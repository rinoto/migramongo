package com.rinoto.migramongo.spring.web;

import static org.mockito.Mockito.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.rinoto.migramongo.MigraMongo;

@Configuration
@ComponentScan(basePackageClasses = MigraMongoSpringWebTestConfig.class)
@EnableWebMvc
public class MigraMongoSpringWebTestConfig extends WebMvcConfigurerAdapter {

    @Bean
    public MigraMongo migraMongo() {
        return mock(MigraMongo.class);
    }

}
