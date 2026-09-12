package com.zbtech.community.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 应用启动后打印全部接口地址清单与接口文档地址，便于联调核对
 */
@Component
public class ApiEndpointPrinter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ApiEndpointPrinter.class);

    private final RequestMappingHandlerMapping handlerMapping;

    @Value("${server.port:8080}")
    private int port;

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    public ApiEndpointPrinter(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @Override
    public void run(ApplicationArguments args) {
        Map<RequestMappingInfo, HandlerMethod> map = handlerMapping.getHandlerMethods();
        List<String[]> rows = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> e : map.entrySet()) {
            RequestMappingInfo info = e.getKey();
            HandlerMethod hm = e.getValue();
            String path = resolvePath(info);
            String methods = info.getMethodsCondition().getMethods().toString();
            String cls = hm.getBeanType().getSimpleName();
            String method = hm.getMethod().getName();
            rows.add(new String[]{cls, methods, path, method});
        }
        rows.sort(Comparator.comparing(r -> r[0]));

        log.info("==================== API 接口清单（共 {} 个） ====================", rows.size());
        log.info(String.format("%-34s %-14s %-70s %s", "Controller", "HTTP", "Path", "Java Method"));
        log.info("------------------------------------------------------------------------------------------");
        for (String[] r : rows) {
            log.info(String.format("%-34s %-14s %-70s %s", r[0], r[1], r[2], r[3]));
        }
        log.info("==========================================================================================");

        String ctx = "/".equals(contextPath) ? "" : contextPath;
        String base = "http://localhost:" + port + ctx;
        log.info("接口文档（springdoc / OpenAPI）已开启：");
        log.info("  Swagger UI : {}/swagger-ui.html", base);
        log.info("  OpenAPI    : {}/v3/api-docs", base);
    }

    private String resolvePath(RequestMappingInfo info) {
        if (info.getPathPatternsCondition() != null) {
            Set<String> patterns = info.getPathPatternsCondition().getPatternValues();
            return String.join(", ", patterns);
        }
        return String.join(", ", info.getDirectPaths());
    }
}
