package com.emr.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emr.common.ApiResponse;
import com.emr.common.PageResult;
import com.emr.common.TrustedUserContext;
import com.emr.system.entity.CarouselEntity;
import com.emr.system.entity.ConfigEntity;
import com.emr.system.entity.MenuEntity;
import com.emr.system.entity.MessageEntity;
import com.emr.system.entity.NewsEntity;
import com.emr.system.entity.SyslogEntity;
import com.emr.system.mapper.CarouselMapper;
import com.emr.system.mapper.ConfigMapper;
import com.emr.system.mapper.MenuMapper;
import com.emr.system.mapper.MessageMapper;
import com.emr.system.mapper.NewsMapper;
import com.emr.system.mapper.SyslogMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统内容接口。
 * 覆盖资讯、留言、菜单、配置、轮播图和操作日志的持久化读写。
 */
@RestController
public class SystemContentController {

    private final NewsMapper newsMapper;
    private final MessageMapper messageMapper;
    private final CarouselMapper carouselMapper;
    private final ConfigMapper configMapper;
    private final MenuMapper menuMapper;
    private final SyslogMapper syslogMapper;

    public SystemContentController(
            NewsMapper newsMapper,
            MessageMapper messageMapper,
            CarouselMapper carouselMapper,
            ConfigMapper configMapper,
            MenuMapper menuMapper,
            SyslogMapper syslogMapper
    ) {
        this.newsMapper = newsMapper;
        this.messageMapper = messageMapper;
        this.carouselMapper = carouselMapper;
        this.configMapper = configMapper;
        this.menuMapper = menuMapper;
        this.syslogMapper = syslogMapper;
    }

    @PostMapping("/news")
    public ApiResponse<Map<String, Object>> createNews(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> payload = request == null ? Map.of() : request;
        NewsEntity entity = new NewsEntity();
        entity.setTitle(requireText(payload.get("title"), "资讯标题不能为空"));
        entity.setCategory(stringValue(payload.getOrDefault("category", "健康资讯")));
        entity.setCoverUrl(stringValue(payload.get("coverUrl")));
        entity.setSummary(stringValue(payload.get("summary")));
        entity.setContent(requireText(payload.get("content"), "资讯内容不能为空"));
        entity.setPublishStatus(stringValue(payload.getOrDefault("publishStatus", "published")));
        entity.setPublisherId(longValue(payload.get("publisherId")));
        entity.setPublisherName(stringValue(payload.get("publisherName")));
        entity.setStatus(1);
        newsMapper.insert(entity);
        return ApiResponse.success(toNewsRow(entity));
    }

    @GetMapping("/news")
    public ApiResponse<PageResult<Map<String, Object>>> listNews(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        List<Map<String, Object>> rows = newsMapper.selectList(new LambdaQueryWrapper<NewsEntity>()
                        .orderByDesc(NewsEntity::getId))
                .stream()
                .map(this::toNewsRow)
                .toList();
        return ApiResponse.success(PageResult.of(rows, page, limit));
    }

    @GetMapping("/news/{id}")
    public ApiResponse<Map<String, Object>> getNews(@PathVariable("id") Long id) {
        return ApiResponse.success(toNewsRow(requireNews(id)));
    }

    @PutMapping("/news/{id}")
    public ApiResponse<Map<String, Object>> updateNews(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, Object> request) {
        NewsEntity entity = requireNews(id);
        Map<String, Object> payload = request == null ? Map.of() : request;
        if (payload.containsKey("title")) {
            entity.setTitle(stringValue(payload.get("title")));
        }
        if (payload.containsKey("category")) {
            entity.setCategory(stringValue(payload.get("category")));
        }
        if (payload.containsKey("coverUrl")) {
            entity.setCoverUrl(stringValue(payload.get("coverUrl")));
        }
        if (payload.containsKey("summary")) {
            entity.setSummary(stringValue(payload.get("summary")));
        }
        if (payload.containsKey("content")) {
            entity.setContent(stringValue(payload.get("content")));
        }
        if (payload.containsKey("publishStatus")) {
            entity.setPublishStatus(stringValue(payload.get("publishStatus")));
        }
        if (payload.containsKey("publisherId")) {
            entity.setPublisherId(longValue(payload.get("publisherId")));
        }
        if (payload.containsKey("publisherName")) {
            entity.setPublisherName(stringValue(payload.get("publisherName")));
        }
        if (payload.containsKey("status")) {
            entity.setStatus(intValue(payload.get("status"), entity.getStatus()));
        }
        newsMapper.updateById(entity);
        return ApiResponse.success(toNewsRow(entity));
    }

    @DeleteMapping("/news/{id}")
    public ApiResponse<Map<String, Object>> deleteNews(@PathVariable("id") Long id) {
        NewsEntity entity = requireNews(id);
        entity.setStatus(0);
        newsMapper.updateById(entity);
        return ApiResponse.success(toNewsRow(entity));
    }

