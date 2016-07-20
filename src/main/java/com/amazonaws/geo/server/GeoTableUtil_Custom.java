package com.amazonaws.geo.server;

/**
 * Created by Lucas on 20/07/2016.
 */
import com.amazonaws.geo.GeoDataManagerConfiguration;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

public class GeoTableUtil_Custom {
    public GeoTableUtil_Custom() {
    }

    public static CreateTableRequest getCreateTableRequest(GeoDataManagerConfiguration_Custom config) {
        CreateTableRequest createTableRequest = (new CreateTableRequest()).withTableName(config.getTableName()).withProvisionedThroughput((new ProvisionedThroughput()).withReadCapacityUnits(Long.valueOf(10L)).withWriteCapacityUnits(Long.valueOf(5L))).withKeySchema(new KeySchemaElement[]{(new KeySchemaElement()).withKeyType(KeyType.HASH).withAttributeName(config.getHashKeyAttributeName()), (new KeySchemaElement()).withKeyType(KeyType.RANGE).withAttributeName(config.getRangeKeyAttributeName())}).withAttributeDefinitions(new AttributeDefinition[]{(new AttributeDefinition()).withAttributeType(ScalarAttributeType.N).withAttributeName(config.getHashKeyAttributeName()), (new AttributeDefinition()).withAttributeType(ScalarAttributeType.S).withAttributeName(config.getRangeKeyAttributeName()), (new AttributeDefinition()).withAttributeType(ScalarAttributeType.N).withAttributeName(config.getGeohashAttributeName())}).withLocalSecondaryIndexes(new LocalSecondaryIndex[]{(new LocalSecondaryIndex()).withIndexName(config.getGeohashIndexName()).withKeySchema(new KeySchemaElement[]{(new KeySchemaElement()).withKeyType(KeyType.HASH).withAttributeName(config.getHashKeyAttributeName()), (new KeySchemaElement()).withKeyType(KeyType.RANGE).withAttributeName(config.getGeohashAttributeName())}).withProjection((new Projection()).withProjectionType(ProjectionType.ALL))});
        return createTableRequest;
    }
}