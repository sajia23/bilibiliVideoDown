package com.sajia23.bilidown.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.sajia23.bilidown.entity.BvidTask;
import com.sajia23.bilidown.entity.DownloadRangeTask;
import com.sajia23.bilidown.entity.DownloadTask;
import com.sajia23.bilidown.service.DownloadService;
import com.sajia23.bilidown.service.Watchman;
import com.sajia23.bilidown.util.ApplicationConfigUtil;
import com.sajia23.bilidown.util.HttpResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

@RestController
@RequestMapping("/api")
public class api {
    @Autowired
    ApplicationConfigUtil applicationConfigUtil;
    @Autowired
    DownloadService downloadservice;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    DownloadService downloadService;

    private static final Logger logger = LoggerFactory.getLogger(api.class);
    BvidTask bvidTask;
    String sessdata = null;
    //cid和cid的title对应的map
    Map<String,String> cid_title_map = null;
    //cid和该cid需要的下载线程数量
    Map<String,Integer> cid_threadnum_map = new HashMap<>();
    //cid和该cid的downloadtask
    Map<String,DownloadTask> cid_downloadTask = null;
    List<Long> cidsizes = null;
    long minsize;
    //long maxsize;
    //
    //网站逻辑 一打开网页 网页调用后端verify函数 如果登录有效即返回sessdata  如果无效则返回错误
    //前端根据返回结果做判断 如果未登录 则登录按钮可用 点击调用后端登录函数

    //登录第一步返回的是url加oauthkey 否则返回的是sessdata
    @GetMapping("/loginfirststep")
    public HttpResult loginfirststep(){
        HttpResponse restemp = HttpRequest.get(applicationConfigUtil.getLoginUrl())
                .header(Header.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:81.0) Gecko/20100101 Firefox/81.0")//头信息，多个头信息多次调用此方法
                .timeout(20000)//超时，毫秒
                .execute();
        JSONObject jsonObject = new JSONObject(restemp.body());
        String oauthkey = (String)((JSONObject)jsonObject.get("data")).get("oauthKey");
        System.out.println("oauthKey:"+oauthkey);
        QrCodeUtil.generate(applicationConfigUtil.getQrcodeUrl()+oauthkey, 300, 300, FileUtil.file(applicationConfigUtil.getQrfilepath()));
        return HttpResult.success(applicationConfigUtil.getQrcodeUrl()+oauthkey);
    }

    //登录第二步返回的是sessdata
    @GetMapping("/loginsecondstep")
    public HttpResult loginsecondstep(@RequestParam(name = "oauthKey") String oauthKey){
        HttpResponse restemp = HttpRequest.post(applicationConfigUtil.getLoginInfo())
                .header(Header.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:81.0) Gecko/20100101 Firefox/81.0")//头信息，多个头信息多次调用此方法
                .form("oauthKey",oauthKey)//表单内容
                .timeout(20000)//超时，毫秒
                .execute();
        JSONObject jsonObject = new JSONObject(restemp.body());

        if(jsonObject.containsKey("code")){
            try{
                String sessdatatemp = restemp.getCookie("SESSDATA").toString();
                sessdatatemp = sessdatatemp.substring(sessdatatemp.indexOf("=")+1,sessdatatemp.length());
                sessdata = sessdatatemp;
                System.out.println("SESSDATA:"+sessdata);
                File file =new File(applicationConfigUtil.getLoginfilepath());
                //if file doesnt exists, then create it
                if(!file.exists()){
                    file.createNewFile();
                }
                FileWriter fileWritter = new FileWriter(file);
                fileWritter.write(sessdata);
                fileWritter.close();
            }catch(IOException e){
                e.printStackTrace();
            }
            return HttpResult.success("已登录");
        }
        else{
            return HttpResult.fail(403,"未扫码登陆成功");
        }
    }

