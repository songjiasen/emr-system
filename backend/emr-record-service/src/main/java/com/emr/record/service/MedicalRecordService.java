package com.emr.record.service;

import com.emr.common.PageResult;
import com.emr.record.dto.MedicalRecordCreateRequest;
import com.emr.record.vo.MedicalRecordResponse;

/**
 * 病历服务接口。
 * 覆盖需求文档中的新增病历、病历列表、病历详情、病历编号和患者查看病历。
 */
public interface MedicalRecordService {

    MedicalRecordResponse createRecord(MedicalRecordCreateRequest request);

    PageResult<MedicalRecordResponse> listRecords(Long patientId, Long doctorId, String archiveStatus, int page, int limit);

    MedicalRecordResponse getRecord(Long id);

    MedicalRecordResponse updateRecord(Long id, MedicalRecordCreateRequest request);

    MedicalRecordResponse deleteRecord(Long id);
}
