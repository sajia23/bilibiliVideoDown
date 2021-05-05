package com.sajia23.bilidown.service;

import com.sajia23.bilidown.WebSocketServers.WebSocketServer;
import com.sajia23.bilidown.entity.DownloadRangeTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Watchman implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DownloadService.class);

    DownloadService downloadService = null;

    public Watchman(DownloadService downloadService){
        this.downloadService = downloadService;
    }

    @Override
    public void run() {
        logger.info("线程 "+Thread.currentThread().getId()+" 是看门狗线程");
        try{
            ServerSocket server = new ServerSocket(8234,100);
            while(true){
                Socket socket = server.accept();
                // 建立好连接后，从socket中获取输入流，并建立缓冲区进行读取
                //InputStream inputStream = socket.getInputStream();
                ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
                Object obj = is.readObject();
                DownloadRangeTask downloadRangeTask = (DownloadRangeTask) obj;
                is.close();
                socket.close();
                if(!downloadRangeTask.getResult() && downloadRangeTask.getTrytime() < 4){
                    logger.info("任务："+downloadRangeTask.getTaskname()+"的分段 "+downloadRangeTask.getStart()+"-"+downloadRangeTask.getEnd()+"尝试重新下载");
                    downloadService.doDownloadRange(downloadRangeTask);
                    logger.info("任务："+downloadRangeTask.getTaskname()+"的分段 "+downloadRangeTask.getStart()+"-"+downloadRangeTask.getEnd()+"分配尝试重新下载任务完成");
                }
                else if(!downloadRangeTask.getResult() && downloadRangeTask.getTrytime() >= 3){
                    logger.error("任务："+downloadRangeTask.getTaskname()+"的分段 "+downloadRangeTask.getStart()+"-"+downloadRangeTask.getEnd()+" 下载失败次数过多");
                    WebSocketServer.BroadCastInfo("false-"+downloadRangeTask.getCid()+"-"+downloadRangeTask.getStart()+"-"+downloadRangeTask.getEnd());
                }
                WebSocketServer.BroadCastInfo("true-"+downloadRangeTask.getCid()+"-"+downloadRangeTask.getStart()+"-"+downloadRangeTask.getEnd());
            }
        }
        catch (Exception e){
            logger.error("线程 "+Thread.currentThread().getId()+"看门狗异常:"+e);
        }
    }
}
