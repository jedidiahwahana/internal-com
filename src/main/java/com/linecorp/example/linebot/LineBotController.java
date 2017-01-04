
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
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.commons.io.IOUtils;

import org.json.JSONObject;
import org.json.JSONArray;
import com.google.gson.Gson;

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
    PostgresHelper client = new PostgresHelper(DbContract.URL);
    
    Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap("cloud_name", "jedidiahwahana",
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
        
        Gson gson = new Gson();
        Payload payload = gson.fromJson(aPayload, Payload.class);
        
        //Variable initialization
        String msgText = " ";
        String upload_url = " ";
        String mJSON = " ";
        String eventType = payload.events[0].type;
        
        if (eventType.equals("join")){
            if (payload.events[0].source.type.equals("group")){
                replyToUser(payload.events[0].replyToken, "Hello Group");
            }
            if (payload.events[0].source.type.equals("room")){
                replyToUser(payload.events[0].replyToken, "Hello Room");
            }
        } else if (eventType.equals("message")){
            //Parsing message from user
            if (!payload.events[0].message.type.equals("text")){
                upload_url = getUserContent(payload.events[0].message.id, payload.events[0].source.userId);
                pushPoster(payload.events[0].source.userId, upload_url);
            } else {
                //Get movie data from OMDb API
                msgText = payload.events[0].message.text;
                msgText = msgText.toLowerCase();
                
                if (!msgText.contains("bot leave")){
                    try {
                        getMovieData(msgText, payload);
                    } catch (IOException e) {
                        System.out.println("Exception is raised ");
                        e.printStackTrace();
                    }
                } else {
                    if (payload.events[0].source.type.equals("group")){
                        leaveGR(payload.events[0].source.groupId, "group");
                    } else if (payload.events[0].source.type.equals("room")){
                        leaveGR(payload.events[0].source.roomId, "room");
                    }
                }
                
                if (payload.events[0].source.type.equals("group")){
                    pushType(payload.events[0].source.groupId, msgText + " - Group");
                } else if (payload.events[0].source.type.equals("room")){
                    pushType(payload.events[0].source.roomId, msgText + " - Room");
                } else if (payload.events[0].source.type.equals("user")){
                    pushType(payload.events[0].source.userId, msgText + " - User");
                }
            }
        }
         
        return new ResponseEntity<String>(HttpStatus.OK);
    }
    
    private void getMovieData(String title, Payload ePayload) throws IOException{
        String userTxt = title;
        title = title.substring(title.indexOf("\"") + 1, title.lastIndexOf("\""));
        title = title.replace(" ", "+");
        System.out.println("Text from User: " + title);
        
        // Act as client with GET method
        String URI = "http://www.omdbapi.com/?t=" + title + "&r=json";
        System.out.println("URI: " +  URI);
        
        String jObjGet = " ";
        CloseableHttpAsyncClient c = HttpAsyncClients.createDefault();
        
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
            jObjGet = resultGet.toString();
            System.out.println("OMDb responses: " + jObjGet);
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Exception is raised ");
            e.printStackTrace();
        } finally {
            c.close();
        }
        
        Gson mGson = new Gson();
        Movie movie = mGson.fromJson(jObjGet, Movie.class);
        String msgToUser = " ";
        
        //Check user request
        if (userTxt.contains("title")){
            msgToUser = movie.getMovie();
            pushPoster(ePayload.events[0].source.userId, movie.getPoster());
        } else if (userTxt.contains("plot")){
            msgToUser = movie.getPlot();
        } else if (userTxt.contains("released")){
            msgToUser = movie.getReleased();
        } else if (userTxt.contains("poster")){
            pushPoster(ePayload.events[0].source.userId, movie.getPoster());
        } else if (userTxt.contains("director")){
            msgToUser = movie.getDirector();
        } else if (userTxt.contains("writer")){
            msgToUser = movie.getWriter();
        } else if (userTxt.contains("awards")){
            msgToUser = movie.getAwards();
        } else if (userTxt.contains("actors")){
            msgToUser = movie.getActors();
        } else if (userTxt.contains("carousel")){
            carouselForUser(movie.getPoster(), ePayload.events[0].source.userId, movie.getTitle());
        }
        
        System.out.println("Message to user: " + msgToUser);
        
        if (msgToUser.length() <= 11 || !ePayload.events[0].message.type.equals("text")){
            replyToUser(ePayload.events[0].replyToken, "Request Timeout");
        } else {
            replyToUser(ePayload.events[0].replyToken, msgToUser);
        }
    }

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
    
    private void pushType(String sourceId, String txt){
        TextMessage textMessage = new TextMessage(txt);
        PushMessage pushMessage = new PushMessage(sourceId,textMessage);
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
    
    private void leaveGR(String id, String type){
        try {
            if (type.equals("group")){
                Response<BotApiResponse> response = LineMessagingServiceBuilder
                    .create(CHANNEL_ACCESS_TOKEN)
                    .build()
                    .leaveGroup(id)
                    .execute();
                System.out.println(response.code() + " " + response.message());
            } else if (type.equals("room")){
                Response<BotApiResponse> response = LineMessagingServiceBuilder
                    .create(CHANNEL_ACCESS_TOKEN)
                    .build()
                    .leaveRoom(id)
                    .execute();
                System.out.println(response.code() + " " + response.message());
            }
        } catch (IOException e) {
            System.out.println("Exception is raised ");
            e.printStackTrace();
        }
    }
}
