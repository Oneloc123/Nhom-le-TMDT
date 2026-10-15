<%-- CSRF token include: đặt TRONG mỗi <form method="post"> --%>
<input type="hidden" name="csrfToken" value="<%= vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf.ensureToken(request) %>">