    //验证返回的是sessdata
    @GetMapping("/verifylogin")
    public HttpResult verifylogin(){
        File file = new File(applicationConfigUtil.getLoginfilepath());

        if(!file.exists()){
            return HttpResult.fail(403,"未登录");
        }
        else{
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                sessdata = reader.readLine();
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e1) {
                    }
                }
            }
            HttpResponse restemp = HttpRequest.get(applicationConfigUtil.getVerifyUrl())
                    .header(Header.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:81.0) Gecko/20100101 Firefox/81.0")//头信息，多个头信息多次调用此方法
                    .header(Header.COOKIE,"SESSDATA="+sessdata)
                    .timeout(20000)//超时，毫秒
                    .execute();
            JSONObject jsonObject = new JSONObject(restemp.body());
            if(jsonObject.containsKey("code")) {
                return HttpResult.success("已登录");
            }
            else{
                return HttpResult.fail(403,"登录已过期");
            }
        }
    }

    @GetMapping("/getVideoInfo")
    public HttpResult getVideoInfo(@RequestParam(name = "bvid") String bvid){
        try{
            HttpResponse restemp = HttpRequest.get(applicationConfigUtil.getVideoInfo()+bvid)
                    .header(Header.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:81.0) Gecko/20100101 Firefox/81.0")//头信息，多个头信息多次调用此方法
                    .header(Header.COOKIE,"SESSDATA="+sessdata)
                    .header(Header.REFERER,"https://www.bilibili.com")
                    .timeout(20000)//超时，毫秒
                    .execute();
            JSONObject jsonObject = new JSONObject(restemp.body());
            jsonObject = (JSONObject)jsonObject.get("data");
            JSONArray jsonArray = (JSONArray)jsonObject.get("pages");
            List<Map<String,Object>> list = new ArrayList<>();
            cid_title_map = new HashMap<>();
            for(int i = 0; i < jsonArray.size(); i ++){
                JSONObject temp = (JSONObject)jsonArray.get(i);
                String part = (String)temp.get("part");
                String cid = temp.get("cid").toString();
                Map<String,Object> maptemp = new HashMap<>();
                maptemp.put("part",part);
                maptemp.put("cid",cid);
                cid_title_map.put(cid,part);
                list.add(maptemp);
            }
            bvidTask = new BvidTask(bvid,(String)jsonObject.get("title"),cid_title_map);
            System.out.println("当前bvidTask:"+bvidTask);
            return HttpResult.success(list);
        }
        catch (Exception e){
            return HttpResult.fail(500,e.getMessage());
        }

    }

    //得到某个分p的具体下载信息
    //Param(name = "bvid") String bvid,@RequestParam(name = "cid") String cid,@RequestParam(name = "qn") String qn
    public List<DownloadTask> getVideoDownloadInfo(String bvid,String cid,String qn){
        System.out.println("进入getVideoDownloadInfo函数");
        System.out.println("请求视频下载链接的url为:"+applicationConfigUtil.getVideoDownloadInfo()+"bvid="+bvid+"&cid="+cid+"&qn="+qn);
        List<DownloadTask> downloadTasks = new ArrayList<>();
        HttpResponse restemp = HttpRequest.get(applicationConfigUtil.getVideoDownloadInfo()+"bvid="+bvid+"&cid="+cid+"&qn="+qn)
                .header(Header.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:81.0) Gecko/20100101 Firefox/81.0")//头信息，多个头信息多次调用此方法
                .header(Header.COOKIE,"SESSDATA="+sessdata)
                .header(Header.REFERER,"https://www.bilibili.com")
                .timeout(20000)//超时，毫秒
                .execute();
        System.out.println(restemp);
        JSONObject jsonObject = new JSONObject(restemp.body());
        jsonObject = (JSONObject)jsonObject.get("data");
        JSONArray jsonArray = (JSONArray)jsonObject.get("durl");
        for(int i = 0; i < jsonArray.size(); i ++){
            JSONObject temp = (JSONObject)jsonArray.get(i);
            String url = (String)temp.get("url");
            DownloadTask downloadTasktemp = new DownloadTask(url,bvid,cid+i,qn,Long.valueOf(temp.get("size").toString()),cid_title_map.get(cid),"bvid占位符",jsonArray.size()>1?true:false);
            downloadTasks.add(downloadTasktemp);
        }
        System.out.println("退出getVideoDownloadInfo函数");
        return downloadTasks;
    }

//----------------------------------------------------------------------第一版下载任务分配函数---------------------------------------------------------------------------------

    //建立下载任务的函数 第一版 根据所有任务 先确定最小的任务 以其他任务对最小任务的倍数确定使用的线程数  第一版使用到的函数 download ,dodownload ,downloadservice.doDownloadRange
