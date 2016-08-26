/*
 * Copyright 2010-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.geo.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.geo.GeoDataManager;
import com.amazonaws.geo.s2.internal.S2Manager;
import com.amazonaws.geo.server.util.Facebook_Class;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.identitymanagement.model.User;
import com.amazonaws.util.json.JSONArray;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.geo.model.DeletePointRequest;
import com.amazonaws.geo.model.DeletePointResult;
import com.amazonaws.geo.model.GeoPoint;
import com.amazonaws.geo.model.GeoQueryResult;
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
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;


/**
 * Servlet implementation class GeoDynamoDBServlet
 */
public class GeoDynamoDBServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private GeoDataManagerConfiguration_Custom config;
	private Custom_GeoDataManager geoDataManager;
	private GeoDataManager geoDataManager2;

	private DynamoDBMapper dynamoDBMapper;

	private ObjectMapper mapper;
	private JsonFactory factory;
	private String facebookId;

	public void init() throws ServletException {
		setupGeoDataManager();

		mapper = new ObjectMapper();
		factory = mapper.getJsonFactory();
	}

	private void setupGeoDataManager() {
		String accessKey = System.getProperty("AWS_ACCESS_KEY_ID");
		String secretKey = System.getProperty("AWS_SECRET_KEY");
		String tableName = System.getProperty("PARAM1");
		String regionName = System.getProperty("PARAM2");

		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
		AmazonDynamoDBClient ddb = new AmazonDynamoDBClient(credentials);
		Region region = Region.getRegion(Regions.fromName(regionName));
		ddb.setRegion(region);

		config = new GeoDataManagerConfiguration_Custom(ddb, tableName);
		geoDataManager = new Custom_GeoDataManager(config);
		dynamoDBMapper = new DynamoDBMapper(ddb);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		try {
			StringBuffer buffer = new StringBuffer();
			String line = null;
			BufferedReader reader = request.getReader();

			while ((line = reader.readLine()) != null) {
				buffer.append(line);
			}

			JSONObject jsonObject = new JSONObject(buffer.toString());
			PrintWriter out = response.getWriter();

			String action = jsonObject.getString("action");
			log("action: " + action);
			JSONObject requestObject = jsonObject.getJSONObject("request");
			log("requestObject: " + requestObject);
			log("section123");
			if (action.equalsIgnoreCase("put-point")) {
				putPoint(requestObject, out);
			} else if (action.equalsIgnoreCase("get-point")) {
				getPoint(requestObject, out);
			} else if (action.equalsIgnoreCase("update-point")) {
				updatePoint(requestObject, out);
			} else if (action.equalsIgnoreCase("query-rectangle")) {
				queryRectangle(requestObject, out);
			} else if (action.equalsIgnoreCase("query-radius")) {
				queryRadius(requestObject, out);
			} else if (action.equalsIgnoreCase("delete-point")) {
				deletePoint(requestObject, out);
			}
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			log(sw.toString());
		}
	}

	private void putPoint(JSONObject requestObject, PrintWriter out) throws IOException, JSONException {
		PutPointResult putPointResult = null;
		String hashToCompare = "";
		GeoPoint geoPoint = new GeoPoint(requestObject.getDouble("lat"), requestObject.getDouble("lng"));
		AttributeValue rangeKeyAttributeValue = new AttributeValue().withS(requestObject.getString("rangeKey"));
//		AttributeValue schoolNameKeyAttributeValue = new AttributeValue().withS(requestObject.getString("schoolName"));
		log("fist section123");
		long geohash = S2Manager.generateGeohash(geoPoint);
		String hashKey = S2Manager.generateHashKey(geohash, this.config.getHashKeyLength()) + "";
		log("second section123");

		//Create the HashReference object to pass to the HashReference table for comparisons.
		facebookId = requestObject.getString("rangeKey");

		log("third section123");

		/** Search item on Id Table for primary key "facebook_id"(rangeKeyAttributeValue) and get the hash key value */
		HashReference idTableItem = null;
		try {
			idTableItem = dynamoDBMapper.load(HashReference.class, facebookId);
        } catch (final AmazonServiceException e) {
			e.printStackTrace();
			log(e.toString());
        }

		if(idTableItem != null){
			hashToCompare = idTableItem.getHashKey();
		} else {
			//Insert new item into HashReference table.
			try{
				idTableItem = new HashReference();
				idTableItem.setFacebookId(facebookId);
				idTableItem.setHashKey(hashKey);
				dynamoDBMapper.save(idTableItem);
			} catch (AmazonClientException e){
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				log(sw.toString());
			}
			hashToCompare = hashKey;
		}

		log("fourth section123");

		if( hashToCompare.equals(hashKey) ){
			//Replace item in location table with new item.
		PutPointRequest putPointRequest = new PutPointRequest(geoPoint, rangeKeyAttributeValue);
//		putPointRequest.getPutItemRequest().addItemEntry("schoolName", schoolNameKeyAttributeValue);
		putPointResult = geoDataManager.putPoint(putPointRequest); //This is where the data is inserted
//			printPutPointResult(putPointResult, out, 0);
			queryRadius(requestObject, out);
 		} else if ( !hashToCompare.equals(hashKey) ){
			//Replace item from id_table and insert new item with new hash key.
			try{
				idTableItem.setHashKey(hashKey);
				dynamoDBMapper.save(idTableItem);
				//Delete previous item in location table because its hash key is old.
				User_Locations itemToDelete = new User_Locations();
				itemToDelete.setHashKey(Integer.parseInt(hashToCompare));
				itemToDelete.setFacebookId(facebookId);
				dynamoDBMapper.delete(itemToDelete);
			} catch (AmazonClientException e){
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				log(sw.toString());
			}

			//Insert new item into location table with newly updated hashkey.
			PutPointRequest putPointRequest = new PutPointRequest(geoPoint, rangeKeyAttributeValue);
//		putPointRequest.getPutItemRequest().addItemEntry("schoolName", schoolNameKeyAttributeValue);
			putPointResult = geoDataManager.putPoint(putPointRequest); //This is where the data is inserted
//			printPutPointResult(putPointResult, out, 1);
			queryRadius(requestObject, out);
		}  else {
			Map<String, String> jsonMap = new HashMap<String, String>();
			jsonMap.put("Nothing", "happened");

			out.println(mapper.writeValueAsString(jsonMap));
			out.flush();
		}


	}

	private void printPutPointResult(PutPointResult putPointResult, PrintWriter out, int i) throws JsonParseException,
			IOException {

		Map<String, String> jsonMap = new HashMap<String, String>();
		jsonMap.put("action", "put-point");
		jsonMap.put("condition", i + "");

		out.println(mapper.writeValueAsString(jsonMap));
		out.flush();

	}

	private void getPoint(JSONObject requestObject, PrintWriter out) throws IOException, JSONException {
		GeoPoint geoPoint = new GeoPoint(requestObject.getDouble("lat"), requestObject.getDouble("lng"));
		AttributeValue rangeKeyAttributeValue = new AttributeValue().withS(requestObject.getString("rangeKey"));

		GetPointRequest getPointRequest = new GetPointRequest(geoPoint, rangeKeyAttributeValue);
		GetPointResult getPointResult = geoDataManager.getPoint(getPointRequest);

		printGetPointRequest(getPointResult, out);
	}

	private void printGetPointRequest(GetPointResult getPointResult, PrintWriter out) throws JsonParseException,
			IOException {
		Map<String, AttributeValue> item = getPointResult.getGetItemResult().getItem();
		String geoJsonString = item.get(config.getGeoJsonAttributeName()).getS();
		JsonParser jsonParser = factory.createJsonParser(geoJsonString);
		JsonNode jsonNode = mapper.readTree(jsonParser);

		double latitude = jsonNode.get("coordinates").get(0).getDoubleValue();
		double longitude = jsonNode.get("coordinates").get(1).getDoubleValue();
		String hashKey = item.get(config.getHashKeyAttributeName()).getN();
		String rangeKey = item.get(config.getRangeKeyAttributeName()).getS();
		String geohash = item.get(config.getGeohashAttributeName()).getN();
//		String schoolName = item.get("schoolName").getS();
		String memo = "";
		if (item.containsKey("memo")) {
			memo = item.get("memo").getS();
		}

		Map<String, String> resultMap = new HashMap<String, String>();
		resultMap.put("latitude", Double.toString(latitude));
		resultMap.put("longitude", Double.toString(longitude));
		resultMap.put("hashKey", hashKey);
		resultMap.put("rangeKey", rangeKey);
		resultMap.put("geohash", geohash);
//		resultMap.put("schoolName", schoolName);
		resultMap.put("memo", memo);

		Map<String, Object> jsonMap = new HashMap<String, Object>();
		jsonMap.put("action", "get-point");
		jsonMap.put("result", resultMap);

		out.println(mapper.writeValueAsString(jsonMap));
		out.flush();
	}

	private void updatePoint(JSONObject requestObject, PrintWriter out) throws IOException, JSONException {
		GeoPoint geoPoint = new GeoPoint(requestObject.getDouble("lat"), requestObject.getDouble("lng"));
		AttributeValue rangeKeyAttributeValue = new AttributeValue().withS(requestObject.getString("rangeKey"));

//		String schoolName = requestObject.getString("schoolName");
//		AttributeValueUpdate schoolNameValueUpdate = null;

		String memo = requestObject.getString("memo");
		AttributeValueUpdate memoValueUpdate = null;

//		if (schoolName == null || schoolName.equalsIgnoreCase("")) {
//			schoolNameValueUpdate = new AttributeValueUpdate().withAction(AttributeAction.DELETE);
//		} else {
//			AttributeValue schoolNameAttributeValue = new AttributeValue().withS(schoolName);
//			schoolNameValueUpdate = new AttributeValueUpdate().withAction(AttributeAction.PUT).withValue(
//					schoolNameAttributeValue);
//		}

		if (memo == null || memo.equalsIgnoreCase("")) {
			memoValueUpdate = new AttributeValueUpdate().withAction(AttributeAction.DELETE);
		} else {
			AttributeValue memoAttributeValue = new AttributeValue().withS(memo);
			memoValueUpdate = new AttributeValueUpdate().withAction(AttributeAction.PUT).withValue(memoAttributeValue);
		}

		UpdatePointRequest updatePointRequest = new UpdatePointRequest(geoPoint, rangeKeyAttributeValue);
//		updatePointRequest.getUpdateItemRequest().addAttributeUpdatesEntry("schoolName", schoolNameValueUpdate);
		updatePointRequest.getUpdateItemRequest().addAttributeUpdatesEntry("memo", memoValueUpdate);

		UpdatePointResult updatePointResult = geoDataManager.updatePoint(updatePointRequest);

		printUpdatePointResult(updatePointResult, out);
	}

	private void printUpdatePointResult(UpdatePointResult updatePointResult, PrintWriter out)
			throws JsonParseException, IOException {

		Map<String, String> jsonMap = new HashMap<String, String>();
		jsonMap.put("action", "update-point");

		out.println(mapper.writeValueAsString(jsonMap));
		out.flush();
	}

	private void queryRectangle(JSONObject requestObject, PrintWriter out) throws IOException, JSONException {
		GeoPoint minPoint = new GeoPoint(requestObject.getDouble("minLat"), requestObject.getDouble("minLng"));
		GeoPoint maxPoint = new GeoPoint(requestObject.getDouble("maxLat"), requestObject.getDouble("maxLng"));
		
		List<String> attributesToGet = new ArrayList<String>();
		attributesToGet.add(config.getRangeKeyAttributeName());
		attributesToGet.add(config.getGeoJsonAttributeName());
//		attributesToGet.add("schoolName");

		QueryRectangleRequest queryRectangleRequest = new QueryRectangleRequest(minPoint, maxPoint);
		queryRectangleRequest.getQueryRequest().setAttributesToGet(attributesToGet);
		QueryRectangleResult queryRectangleResult = geoDataManager.queryRectangle(queryRectangleRequest);

		printGeoQueryResult(queryRectangleResult, out, "someString");
	}

	private void queryRadius(JSONObject requestObject, PrintWriter out) throws IOException, JSONException {
		GeoPoint centerPoint = new GeoPoint(requestObject.getDouble("lat"), requestObject.getDouble("lng"));
		double radiusInMeter = requestObject.getDouble("radiusInMeter");
		int count = requestObject.getInt("count");

		//This is the to test the facebook api working from the server but i am getting a FileNotFoundException.
		String accessToken = requestObject.getString("accessToken");
//		String hometown = Facebook_Class.getHometown(accessToken);
		String hometown = " ";
		
		List<String> attributesToGet = new ArrayList<String>();
		attributesToGet.add(config.getRangeKeyAttributeName());
		attributesToGet.add(config.getGeoJsonAttributeName());
//		attributesToGet.add("schoolName");

		QueryRadiusRequest queryRadiusRequest = new QueryRadiusRequest(centerPoint, radiusInMeter);
		queryRadiusRequest.getQueryRequest().setAttributesToGet(attributesToGet);
		QueryRadiusResult queryRadiusResult = geoDataManager.queryRadius(queryRadiusRequest, count);

		printGeoQueryResult(queryRadiusResult, out, hometown);
	}

	private void printGeoQueryResult(GeoQueryResult geoQueryResult, PrintWriter out, String homeTown) throws JsonParseException,
			IOException {

		Map<String, Object> jsonMap = new HashMap<String, Object>();
		List<Map<String, String>> resultArray = new ArrayList<Map<String, String>>();

		for (Map<String, AttributeValue> item : geoQueryResult.getItem()) {
			Map<String, String> itemMap = new HashMap<String, String>();

			String geoJsonString = item.get(config.getGeoJsonAttributeName()).getS();
			JsonParser jsonParser = factory.createJsonParser(geoJsonString);
			JsonNode jsonNode = mapper.readTree(jsonParser);

			double latitude = jsonNode.get("coordinates").get(0).getDoubleValue();
			double longitude = jsonNode.get("coordinates").get(1).getDoubleValue();
			String rangeKey = item.get(config.getRangeKeyAttributeName()).getS();

			String userToCompare = rangeKey;
			int badgeNumber = getBadge(facebookId, userToCompare);

			itemMap.put("latitude", Double.toString(latitude));
			itemMap.put("longitude", Double.toString(longitude));
			itemMap.put("rangeKey", rangeKey);
			itemMap.put("badge", Integer.toString(badgeNumber));

//			itemMap.put("schoolName", schoolName);

			resultArray.add(itemMap);
		}

		jsonMap.put("action", "query");
//		jsonMap.put("homeTown", homeTown);
		jsonMap.put("result", resultArray);

		out.println(mapper.writeValueAsString(jsonMap));
		out.flush();
	}

	/** This method compares the badge variables of the users against eachother **/
	private int getBadge(String facebookId_1, String facebookId_2) {
		User_Badges userBadges1 = new User_Badges();
		User_Badges userBadges2 = new User_Badges();


		/** Get both of the user's badges **/
		try {
			userBadges1 = dynamoDBMapper.load(User_Badges.class, facebookId_1);
			userBadges2 = dynamoDBMapper.load(User_Badges.class, facebookId_2);
		} catch (final AmazonServiceException e) {
			e.printStackTrace();
			log(e.toString());
		}

		/** Compare the badge items **/
		if(userBadges1 != null && userBadges2 != null){

			//Compare workplace
			if(!userBadges1.getWorkplace_json().equals("") && !userBadges2.getWorkplace_json().equals("")){
				try{
					String string1 = userBadges1.getWorkplace_json();
					JSONObject json_workplace1 = new JSONObject(string1);
					JSONArray jsonArray1 = json_workplace1.getJSONArray("work");

					String string2 = userBadges2.getWorkplace_json();
					JSONObject json_workplace2 = new JSONObject(string2);
					JSONArray jsonArray2 = json_workplace2.getJSONArray("work");

					//Comparison of workplaces here
					//If one of the employers match, return the common workplace badge int
					for(int i = 0; i < jsonArray1.length(); i ++){
						JSONObject jsonObject = jsonArray1.getJSONObject(i);
						String employer = jsonObject.getString("employer");

						if(jsonArray2.getJSONObject(i).getString("employer").equals(employer)){
							return 1;
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			//Compare birthday
			if(!userBadges1.getBirthday().equals("") && !userBadges2.getBirthday().equals("")){
				if(userBadges1.getBirthday().equals(userBadges2.getBirthday())){
					return 2;
				}
			}

			//Compare skills
			if(!userBadges1.getProfessionalSkills_json().equals("") && !userBadges2.getProfessionalSkills_json().equals("")){
				try{
					String string1 = userBadges1.getProfessionalSkills_json();
					JSONObject json_skills1 = new JSONObject(string1);
					JSONArray jsonArray1 = json_skills1.getJSONArray("skills");

					String string2 = userBadges2.getProfessionalSkills_json();
					JSONObject json_skills2 = new JSONObject(string2);
					JSONArray jsonArray2 = json_skills2.getJSONArray("skills");

					//Comparison of skills here
					//If one of the skills match, return the common skill badge int
					for(int i = 0; i < jsonArray1.length(); i ++){
						String skill = jsonArray1.get(i).toString();
						if(jsonArray2.toString().contains(skill)){
							return 3;
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			//Compare music
			if(!userBadges1.getMusic_json().equals("") && !userBadges2.getMusic_json().equals("")){
				try{
					String string1 = userBadges1.getMusic_json();
					JSONObject json_music1 = new JSONObject(string1);
					JSONArray jsonArray1 = json_music1.getJSONArray("music");

					String string2 = userBadges2.getMusic_json();
					JSONObject json_music2 = new JSONObject(string2);
					JSONArray jsonArray2 = json_music2.getJSONArray("music");

					//Comparison of music here
					//If one of the music match, return the common music badge int
					for(int i = 0; i < jsonArray1.length(); i ++){
						String artist = jsonArray1.get(i).toString();
						if(jsonArray2.toString().contains(artist)){
							return 4;
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			//Compare books
			if(!userBadges1.getBooks_json().equals("") && !userBadges2.getBooks_json().equals("")){
				try{
					String string1 = userBadges1.getBooks_json();
					JSONObject json_books1 = new JSONObject(string1);
					JSONArray jsonArray1 = json_books1.getJSONArray("books");

					String string2 = userBadges2.getBooks_json();
					JSONObject json_books2 = new JSONObject(string2);
					JSONArray jsonArray2 = json_books2.getJSONArray("books");

					//Comparison of books here
					//If one of the books match, return the common books badge int
					for(int i = 0; i < jsonArray1.length(); i ++){
						String book = jsonArray1.get(i).toString();
						if(jsonArray2.toString().contains(book)){
							return 5;
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			//Compare movies
			if(!userBadges1.getMovies_json().equals("") && !userBadges2.getMovies_json().equals("")){
				try{
					String string1 = userBadges1.getMovies_json();
					JSONObject json_movies1 = new JSONObject(string1);
					JSONArray jsonArray1 = json_movies1.getJSONArray("movies");

					String string2 = userBadges2.getMovies_json();
					JSONObject json_movies2 = new JSONObject(string2);
					JSONArray jsonArray2 = json_movies2.getJSONArray("movies");

					//Comparison of movies here
					//If one of the movies match, return the common movies badge int
					for(int i = 0; i < jsonArray1.length(); i ++){
						String movie = jsonArray1.get(i).toString();
						if(jsonArray2.toString().contains(movie)){
							return 6;
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			//Compare schools
			if(!userBadges1.getSchool_json().equals("") && !userBadges2.getSchool_json().equals("")){
				try{
					String string1 = userBadges1.getSchool_json();
					JSONObject json_schools1 = new JSONObject(string1);
					JSONArray jsonArray1 = json_schools1.getJSONArray("schools");

					String string2 = userBadges2.getSchool_json();
					JSONObject json_schools2 = new JSONObject(string2);
					JSONArray jsonArray2 = json_schools2.getJSONArray("schools");

					//Comparison of schools here
					//If one of the schools match, return the common school badge int
					for(int i = 0; i < jsonArray1.length(); i ++){
						String school = jsonArray1.get(i).toString();
						if(jsonArray2.toString().contains(school)){
							return 7;
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			//Compare hometown
			if(!userBadges1.getHometown().equals("") && !userBadges2.getHometown().equals("")){
				if(userBadges1.getHometown().equals(userBadges2.getHometown())){
					return 8;
				}
			}

			//Compare location
			if(!userBadges1.getLocation().equals("") && !userBadges2.getLocation().equals("")){
				if(userBadges1.getLocation().equals(userBadges2.getLocation())){
					return 9;
				}
			}

			//Compare likes
			if(!userBadges1.getLikes_json().equals("") && !userBadges2.getLikes_json().equals("")){
				try{
					String string1 = userBadges1.getLikes_json();
					JSONObject json_likes1 = new JSONObject(string1);
					JSONArray jsonArray1 = json_likes1.getJSONArray("likes");

					String string2 = userBadges2.getLikes_json();
					JSONObject json_likes2 = new JSONObject(string2);
					JSONArray jsonArray2 = json_likes2.getJSONArray("likes");

					//Comparison of likes here
					//If one of the likes match, return the common like badge int
					for(int i = 0; i < jsonArray1.length(); i ++){
						String like = jsonArray1.get(i).toString();
						if(jsonArray2.toString().contains(like)){
							return 10;
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

		}

		return 0;
	}

	private void deletePoint(JSONObject requestObject, PrintWriter out) throws IOException, JSONException {
		GeoPoint geoPoint = new GeoPoint(requestObject.getDouble("lat"), requestObject.getDouble("lng"));
		AttributeValue rangeKeyAttributeValue = new AttributeValue().withS(requestObject.getString("rangeKey"));

		DeletePointRequest deletePointRequest = new DeletePointRequest(geoPoint, rangeKeyAttributeValue);
		DeletePointResult deletePointResult = geoDataManager.deletePoint(deletePointRequest);

//		printDeletePointResult(deletePointResult, out);
	}

	private void printDeletePointResult(DeletePointResult deletePointResult, PrintWriter out)
			throws JsonParseException, IOException {

		Map<String, String> jsonMap = new HashMap<String, String>();
		jsonMap.put("action", "delete-point");

		out.println(mapper.writeValueAsString(jsonMap));
		out.flush();
	}

	public void destroy() {
		config.getExecutorService().shutdownNow();
	}
}