    @PostMapping("/messages")
    public ApiResponse<Map<String, Object>> createMessage(
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        Map<String, Object> payload = request == null ? Map.of() : request;
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        MessageEntity entity = new MessageEntity();
        entity.setUserId(context.authenticated() ? context.userId() : longValue(payload.get("userId")));
        entity.setUsername(context.authenticated() ? context.username() : stringValue(payload.getOrDefault("username", payload.get("userName"))));
        entity.setRoleCode(context.authenticated() ? context.roleCode() : stringValue(payload.getOrDefault("roleCode", "patient")));
        entity.setTitle(stringValue(payload.getOrDefault("title", "在线咨询")));
        entity.setContent(requireText(payload.get("content"), "留言内容不能为空"));
        entity.setImageUrl(stringValue(payload.get("imageUrl")));
        entity.setStatus(1);
        messageMapper.insert(entity);
        return ApiResponse.success(toMessageRow(entity));
    }

    @GetMapping("/messages")
    public ApiResponse<PageResult<Map<String, Object>>> listMessages(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        LambdaQueryWrapper<MessageEntity> wrapper = new LambdaQueryWrapper<MessageEntity>()
                .orderByDesc(MessageEntity::getId);
        List<Map<String, Object>> rows = messageMapper.selectList(wrapper)
                .stream()
                .map(this::toMessageRow)
                .toList();
        return ApiResponse.success(PageResult.of(rows, page, limit));
    }

    @PostMapping("/messages/{id}/reply")
    public ApiResponse<Map<String, Object>> replyMessage(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-Role-Code", required = false) String roleCode,
            @RequestHeader(value = "X-User-Table", required = false) String tableName
    ) {
        Map<String, Object> payload = request == null ? Map.of() : request;
        TrustedUserContext context = TrustedUserContext.fromHeaders(userIdHeader, username, roleCode, tableName);
        ensureMessageReplyRole(context);
        MessageEntity entity = requireMessage(id);
        entity.setReplyContent(requireText(payload.get("replyContent"), "回复内容不能为空"));
        entity.setReplyUserId(context.authenticated() ? context.userId() : longValue(payload.get("replyUserId")));
        entity.setReplyUserName(context.authenticated() ? context.username() : stringValue(payload.get("replyUserName")));
        entity.setReplyImageUrl(stringValue(payload.get("replyImageUrl")));
        entity.setReplyTime(LocalDateTime.now());
        messageMapper.updateById(entity);
        return ApiResponse.success(toMessageRow(entity));
    }

    @DeleteMapping("/messages/{id}")
    public ApiResponse<Map<String, Object>> deleteMessage(@PathVariable("id") Long id) {
        MessageEntity entity = requireMessage(id);
        entity.setStatus(0);
        messageMapper.updateById(entity);
        return ApiResponse.success(toMessageRow(entity));
    }

    @PostMapping("/carousels")
    public ApiResponse<Map<String, Object>> createCarousel(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> payload = request == null ? Map.of() : request;
        CarouselEntity entity = new CarouselEntity();
        entity.setTitle(requireText(payload.get("title"), "轮播图标题不能为空"));
        entity.setImageUrl(requireText(payload.get("imageUrl"), "轮播图图片不能为空"));
        entity.setLinkUrl(stringValue(payload.get("linkUrl")));
        entity.setSortNo(intValue(payload.get("sortNo"), 0));
        entity.setStatus(1);
        carouselMapper.insert(entity);
        return ApiResponse.success(toCarouselRow(entity));
    }

    @GetMapping("/carousels")
    public ApiResponse<PageResult<Map<String, Object>>> listCarousels(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        List<Map<String, Object>> rows = carouselMapper.selectList(new LambdaQueryWrapper<CarouselEntity>()
                        .orderByDesc(CarouselEntity::getId))
                .stream()
                .map(this::toCarouselRow)
                .toList();
        return ApiResponse.success(PageResult.of(rows, page, limit));
    }

    @PutMapping("/carousels/{id}")
    public ApiResponse<Map<String, Object>> updateCarousel(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, Object> request) {
        CarouselEntity entity = requireCarousel(id);
        Map<String, Object> payload = request == null ? Map.of() : request;
        if (payload.containsKey("title")) {
            entity.setTitle(stringValue(payload.get("title")));
        }
        if (payload.containsKey("imageUrl")) {
            entity.setImageUrl(stringValue(payload.get("imageUrl")));
        }
        if (payload.containsKey("linkUrl")) {
            entity.setLinkUrl(stringValue(payload.get("linkUrl")));
        }
        if (payload.containsKey("sortNo")) {
            entity.setSortNo(intValue(payload.get("sortNo"), entity.getSortNo()));
        }
        if (payload.containsKey("status")) {
            entity.setStatus(intValue(payload.get("status"), entity.getStatus()));
        }
        carouselMapper.updateById(entity);
        return ApiResponse.success(toCarouselRow(entity));
    }

