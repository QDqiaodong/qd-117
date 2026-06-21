package com.inventory.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSchemaInitializer {

    private final DataSource dataSource;

    @PostConstruct
    public void initialize() {
        ensureShelfCapacityTable();
        ensureLineQuotaTable();
    }

    private void ensureShelfCapacityTable() {
        String ddl = """
                CREATE TABLE IF NOT EXISTS shelf_capacity (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    shelf_no VARCHAR(50) NOT NULL COMMENT '货架编号',
                    max_pin_boxes INT NOT NULL DEFAULT 0 COMMENT '顶针最大盒数',
                    max_shim_packs INT NOT NULL DEFAULT 0 COMMENT '垫片最大包数',
                    current_pin_boxes INT NOT NULL DEFAULT 0 COMMENT '当前顶针盒数',
                    current_shim_packs INT NOT NULL DEFAULT 0 COMMENT '当前垫片包数',
                    remark VARCHAR(500) COMMENT '备注',
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_shelf_no (shelf_no),
                    INDEX idx_shelf_no (shelf_no)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货架容量表'
                """;
        executeDdl("shelf_capacity", ddl);
    }

    private void ensureLineQuotaTable() {
        String ddl = """
                CREATE TABLE IF NOT EXISTS line_quota (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    quarter VARCHAR(20) NOT NULL COMMENT '季度：2026-Q2',
                    production_line VARCHAR(100) NOT NULL COMMENT '领用产线：一号装配线',
                    part_type VARCHAR(50) NOT NULL COMMENT '小件类型：顶针/限位垫片',
                    max_quantity INT NOT NULL DEFAULT 0 COMMENT '季度配额上限',
                    used_quantity INT NOT NULL DEFAULT 0 COMMENT '当前已领用数量',
                    remark VARCHAR(500) COMMENT '备注',
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_quarter_line_type (quarter, production_line, part_type),
                    INDEX idx_quarter (quarter),
                    INDEX idx_production_line (production_line)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产线领用配额表'
                """;
        executeDdl("line_quota", ddl);
    }

    private void executeDdl(String tableName, String ddl) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(ddl);
            log.info("数据库表 {} 已确认可用", tableName);
        } catch (Exception e) {
            throw new IllegalStateException("初始化 " + tableName + " 表失败", e);
        }
    }
}
