package com.amazonaws.geo.server;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;


/**
 * Created by Lucas on 20/06/2016.
 */
@DynamoDBTable(tableName = "HashReference")
public class HashReference {
    private String facebookId;
    private String hashKey;

    @DynamoDBHashKey(attributeName = "facebook_id")
    public String getFacebookId(){
        return facebookId;
    }

    public void setFacebookId(String facebookId){
        this.facebookId = facebookId;
    }


    @DynamoDBAttribute(attributeName = "hashKey")
    public String getHashKey() {
        return hashKey;
    }

    public void setHashKey(String hashKey) {
        this.hashKey = hashKey;
    }


}

