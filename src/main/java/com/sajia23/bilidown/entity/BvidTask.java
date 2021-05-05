package com.sajia23.bilidown.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class BvidTask {
    private String bvid;
    private String title;
    private Map<String,String> cid_title_map;

}
