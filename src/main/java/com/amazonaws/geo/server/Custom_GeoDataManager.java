package com.amazonaws.geo.server;

import com.amazonaws.AmazonClientException;
import com.amazonaws.geo.GeoDataManager;
import com.amazonaws.geo.GeoDataManagerConfiguration;
import com.amazonaws.geo.dynamodb.internal.DynamoDBManager;
import com.amazonaws.geo.dynamodb.internal.DynamoDBUtil;
import com.amazonaws.geo.s2.internal.S2Manager;
import com.amazonaws.geo.s2.internal.S2Util;
import com.amazonaws.geo.util.GeoJsonMapper;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2CellUnion;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2LatLngRect;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


import com.amazonaws.geo.model.DeletePointRequest;
import com.amazonaws.geo.model.DeletePointResult;
import com.amazonaws.geo.model.GeoPoint;
import com.amazonaws.geo.model.GeoQueryRequest;
import com.amazonaws.geo.model.GeoQueryResult;
import com.amazonaws.geo.model.GeohashRange;
import com.amazonaws.geo.model.GetPointRequest;
import com.amazonaws.geo.model.GetPointResult;
import com.amazonaws.geo.model.PutPointRequest;
import com.amazonaws.geo.model.PutPointResult;
import com.amazonaws.geo.model.QueryRadiusRequest;
import com.amazonaws.geo.model.QueryRadiusResult;
import com.amazonaws.geo.model.QueryRectangleRequest;
import com.amazonaws.geo.model.QueryRectangleResult;
import com.amazonaws.geo.model.UpdatePointRequest;
import com.amazonaws.geo.model.UpdatePointResult;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;

/**
 * <p>
 * Manager to hangle geo spatial data in Amazon DynamoDB tables. All service calls made using this client are blocking,
 * and will not return until the service call completes.
 * </p>
 * <p>
 * This class is designed to be thread safe; however, once constructed GeoDataManagerConfiguration should not be
 * modified. Modifying GeoDataManagerConfiguration may cause unspecified behaviors.
 * </p>
 * */

public class Custom_GeoDataManager {
    private GeoDataManagerConfiguration_Custom config;
    private DynamoDBManager_Custom dynamoDBManager;
    private int count;

    public Custom_GeoDataManager(GeoDataManagerConfiguration_Custom config) {
        this.config = config;
        this.dynamoDBManager = new DynamoDBManager_Custom(this.config);
    }

    public GeoDataManagerConfiguration_Custom getGeoDataManagerConfiguration() {
        return this.config;
    }

    public PutPointResult putPoint(PutPointRequest putPointRequest) {
        return this.dynamoDBManager.putPoint(putPointRequest);
    }

    public GetPointResult getPoint(GetPointRequest getPointRequest) {
        return this.dynamoDBManager.getPoint(getPointRequest);
    }

    public QueryRectangleResult queryRectangle(QueryRectangleRequest queryRectangleRequest) {
        S2LatLngRect latLngRect = S2Util.getBoundingLatLngRect(queryRectangleRequest);
        S2CellUnion cellUnion = S2Manager.findCellIds(latLngRect);
        List ranges = this.mergeCells(cellUnion);
        cellUnion = null;
        return new QueryRectangleResult(this.dispatchQueries(ranges, queryRectangleRequest));
    }

    public QueryRadiusResult queryRadius(QueryRadiusRequest queryRadiusRequest, int count) {
        this.count = count;
        S2LatLngRect latLngRect = S2Util.getBoundingLatLngRect(queryRadiusRequest);
        S2CellUnion cellUnion = S2Manager.findCellIds(latLngRect);
        List ranges = this.mergeCells(cellUnion);
        cellUnion = null;
        return new QueryRadiusResult(this.dispatchQueries(ranges, queryRadiusRequest));
    }

    public UpdatePointResult updatePoint(UpdatePointRequest updatePointRequest) {
        return this.dynamoDBManager.updatePoint(updatePointRequest);
    }

    public DeletePointResult deletePoint(DeletePointRequest deletePointRequest) {
        return this.dynamoDBManager.deletePoint(deletePointRequest);
    }

    private List<GeohashRange> mergeCells(S2CellUnion cellUnion) {
        ArrayList ranges = new ArrayList();
        Iterator i$ = cellUnion.cellIds().iterator();

        while(i$.hasNext()) {
            S2CellId c = (S2CellId)i$.next();
            GeohashRange range = new GeohashRange(c.rangeMin().id(), c.rangeMax().id());
            boolean wasMerged = false;
            Iterator i$1 = ranges.iterator();

            while(i$1.hasNext()) {
                GeohashRange r = (GeohashRange)i$1.next();
                if(r.tryMerge(range)) {
                    wasMerged = true;
                    break;
                }
            }

            if(!wasMerged) {
                ranges.add(range);
            }
        }

        return ranges;
    }

    private GeoQueryResult dispatchQueries(List<GeohashRange> ranges, GeoQueryRequest geoQueryRequest) {
        GeoQueryResult geoQueryResult = new GeoQueryResult();
        ExecutorService executorService = this.config.getExecutorService();
        ArrayList futureList = new ArrayList();
        Iterator i = ranges.iterator();

        while(i.hasNext()) {
            GeohashRange e = (GeohashRange)i.next();
            Iterator j = e.trySplit(this.config.getHashKeyLength()).iterator();

            while(j.hasNext()) {
                GeohashRange range = (GeohashRange)j.next();
                GeoQueryThread geoQueryThread = new GeoQueryThread(geoQueryRequest, geoQueryResult, range);
                futureList.add(executorService.submit(geoQueryThread));
            }
        }

        ranges = null;

        for(int var12 = 0; var12 < futureList.size(); ++var12) {
            try {
                ((Future)futureList.get(var12)).get();
            } catch (Exception var11) {
                for(int var13 = var12 + 1; var13 < futureList.size(); ++var13) {
                    ((Future)futureList.get(var13)).cancel(true);
                }

                throw new AmazonClientException("Querying Amazon DynamoDB failed.", var11);
            }
        }

        futureList = null;
        return geoQueryResult;
    }

