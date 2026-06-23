package com.emr.record;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TemplateArchivePersistenceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createTemplatePersistsToTemplateTable() {
        Map response = restTemplate.postForObject("/medical-record-templates", Map.of(
                "templateName", "门诊初诊模板",
                "templateType", "outpatient",
                "content", "主诉：\n现病史：\n诊断："
        ), Map.class);

        Integer rowCount = jdbcTemplate.queryForObject(
                "select count(*) from binglimoban where template_name = ? and template_type = ? and status = 1",
                Integer.class,
                "门诊初诊模板",
                "outpatient"
        );

        assertThat(response).containsEntry("code", 0);
        assertThat(rowCount).isEqualTo(1);
    }

    @Test
    void deleteTemplateSoftDeletesPersistedTemplate() {
        jdbcTemplate.update(
                "insert into binglimoban (template_name, template_type, content, status) values (?, ?, ?, ?)",
                "删除模板",
                "common",
                "模板正文",
                1
        );
        Long id = jdbcTemplate.queryForObject(
                "select id from binglimoban where template_name = ?",
                Long.class,
                "删除模板"
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                "/medical-record-templates/" + id,
                HttpMethod.DELETE,
                null,
                Map.class
        );

        Integer status = jdbcTemplate.queryForObject(
                "select status from binglimoban where id = ?",
                Integer.class,
                id
        );

        assertThat(response.getBody()).containsEntry("code", 0);
        assertThat(status).isEqualTo(0);
    }

    @Test
    void createArchiveApplicationPersistsAndUpdatesRecordStatus() {
        jdbcTemplate.update(
                "insert into binglixinxi (record_no, patient_id, patient_name, doctor_id, doctor_name, visit_time, diagnosis, archive_status) values (?, ?, ?, ?, ?, ?, ?, ?)",
                "MR202606260010",
                5101L,
                "归档申请患者",
                61L,
                "归档医生",
                "2026-06-26 13:00:00",
                "待归档诊断",
                "not_submitted"
        );
        Long recordId = jdbcTemplate.queryForObject(
                "select id from binglixinxi where record_no = ?",
                Long.class,
                "MR202606260010"
        );

        Map response = restTemplate.postForObject("/medical-record-archives/applications", Map.of(
                "recordId", recordId
        ), Map.class);

        Integer applicationCount = jdbcTemplate.queryForObject(
                "select count(*) from bingliguidangshenqing where record_id = ? and status = ?",
                Integer.class,
                recordId,
                "pending"
        );
        String archiveStatus = jdbcTemplate.queryForObject(
                "select archive_status from binglixinxi where id = ?",
                String.class,
                recordId
        );

        assertThat(response).containsEntry("code", 0);
        assertThat(applicationCount).isEqualTo(1);
        assertThat(archiveStatus).isEqualTo("pending");
    }

    @Test
    void approveArchiveApplicationCreatesArchiveSnapshot() {
        jdbcTemplate.update(
                "insert into binglixinxi (record_no, patient_id, patient_name, doctor_id, doctor_name, visit_time, diagnosis, archive_status) values (?, ?, ?, ?, ?, ?, ?, ?)",
                "MR202606260011",
                5201L,
                "归档审核患者",
                62L,
                "归档审核医生",
                "2026-06-26 14:00:00",
                "归档审核诊断",
                "pending"
        );
        Long recordId = jdbcTemplate.queryForObject(
                "select id from binglixinxi where record_no = ?",
                Long.class,
                "MR202606260011"
        );
        jdbcTemplate.update(
                "insert into bingliguidangshenqing (application_no, record_id, record_no, patient_id, patient_name, doctor_id, doctor_name, status) values (?, ?, ?, ?, ?, ?, ?, ?)",
                "ARCHAPP202606260001",
                recordId,
                "MR202606260011",
                5201L,
                "归档审核患者",
                62L,
                "归档审核医生",
                "pending"
        );
        Long applicationId = jdbcTemplate.queryForObject(
                "select id from bingliguidangshenqing where application_no = ?",
                Long.class,
                "ARCHAPP202606260001"
        );

        Map response = restTemplate.postForObject(
                "/medical-record-archives/applications/" + applicationId + "/audit",
                Map.of(
                        "auditResult", "approved",
                        "auditUserId", 9001,
                        "auditUserName", "主任审核员",
                        "archiveContent", "归档快照正文"
                ),
                Map.class
        );

        Integer archiveCount = jdbcTemplate.queryForObject(
                "select count(*) from bingliguidang where application_id = ? and record_id = ?",
                Integer.class,
                applicationId,
                recordId
        );
        String applicationStatus = jdbcTemplate.queryForObject(
                "select status from bingliguidangshenqing where id = ?",
                String.class,
                applicationId
        );
        String recordArchiveStatus = jdbcTemplate.queryForObject(
                "select archive_status from binglixinxi where id = ?",
                String.class,
                recordId
        );

        assertThat(response).containsEntry("code", 0);
        assertThat(archiveCount).isEqualTo(1);
        assertThat(applicationStatus).isEqualTo("approved");
        assertThat(recordArchiveStatus).isEqualTo("archived");
    }
}
