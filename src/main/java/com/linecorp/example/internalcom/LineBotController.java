
package com.linecorp.example.internalcom;

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
import java.nio.charset.Charset;
import java.io.UnsupportedEncodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.*;


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.entity.StringEntity;
import org.apache.commons.io.IOUtils;

import org.json.JSONObject;
import org.json.JSONArray;
import com.google.gson.Gson;

import com.linecorp.bot.client.LineSignatureValidator;
import com.linecorp.bot.client.LineMessagingServiceBuilder;

import com.cloudinary.*;
import com.cloudinary.utils.ObjectUtils;

import com.ibm.watson.developer_cloud.conversation.v1.ConversationService;
import com.ibm.watson.developer_cloud.conversation.v1.model.*;
import com.ibm.watson.developer_cloud.retrieve_and_rank.v1.RetrieveAndRank;
import com.ibm.watson.developer_cloud.retrieve_and_rank.v1.model.*;

import com.linecorp.example.internalcom.model.*;

@RestController
@RequestMapping(value="/linebot")
public class LineBotController
{
    @Autowired
    @Qualifier("com.linecorp.channel_secret")
    String lChannelSecret;
    
    @Autowired
    @Qualifier("com.linecorp.channel_access_token")
    String lChannelAccessToken;
    
    @Autowired
    @Qualifier("com.ibm.conversations.username")
    String cUsername;
    
    @Autowired
    @Qualifier("com.ibm.conversations.password")
    String cPassword;
    
    @Autowired
    @Qualifier("com.ibm.rr.username")
    String rUsername;
    
    @Autowired
    @Qualifier("com.ibm.rr.password")
    String rPassword;

    @RequestMapping(value="/callback", method=RequestMethod.POST)
    public ResponseEntity<String> callback(
        @RequestHeader("X-Line-Signature") String aXLineSignature,
        @RequestBody String aPayload)
    {
        // compose body
        final String text=String.format("The Signature is: %s",
            (aXLineSignature!=null && aXLineSignature.length() > 0) ? aXLineSignature : "N/A");
        
        System.out.println(text);
        
        final boolean valid=new LineSignatureValidator(lChannelSecret.getBytes()).validateSignature(aPayload.getBytes(), aXLineSignature);
        
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
        String idTarget = " ";
        String eventType = payload.events[0].type;
        String reply_token = payload.events[0].replyToken;
        
        if (eventType.equals("join")){
            if (payload.events[0].source.type.equals("group")){
                replyToUser(reply_token, "Hello Group");
            }
            if (payload.events[0].source.type.equals("room")){
                replyToUser(reply_token, "Hello Room");
            }
        } else if (eventType.equals("message")){
            if (payload.events[0].source.type.equals("group")){
                idTarget = payload.events[0].source.groupId;
            } else if (payload.events[0].source.type.equals("room")){
                idTarget = payload.events[0].source.roomId;
            } else if (payload.events[0].source.type.equals("user")){
                idTarget = payload.events[0].source.userId;
            }
            
            //Parsing message from user
            if (!payload.events[0].message.type.equals("text")){
                replyToUser(reply_token, "Unknown message");
            } else {
                //Get data from conversation and R&R services
                msgText = payload.events[0].message.text;
                msgText = msgText.toLowerCase();
                
//                getInsight(msgText);
                
//                String emo = new String(Character.toChars(0x10008D));
                replyToUser(reply_token, "Hello");
            }
        }
         
        return new ResponseEntity<String>(HttpStatus.OK);
    }
    
    private void getInsight(String text){
        ConversationService service = new ConversationService(ConversationService.VERSION_DATE_2016_07_11);
        service.setUsernameAndPassword(cUsername, cPassword);
        
        MessageRequest newMessage = new MessageRequest.Builder().inputText(text).build();
        MessageResponse response = service.message("Conversation-ic", newMessage).execute();
        System.out.println(response);
    }
    
//    private void getData(String text){
//        RetrieveAndRank service = new RetrieveAndRank();
//        service.setUsernameAndPassword(rUsername, rPassword);
//        
//        // 1 create the Solr Cluster
//        SolrClusterOptions options = new SolrClusterOptions("my-cluster-name", 1);
//        SolrCluster cluster = service.createSolrCluster(options).execute();
//        System.out.println("Solr cluster: " + cluster);
//        
//        // 2 wait until the Solr Cluster is available
//        while (cluster.getStatus() == Status.NOT_AVAILABLE) {
//            Thread.sleep(10000); // sleep 10 seconds
//            cluster = service.getSolrCluster(cluster.getId()).execute();
//            System.out.println("Solr cluster status: " + cluster.getStatus());
//        }
//        
//        // 3 list Solr Clusters
//        System.out.println("Solr clusters: " + service.getSolrClusters().execute());
//    }
    
    private void replyToUser(String reply_token, String message_text){
        String url = "https://api.line.me/v2/bot/message/reply";
        
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(url);
        
        try{
            // add header
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Authorization", "Bearer " + lChannelAccessToken);
            
            String jsonData = "{\"replyToken\":\""+reply_token+"\",\"messages\":[{\"type\":\"text\",\"text\":\""+message_text+"\"}]}";
            System.out.println(jsonData);
            
            StringEntity params =new StringEntity(jsonData);
            
            post.setEntity(params);
            
            HttpResponse response = client.execute(post);
            System.out.println("Response Code : "
                               + response.getStatusLine().getStatusCode());
            
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            
            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException e){
            System.out.println("Exception is raised ");
            e.printStackTrace();
        }
    }
}
