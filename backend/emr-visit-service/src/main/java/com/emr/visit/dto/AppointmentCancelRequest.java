package com.emr.visit.dto;

/**
 * 取消预约请求。
 * cancelReason 用于记录患者取消原因，便于后台预约管理追溯。
 */
public record AppointmentCancelRequest(String cancelReason) {
}

