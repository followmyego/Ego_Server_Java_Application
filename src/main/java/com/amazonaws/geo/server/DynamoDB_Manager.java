package com.amazonaws.geo.server;


import com.amazonaws.geo.model.DeletePointRequest;
import com.amazonaws.geo.model.DeletePointResult;
import com.amazonaws.geo.model.GeohashRange;
import com.amazonaws.geo.model.GetPointRequest;
import com.amazonaws.geo.model.GetPointResult;
import com.amazonaws.geo.model.PutPointRequest;
import com.amazonaws.geo.model.PutPointResult;
import com.amazonaws.geo.model.UpdatePointRequest;
import com.amazonaws.geo.model.UpdatePointResult;
import com.amazonaws.geo.s2.internal.S2Manager;
import com.amazonaws.geo.util.GeoJsonMapper;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnConsumedCapacity;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamoDB_Manager {
    private GeoDataManager_Configuration config;

    public DynamoDB_Manager(GeoDataManager_Configuration config) {
        this.config = config;
    }

    //The argument "count" is used to determine how far the user is scrolled in the map
    public List<QueryResult> queryGeohash(QueryRequest queryRequest, long hashKey, GeohashRange range, int count) {
        ArrayList queryResults = new ArrayList();
        Map lastEvaluatedKey = null;
        int counter = 0;
        do {
            //Create a HashMap to hold our Conditions
            HashMap keyConditions = new HashMap();

            //Create a Condition: Item must == this hashkey
            Condition hashKeyCondition = (new Condition())
                    .withComparisonOperator(ComparisonOperator.EQ)
                    .withAttributeValueList(new AttributeValue[]{(new AttributeValue()).withN(String.valueOf(hashKey))});

            //Add condition to our HashMap of Conditions
            keyConditions.put(this.config.getHashKeyAttributeName(), hashKeyCondition);

            //Create the attribute values for the minimum and maximum range
            AttributeValue minRange = (new AttributeValue()).withN(Long.toString(range.getRangeMin()));
            AttributeValue maxRange = (new AttributeValue()).withN(Long.toString(range.getRangeMax()));

            //Create a Condition:
            Condition geohashCondition = (new Condition())
                    .withComparisonOperator(ComparisonOperator.BETWEEN)
                    .withAttributeValueList(new AttributeValue[]{minRange, maxRange});

            //Add condition to our HashMap of Conditions
            keyConditions.put(this.config.getGeohashAttributeName(), geohashCondition);

            //Set up the query request with the Key Conditions and Index name
            queryRequest.withTableName(this.config.getTableName())
                    .withKeyConditions(keyConditions)
                    .withIndexName(this.config.getGeohashIndexName())
                    .withConsistentRead(Boolean.valueOf(true))
                    .withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);

            if(lastEvaluatedKey != null) {
                queryRequest.addExclusiveStartKeyEntry(this.config.getHashKeyAttributeName(), (AttributeValue)lastEvaluatedKey.get(this.config.getHashKeyAttributeName()));
            }

            QueryResult queryResult = this.config.getDynamoDBClient().query(queryRequest);
            queryResults.add(queryResult);

            lastEvaluatedKey = queryResult.getLastEvaluatedKey();

        } while(lastEvaluatedKey != null);

        return queryResults;
    }

    public GetPointResult getPoint(GetPointRequest getPointRequest) {
        long geohash = S2Manager.generateGeohash(getPointRequest.getGeoPoint());
        long hashKey = S2Manager.generateHashKey(geohash, this.config.getHashKeyLength());
        GetItemRequest getItemRequest = getPointRequest.getGetItemRequest();
        getItemRequest.setTableName(this.config.getTableName());
        AttributeValue hashKeyValue = (new AttributeValue()).withN(String.valueOf(hashKey));
        getItemRequest.getKey().put(this.config.getHashKeyAttributeName(), hashKeyValue);
        getItemRequest.getKey().put(this.config.getRangeKeyAttributeName(), getPointRequest.getRangeKeyValue());
        GetItemResult getItemResult = this.config.getDynamoDBClient().getItem(getItemRequest);
        GetPointResult getPointResult = new GetPointResult(getItemResult);
        return getPointResult;
    }

    public PutPointResult putPoint(PutPointRequest putPointRequest) {
        long geohash = S2Manager.generateGeohash(putPointRequest.getGeoPoint());
        long hashKey = S2Manager.generateHashKey(geohash, this.config.getHashKeyLength());
        String geoJson = GeoJsonMapper.stringFromGeoObject(putPointRequest.getGeoPoint());
        PutItemRequest putItemRequest = putPointRequest.getPutItemRequest();
        putItemRequest.setTableName(this.config.getTableName());
        AttributeValue hashKeyValue = (new AttributeValue()).withN(String.valueOf(hashKey));
        putItemRequest.getItem().put(this.config.getHashKeyAttributeName(), hashKeyValue);
        putItemRequest.getItem().put(this.config.getRangeKeyAttributeName(), putPointRequest.getRangeKeyValue());
        AttributeValue geohashValue = (new AttributeValue()).withN(Long.toString(geohash));
        putItemRequest.getItem().put(this.config.getGeohashAttributeName(), geohashValue);
        AttributeValue geoJsonValue = (new AttributeValue()).withS(geoJson);
        putItemRequest.getItem().put(this.config.getGeoJsonAttributeName(), geoJsonValue);
        PutItemResult putItemResult = this.config.getDynamoDBClient().putItem(putItemRequest);
        PutPointResult putPointResult = new PutPointResult(putItemResult);
        return putPointResult;
    }

    public UpdatePointResult updatePoint(UpdatePointRequest updatePointRequest) {
        long geohash = S2Manager.generateGeohash(updatePointRequest.getGeoPoint());
        long hashKey = S2Manager.generateHashKey(geohash, this.config.getHashKeyLength());
        UpdateItemRequest updateItemRequest = updatePointRequest.getUpdateItemRequest();
        updateItemRequest.setTableName(this.config.getTableName());
        AttributeValue hashKeyValue = (new AttributeValue()).withN(String.valueOf(hashKey));
        updateItemRequest.getKey().put(this.config.getHashKeyAttributeName(), hashKeyValue);
        updateItemRequest.getKey().put(this.config.getRangeKeyAttributeName(), updatePointRequest.getRangeKeyValue());
        updateItemRequest.getAttributeUpdates().remove(this.config.getGeohashAttributeName());
        updateItemRequest.getAttributeUpdates().remove(this.config.getGeoJsonAttributeName());
        UpdateItemResult updateItemResult = this.config.getDynamoDBClient().updateItem(updateItemRequest);
        UpdatePointResult updatePointResult = new UpdatePointResult(updateItemResult);
        return updatePointResult;
    }

    public DeletePointResult deletePoint(DeletePointRequest deletePointRequest) {
        long geohash = S2Manager.generateGeohash(deletePointRequest.getGeoPoint());
        long hashKey = S2Manager.generateHashKey(geohash, this.config.getHashKeyLength());
        DeleteItemRequest deleteItemRequest = deletePointRequest.getDeleteItemRequest();
        deleteItemRequest.setTableName(this.config.getTableName());
        AttributeValue hashKeyValue = (new AttributeValue()).withN(String.valueOf(hashKey));
        deleteItemRequest.getKey().put(this.config.getHashKeyAttributeName(), hashKeyValue);
        deleteItemRequest.getKey().put(this.config.getRangeKeyAttributeName(), deletePointRequest.getRangeKeyValue());
        DeleteItemResult deleteItemResult = this.config.getDynamoDBClient().deleteItem(deleteItemRequest);
        DeletePointResult deletePointResult = new DeletePointResult(deleteItemResult);
        return deletePointResult;
    }
}
