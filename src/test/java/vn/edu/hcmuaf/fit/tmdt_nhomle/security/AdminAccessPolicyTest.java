package vn.edu.hcmuaf.fit.tmdt_nhomle.security;
import org.junit.jupiter.api.Test;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
class AdminAccessPolicyTest {
 @Test void requiresActiveAdmin() {
  assertTrue(AdminAccessPolicy.canAccess(user("ADMIN","ACTIVE",false)));
  assertFalse(AdminAccessPolicy.canAccess(user("CUSTOMER","ACTIVE",false)));
  assertFalse(AdminAccessPolicy.canAccess(user("ADMIN","LOCKED",false)));
  assertFalse(AdminAccessPolicy.canAccess(user("ADMIN","ACTIVE",true)));
  assertFalse(AdminAccessPolicy.canAccess(null));
 }
 @Test void adminMutationPathsRequireCsrf() throws Exception {
  assertTrue(requiresCsrf("/admin/api/shipments/8/update"));
  assertTrue(requiresCsrf("/admin/api/reconciliation/7/refund"));
  assertTrue(requiresCsrf("/admin/tickets"));
  assertFalse(requiresCsrf("/public/catalog"));
 }
 private static User user(String role,String status,boolean deleted) {
  User u=new User(); u.setRole(role); u.setStatus(status); u.setDeleted(deleted); return u;
 }
 private static boolean requiresCsrf(String path)throws Exception {
  Method m=AuthFilter.class.getDeclaredMethod("requiresCsrf",String.class); m.setAccessible(true);
  return (boolean)m.invoke(null,path);
 }}