package com.sajia23.bilidown.service;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.sajia23.bilidown.entity.DownloadRangeTask;
import com.sajia23.bilidown.entity.DownloadTask;
import com.sajia23.bilidown.util.ApplicationConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Service
public class DownloadService {
    @Autowired
    ApplicationConfigUtil applicationConfigUtil;

    private static final Logger logger = LoggerFactory.getLogger(DownloadService.class);
// 第一版所使用的下载函数
//    @Async("asyncServiceExecutor")
//    public void doDownloadRange(String downloadurl, String sessdata, long start, long end, File file) throws IOException, InterruptedException {
//        //System.out.println("");
//        logger.info("进入doDownloadRange函数 任务 start : " + start + " end: "+ end + "开始下载");
//        RandomAccessFile raf = new RandomAccessFile(file, "rwd");
//        URL url = new URL(downloadurl);
//        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//        conn.setRequestMethod("GET");
//        // 已经下载的字节数
//        //long alreadySize = 0;
//
//        conn.addRequestProperty("Cookie", "SESSDATA=" + sessdata);
//        conn.addRequestProperty("Referer", "https://www.bilibili.com");
//        conn.addRequestProperty("range", "bytes=" + start + "-" + end);
//        conn.connect();
//        int code = conn.getResponseCode();
//        if (code == 206) {
//            raf.seek(start);
//            // 获取未下载的文件的大小
//            // 本方法用来获取响应正文的大小，但因为设置了range请求头，那么这个方法返回的就是剩余的大小
//            long unfinishedSize = conn.getContentLength();
//            // 文件的大小
//            //long size = alreadySize + unfinishedSize;
//
//            // 获取输入流
//            InputStream in = conn.getInputStream();
//            // 获取输出对象,参数一：目标文件，参数2表示在原来的文件中追加
//            //OutputStream out = new BufferedOutputStream(new FileOutputStream(file, true));
//
//            // 开始下载
//            byte[] buff = new byte[2048];
//            int len;
//            StringBuilder sb = new StringBuilder();
//            while ((len = in.read(buff)) != -1) {
//                raf.write(buff, 0, len);
//                // 将下载的累加到alreadSize中
//                //alreadySize += len;
//                // 下载进度
//                //System.out.printf("%.2f%%\n", alreadySize * 1.0 / size * 100);
//                // 由于文件大小可以看得到，那么我们这里使用阻塞
//                //Thread.sleep(2);
//                //break;
//            }
//            raf.close();
//            in.close();
//            //latch.countDown();
//
//        } else {
//            logger.info("下载任务 start : " + start + " end: "+ end + "失败");
//        }
//
//        // 断开连接
//        conn.disconnect();
//        logger.info("退出doDownloadRange函数 任务 start : " + start + " end: "+ end + "下载完成");
//    }
    @Async("asyncServiceExecutor")
    public void doDownloadRange(DownloadRangeTask downloadRangeTask){
        Socket socket = null;
        try{
            String downloadurl = downloadRangeTask.getDownloadurl();
            String sessdata = downloadRangeTask.getSessdata();
            long start = downloadRangeTask.getStart();
            long end = downloadRangeTask.getEnd();
            File file = downloadRangeTask.getFile();
            boolean last = downloadRangeTask.getLast();
            //进程间通信信息
            logger.info("线程 "+Thread.currentThread().getId()+" 进入doDownloadRange函数 任务"+downloadRangeTask.getCid() + downloadRangeTask.getTaskname() +" start : " + start + " end: "+ end + "开始下载");
            String host = "127.0.0.1";
            int port = 8234;
            // 与服务端建立连接
            socket = new Socket(host, port);
            // 建立连接后获得输出流
            OutputStream outputStream = socket.getOutputStream();

            try{
                RandomAccessFile raf = new RandomAccessFile(file, "rwd");
                URL url = new URL(downloadurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                // 已经下载的字节数
                //long alreadySize = 0;

                conn.addRequestProperty("Cookie", "SESSDATA=" + sessdata);
                conn.addRequestProperty("Referer", "https://www.bilibili.com");
                conn.addRequestProperty("range", "bytes=" + start + "-" + end);
                conn.connect();
                int code = conn.getResponseCode();
                if (code == 206) {
                    raf.seek(start);
                    // 获取未下载的文件的大小
                    // 本方法用来获取响应正文的大小，但因为设置了range请求头，那么这个方法返回的就是剩余的大小
                    long unfinishedSize = conn.getContentLength();
                    // 文件的大小
                    //long size = alreadySize + unfinishedSize;

                    // 获取输入流
                    InputStream in = conn.getInputStream();
                    // 获取输出对象,参数一：目标文件，参数2表示在原来的文件中追加
                    //OutputStream out = new BufferedOutputStream(new FileOutputStream(file, true));

                    // 开始下载
                    byte[] buff = new byte[2048];
                    int len;
                    StringBuilder sb = new StringBuilder();
                    while ((len = in.read(buff)) != -1) {
                        raf.write(buff, 0, len);
                        // 将下载的累加到alreadSize中
                        //alreadySize += len;
                        // 下载进度
                        //System.out.printf("%.2f%%\n", alreadySize * 1.0 / size * 100);
                        // 由于文件大小可以看得到，那么我们这里使用阻塞
                        //Thread.sleep(2);
                        //break;
                    }
                    raf.close();
                    in.close();
                    //latch.countDown();

                } else {
                    logger.error("下载时发现未登录！");
                    return;
                }
                // 断开连接
                conn.disconnect();
                logger.info("退出doDownloadRange函数 任务 "+downloadRangeTask.getCid() + downloadRangeTask.getTaskname() +" start : " + start + " end: "+ end + "下载完成");
                downloadRangeTask.setResult(true);
                downloadRangeTask.setTrytime(downloadRangeTask.getTrytime()+1);
                String message="任务 "+downloadRangeTask.getCid() + downloadRangeTask.getTaskname() +" start : " + start + " end: "+ end + "下载完成";
                ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
                os.writeObject(downloadRangeTask);
                outputStream.close();
                socket.close();
            }
            catch (Exception e){
                logger.error("线程 "+Thread.currentThread().getId()+"退出doDownloadRange函数 任务 "+downloadRangeTask.getCid() + downloadRangeTask.getTaskname() +" start : " + start + " end: "+ end + "下载出错：" + e.getMessage());
                downloadRangeTask.setTrytime(downloadRangeTask.getTrytime()+1);
                ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
                os.writeObject(downloadRangeTask);
                outputStream.close();
                socket.close();
                logger.info("线程 "+Thread.currentThread().getId()+"关闭连接并推出doDownloadRange函数");
            }
        }
        catch (Exception e) {
            logger.error("线程 "+Thread.currentThread().getId()+"有关socket连接的异常"+e.getMessage());
        }
    }
}
