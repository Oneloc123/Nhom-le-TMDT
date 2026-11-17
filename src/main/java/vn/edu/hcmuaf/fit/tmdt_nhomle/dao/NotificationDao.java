package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Notification;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
public final class NotificationDao {
 private NotificationDao(){}
 public static List<Notification> listForUser(long userId,String filter,int page,int pageSize){
  String f=normalizeFilter(filter);int p=Math.max(1,page),size=Math.max(1,Math.min(pageSize,100));long offset=(long)(p-1)*size;
  String sql="SELECT notification_id,title,message,target_url,created_at,is_read FROM notifications WHERE user_id=?"+filterClause(f)+" ORDER BY created_at DESC,notification_id DESC LIMIT ? OFFSET ?";
  List<Notification> list=new ArrayList<>();
  try(Connection c=Database.getConnection();PreparedStatement ps=c.prepareStatement(sql)){
   ps.setLong(1,userId);ps.setInt(2,size);ps.setLong(3,offset);
   try(ResultSet rs=ps.executeQuery()){while(rs.next()){Timestamp t=rs.getTimestamp("created_at");LocalDateTime d=t==null?null:t.toLocalDateTime();list.add(new Notification(rs.getLong("notification_id"),rs.getString("title"),rs.getString("message"),rs.getString("target_url"),d,rs.getBoolean("is_read")));}}
   return list;
  }catch(SQLException e){throw DataAccessException.of(e);}
 }
 public static int countForUser(long userId,String filter){
  try(Connection c=Database.getConnection();PreparedStatement ps=c.prepareStatement("SELECT COUNT(*) FROM notifications WHERE user_id=?"+filterClause(normalizeFilter(filter)))){
   ps.setLong(1,userId);try(ResultSet rs=ps.executeQuery()){return rs.next()?rs.getInt(1):0;}
  }catch(SQLException e){throw DataAccessException.of(e);}
 }
 public static int unreadCount(long userId){return countForUser(userId,"UNREAD");}
 public static boolean markRead(long userId,long id){
  try(Connection c=Database.getConnection();PreparedStatement ps=c.prepareStatement("UPDATE notifications SET is_read=1 WHERE user_id=? AND notification_id=? AND is_read=0")){
   ps.setLong(1,userId);ps.setLong(2,id);return ps.executeUpdate()==1;
  }catch(SQLException e){throw DataAccessException.of(e);}
 }
 public static int markAllRead(long userId){
  try(Connection c=Database.getConnection();PreparedStatement ps=c.prepareStatement("UPDATE notifications SET is_read=1 WHERE user_id=? AND is_read=0")){
   ps.setLong(1,userId);return ps.executeUpdate();
  }catch(SQLException e){throw DataAccessException.of(e);}
 }
 public static String normalizeFilter(String value){if(value==null)return "ALL";return switch(value.trim().toUpperCase(java.util.Locale.ROOT)){case "UNREAD"->"UNREAD";case "READ"->"READ";default->"ALL";};}
 private static String filterClause(String f){return switch(f){case "UNREAD"->" AND is_read=0";case "READ"->" AND is_read=1";default->"";};}
}
