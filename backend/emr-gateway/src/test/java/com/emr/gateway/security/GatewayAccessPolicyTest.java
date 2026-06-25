package com.emr.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayAccessPolicyTest {

    private final GatewayAccessPolicy policy = new GatewayAccessPolicy();

    @Test
    void publicEndpointsMatchOnlyDocumentedAnonymousOperations() {
        assertThat(policy.isPublic(HttpMethod.POST, "/cl584734139/auth/login")).isTrue();
        assertThat(policy.isPublic(HttpMethod.POST, "/cl584734139/auth/patient/register")).isTrue();
        assertThat(policy.isPublic(HttpMethod.POST, "/cl584734139/auth/password/reset")).isTrue();
        assertThat(policy.isPublic(HttpMethod.GET, "/cl584734139/doctors")).isTrue();
        assertThat(policy.isPublic(HttpMethod.GET, "/cl584734139/departments")).isTrue();
        assertThat(policy.isPublic(HttpMethod.GET, "/cl584734139/news/1")).isTrue();
        assertThat(policy.isPublic(HttpMethod.GET, "/cl584734139/carousels")).isTrue();
        assertThat(policy.isPublic(HttpMethod.POST, "/cl584734139/news")).isFalse();
    }

    @Test
    void patientCannotUseAdministrationEndpoints() {
        assertThat(policy.isAllowed("patient", HttpMethod.GET, "/cl584734139/appointments")).isTrue();
        assertThat(policy.isAllowed("patient", HttpMethod.GET, "/cl584734139/medical-records")).isTrue();
        assertThat(policy.isAllowed("patient", HttpMethod.POST, "/cl584734139/fees/1/pay")).isTrue();
        assertThat(policy.isAllowed("patient", HttpMethod.GET, "/cl584734139/user-management/patients")).isFalse();
        assertThat(policy.isAllowed("patient", HttpMethod.POST, "/cl584734139/news")).isFalse();
    }

    @Test
    void nurseAndDirectorReceiveOnlyTheirWorkflowCapabilities() {
        assertThat(policy.isAllowed("nurse", HttpMethod.GET, "/cl584734139/appointments")).isTrue();
        assertThat(policy.isAllowed("nurse", HttpMethod.POST, "/cl584734139/appointments")).isFalse();
        assertThat(policy.isAllowed("nurse", HttpMethod.POST, "/cl584734139/triage-records")).isTrue();
        assertThat(policy.isAllowed("nurse", HttpMethod.POST, "/cl584734139/medical-orders/1/execute")).isTrue();
        assertThat(policy.isAllowed("nurse", HttpMethod.POST, "/cl584734139/medical-orders/1/audit")).isFalse();
        assertThat(policy.isAllowed("director", HttpMethod.GET, "/cl584734139/medical-records")).isTrue();
        assertThat(policy.isAllowed("director", HttpMethod.POST, "/cl584734139/medical-records")).isFalse();
        assertThat(policy.isAllowed("director", HttpMethod.PUT, "/cl584734139/medical-records/1")).isFalse();
        assertThat(policy.isAllowed("director", HttpMethod.DELETE, "/cl584734139/medical-records/1")).isFalse();
        assertThat(policy.isAllowed("director", HttpMethod.POST, "/cl584734139/medical-orders/1/audit")).isFalse();
        assertThat(policy.isAllowed("director", HttpMethod.POST, "/cl584734139/medical-orders/1/audit-result")).isTrue();
        assertThat(policy.isAllowed("director", HttpMethod.POST, "/cl584734139/test-requests/1/audit-result")).isTrue();
        assertThat(policy.isAllowed("director", HttpMethod.POST, "/cl584734139/medical-record-archives/applications/1/audit")).isTrue();
        assertThat(policy.isAllowed("director", HttpMethod.POST, "/cl584734139/user-management/admins")).isFalse();
    }

    @Test
    void administratorCanManageAllBusinessModules() {
        assertThat(policy.isAllowed("admin", HttpMethod.DELETE, "/cl584734139/news/1")).isTrue();
        assertThat(policy.isAllowed("super_admin", HttpMethod.POST, "/cl584734139/user-management/admins")).isTrue();
    }
}
