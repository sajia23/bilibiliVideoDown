package com.sajia23.bilidown.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.io.Serializable;

@Data
@AllArgsConstructor
public class DownloadRangeTask implements Serializable {
    Boolean result;
    String cid;
    String taskname;
    String downloadurl;
    String sessdata;
    long start;
    long end;
    File file;
    Boolean last;
    Integer trytime;
}
