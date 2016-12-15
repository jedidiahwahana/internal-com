
package com.linecorp.example.linebot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.CharBuffer;
import java.time.LocalDateTime;
import java.sql.SQLException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.apache.http.HttpResponse;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
//import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.client.methods.AsyncCharConsumer;
import org.apache.http.nio.IOControl;
import org.apache.http.protocol.HttpContext;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.concurrent.FutureCallback;
//import org.apache.http.annotation.ThreadingBehavior;
import org.apache.commons.io.IOUtils;

import org.json.JSONObject;
import org.json.JSONArray;

import okhttp3.ResponseBody;
import retrofit2.Response;

import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.VideoMessage;
import com.linecorp.bot.model.message.ImageMessage;
import com.linecorp.bot.model.message.template.CarouselColumn;
import com.linecorp.bot.model.message.template.CarouselTemplate;
import com.linecorp.bot.model.message.template.ButtonsTemplate;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.client.LineSignatureValidator;
import com.linecorp.bot.client.LineMessagingServiceBuilder;

import com.linecorp.example.linebot.db.DbContract;
import com.linecorp.example.linebot.db.PostgresHelper;

import com.cloudinary.*;
import com.cloudinary.utils.ObjectUtils;

@RestController
@RequestMapping(value="/linebot")
public class LineBotController
{
//    RequestConfig requestConfig = RequestConfig.custom()
//                                        .setConnectTimeout(10 * 1000)
//                                        .setConnectionRequestTimeout(10 * 1000)
//                                        .setSocketTimeout(10 * 1000).build();
//    CloseableHttpClient c = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
    CloseableHttpAsyncClient c = HttpAsyncClients.createDefault();
    PostgresHelper client = new PostgresHelper(DbContract.URL);
    
    Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                                                             "cloud_name", "jedidiahwahana",
                                                             "api_key", "265895536179732",
                                                             "api_secret", "sECFj7nAX6PWG29qf7OsU-BC7kY"));
    
    private static final String CHANNEL_SECRET = "17ba02a5c5c2307b0f9579d52ec48c58";
    private static final String CHANNEL_ACCESS_TOKEN = "3eb2RUEBHvOVQwBmD25oX8cjEDlIbzElCeVRNM2DeAXOLt8HV6dsSYcFDqtgNOtsCA8ylswoY2DEyeirTSNxrNKOTosCqEsS2ctLomVuy3KOCs8SB2BTwj8S3h5CYxnAkTnuT3pS2AxGaIvxHK7ofwdB04t89/1O/w1cDnyilFU=";
    
    @RequestMapping(value="/callback", method=RequestMethod.POST)
    public ResponseEntity<String> callback(
        @RequestHeader("X-Line-Signature") String aXLineSignature,
        @RequestBody String aPayload)
    {
        // compose body
        final String text=String.format("The Signature is: %s",
            (aXLineSignature!=null && aXLineSignature.length() > 0) ? aXLineSignature : "N/A");
        
        System.out.println(text);
        
        final boolean valid=new LineSignatureValidator(CHANNEL_SECRET.getBytes()).validateSignature(aPayload.getBytes(), aXLineSignature);
        
        System.out.println("The signature is: " + (valid ? "valid" : "tidak valid"));
        
        //Get events from source
        if(aPayload!=null && aPayload.length() > 0)
        {
            System.out.println("Payload: " + aPayload);
        }
        
        //Parsing JSONObject from source
        JSONObject jObject = new JSONObject(aPayload);
        JSONArray jArray = jObject.getJSONArray("events");
        JSONObject jObj = jArray.getJSONObject(0);
        String reply_token = jObj.getString("replyToken");
        JSONObject jMessage = jObj.getJSONObject("message");
        String msgType = jMessage.getString("type");
        String msgId = jMessage.getString("id");
        JSONObject jSource = jObj.getJSONObject("source");
        String srcId = jSource.getString("userId");
        
        //Variable initialization
        String msgText = " ";
        String mPlot = " ";
        String mReleased = " ";
        String mDirector = " ";
        String mWriter = " ";
        String mAwards = " ";
        String mActors = " ";
        String mPoster = " ";
        String mTitle = " ";
        String upload_url = " ";
        JSONObject mJSON = new JSONObject();
        
        //Parsing message from user
        if (!msgType.equals("text")){
            msgText = " ";
            upload_url = getUserContent(msgId, srcId);
            pushPoster(srcId, upload_url);
        } else {
            //Get movie data from OMDb API
            msgText = jMessage.getString("text");
            msgText = msgText.toLowerCase();
            try {
                mJSON = getMovieData(msgText);
                mPlot = mJSON.getString("Plot");
                mReleased = mJSON.getString("Released");
                mDirector = mJSON.getString("Director");
                mWriter = mJSON.getString("Writer");
                mAwards = mJSON.getString("Awards");
                mActors = mJSON.getString("Actors");
                mPoster = mJSON.getString("Poster");
                mTitle = mJSON.getString("Title");
            } catch (IOException e) {
                System.out.println("Exception is raised ");
                e.printStackTrace();
            }
        }
        
        String msgToUser = " ";
        
        //Check user request
        if (msgText.contains("title")){
            msgToUser = "Plot: " + mPlot + "\nReleased: " + mReleased + "\nDirector: " + mDirector + "\nWriter: " + mWriter + "\nAwards: " + mAwards + "\nActors: " + mActors;
            pushPoster(srcId, mPoster);
        } else if (msgText.contains("plot")){
            msgToUser = "Plot: " + mPlot;
        } else if (msgText.contains("released")){
            msgToUser = "Released: " + mReleased;
        } else if (msgText.contains("poster")){
            pushPoster(srcId, mPoster);
        } else if (msgText.contains("director")){
            msgToUser = "Director: " + mDirector;
        } else if (msgText.contains("writer")){
            msgToUser = "Writer: " + mWriter;
        } else if (msgText.contains("awards")){
            msgToUser = "Awards: " + mAwards;
        } else if (msgText.contains("actors")){
            msgToUser = "Actors: " + mActors;
        } else if (msgText.contains("carousel")){
            carouselForUser(mPoster, srcId, mTitle);
        }
        
        System.out.println("OMDb responses: " + msgToUser);
        
        if (msgToUser.length() <= 11){
            replyToUser(reply_token, "Request Timeout");
        } else {
            replyToUser(reply_token, msgToUser);
        }
         
        return new ResponseEntity<String>(HttpStatus.OK);
    }
    
    private JSONObject getMovieData(String title) throws IOException{
        title = title.substring(title.indexOf("\"") + 1, title.lastIndexOf("\""));
        title = title.replace(" ", "+");
        System.out.println("Text from User: " + title);
        
        // Act as client with GET method
        String URI = "http://www.omdbapi.com/?t=" + title + "&r=json";
        System.out.println("URI: " +  URI);
        
        JSONObject jObjGet = new JSONObject();
//        try{
//            c.start();
//            jObjGet = sendAsyncGetRequest(URI);
//        } finally {
//            c.close();
//        }
        
        try{
            c.start();
            HttpGet get = new HttpGet(URI);
            
            Future<HttpResponse> future = c.execute(get, null);
            HttpResponse responseGet = future.get();
            System.out.println("HTTP executed");
            System.out.println("HTTP Status of response: " + responseGet.getStatusLine().getStatusCode());
            
            // Get the response from the GET request
            BufferedReader brd = new BufferedReader(new InputStreamReader(responseGet.getEntity().getContent()));
            
            StringBuffer resultGet = new StringBuffer();
            String lineGet = "";
            while ((lineGet = brd.readLine()) != null) {
                resultGet.append(lineGet);
            }
            System.out.println("Got result");
            
            // Change type of resultGet to JSONObject
            jObjGet = new JSONObject(resultGet.toString());
            System.out.println("OMDb responses: " + resultGet.toString());
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Exception is raised ");
            e.printStackTrace();
//        } finally {
//            c.close();
        }
        
        return jObjGet;
    }
    
