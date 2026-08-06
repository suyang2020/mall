package com.macro.mall.portal.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 登录日志（MongoDB文档）
 * Created on 2026/8/6.
 */
@Document(collection = "login_logs")
public class LoginLog {

    @Id
    private String id;

    @Indexed
    private Long memberId;

    @Indexed
    private String username;

    @Indexed
    private String ip;

    /**
     * 登录类型：PASSWORD, PHONE_CODE
     */
    private String loginType;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 失败原因
     */
    private String failReason;

    private Date createTime;

    public LoginLog() {
    }

    public LoginLog(Long memberId, String username, String ip, String loginType, boolean success, String failReason) {
        this.memberId = memberId;
        this.username = username;
        this.ip = ip;
        this.loginType = loginType;
        this.success = success;
        this.failReason = failReason;
        this.createTime = new Date();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