    private List<Map<String, AttributeValue>> filter(List<Map<String, AttributeValue>> list, GeoQueryRequest geoQueryRequest) {
        ArrayList result = new ArrayList();
        ArrayList<double[]> points = new ArrayList<double[]>();
        ArrayList<String> facebookIds = new ArrayList<String>();
        double[] centerPointCoordinates = new double[2];

        GeoPoint centerPoint = null;
        S2LatLngRect latLngRect = null;
        S2LatLng centerLatLng = null;
        double radiusInMeter = 0.0D;
        if(geoQueryRequest instanceof QueryRectangleRequest) {
            latLngRect = S2Util.getBoundingLatLngRect(geoQueryRequest);
        } else if(geoQueryRequest instanceof QueryRadiusRequest) {
            centerPoint = ((QueryRadiusRequest)geoQueryRequest).getCenterPoint();
            centerLatLng = S2LatLng.fromDegrees(centerPoint.getLatitude(), centerPoint.getLongitude());
        }



        if(centerPoint != null){
            centerPointCoordinates[0] = centerPoint.getLongitude();
            centerPointCoordinates[1] = centerPoint.getLatitude();
        }


        Iterator iterator = list.iterator();
        while(true) {
            while(iterator.hasNext()) {
                Map item = (Map)iterator.next();
                String geoJson = ((AttributeValue)item.get(this.config.getGeoJsonAttributeName())).getS();
                String rangeKey = ((AttributeValue)item.get(this.config.getRangeKeyAttributeName())).getS();
                GeoPoint geoPoint = GeoJsonMapper.geoPointFromString(geoJson);
                double latitude = geoPoint.getLatitude();
                double longitude = geoPoint.getLongitude();


                double[] point = { latitude, longitude };
                points.add(point);
                facebookIds.add(rangeKey);

            }



            while(points.size() != 0){
                //find the nearestPoint
                int nearestPoint = nearestPoint(centerPointCoordinates, points);

                //remove item at index nearest point
                points.remove(nearestPoint);

                //Add the item to the new ArrayList
                Map item = (Map)list.get(nearestPoint);
                result.add(item);
                list.remove(nearestPoint);
            };



            //Determine what users to return back based on how far the user is scrolled in the map
            ArrayList newResult = new ArrayList();
            int increment = count * 16;
        if(count == 0){
            for(int i = 0; i < 16; i++){
                if(result.size() > i){
                    newResult.add(result.get(i));
                } else {
                    return newResult;
                }
            }
        } else {
            for(int i = increment; i < increment + 16; i++
                    ){
                if(result.size() > i){
                    newResult.add(result.get(i));
                } else {
                    return newResult;
                }
            }
        }
            return newResult;
        }

    }


    private static double distance(double x1, double y1, double x2, double y2 ){
        double x = Math.pow(x2 - x1, 2);
        double y = Math.pow(y2 - y1, 2);
        return Math.sqrt( x + y);
    }

    private static int nearestPoint(double[] coordinate, ArrayList<double[]> points){
        final int X = 0;
        final int Y = 1;
        int itemNumber = 0;
        double[] closestPoint = points.get(0);
        double closestDist = distance(coordinate[X], coordinate[Y], closestPoint[X], closestPoint[X]);

        //Traverse the ArrayList
        for(int i = 0; i < points.size(); i++){
            double dist = distance(coordinate[X], coordinate[Y], points.get(i)[X], points.get(i)[Y]);
            if (dist < closestDist){
                closestDist = dist;
                closestPoint = points.get(i);
                itemNumber = i;
            }
        }
        return itemNumber;
    }

    private class GeoQueryThread extends Thread {
        private GeoQueryRequest geoQueryRequest;
        private GeoQueryResult geoQueryResult;
        private GeohashRange range;

        public GeoQueryThread(GeoQueryRequest geoQueryRequest, GeoQueryResult geoQueryResult, GeohashRange range) {
            this.geoQueryRequest = geoQueryRequest;
            this.geoQueryResult = geoQueryResult;
            this.range = range;
        }

        public void run() {
            QueryRequest queryRequest = DynamoDBUtil.copyQueryRequest(this.geoQueryRequest.getQueryRequest());
            long hashKey = S2Manager.generateHashKey(this.range.getRangeMin(), Custom_GeoDataManager.this.config.getHashKeyLength());
            List queryResults = Custom_GeoDataManager.this.dynamoDBManager.queryGeohash(queryRequest, hashKey, this.range, count);
            Iterator iterator = queryResults.iterator();


            while(iterator.hasNext()) {
                QueryResult queryResult = (QueryResult)iterator.next();
                if(this.isInterrupted()) {
                    return;
                }

                this.geoQueryResult.getQueryResults().add(queryResult);
                List filteredQueryResult = Custom_GeoDataManager.this.filter(queryResult.getItems(), this.geoQueryRequest);
                this.geoQueryResult.getItem().addAll(filteredQueryResult);
            }

        }
    }
}