//    private JSONObject sendAsyncGetRequest(String url){
//        JSONObject jObj = new JSONObject();
//        System.out.println("Async call to URL...");
//        HttpGet request = new HttpGet(url);
//        HttpAsyncRequestProducer producer = HttpAsyncMethods.create(request);
//        AsyncCharConsumer<HttpResponse> consumer = new AsyncCharConsumer<HttpResponse>() {
//            
//            HttpResponse response;
//            
//            @Override
//            protected void onResponseReceived(final HttpResponse response) {
//                this.response = response;
//            }
//            
//            @Override
//            protected void onCharReceived(final CharBuffer buf, final IOControl ioctrl) throws IOException {
//                // Do something useful
//            }
//            
//            @Override
//            protected void releaseResources() {
//            }
//            
//            @Override
//            protected HttpResponse buildResult(final HttpContext context) {
//                return this.response;
//            }
//        };
//        
//        c.execute(producer, consumer, new FutureCallback<HttpResponse>() {
//            
//            @Override
//            public void completed(HttpResponse response) {
//                BufferedReader brd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
//                
//                StringBuffer resultGet = new StringBuffer();
//                String lineGet = "";
//                while ((lineGet = brd.readLine()) != null) {
//                    resultGet.append(lineGet);
//                }
//                System.out.println("Got result");
//                
//                // Change type of resultGet to JSONObject
//                jObj = new JSONObject(resultGet.toString());
//                System.out.println("OMDb responses: " + resultGet.toString());
//                System.out.println(response.toString());
//            }
//            
//            @Override
//            public void failed(Exception ex) {
//                System.out.println("!!! Async http request failed!");
//            }
//            
//            @Override
//            public void cancelled() {
//                System.out.println("Async http request canceled!");
//            }
//        });
//        return jObj;
//    }
    
    private void replyToUser(String rToken, String messageToUser){
        TextMessage textMessage = new TextMessage(messageToUser);
        ReplyMessage replyMessage = new ReplyMessage(rToken, textMessage);
        try {
            Response<BotApiResponse> response = LineMessagingServiceBuilder
                .create(CHANNEL_ACCESS_TOKEN)
                .build()
                .replyMessage(replyMessage)
                .execute();
            System.out.println("Reply Message: " + response.code() + " " + response.message());
        } catch (IOException e) {
            System.out.println("Exception is raised ");
            e.printStackTrace();
        }
    }
    
    private void pushPoster(String sourceId, String poster_url){
        ImageMessage imageMessage = new ImageMessage(poster_url, poster_url);
        PushMessage pushMessage = new PushMessage(sourceId,imageMessage);
        try {
            Response<BotApiResponse> response = LineMessagingServiceBuilder
                .create(CHANNEL_ACCESS_TOKEN)
                .build()
                .pushMessage(pushMessage)
                .execute();
            System.out.println(response.code() + " " + response.message());
        } catch (IOException e) {
            System.out.println("Exception is raised ");
            e.printStackTrace();
        }
    }
    
    private void carouselForUser(String poster_url, String sourceId, String title){
        CarouselTemplate carouselTemplate = new CarouselTemplate(
                    Arrays.asList(new CarouselColumn
                                    (poster_url, title, "Select one for more info", Arrays.asList
                                        (new MessageAction("Full Data", "Title \"" + title + "\""),
                                         new MessageAction("Summary", "Plot \"" + title + "\""),
                                         new MessageAction("Poster", "Poster \"" + title + "\""))),
                                  new CarouselColumn
                                    (poster_url, title, "Select one for more info", Arrays.asList
                                        (new MessageAction("Released Date", "Released \"" + title + "\""),
                                         new MessageAction("Actors", "Actors \"" + title + "\""),
                                         new MessageAction("Awards", "Awards \"" + title + "\"")))));
        TemplateMessage templateMessage = new TemplateMessage("Your search result", carouselTemplate);
        PushMessage pushMessage = new PushMessage(sourceId,templateMessage);
        try {
            Response<BotApiResponse> response = LineMessagingServiceBuilder
                .create(CHANNEL_ACCESS_TOKEN)
                .build()
                .pushMessage(pushMessage)
                .execute();
            System.out.println(response.code() + " " + response.message());
        } catch (IOException e) {
            System.out.println("Exception is raised ");
            e.printStackTrace();
        }
    }
    
    private String getUserContent(String messageId, String source_id){
        String uploadURL = " ";
        try {
            Response<ResponseBody> response = LineMessagingServiceBuilder
                .create(CHANNEL_ACCESS_TOKEN)
                .build()
                .getMessageContent(messageId)
                .execute();
            if (response.isSuccessful()) {
                ResponseBody content = response.body();
                try {
                    InputStream imageStream = content.byteStream();
                    Path path = Files.createTempFile(messageId, ".jpg");
                    try (FileOutputStream out = new FileOutputStream(path.toFile())) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = imageStream.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                        }
                    } catch (Exception e) {
                        System.out.println("Exception is raised ");
                    }
                    Map uploadResult = cloudinary.uploader().upload(path.toFile(), ObjectUtils.emptyMap());
                    System.out.println(uploadResult.toString());
                    JSONObject jUpload = new JSONObject(uploadResult);
                    uploadURL = jUpload.getString("secure_url");
                    if (client.connect()) {
                        System.out.println("DB connected");
                        if (client.insert("files", messageId, content.byteStream(), source_id) == 1) {
                            System.out.println("Record added");
                        }
                    }
                    
                } catch (ClassNotFoundException | SQLException e) {
                    System.out.println("Exception is raised ");
                    e.printStackTrace();
                }
            } else {
                System.out.println(response.code() + " " + response.message());
            }
        } catch (IOException e) {
            System.out.println("Exception is raised ");
            e.printStackTrace();
        }
        return uploadURL;
    }
}
