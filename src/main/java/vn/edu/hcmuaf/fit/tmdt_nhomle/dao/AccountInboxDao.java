package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class AccountInboxDao {
    private AccountInboxDao() { }
    public static long unreadNotifications(long userId) { return count("SELECT COUNT(*) FROM notifications WHERE user_id=? AND is_read=0", userId); }
    public static long unreadMessages(long userId) { return count("SELECT COUNT(*) FROM order_messages WHERE recipient_user_id=? AND read_at IS NULL", userId); }
    public static java.util.List<java.util.Map<String,Object>> latestNotifications(long userId, int limit) {
        int safeLimit=Math.max(1,Math.min(limit,10));
        String sql="SELECT notification_id,title,message,target_url,is_read,created_at FROM notifications WHERE user_id=? ORDER BY created_at DESC,notification_id DESC LIMIT ?";
        java.util.List<java.util.Map<String,Object>> rows=new java.util.ArrayList<>();
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            p.setLong(1,userId);p.setInt(2,safeLimit);try(ResultSet r=p.executeQuery()){while(r.next()){
                java.util.Map<String,Object> m=new java.util.LinkedHashMap<>();m.put("id",r.getLong("notification_id"));m.put("title",r.getString("title"));m.put("message",r.getString("message"));
                String url=r.getString("target_url");m.put("url",url!=null&&url.startsWith("/")&&!url.startsWith("//")&&!url.contains("\\")?url:null);
                m.put("read",r.getBoolean("is_read"));var ts=r.getTimestamp("created_at");m.put("createdAt",ts==null?"":ts.toLocalDateTime().toString().replace('T',' '));rows.add(m);
            }}return rows;
        }catch(SQLException e){throw DataAccessException.of(e);}
    }
    private static long count(String sql, long userId) {
        try (Connection c=Database.getConnection(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setLong(1,userId); try(ResultSet r=p.executeQuery()){ return r.next()?r.getLong(1):0; }
        } catch(SQLException e){ throw DataAccessException.of(e); }
    }
}
