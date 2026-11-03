package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CustomerAccountService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.AjaxResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json;

import java.io.IOException;
import java.util.*;

/** Sổ địa chỉ của khách hàng; endpoint JSON cũ được giữ bằng code mới để không gãy tích hợp. */
@WebServlet(name="userAddressBookV2",urlPatterns={"/addresses","/addresses/api"})
public class AddressBookController extends UserControllerSupport {
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        prepare(req);long uid=currentUser(req).getId();
        List<Map<String,Object>> addresses=CustomerAccountService.listAddresses(uid);
        if("/addresses/api".equals(req.getServletPath())||AjaxResponse.isRequested(req)){
            List<Map<String,Object>> data=new ArrayList<>();
            for(Map<String,Object> row:addresses){Map<String,Object> item=new LinkedHashMap<>();
                item.put("addressId",row.get("address_id"));item.put("recipientName",row.get("recipient_name"));
                item.put("recipientPhone",row.get("recipient_phone"));item.put("province",row.get("province"));
                item.put("district",row.get("district"));item.put("ward",row.get("ward"));item.put("addressLine",row.get("address_line"));
                item.put("label",row.get("label"));item.put("deliveryNote",row.get("delivery_note"));
                item.put("defaultAddress",row.get("is_default"));data.add(item);}
            AjaxResponse.send(resp,HttpServletResponse.SC_OK,Json.stringify(Map.of("success",true,"data",data)));return;
        }
        req.setAttribute("addresses",addresses);
        try{long editId=Long.parseLong(value(req.getParameter("edit")));req.setAttribute("editingAddress",CustomerAccountService.addressById(uid,editId).orElse(null));}catch(Exception ignored){}
        req.getRequestDispatcher("/WEB-INF/views/user/addresses.jsp").forward(req,resp);
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        req.setCharacterEncoding("UTF-8");User user=currentUser(req);String action=value(req.getParameter("action"));
        boolean api="/addresses/api".equals(req.getServletPath())||AjaxResponse.isRequested(req);
        try{
            long id=parseLong(req.getParameter("addressId"));
            if("save".equals(action)||"add".equals(action)||"update".equals(action)){
                if("add".equals(action))id=0;
                if("update".equals(action)&&id<=0)throw new IllegalArgumentException("Địa chỉ cần cập nhật không hợp lệ.");
                Map<String,String> form=new LinkedHashMap<>();form.put("recipientName",req.getParameter("recipientName"));form.put("recipientPhone",req.getParameter("recipientPhone"));
                form.put("province",req.getParameter("province"));form.put("district",req.getParameter("district"));form.put("ward",req.getParameter("ward"));
                form.put("addressLine",req.getParameter("addressLine"));form.put("label",req.getParameter("label"));form.put("deliveryNote",req.getParameter("deliveryNote"));
                form.put("isDefault",req.getParameter("isDefault")!=null?req.getParameter("isDefault"):req.getParameter("defaultAddress"));
                CustomerAccountService.saveAddress(user.getId(),id,form);
            }else if("delete".equals(action)){if(id<=0||!CustomerAccountService.deleteAddress(user.getId(),id))throw new IllegalArgumentException("Không tìm thấy địa chỉ cần xóa.");}
            else if("default".equals(action)||"setDefault".equals(action)){if(id<=0||!CustomerAccountService.setDefaultAddress(user.getId(),id))throw new IllegalArgumentException("Không tìm thấy địa chỉ cần đặt mặc định.");}
            else throw new IllegalArgumentException("Thao tác không hợp lệ.");
            if(api){AjaxResponse.send(resp,HttpServletResponse.SC_OK,Json.stringify(Map.of("success",true,"message","Sổ địa chỉ đã được cập nhật.")));return;}
            resp.sendRedirect(req.getContextPath()+"/addresses?updated=1");
        }catch(RuntimeException e){
            if(api){AjaxResponse.send(resp,HttpServletResponse.SC_BAD_REQUEST,Json.stringify(Map.of("success",false,"message",e.getMessage()==null?"Không thể cập nhật địa chỉ.":e.getMessage())));return;}
            prepare(req);req.setAttribute("error",e.getMessage());req.setAttribute("addresses",CustomerAccountService.listAddresses(user.getId()));
            if(idFromRequest(req)>0)req.setAttribute("editingAddress",CustomerAccountService.addressById(user.getId(),idFromRequest(req)).orElse(null));
            req.getRequestDispatcher("/WEB-INF/views/user/addresses.jsp").forward(req,resp);
        }
    }
    private static long parseLong(String raw){try{return raw==null||raw.isBlank()?0:Long.parseLong(raw);}catch(NumberFormatException e){return 0;}}
    private static long idFromRequest(HttpServletRequest req){return parseLong(req.getParameter("addressId"));}
}