//    @GetMapping("/download")
//    public HttpResult download(@RequestParam(name = "bvid") String bvid,@RequestParam(name = "cid") String cid,@RequestParam(name = "qn") String qn) throws IOException, InterruptedException {
//        //System.out.println("进入download函数");
//        logger.info("进入download函数");
//        cidsizes = new ArrayList<>();
//        HttpResult hr = verifylogin();
//        if(hr.getCode() != 200){
//            return HttpResult.fail(403,"未登录");
//        }
//        String downloadPath = applicationConfigUtil.getDownloadpath()+"\\\\"+bvidTask.getTitle();
//        //String downloadPath = applicationConfigUtil.getDownloadpath()+"/"+bvidTask.getTitle();
//        File file = new File(downloadPath);
//        if(!file.exists()){
//            file.mkdir();
//        }
//        String[] cids = cid.split(",");
//        cid_downloadTask = new HashMap<>();
//        for(int i = 0; i < cids.length; i ++){
//            List<DownloadTask> downloadTasks = getVideoDownloadInfo(bvid,cids[i],qn);
//            for(int j = 0; j < downloadTasks.size(); j ++){
//                cidsizes.add(downloadTasks.get(j).getSize());
//                cid_downloadTask.put(downloadTasks.get(j).getCid(),downloadTasks.get(j));
//            }
//        }
//        Collections.sort(cidsizes);
//        minsize = cidsizes.get(0);
//        DownloadTask downloadTask = null;
//        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor)applicationContext.getBean("asyncServiceExecutor");
//        for(String key : cid_downloadTask.keySet()){
//            System.out.println("当前存活线程数量："+executor.getActiveCount());
//            downloadTask = cid_downloadTask.get(key);
//            //计算当前cid任务需要的线程数
//            long threadnum = downloadTask.getSize()/minsize;
//            long threadnum_remainder = downloadTask.getSize()%minsize;
//            if(threadnum_remainder/(double)minsize >= 0.5){
//                threadnum ++;
//            }
//            if(executor.getMaxPoolSize() < threadnum){
//                threadnum = executor.getMaxPoolSize();
//            }
//            while(true){
//                if((executor.getMaxPoolSize()-executor.getActiveCount()) >= threadnum){
//                    doDownload(downloadTask,sessdata,cid_title_map,downloadPath,threadnum,minsize);
//                    break;
//                }
//            }
//        }
//        //System.out.println("退出download函数");
//        logger.info("退出download函数");
//        return HttpResult.success("下载完成");
//    }

