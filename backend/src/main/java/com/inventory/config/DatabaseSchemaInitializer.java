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
        ensureScrapReasonTable();
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

    private void ensureScrapReasonTable() {
        String ddl = """
                CREATE TABLE IF NOT EXISTS scrap_reason (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    reason_name VARCHAR(100) NOT NULL COMMENT '原因名称：顶针弯曲/针尖磨损/垫片变形/孔径偏差等',
                    reason_code VARCHAR(50) NOT NULL COMMENT '原因编码',
                    part_type VARCHAR(50) NOT NULL COMMENT '适用零件类型：顶针/限位垫片/全部',
                    sort INT NOT NULL DEFAULT 0 COMMENT '排序号，数字越小越靠前',
                    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
                    remark VARCHAR(500) COMMENT '备注',
                    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_reason_code (reason_code),
                    UNIQUE KEY uk_reason_name (reason_name),
                    INDEX idx_part_type (part_type),
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='破损原因字典表'
                """;
        executeDdl("scrap_reason", ddl);
        initDefaultScrapReasons();
    }

    private void initDefaultScrapReasons() {
        String checkSql = "SELECT COUNT(*) FROM scrap_reason";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             var rs = statement.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        } catch (Exception e) {
            log.warn("检查破损原因数据失败", e);
            return;
        }

        String[][] defaults = {
                {"PIN_BEND", "顶针弯曲", "顶针", "1"},
                {"PIN_WEAR", "针尖磨损", "顶针", "2"},
                {"PIN_BREAK", "顶针断裂", "顶针", "3"},
                {"PIN_OTHER", "顶针其他", "顶针", "4"},
                {"SHIM_DEFORM", "垫片变形", "限位垫片", "5"},
                {"HOLE_DEVIATION", "孔径偏差", "限位垫片", "6"},
                {"SHIM_WEAR", "垫片磨损", "限位垫片", "7"},
                {"SHIM_OTHER", "垫片其他", "限位垫片", "8"},
                {"COMMON_OTHER", "其他", "全部", "99"}
        };

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String[] row : defaults) {
                String insert = String.format(
                        "INSERT INTO scrap_reason (reason_code, reason_name, part_type, sort, status, create_time, update_time) " +
                                "VALUES ('%s', '%s', '%s', %s, 1, NOW(), NOW())",
                        row[0], row[1], row[2], row[3]);
                statement.execute(insert);
            }
            log.info("初始化破损原因字典默认数据完成");
        } catch (Exception e) {
            log.warn("初始化破损原因默认数据失败", e);
        }
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
