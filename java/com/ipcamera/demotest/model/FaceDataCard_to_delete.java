package com.ipcamera.demotest.model;

public class FaceDataCard_to_delete {

    private String faceName;
    private String faceRelationShip;
    private String frPriorityLevel;
    private long regTime;
    private String faceNumber = "";
    private Boolean isSelected = false;

    public void setFaceName(String faceName) {
        this.faceName = faceName;
    }

    public void setFaceRelationShip(String faceRelationShip) {
        this.faceRelationShip = faceRelationShip;
    }

    public void setFrPriorityLevel(String frPriorityLevel) {
        this.frPriorityLevel = frPriorityLevel;
    }

    public void setRegTime(long regTime) {
        this.regTime = regTime;
    }

    public void setFaceNumber(String faceNumber) {
        this.faceNumber = faceNumber;
    }

    public void setSelected(Boolean selected) {
        isSelected = selected;
    }

    public String getFaceName() {
        return faceName;
    }

    public String getFaceNumber() {
        return faceNumber;
    }

    public String getFaceRelationShip() {
        return faceRelationShip;
    }

    public String getFrPriorityLevel() {
        return frPriorityLevel;
    }

    public Boolean getSelected() {
        return isSelected;
    }

    public long getRegTime() {
        return regTime;
    }
}
