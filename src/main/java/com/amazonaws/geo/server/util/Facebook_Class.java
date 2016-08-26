package com.amazonaws.geo.server.util;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.types.User;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by Lucas on 09/08/2016.
 */
public class Facebook_Class {
    private static String accessToken;
    public static Properties properties = new Properties();
//    public FacebookClass(String accessToken) {
//        this.accessToken = accessToken;
//    }

    public static String getHometown(String accessToken){
        FacebookClient fbClient = null;
        try{
            properties.load(new FileInputStream("fb.properties"));
            fbClient = new DefaultFacebookClient(accessToken);
        } catch (FileNotFoundException e) {

        } catch (IOException e) {
            e.printStackTrace();
        }

        if(fbClient != null){
            User me = fbClient.fetchObject("me", User.class);
            return me.getHometownName();
        }
        return "someTown";
    }


}

