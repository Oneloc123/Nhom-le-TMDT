package vn.edu.hcmuaf.fit.tmdt_nhomle.security;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CsrfTokenTest {
 @Test void acceptsMatchingSessionTokenFromHeaderOrForm() {
  Map<String,Object> attrs=new HashMap<>(); HttpSession s=session(attrs);
  String token=Csrf.ensureToken(request(s,null,null));
  assertNotNull(token); assertTrue(Csrf.validate(request(s,token,null)));
  assertTrue(Csrf.validate(request(s,null,token)));
 }
 @Test void rejectsMissingWrongAndSessionlessTokens() {
  HttpSession s=session(new HashMap<>()); String token=Csrf.ensureToken(request(s,null,null));
  assertFalse(Csrf.validate(request(s,null,null)));
  assertFalse(Csrf.validate(request(s,"wrong",null)));
  assertFalse(Csrf.validate(request(null,token,null)));
 }
 private static HttpSession session(Map<String,Object> attrs) {
  return (HttpSession)Proxy.newProxyInstance(HttpSession.class.getClassLoader(),new Class<?>[]{HttpSession.class},
   (p,m,a)->switch(m.getName()) {
    case "getAttribute" -> attrs.get(a[0]);
    case "setAttribute" -> {attrs.put((String)a[0],a[1]);yield null;}
    case "removeAttribute" -> {attrs.remove(a[0]);yield null;}
    case "getAttributeNames" -> java.util.Collections.enumeration(attrs.keySet());
    case "getId" -> "test-session";
    default -> defaultValue(m.getReturnType());
   });
 }
 private static HttpServletRequest request(HttpSession s,String header,String param) {
  return (HttpServletRequest)Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),new Class<?>[]{HttpServletRequest.class},
   (p,m,a)->switch(m.getName()) {
    case "getSession" -> s;
    case "getHeader" -> "X-CSRF-Token".equals(a[0])?header:null;
    case "getParameter" -> "csrfToken".equals(a[0])?param:null;
    default -> defaultValue(m.getReturnType());
   });
 }
 private static Object defaultValue(Class<?> t) {
  if(!t.isPrimitive()||t==void.class)return null;
  if(t==boolean.class)return false;if(t==char.class)return '\0';if(t==byte.class)return (byte)0;
  if(t==short.class)return (short)0;if(t==int.class)return 0;if(t==long.class)return 0L;
  if(t==float.class)return 0F;if(t==double.class)return 0D;return null;
 }
}