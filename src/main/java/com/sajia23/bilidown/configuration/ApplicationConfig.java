package com.sajia23.bilidown.configuration;

import com.sajia23.bilidown.service.DownloadService;
import com.sajia23.bilidown.service.Watchman;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class ApplicationConfig {

    @Autowired
    DownloadService downloadService;

    @PostConstruct
    public void postConstruct(){
        Thread t = new Thread(new Watchman(downloadService));
        t.start();
    }
}
