package com.example.kosarlabda;


import java.util.Date;

public class Match {
    private String id;
    private String teamA;
    private String teamB;
    private int scoreA;
    private int scoreB;
    private String location;
    private Date dateTime;
    private String ownerUid;

    public Match() { }

    public Match(String id,String teamA, int scoreA, String teamB, int scoreB, String location, Date dateTime, String ownerUid) {
        this.id=id;
        this.teamA = teamA;
        this.scoreA = scoreA;
        this.teamB = teamB;
        this.scoreB = scoreB;
        this.location = location;
        this.dateTime = dateTime;
        this.ownerUid = ownerUid;
    }


    // Konstruktor, getterek és setterek…
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTeamA() { return teamA; }
    public void setTeamA(String teamA) { this.teamA = teamA; }
    public String getTeamB() { return teamB; }
    public void setTeamB(String teamB) { this.teamB = teamB; }
    public int getScoreA() { return scoreA; }
    public void setScoreA(int scoreA) { this.scoreA = scoreA; }
    public int getScoreB() { return scoreB; }
    public void setScoreB(int scoreB) { this.scoreB = scoreB; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Date getDateTime() { return dateTime; }
    public void setDateTime(Date dateTime) { this.dateTime = dateTime; }
    public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }
}
