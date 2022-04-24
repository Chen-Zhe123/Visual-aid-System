package com.ipcamera.demotest.service;

public class LiveStreamState {

    /**
     * 确保StartPPPPLivestream()不会重复调用
     */
    public static Boolean isStarted = false;

}
