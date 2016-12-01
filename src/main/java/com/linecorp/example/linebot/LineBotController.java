
package com.linecorp.example.linebot;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/linebot")
public class LineBotController
{
    @RequestMapping(value="/callback", method=RequestMethod.POST)
    public ResponseEntity<String> callback(@RequestHeader("X-Line-Signature") String aXLineSignature)
    {        
        // compose body
        final String text=String.format("The Signature is: %s",
            (aXLineSignature!=null && aXLineSignature.length() > 0) ? aXLineSignature : "N/A");
        
        System.out.println(text);
        
        return new ResponseEntity<String>("Hello", HttpStatus.OK);
    }
};
