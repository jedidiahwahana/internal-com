
package com.linecorp.example.linebot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.LocalDateTime;
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
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

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

@RestController
@RequestMapping(value="/linebot")
public class LineBotController
{
    HttpClient c = HttpClientBuilder.create().build();
    private static final String CHANNEL_SECRET = "caa222f011bb7e3b992540c00e94d763";
    private static final String CHANNEL_ACCESS_TOKEN = "i4iDYDwh7VEyNHSAMRMGjqFjlZbi9CNng34yVW+b6d2DIggg1WExUoZNIYqj749IsJC+nbEt1ciuqy/oHR2XkwYDqB/fC5jN6FHYM9F2MMcOQVQpIcAkyxUskdg8jTOP6g005lISkzpZRkoxTUcRGgdB04t89/1O/w1cDnyilFU=";
    
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
        
        if(aPayload!=null && aPayload.length() > 0)
        {
            System.out.println("Payload: " + aPayload);
        }
        
        JSONObject jObject = new JSONObject(aPayload);
        JSONArray jArray = jObject.getJSONArray("events");
        JSONObject jObj = jArray.getJSONObject(0);
        String reply_token = jObj.getString("replyToken");
        JSONObject jMessage = jObj.getJSONObject("message");
        String msgType = jMessage.getString("type");
        String msgId = jMessage.getString("id");
        JSONObject jSource = jObj.getJSONObject("source");
        String srcId = jSource.getString("userId");
        
        String msgText;
        
        if (!msgType.equals("text")){
            msgText = " ";
        } else {
            msgText = jMessage.getString("text");
        }
        
        msgText = msgText.replace(" ", "+");
        
        System.out.println("Text from User: " + msgText);
        
        JSONObject jResponse = new JSONObject();
        try {
            jResponse = getMovieData(msgText);
            String moviePlot = jResponse.getString("Plot");
            String posterURL = jResponse.getString("Poster");
            replyToUser(reply_token, moviePlot, posterURL);
            pushToUser(srcId);
//            templateForUser(posterURL, srcId);
            carouselForUser(posterURL, srcId);
        } catch (IOException e) {
            System.out.println("Exception is raised ");
            e.printStackTrace();
        }
        catch(Exception e)
        {
            System.out.println("Unknown exception occurs");
        }
        
//        String fileURL = "https://api.line.me/v2/bot/message/" + messageId + "/content";
//        String saveDir = "/User/line/Downloads";
//        try {
//            HttpDownloadUtility.downloadFile(fileURL, saveDir, CHANNEL_ACCESS_TOKEN);
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
        
//        try {
//            if (!msgType.equals("text")){
//                getUserContent(msgId);
//                System.out.println("Get User Content function is called");
//            }
//        } catch (IOException e) {
//            System.out.println("Exception is raised ");
//            e.printStackTrace();
//        }
//        catch(Exception e)
//        {
//            System.out.println("Unknown exception occurs");
//        }
        
        return new ResponseEntity<String>(HttpStatus.OK);
    }
    
    private JSONObject getMovieData(String title) throws IOException{
        // Act as client with GET method
        
        String URI = "http://www.omdbapi.com/?t=" + title + "&r=json";
        
        HttpGet get = new HttpGet(URI);
        
        HttpResponse responseGet = c.execute(get);
        
        // Get the response from the GET request
        BufferedReader brd = new BufferedReader(new InputStreamReader(responseGet.getEntity().getContent()));
        
        StringBuffer resultGet = new StringBuffer();
        String lineGet = "";
        while ((lineGet = brd.readLine()) != null) {
            resultGet.append(lineGet);
        }
        
        // Change type of resultGet to JSONObject
        JSONObject jObjGet = new JSONObject(resultGet.toString());
        System.out.println("OMDb responses: " + resultGet.toString());
        
        return jObjGet;
    }
    
    private void replyToUser(String rToken, String movie_plot, String poster_url) throws IOException{
        
        TextMessage textMessage = new TextMessage(movie_plot);
        ImageMessage imageMessage = new ImageMessage(poster_url, poster_url);
        
        List<Message> allMessage = new ArrayList<Message>();
        allMessage.add(textMessage);
        allMessage.add(imageMessage);
        
        ReplyMessage replyMessage = new ReplyMessage(rToken, allMessage);
        
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
        catch(Exception e)
        {
            System.out.println("Unknown exception occurs");
        }
    }
    
    private void pushToUser(String sourceId) throws IOException{
        TextMessage textMessage = new TextMessage("hello");
        PushMessage pushMessage = new PushMessage(sourceId,textMessage);
        Response<BotApiResponse> response = LineMessagingServiceBuilder
                    .create(CHANNEL_ACCESS_TOKEN)
                    .build()
                    .pushMessage(pushMessage)
                    .execute();
        System.out.println(response.code() + " " + response.message());
    
    }
    
    private void templateForUser(String poster_url, String sourceId) throws IOException{
        ButtonsTemplate buttonsTemplate = new ButtonsTemplate(poster_url,"My button sample", "Hello, my button", Arrays.asList(new URIAction("Go to line.me","https://line.me"),
                                new PostbackAction("Say hello1","hello こんにちは"),
                                new PostbackAction("言 hello2","hello こんにちは","hello こんにちは"),
                                new MessageAction("Say message","Rice=米")));
        TemplateMessage templateMessage = new TemplateMessage("Button alt text", buttonsTemplate);
        PushMessage pushMessage = new PushMessage(sourceId,templateMessage);
        Response<BotApiResponse> response = LineMessagingServiceBuilder
            .create(CHANNEL_ACCESS_TOKEN)
            .build()
            .pushMessage(pushMessage)
            .execute();
        System.out.println(response.code() + " " + response.message());
    }
    
    private void carouselForUser(String poster_url, String sourceId) throws IOException{
        CarouselTemplate carouselTemplate = new CarouselTemplate(
                    Arrays.asList(new CarouselColumn
                                    (poster_url, "hoge", "fuga", Arrays.asList(new URIAction("Go to line.me", "https://line.me"), new PostbackAction("Say hello1", "hello こんにちは"))),
                                  new CarouselColumn(poster_url, "hoge", "fuga", Arrays.asList(new PostbackAction("Next", "Rambo", "You search for Rambo"), new MessageAction("Say message", "Rice=米")))));
        TemplateMessage templateMessage = new TemplateMessage("Carousel alt text", carouselTemplate);
        PushMessage pushMessage = new PushMessage(sourceId,templateMessage);
        Response<BotApiResponse> response = LineMessagingServiceBuilder
            .create(CHANNEL_ACCESS_TOKEN)
            .build()
            .pushMessage(pushMessage)
            .execute();
        System.out.println(response.code() + " " + response.message());
    }
    
    private void getUserContent(String messageId) throws IOException{
        Response<ResponseBody> response = LineMessagingServiceBuilder
                .create(CHANNEL_ACCESS_TOKEN)
                .build()
                .getMessageContent(messageId)
                .execute();
        System.out.println("Success:" + response.isSuccessful());
        if (response.isSuccessful()) {
            ResponseBody content = response.body();
            Files.copy(content.byteStream(), Files.createTempFile("foo", "bar"));
        } else {
            System.out.println(response.code() + " " + response.message());
        }
    }
}
