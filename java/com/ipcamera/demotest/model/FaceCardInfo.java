package com.ipcamera.demotest.model;

public class FaceCardInfo {

    private FaceCardInfo faceCardInfo;
    private String id;
    private String name;
    private String relationship;
    private String priority;
    private String regtime;

    public FaceCardInfo getFaceCardInfo(){
        return this;
    }
    public void setFaceCardInfo(FaceCardInfo faceCardInfo){
        this.faceCardInfo = faceCardInfo;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFaceRelationShip(String faceRelationShip) {
        this.relationship = faceRelationShip;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setRegtime(String regtime) {
        this.regtime = regtime;
    }

    public String getName() {
        return name;
    }

    public String getFaceRelationShip() {
        return relationship;
    }

    public String getPriority() {
        return priority;
    }

    public String getRegtime() {
        return regtime;
    }
}