    @DeleteMapping("/carousels/{id}")
    public ApiResponse<Map<String, Object>> deleteCarousel(@PathVariable("id") Long id) {
        CarouselEntity entity = requireCarousel(id);
        entity.setStatus(0);
        carouselMapper.updateById(entity);
        return ApiResponse.success(toCarouselRow(entity));
    }

    @PostMapping("/config")
    public ApiResponse<Map<String, Object>> saveConfig(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> payload = request == null ? Map.of() : request;
        String key = requireText(payload.get("configKey"), "配置键不能为空");
        ConfigEntity entity = configMapper.selectOne(new LambdaQueryWrapper<ConfigEntity>()
                .eq(ConfigEntity::getConfigKey, key)
                .last("limit 1"));
        if (entity == null) {
            entity = new ConfigEntity();
            entity.setConfigKey(key);
            entity.setConfigValue(stringValue(payload.get("configValue")));
            entity.setDescription(stringValue(payload.get("description")));
            configMapper.insert(entity);
        } else {
            entity.setConfigValue(stringValue(payload.get("configValue")));
            entity.setDescription(stringValue(payload.get("description")));
            configMapper.updateById(entity);
        }
        return ApiResponse.success(toConfigRow(entity));
    }

    @GetMapping("/config/{key}")
    public ApiResponse<Map<String, Object>> getConfig(@PathVariable("key") String key) {
        ConfigEntity entity = configMapper.selectOne(new LambdaQueryWrapper<ConfigEntity>()
                .eq(ConfigEntity::getConfigKey, key)
                .last("limit 1"));
        if (entity == null) {
            throw new IllegalArgumentException("配置不存在");
        }
        return ApiResponse.success(toConfigRow(entity));
    }

    @PostMapping("/menus")
    public ApiResponse<Map<String, Object>> saveMenu(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> payload = request == null ? Map.of() : request;
        String roleCode = requireText(payload.get("roleCode"), "角色编码不能为空");
        MenuEntity entity = menuMapper.selectOne(new LambdaQueryWrapper<MenuEntity>()
                .eq(MenuEntity::getRoleCode, roleCode)
                .orderByDesc(MenuEntity::getId)
                .last("limit 1"));
        if (entity == null) {
            entity = new MenuEntity();
            entity.setRoleCode(roleCode);
            entity.setName(stringValue(payload.getOrDefault("name", roleCode + "菜单")));
            entity.setMenujson(stringValue(payload.getOrDefault("menujson", "[]")));
            menuMapper.insert(entity);
        } else {
            entity.setName(stringValue(payload.getOrDefault("name", roleCode + "菜单")));
            entity.setMenujson(stringValue(payload.getOrDefault("menujson", "[]")));
            menuMapper.updateById(entity);
        }
        return ApiResponse.success(toMenuRow(entity));
    }

    @GetMapping("/menus/{roleCode}")
    public ApiResponse<Map<String, Object>> getMenu(@PathVariable("roleCode") String roleCode) {
        MenuEntity entity = menuMapper.selectOne(new LambdaQueryWrapper<MenuEntity>()
                .eq(MenuEntity::getRoleCode, roleCode)
                .orderByDesc(MenuEntity::getId)
                .last("limit 1"));
        if (entity == null) {
            throw new IllegalArgumentException("菜单不存在");
        }
        return ApiResponse.success(toMenuRow(entity));
    }

    @PostMapping("/syslogs")
    public ApiResponse<Map<String, Object>> createLog(@RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> payload = request == null ? Map.of() : request;
        SyslogEntity entity = new SyslogEntity();
        entity.setUserId(longValue(payload.get("userId")));
        entity.setUsername(stringValue(payload.get("username")));
        entity.setRoleCode(stringValue(payload.get("roleCode")));
        entity.setOperation(requireText(payload.get("operation"), "操作名称不能为空"));
        entity.setRequestUri(stringValue(payload.get("requestUri")));
        entity.setRequestMethod(stringValue(payload.get("requestMethod")));
        entity.setRequestParams(stringValue(payload.get("requestParams")));
        entity.setIpAddress(stringValue(payload.get("ipAddress")));
        entity.setCostMillis(longValue(payload.get("costMillis")));
        syslogMapper.insert(entity);
        return ApiResponse.success(toSyslogRow(entity));
    }

    @GetMapping("/syslogs")
    public ApiResponse<PageResult<Map<String, Object>>> listLogs(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        List<Map<String, Object>> rows = syslogMapper.selectList(new LambdaQueryWrapper<SyslogEntity>()
                        .orderByDesc(SyslogEntity::getId))
                .stream()
                .map(this::toSyslogRow)
                .toList();
        return ApiResponse.success(PageResult.of(rows, page, limit));
    }

