package vn.edu.hcmuaf.fit.tmdt_nhomle.db;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ReturnRefundDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.NotificationService;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Opt-in integration checks; only run against a disposable *_test or *_integration schema. */
@EnabledIfSystemProperty(named="pk.adminIntegration",matches="true")
class AdminOperationsPersistenceIntegrationTest {
 @Test void notificationPersistsAcrossIndependentConnection() throws Exception {
  DbConfig cfg=DbConfig.get(); assumeTestSchema(cfg.database());
  long userId=Long.getLong("pk.adminIntegrationUserId",0L);
  assumeTrue(userId>0,"Set pk.adminIntegrationUserId to a disposable fixture account.");
  String marker="admin-integration-"+UUID.randomUUID(), target="/__integration__/"+marker;
  try {
   NotificationService.notifyUser(userId,"INTEGRATION_TEST",marker,"Notification durability test "+marker,target,"INTEGRATION_TEST",null);
   try(var c=DriverManager.getConnection(cfg.jdbcUrl(),cfg.user(),cfg.password());
       PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM notifications WHERE user_id=? AND notification_type='INTEGRATION_TEST' AND target_url=?")) {
    p.setLong(1,userId);p.setString(2,target);try(ResultSet rs=p.executeQuery()){assertTrue(rs.next());assertEquals(1,rs.getLong(1));}
   }
  } finally {
   try(var c=DriverManager.getConnection(cfg.jdbcUrl(),cfg.user(),cfg.password());
       PreparedStatement p=c.prepareStatement("DELETE FROM notifications WHERE user_id=? AND notification_type='INTEGRATION_TEST' AND target_url=?")) {
    p.setLong(1,userId);p.setString(2,target);p.executeUpdate();
   }
  }
 }
 @Test void evidenceIsScopedToOwner() {
  DbConfig cfg=DbConfig.get();assumeTestSchema(cfg.database());
  long evidence=Long.getLong("pk.adminIntegrationEvidenceId",0L),owner=Long.getLong("pk.adminIntegrationOwnerUserId",0L),other=Long.getLong("pk.adminIntegrationOtherUserId",0L);
  assumeTrue(evidence>0&&owner>0&&other>0&&owner!=other,"Set evidence, owner and other-user fixture IDs.");
  Map<String,Object> owned=ReturnRefundDao.evidenceForUser(evidence,owner);
  assertNotNull(owned);assertNull(ReturnRefundDao.evidenceForUser(evidence,other));
  assertNotNull(ReturnRefundDao.evidenceForAdmin(evidence));
 }
 private static void assumeTestSchema(String database) {
  String n=database==null?"":database.toLowerCase(Locale.ROOT);
  assumeTrue(n.endsWith("_test")||n.endsWith("_integration"),"Only dedicated test schemas are allowed.");
 }
}