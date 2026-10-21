package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.util.ArrayList;
import java.util.List;

public class ProductComment {

    private long commentId;
    private long productId;
    private long userId;
    private Long parentId;
    private String content;
    private boolean sellerReply;
    private String createdAtText;
    private String userDisplayName;
    private String userEmail;
    private String userRole;
    private final List<ProductComment> replies = new ArrayList<>();

    public long getCommentId() {
        return commentId;
    }

    public void setCommentId(long commentId) {
        this.commentId = commentId;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isSellerReply() {
        return sellerReply;
    }

    public void setSellerReply(boolean sellerReply) {
        this.sellerReply = sellerReply;
    }

    public String getCreatedAtText() {
        return createdAtText;
    }

    public void setCreatedAtText(String createdAtText) {
        this.createdAtText = createdAtText;
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public void setUserDisplayName(String userDisplayName) {
        this.userDisplayName = userDisplayName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public List<ProductComment> getReplies() {
        return replies;
    }

    public void addReply(ProductComment reply) {
        this.replies.add(reply);
    }

    public boolean isReply() {
        return parentId != null && parentId > 0;
    }

    public int getRepliesCount() {
        return replies.size();
    }

    public String getUserInitial() {
        if (userDisplayName == null || userDisplayName.isBlank()) {
            return "K";
        }
        return userDisplayName.trim().substring(0, 1).toUpperCase();
    }
}
