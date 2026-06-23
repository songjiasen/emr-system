package com.emr.user.service;

import com.emr.common.PageResult;
import com.emr.user.vo.DoctorSummaryResponse;

/**
 * 医生资料服务接口。
 * 支撑患者端医生列表、预约挂号选医生，以及后台医生管理的基础查询能力。
 */
public interface DoctorService {

    PageResult<DoctorSummaryResponse> listDoctors(int page, int limit);

    DoctorSummaryResponse getDoctor(Long id);
}