//    public void doDownload(DownloadTask downloadTask, String sessdata, Map<String, String> cid_title_map, String bvidDownloadPath, long threadnum, long minsize) throws IOException, InterruptedException {
//        //System.out.println("进入doDownload函数 开始下载cid为" + downloadTask.getCid() + "的视频");
//        logger.info("进入doDownload函数 开始下载cid为" + downloadTask.getCid() + "的视频");
//        String cidsfilepath = bvidDownloadPath + "\\\\" + cid_title_map.get(downloadTask.getCid().substring(0,downloadTask.getCid().length()-1));
//        //String cidsfilepath = bvidDownloadPath + "/" + cid_title_map.get(downloadTask.getCid().substring(0,downloadTask.getCid().length()-1));
//        File cidsfile = new File(cidsfilepath);
//        if (!cidsfile.exists()) {
//            cidsfile.mkdir();
//        }
//        long last = 0;
//        File cidfile = null;
//        if (downloadTask.isCidmember()) {
//            String cidname = cidsfilepath + "\\\\" + downloadTask.getCid() + ".flv";
//            //String cidname = cidsfilepath + "/" + downloadTask.getCid() + ".flv";
//            cidfile = new File(cidname);
//        } else {
//            String cidname = cidsfilepath + "\\\\" + downloadTask.getCidtitle() + ".flv";
//            //String cidname = cidsfilepath + "/" + downloadTask.getCidtitle() + ".flv";
//            cidfile = new File(cidname);
//        }
//        RandomAccessFile raf = new RandomAccessFile(cidfile, "rw");
//        //设置下载文件大小
//        raf.seek(downloadTask.getSize());
//        raf.close();
//        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor)applicationContext.getBean("asyncServiceExecutor");
//        if(threadnum == executor.getMaxPoolSize()){
//            minsize = downloadTask.getSize()/threadnum;
//        }
//        for (int i = 0; i < threadnum; i++) {
//            downloadservice.doDownloadRange(downloadTask.getUrl(), sessdata, last, i == (threadnum - 1) ? downloadTask.getSize() - 1 : last + minsize - 1, cidfile);
//            last += minsize;
//        }
//        //doDownloadRange();
//        //System.out.println("退出doDownload函数");
//        logger.info("退出doDownload函数 开始下载cid为" + downloadTask.getCid() + "的视频");
//    }


    //----------------------------------------------------------------------第二版下载任务分配函数---------------------------------------------------------------------------------

    //第二版下载任务分配函数 直接将视频按照预定下载清晰度进行切割分配
    @GetMapping("/download")
    public HttpResult download(@RequestParam(name = "bvid") String bvid,@RequestParam(name = "cid") String cid,@RequestParam(name = "qn") String qn) throws IOException, InterruptedException {
        logger.info("进入download函数");

        HttpResult hr = verifylogin();
        if(hr.getCode() != 200){
            return HttpResult.fail(403,"未登录");
        }
        //创建bvid目录
        String downloadPath = applicationConfigUtil.getDownloadpath()+File.separator+bvidTask.getTitle();
        File file = new File(downloadPath);
        if(!file.exists()){
            file.mkdir();
        }
        String[] cids = cid.split(",");
        cid_downloadTask = new HashMap<>();
        for(int i = 0; i < cids.length; i ++){
            List<DownloadTask> downloadTasks = getVideoDownloadInfo(bvid,cids[i],qn);
            for(int j = 0; j < downloadTasks.size(); j ++){
                cid_downloadTask.put(downloadTasks.get(j).getCid(),downloadTasks.get(j));
            }
        }

        DownloadTask downloadTask = null;
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor)applicationContext.getBean("asyncServiceExecutor");
        for(String key : cid_downloadTask.keySet()){
            downloadTask = cid_downloadTask.get(key);
            doDownload(downloadTask,sessdata,cid_title_map,downloadPath);
        }
        logger.info("退出download函数");
        return HttpResult.success("下载任务分配完成");
    }

    @GetMapping("/downloadAll")
    public HttpResult downloadAll(@RequestParam(name = "bvid") String bvid,@RequestParam(name = "qn") String qn) throws IOException, InterruptedException {
        logger.info("进入downloadall函数");
        HttpResult hr = verifylogin();
        if(hr.getCode() != 200){
            return HttpResult.fail(403,"未登录");
        }
        getVideoInfo(bvid);
        StringBuilder cids = new StringBuilder();
        for(String key : cid_title_map.keySet()){
            cids.append(key).append(',');
        }
        cids.deleteCharAt(cids.length()-1);
        System.out.println("要下载的所有cid:"+cids.toString());
        download(bvid,cids.toString(),qn);
        logger.info("退出downloadall函数");
        return HttpResult.success("下载任务分配完成");
    }

    public void doDownload(DownloadTask downloadTask, String sessdata, Map<String, String> cid_title_map, String bvidDownloadPath) throws IOException, InterruptedException {
        logger.info("进入doDownload函数 开始下载cid为" + downloadTask.getCid() + "的视频");
        String cidsfilepath = bvidDownloadPath + File.separator + cid_title_map.get(downloadTask.getCid().substring(0,downloadTask.getCid().length()-1));

        File cidsfile = new File(cidsfilepath);
        if (!cidsfile.exists()) {
            cidsfile.mkdir();
        }

        File cidfile = null;
        if (downloadTask.isCidmember()) {
            String cidname = cidsfilepath + File.separator + downloadTask.getCid() + ".flv";
            cidfile = new File(cidname);
        } else {
            String cidname = cidsfilepath + File.separator + downloadTask.getCidtitle() + ".flv";
            cidfile = new File(cidname);
        }
        RandomAccessFile raf = new RandomAccessFile(cidfile, "rw");
        //设置下载文件大小
        raf.seek(downloadTask.getSize());
        raf.close();
        long threadnum = 0;
        long last = 0; //标识每个下载任务的结束点
        long unitLength = 0;
        if(downloadTask.getQn().equals("80")){
            unitLength = 10485760;
            if(downloadTask.getSize()%unitLength > 0){
                threadnum = downloadTask.getSize()/unitLength + 1;
            }
            else{
                threadnum = downloadTask.getSize()/unitLength;
            }
        }
        for (int i = 0; i < threadnum; i++) {
            //while(true){
                //System.out.println("当前任务为"+downloadTask.getCid()+" 当前activecount为:"+executor.getActiveCount());
                //if((executor.getMaxPoolSize()-executor.getActiveCount()) > 0){
            DownloadRangeTask downloadRangeTask = new DownloadRangeTask(false, downloadTask.getCid(), cid_title_map.get(downloadTask.getCid().substring(0,downloadTask.getCid().length()-1)), downloadTask.getUrl(), sessdata, last, i == (threadnum - 1) ? downloadTask.getSize() - 1 : last + unitLength - 1, cidfile, i == (threadnum - 1) ? true : false,0);
                    downloadservice.doDownloadRange(downloadRangeTask);
                    //break;
                //}
            //}
            last += unitLength;
        }
        logger.info("退出doDownload函数 cid为" + downloadTask.getCid() + "的视频任务分配完成");
    }

}
