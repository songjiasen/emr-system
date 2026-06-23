package com.emr.record;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MedicalRecordControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createMedicalRecordGeneratesRecordNoAndDefaultArchiveStatus() {
        Map<String, Object> request = Map.of(
                "appointmentId", 1,
                "appointmentNo", "APPT202606220001",
                "patientId", 1001,
                "patientName", "测试患者",
                "doctorId", 1,
                "doctorName", "王医生",
                "visitTime", "2026-06-23 10:00:00",
                "chiefComplaint", "胸闷一天",
                "diagnosis", "疑似心律失常",
                "treatmentAdvice", "完善心电图检查"
        );

        Map response = restTemplate.postForObject("/medical-records", request, Map.class);
        Map data = (Map) response.get("data");

        assertThat(response).containsEntry("code", 0);
        assertThat((String) data.get("recordNo")).startsWith("MR");
        assertThat(data).containsEntry("archiveStatus", "not_submitted");
        assertThat(data).containsEntry("patientName", "测试患者");
        assertThat(data).containsEntry("doctorName", "王医生");
    }

    @Test
    void listAndDetailReturnCreatedMedicalRecord() {
        Map<String, Object> request = Map.of(
                "patientId", 2001,
                "patientName", "病历患者",
                "doctorId", 2,
                "doctorName", "李医生",
                "visitTime", "2026-06-24 10:30:00",
                "chiefComplaint", "咳嗽三天",
                "presentIllness", "无发热",
                "pastHistory", "无特殊既往史",
                "diagnosis", "上呼吸道感染",
                "treatmentAdvice", "多饮水，必要时复诊"
        );

        Map createResponse = restTemplate.postForObject("/medical-records", request, Map.class);
        Number id = (Number) ((Map<?, ?>) createResponse.get("data")).get("id");

        Map listResponse = restTemplate.getForObject("/medical-records?patientId=2001", Map.class);
        List<?> rows = (List<?>) ((Map<?, ?>) listResponse.get("data")).get("rows");

        Map detailResponse = restTemplate.getForObject("/medical-records/" + id.longValue(), Map.class);
        Map detail = (Map) detailResponse.get("data");

        assertThat(listResponse).containsEntry("code", 0);
        assertThat(rows).isNotEmpty();
        assertThat(detailResponse).containsEntry("code", 0);
        assertThat(detail).containsEntry("diagnosis", "上呼吸道感染");
        assertThat(detail).containsEntry("treatmentAdvice", "多饮水，必要时复诊");
    }
}
