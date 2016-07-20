package com.amazonaws.geo.server;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeoDataManagerConfiguration_Custom {
    public static final long MERGE_THRESHOLD = 2L;
    private static final String DEFAULT_HASHKEY_ATTRIBUTE_NAME = "hashKey";
    private static final String DEFAULT_RANGEKEY_ATTRIBUTE_NAME = "rangeKey";
    private static final String DEFAULT_GEOHASH_ATTRIBUTE_NAME = "geohash";
    private static final String DEFAULT_GEOJSON_ATTRIBUTE_NAME = "geoJson";
    private static final String DEFAULT_GEOHASH_INDEX_ATTRIBUTE_NAME = "geohash-index";
    private static final int DEFAULT_HASHKEY_LENGTH = 5;
    private static final int DEFAULT_THREAD_POOL_SIZE = 10;
    private String tableName;
    private String hashKeyAttributeName = "hashKey";
    private String rangeKeyAttributeName = "rangeKey";
    private String geohashAttributeName = "geohash";
    private String geoJsonAttributeName = "geoJson";
    private String geohashIndexName = "geohash-index";
    private int hashKeyLength = 5;
    private AmazonDynamoDBClient dynamoDBClient;
    private ExecutorService executorService;

    public GeoDataManagerConfiguration_Custom(AmazonDynamoDBClient dynamoDBClient, String tableName) {
        this.dynamoDBClient = dynamoDBClient;
        this.tableName = tableName;
    }

    public String getTableName() {
        return this.tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getHashKeyAttributeName() {
        return this.hashKeyAttributeName;
    }

    public void setHashKeyAttributeName(String hashKeyAttributeName) {
        this.hashKeyAttributeName = hashKeyAttributeName;
    }

    public GeoDataManagerConfiguration_Custom withHashKeyAttributeName(String hashKeyAttributeName) {
        this.setHashKeyAttributeName(hashKeyAttributeName);
        return this;
    }

    public String getRangeKeyAttributeName() {
        return this.rangeKeyAttributeName;
    }

    public void setRangeKeyAttributeName(String rangeKeyAttributeName) {
        this.rangeKeyAttributeName = rangeKeyAttributeName;
    }

    public GeoDataManagerConfiguration_Custom withRangeKeyAttributeName(String rangeKeyAttributeName) {
        this.setRangeKeyAttributeName(rangeKeyAttributeName);
        return this;
    }

    public String getGeohashAttributeName() {
        return this.geohashAttributeName;
    }

    public void setGeohashAttributeName(String geohashAttributeName) {
        this.geohashAttributeName = geohashAttributeName;
    }

    public GeoDataManagerConfiguration_Custom withGeohashAttributeName(String geohashAttributeName) {
        this.setGeohashAttributeName(geohashAttributeName);
        return this;
    }

    public String getGeoJsonAttributeName() {
        return this.geoJsonAttributeName;
    }

    public void setGeoJsonAttributeName(String geoJsonAttributeName) {
        this.geoJsonAttributeName = geoJsonAttributeName;
    }

    public GeoDataManagerConfiguration_Custom withGeoJsonAttributeName(String geoJsonAttributeName) {
        this.setGeoJsonAttributeName(geoJsonAttributeName);
        return this;
    }

    public String getGeohashIndexName() {
        return this.geohashIndexName;
    }

    public void setGeohashIndexName(String geohashIndexName) {
        this.geohashIndexName = geohashIndexName;
    }

    public GeoDataManagerConfiguration_Custom withGeohashIndexName(String geohashIndexName) {
        this.setGeohashIndexName(geohashIndexName);
        return this;
    }

    public int getHashKeyLength() {
        return this.hashKeyLength;
    }

    public void setHashKeyLength(int hashKeyLength) {
        this.hashKeyLength = hashKeyLength;
    }

    public GeoDataManagerConfiguration_Custom withHashKeyLength(int hashKeyLength) {
        this.setHashKeyLength(hashKeyLength);
        return this;
    }

    public AmazonDynamoDBClient getDynamoDBClient() {
        return this.dynamoDBClient;
    }

    public void setDynamoDBClient(AmazonDynamoDBClient dynamoDBClient) {
        this.dynamoDBClient = dynamoDBClient;
    }

    public ExecutorService getExecutorService() {
        synchronized(this) {
            if(this.executorService == null) {
                this.executorService = Executors.newFixedThreadPool(10);
            }
        }

        return this.executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        synchronized(this) {
            this.executorService = executorService;
        }
    }
}

