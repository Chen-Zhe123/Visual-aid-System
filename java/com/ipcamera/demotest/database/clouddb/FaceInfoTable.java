package com.ipcamera.demotest.database.clouddb;

import android.util.Log;

import com.ipcamera.demotest.model.FaceCardInfo;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import java.sql.SQLException;
import java.util.List;

/**
 * 对数据库的info表增删改查
 */
public class FaceInfoTable {
    QueryRunner queryRunner;

    public FaceInfoTable(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    public Boolean addFace(FaceCardInfo info) {
        String id = info.getId();
        String name = info.getName();
        String relationship = info.getFaceRelationShip();
        String priority = info.getPriority();
        String regtime = String.valueOf(info.getRegtime());
        String sql = "insert into info(id,name,relationship,priority,regtime) values(?,?,?,?,?);";
        try {
            if (queryRunner.update(sql, id, name, relationship, priority, regtime) > 0) {
                //TODO 更新内存中的人脸信息列表,（更新适配器资料卡列表）
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }
    public Boolean addFace(String id,String name,String relationship,String regTime) {
        String sql = "insert into info(id,name,relationship,priority,regtime) values(?,?,?,?,?);";
        try {
            Log.d("1234", "addFace: 1");
            if (queryRunner.update(sql, id, name, relationship,"", regTime) > 0) {
                //TODO 更新内存中的人脸信息列表,（更新适配器资料卡列表）
                Log.d("1234", "addFace: 2");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Log.d("1234", "addFace: 3"+e);
            return false;
        }
        Log.d("1234", "addFace: 4");
        return false;
    }

    public Boolean deleteFace(String id) {
        String sql = "delete * from info where id = " + "'" + id + "'";
        try {
            if (queryRunner.update(sql) > 0) {
                // TODO 更新内存中的人脸信息列表,（更新适配器资料卡列表）
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public Boolean modifyFace(String id, FaceCardInfo info) {
        String name = info.getName();
        String relationship = info.getFaceRelationShip();
        String priority = info.getPriority();
        String regtime = String.valueOf(info.getRegtime());
        String sql = "update info " +
                "set name = '" + name + "',relationship = '" + relationship +
                "',priority = '" + priority + "',regtime = '" + regtime +
                "' where id = '" + id + "'";
        try {
            if (queryRunner.update(sql, id, name, relationship, priority, regtime) > 0) {
                //TODO 更新内存中的人脸信息列表,（更新适配器资料卡列表）
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public List<FaceCardInfo> queryFaces(String id) {
        String sql = "select * from info";
        try {
            return queryRunner.query(sql, new BeanListHandler<>(FaceCardInfo.class));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
