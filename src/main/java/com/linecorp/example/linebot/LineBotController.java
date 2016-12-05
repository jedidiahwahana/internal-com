
package com.linecorp.example.linebot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
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

import retrofit2.Response;

import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.client.LineSignatureValidator;
import com.linecorp.bot.client.LineMessagingServiceBuilder;

@RestController
@RequestMapping(value="/linebot")
public class LineBotController
{
    HttpClient c = HttpClientBuilder.create().build();
    
    @RequestMapping(value="/callback", method=RequestMethod.POST)
    public ResponseEntity<String> callback(
        @RequestHeader("X-Line-Signature") String aXLineSignature,
        @RequestBody String aPayload)
    {        
        // compose body
        final String text=String.format("The Signature is: %s",
            (aXLineSignature!=null && aXLineSignature.length() > 0) ? aXLineSignature : "N/A");
        
        System.out.println(text);
        
        final boolean valid=new LineSignatureValidator("caa222f011bb7e3b992540c00e94d763".getBytes()).validateSignature(aPayload.getBytes(), aXLineSignature);
        
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
        String msgText = jMessage.getString("text");
        
        System.out.println("Text from User: " + msgText);
        
        JSONObject jResponse = new JSONObject();
        try {
            jResponse = getMovieData(msgText);
        } catch (IOException e) {
            System.out.println("Exception is raised ");
            e.printStackTrace();
        }
        catch(Exception e)
        {
            System.out.println("Unknown exception occurs");
        }
        
        String moviePlot = jResponse.getString("Plot");
        
        try {
            replyToUser(reply_token, moviePlot);
        } catch (IOException e) {
            System.out.println("Exception is raised ");
            e.printStackTrace();
        }
        catch(Exception e)
        {
            System.out.println("Unknown exception occurs");
        }
        
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
    
    private void replyToUser(String rToken, String movie_plot) throws IOException{
        
        TextMessage textMessage = new TextMessage(movie_plot);
        ReplyMessage replyMessage = new ReplyMessage(rToken, textMessage);
        
//        try {
//            Response<BotApiResponse> response = LineMessagingServiceBuilder
//                .create("caa222f011bb7e3b992540c00e94d763")
//                .build()
//                .replyMessage(replyMessage)
//                .execute();
//            System.out.println("Reply Message: " + response.code() + " " + response.message());
//        } catch (IOException e) {
//            System.out.println("Exception is raised ");
//            e.printStackTrace();
//        }
//        catch(Exception e)
//        {
//            System.out.println("Unknown exception occurs");
//        }
        
        HttpPost post = new HttpPost("https://api.line.me/v2/bot/message/reply");
        
        //  Add header
        post.setHeader("Content-type", "application/json");
        post.setHeader("Authorization", "Bearer caa222f011bb7e3b992540c00e94d763");
        
        // Insert parameters for request in ArrayList
        List<NameValuePair> content = new ArrayList<NameValuePair>();
        content.add(new BasicNameValuePair("replyToken", rToken));
        content.add(new BasicNameValuePair("messages", movie_plot));
        
        // Hands ArrayList of parameters to the request
        post.setEntity(new UrlEncodedFormEntity(content));
        
        HttpResponse response = c.execute(post);
        
        // Get the response from the POST request
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        
        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null)
        {
            result.append(line);
        }
        
        // Change type of result to JSONObject
        JSONObject jObj = new JSONObject(result.toString());
        System.out.println("Response: " + result.toString());
    }
}
