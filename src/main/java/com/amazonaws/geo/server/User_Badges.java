package com.amazonaws.geo.server;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

/**
 * Created by Lucas on 23/08/2016.
 */

@DynamoDBTable(tableName = "user_badges")
public class User_Badges {

    private String facebookId;

    private String friend = "";
    private String friendsOfFriends = "";
    private String instagram_follower = "";
    private String instagram_following = "";
    private String location = "";
    private String hometown = "";
    private String birthday = "";
    private String likes_json = "";
    private String workplace_json = "";
    private String school_json = "";
    private String music_json = "";
    private String movies_json = "";
    private String books_json = "";
    private String professionalSkills_json = "";


    @DynamoDBHashKey(attributeName = "facebook_id")
    public String getFacebookId() {
        return facebookId;
    }

    public void setFacebookId(String facebookId) {
        this.facebookId = facebookId;
    }

    @DynamoDBAttribute(attributeName = "friend")
    public String getFriend() {
        return friend;
    }

    public void setFriend(String friend) {
        this.friend = friend;
    }

    @DynamoDBAttribute(attributeName = "instagram_following")
    public String getInstagram_following() {
        return instagram_following;
    }

    public void setInstagram_following(String instagram_following) {
        this.instagram_following = instagram_following;
    }

    @DynamoDBAttribute(attributeName = "instagram_follower")
    public String getInstagram_follower() {
        return instagram_follower;
    }

    public void setInstagram_follower(String instagram_follower) {
        this.instagram_follower = instagram_follower;
    }

    @DynamoDBAttribute(attributeName = "location")
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @DynamoDBAttribute(attributeName = "hometown")
    public String getHometown() {
        return hometown;
    }

    public void setHometown(String hometown) {
        this.hometown = hometown;
    }

    @DynamoDBAttribute(attributeName = "birthday")
    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    @DynamoDBAttribute(attributeName = "likes_json")
    public String getLikes_json() {
        return likes_json;
    }

    public void setLikes_json(String likes_json) {
        this.likes_json = likes_json;
    }

    @DynamoDBAttribute(attributeName = "workplaces_json")
    public String getWorkplace_json() {
        return workplace_json;
    }

    public void setWorkplace_json(String workplace_json) {
        this.workplace_json = workplace_json;
    }

    @DynamoDBAttribute(attributeName = "schools_json")
    public String getSchool_json() {
        return school_json;
    }

    public void setSchool_json(String school_json) {
        this.school_json = school_json;
    }

    @DynamoDBAttribute(attributeName = "music_json")
    public String getMusic_json() {
        return music_json;
    }

    public void setMusic_json(String music_json) {
        this.music_json = music_json;
    }

    @DynamoDBAttribute(attributeName = "movies_json")
    public String getMovies_json() {
        return movies_json;
    }

    public void setMovies_json(String movies_json) {
        this.movies_json = movies_json;
    }

    @DynamoDBAttribute(attributeName = "books_json")
    public String getBooks_json() {
        return books_json;
    }

    public void setBooks_json(String books_json) {
        this.books_json = books_json;
    }

    @DynamoDBAttribute(attributeName = "friends_of_friends")
    public String getFriendsOfFriends() {
        return friendsOfFriends;
    }

    public void setFriendsOfFriends(String friendsOfFriends) {
        this.friendsOfFriends = friendsOfFriends;
    }

    public String getProfessionalSkills_json(){
        return professionalSkills_json;
    }

    @DynamoDBAttribute(attributeName = "professional_skills_json")
    public void setProfessionalSkills_json(String professionalSkills_json){
        this.professionalSkills_json = professionalSkills_json;
    }



}
