package com.sajia23.bilidown;

import com.sajia23.bilidown.service.DownloadService;
import com.sajia23.bilidown.service.Watchman;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cglib.core.DebuggingClassWriter;

@SpringBootApplication
@EnableCaching
public class BilidownApplication {

    public static void main(String[] args) {
        //System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "D:\\class");
        SpringApplication.run(BilidownApplication.class, args);
    }

}
