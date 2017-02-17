
package com.linecorp.example.internalcom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

@Configuration
@PropertySource("classpath:application.properties")
public class Config
{
    @Autowired
    Environment mEnv;
    
    @Bean(name="com.linecorp.channel_secret")
    public String getChannelSecret()
    {
        return mEnv.getProperty("com.linecorp.channel_secret");
    }
    
    @Bean(name="com.linecorp.channel_access_token")
    public String getChannelAccessToken()
    {
        return mEnv.getProperty("com.linecorp.channel_access_token");
    }
    
    @Bean(name="com.ibm.conversations.username")
    public String getConversationsUsername()
    {
        return mEnv.getProperty("com.ibm.conversations.username");
    }
    
    @Bean(name="com.ibm.conversations.password")
    public String getConversationsPassword()
    {
        return mEnv.getProperty("com.ibm.conversations.password");
    }
    
    @Bean(name="com.ibm.rr.username")
    public String getRRUsername()
    {
        return mEnv.getProperty("com.ibm.rr.username");
    }
    
    @Bean(name="com.ibm.rr.password")
    public String getRRPassword()
    {
        return mEnv.getProperty("com.ibm.rr.password");
    }
};
