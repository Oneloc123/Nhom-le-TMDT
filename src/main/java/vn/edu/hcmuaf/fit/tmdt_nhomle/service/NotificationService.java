package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class NotificationService {
    private NotificationService() { }
    public static void notifyUser(long userId, String type, String title, String message, String targetUrl, String relatedType, Long relatedId) {
        String sql="INSERT INTO notifications (user_id,notification_type,title,message,target_url,related_type,related_id,is_read,created_at) VALUES (?,?,?,?,?,?,?,0,NOW())";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            p.setLong(1,userId); p.setString(2,clip(type,40));p.setString(3,clip(title,180));p.setString(4,clip(message,1000));p.setString(5,clip(targetUrl,800));p.setString(6,clip(relatedType,40));
            if(relatedId==null)p.setNull(7,java.sql.Types.BIGINT);else p.setLong(7,relatedId);p.executeUpdate();
        }catch(SQLException e){throw DataAccessException.of(e);}
    }
    private static String clip(String s,int max){if(s==null)return null;return s.length()<=max?s:s.substring(0,max);}
}
