package com.emr.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 留言实体。
 * 当前既承载用户留言，也承载医生/管理员回复信息，接口仍旧通过同一张表完成留言流转。
 */
@TableName("messages")
public class MessageEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String roleCode;
    private String title;
    private String content;
    private String imageUrl;
    private String replyContent;
    private String replyImageUrl;
    private Long replyUserId;
    private String replyUserName;
    private LocalDateTime replyTime;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getReplyContent() { return replyContent; }
    public void setReplyContent(String replyContent) { this.replyContent = replyContent; }
    public String getReplyImageUrl() { return replyImageUrl; }
    public void setReplyImageUrl(String replyImageUrl) { this.replyImageUrl = replyImageUrl; }
    public Long getReplyUserId() { return replyUserId; }
    public void setReplyUserId(Long replyUserId) { this.replyUserId = replyUserId; }
    public String getReplyUserName() { return replyUserName; }
    public void setReplyUserName(String replyUserName) { this.replyUserName = replyUserName; }
    public LocalDateTime getReplyTime() { return replyTime; }
    public void setReplyTime(LocalDateTime replyTime) { this.replyTime = replyTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
