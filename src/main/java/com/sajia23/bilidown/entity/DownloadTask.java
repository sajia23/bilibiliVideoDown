package com.sajia23.bilidown.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DownloadTask {
    private String url;
    private String bvid;
    private String cid;
    private String qn;
    private Long size;
    private String cidtitle;
    private String bvidtitle;
    private boolean cidmember;//该downloadtask是否是cid下的一个子任务
}
