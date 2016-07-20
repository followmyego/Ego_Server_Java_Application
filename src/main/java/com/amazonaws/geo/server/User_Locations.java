package com.amazonaws.geo.server;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "user_locations")
public class User_Locations {
    private int hashKey;
    private String facebookId;
    private String geoJson;
    private String geoHash;

    @DynamoDBHashKey(attributeName = "hashKey")
    @DynamoDBAttribute(attributeName = "hashKey")
    public int getHashKey() {
        return hashKey;
    }

    public void setHashKey(int hashKey) {
        this.hashKey = hashKey;
    }

    @DynamoDBRangeKey(attributeName = "rangeKey")
    public String getFacebookId(){
        return facebookId;
    }

    public void setFacebookId(String facebookId){
        this.facebookId = facebookId;
    }

    @DynamoDBAttribute(attributeName = "geoJson")
    public String getGeoJson() {
        return geoJson;
    }

    public void setGeoJson(String geoJson) {
        this.geoJson = geoJson;
    }

    @DynamoDBAttribute(attributeName = "geohash")
    public String getGeoHash() {
        return geoHash;
    }

    public void setGeoHash(String geoHash) {
        this.geoHash = geoHash;
    }
}
