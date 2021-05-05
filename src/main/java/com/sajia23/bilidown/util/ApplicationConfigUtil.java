package com.sajia23.bilidown.util;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class ApplicationConfigUtil {
    @Value("${file_path.loginfilepath}")
    private String loginfilepath;

    @Value("${file_path.qrfilepath}")
    private String qrfilepath;

    @Value("${file_path.downloadpath}")
    private String downloadpath;

    @Value("${biliurl.loginUrl}")
    private String loginUrl;

    @Value("${biliurl.loginInfo}")
    private String loginInfo;

    @Value("${biliurl.qrcodeUrl}")
    private String qrcodeUrl;

    @Value("${biliurl.verifyUrl}")
    private String verifyUrl;

    @Value("${biliurl.videoInfo}")
    private String videoInfo;

    @Value("${biliurl.videoDownloadInfo}")
    private String videoDownloadInfo;

}