    /**
     * 查询资讯并在缺失时抛出业务错误。
     * 控制层统一在这里兜底，避免各个接口重复写空值判断。
     */
    private NewsEntity requireNews(Long id) {
        NewsEntity entity = id == null ? null : newsMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("资讯不存在");
        }
        return entity;
    }

    /**
     * 查询留言并在缺失时抛出业务错误。
     * 留言回复和删除都必须先确认主留言存在，避免写出无主回复。
     */
    private MessageEntity requireMessage(Long id) {
        MessageEntity entity = id == null ? null : messageMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("留言不存在");
        }
        return entity;
    }

    /**
     * 查询轮播图并在缺失时抛出业务错误。
     * 更新和停用轮播图都依赖同一条存在性校验。
     */
    private CarouselEntity requireCarousel(Long id) {
        CarouselEntity entity = id == null ? null : carouselMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("轮播图不存在");
        }
        return entity;
    }

    private Map<String, Object> toNewsRow(NewsEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("title", entity.getTitle());
        row.put("category", entity.getCategory());
        row.put("coverUrl", entity.getCoverUrl());
        row.put("summary", entity.getSummary());
        row.put("content", entity.getContent());
        row.put("publishStatus", entity.getPublishStatus());
        row.put("publisherId", entity.getPublisherId());
        row.put("publisherName", entity.getPublisherName());
        row.put("status", entity.getStatus());
        row.put("createdAt", stringValue(entity.getCreatedAt()));
        row.put("updatedAt", stringValue(entity.getUpdatedAt()));
        return row;
    }

    private Map<String, Object> toMessageRow(MessageEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("userId", entity.getUserId());
        row.put("username", entity.getUsername());
        row.put("userName", entity.getUsername());
        row.put("roleCode", entity.getRoleCode());
        row.put("title", entity.getTitle());
        row.put("content", entity.getContent());
        row.put("imageUrl", entity.getImageUrl());
        row.put("replyContent", entity.getReplyContent());
        row.put("replyImageUrl", entity.getReplyImageUrl());
        row.put("replyUserId", entity.getReplyUserId());
        row.put("replyUserName", entity.getReplyUserName());
        row.put("replyTime", stringValue(entity.getReplyTime()));
        row.put("status", entity.getStatus() != null && entity.getStatus() == 1 ? "pending" : entity.getStatus());
        row.put("createdAt", stringValue(entity.getCreatedAt()));
        row.put("updatedAt", stringValue(entity.getUpdatedAt()));
        return row;
    }

    private Map<String, Object> toCarouselRow(CarouselEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("title", entity.getTitle());
        row.put("imageUrl", entity.getImageUrl());
        row.put("linkUrl", entity.getLinkUrl());
        row.put("sortNo", entity.getSortNo());
        row.put("status", entity.getStatus());
        row.put("createdAt", stringValue(entity.getCreatedAt()));
        row.put("updatedAt", stringValue(entity.getUpdatedAt()));
        return row;
    }

    private Map<String, Object> toConfigRow(ConfigEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("configKey", entity.getConfigKey());
        row.put("configValue", entity.getConfigValue());
        row.put("description", entity.getDescription());
        row.put("createdAt", stringValue(entity.getCreatedAt()));
        row.put("updatedAt", stringValue(entity.getUpdatedAt()));
        return row;
    }

    private Map<String, Object> toMenuRow(MenuEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("roleCode", entity.getRoleCode());
        row.put("name", entity.getName());
        row.put("menujson", entity.getMenujson());
        row.put("createdAt", stringValue(entity.getCreatedAt()));
        row.put("updatedAt", stringValue(entity.getUpdatedAt()));
        return row;
    }

    private Map<String, Object> toSyslogRow(SyslogEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("userId", entity.getUserId());
        row.put("username", entity.getUsername());
        row.put("roleCode", entity.getRoleCode());
        row.put("operation", entity.getOperation());
        row.put("requestUri", entity.getRequestUri());
        row.put("requestMethod", entity.getRequestMethod());
        row.put("requestParams", entity.getRequestParams());
        row.put("ipAddress", entity.getIpAddress());
        row.put("costMillis", entity.getCostMillis());
        row.put("createdAt", stringValue(entity.getCreatedAt()));
        return row;
    }

    private String requireText(Object value, String message) {
        if (value == null || value.toString().trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.toString().trim();
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer intValue(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(400, exception.getMessage());
    }

    private void ensureMessageReplyRole(TrustedUserContext context) {
        if (!context.authenticated()) {
            return;
        }
        if (context.isPatient()) {
            throw new IllegalArgumentException("当前登录角色无权回复留言");
        }
    }
}
