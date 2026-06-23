package com.emr.visit.service;

import com.emr.common.PageResult;
import com.emr.visit.dto.AppointmentCancelRequest;
import com.emr.visit.dto.AppointmentCreateRequest;
import com.emr.visit.vo.AppointmentResponse;

/**
 * 预约挂号服务接口。
 * 覆盖需求文档中的预约挂号、预约列表、预约详情、取消预约和预约编号生成。
 */
public interface AppointmentService {

    AppointmentResponse createAppointment(AppointmentCreateRequest request);

    PageResult<AppointmentResponse> listAppointments(Long patientId, Long doctorId, String status, int page, int limit);

    AppointmentResponse getAppointment(Long id);

    AppointmentResponse cancelAppointment(Long id, AppointmentCancelRequest request);
}

