package com.emr.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRouteConfigTest {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void gatewayKeepsOriginalFrontendPrefixAndRoutesEveryService() {
        Map<String, RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block()
                .stream()
                .collect(Collectors.toMap(RouteDefinition::getId, route -> route));

        assertRoute(routes, "emr-auth-service", "http://localhost:8101", "/cl584734139/auth/**");
        assertRoute(routes, "emr-user-service", "http://localhost:8102", "/cl584734139/doctors/**");
        assertRoute(routes, "emr-visit-service", "http://localhost:8103", "/cl584734139/appointments/**");
        assertRoute(routes, "emr-record-service", "http://localhost:8104", "/cl584734139/medical-records/**");
        assertRoute(routes, "emr-clinical-service", "http://localhost:8105", "/cl584734139/medical-orders/**");
        assertRoute(routes, "emr-workflow-service", "http://localhost:8106", "/cl584734139/workflow/**");
        assertRoute(routes, "emr-billing-service", "http://localhost:8107", "/cl584734139/fees/**");
        assertRoute(routes, "emr-system-service", "http://localhost:8108", "/cl584734139/news/**");
        assertRoute(routes, "emr-system-service", "http://localhost:8108", "/cl584734139/carousels/**");
        assertRoute(routes, "emr-ai-service", "http://localhost:8109", "/cl584734139/ai/**");
    }

    /**
     * 校验网关展示路径和后端真实路径之间的兼容关系：
     * 前端保留历史 `/cl584734139` 前缀，网关转发前只剥掉这一层前缀。
     */
    private void assertRoute(Map<String, RouteDefinition> routes, String routeId, String uri, String pathPattern) {
        RouteDefinition route = routes.get(routeId);

        assertThat(route).as(routeId + " 路由必须存在").isNotNull();
        assertThat(route.getUri()).isEqualTo(URI.create(uri));
        assertThat(route.getPredicates())
                .extracting(PredicateDefinition::toString)
                .anySatisfy(predicate -> assertThat(predicate).contains("Path").contains(pathPattern));
        assertThat(route.getFilters())
                .extracting(FilterDefinition::toString)
                .anySatisfy(filter -> assertThat(filter).contains("StripPrefix").contains("1"));
    }
}
