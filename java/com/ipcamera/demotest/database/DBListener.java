package com.ipcamera.demotest.database;

import com.ipcamera.demotest.model.FaceCardInfo;

/**
 * 数据库发生变化后回调
 */
public interface DBListener {

    /**
     * 当出现异常时执行
     */
    void onError(Exception e);

    /**
     * 增加人脸后执行
     */
    void onAddFace(FaceCardInfo info);

    /**
     * 删除人脸后执行
     */
    void onDeleteFace(String id);

    /**
     * 更改人脸信息后执行
     */
    void onModifyFace(String id, FaceCardInfo info);

}
