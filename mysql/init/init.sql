CREATE TABLE IF NOT EXISTS small_part (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    part_model VARCHAR(100) NOT NULL COMMENT '零件型号',
    part_name VARCHAR(100) NOT NULL COMMENT '零件名称',
    part_type VARCHAR(50) NOT NULL COMMENT '零件类型：顶针/限位垫片',
    spec_params VARCHAR(500) COMMENT '规格参数(JSON)',
    shelf_no VARCHAR(50) NOT NULL COMMENT '存放货架编号',
    stock_quantity INT NOT NULL DEFAULT 0 COMMENT '当前库存数量',
    unit VARCHAR(20) DEFAULT '件' COMMENT '单位',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_part_model (part_model)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小件库存表';

CREATE TABLE IF NOT EXISTS stock_in_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    part_id BIGINT NOT NULL COMMENT '小件ID',
    part_model VARCHAR(100) NOT NULL COMMENT '零件型号',
    quantity INT NOT NULL COMMENT '入库数量',
    shelf_no VARCHAR(50) NOT NULL COMMENT '存放货架编号',
    operator VARCHAR(50) NOT NULL COMMENT '操作人',
    remark VARCHAR(500) COMMENT '备注',
    box_nos VARCHAR(2000) COMMENT '盒号列表（逗号分隔）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_part_id (part_id),
    INDEX idx_create_time (create_time),
    INDEX idx_part_model_create_time (part_model, create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库记录表';

CREATE TABLE IF NOT EXISTS stock_out_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    part_id BIGINT NOT NULL COMMENT '小件ID',
    part_model VARCHAR(100) NOT NULL COMMENT '零件型号',
    quantity INT NOT NULL COMMENT '领用数量',
    production_line VARCHAR(100) NOT NULL COMMENT '领用产线',
    operator VARCHAR(50) NOT NULL COMMENT '操作人',
    receiver VARCHAR(50) COMMENT '领用人',
    remark VARCHAR(500) COMMENT '备注',
    box_nos VARCHAR(2000) COMMENT '盒号列表（逗号分隔）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_part_id (part_id),
    INDEX idx_create_time (create_time),
    INDEX idx_part_model_create_time (part_model, create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库领用记录表';

CREATE TABLE IF NOT EXISTS stock_check_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quarter VARCHAR(20) NOT NULL COMMENT '盘点季度：2024-Q1',
    part_id BIGINT NOT NULL COMMENT '小件ID',
    part_model VARCHAR(100) NOT NULL COMMENT '零件型号',
    part_name VARCHAR(100) NOT NULL COMMENT '零件名称',
    part_type VARCHAR(50) NOT NULL COMMENT '零件类型：顶针/限位垫片',
    frozen_stock_quantity INT NOT NULL COMMENT '冻结时账面库存数量',
    frozen_shelf_no VARCHAR(50) NOT NULL COMMENT '冻结时货架编号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_quarter (quarter),
    INDEX idx_part_id (part_id),
    UNIQUE KEY uk_quarter_part_id (quarter, part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='季度盘点库存快照表';

CREATE TABLE IF NOT EXISTS stock_check_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    part_id BIGINT NOT NULL COMMENT '小件ID',
    part_model VARCHAR(100) NOT NULL COMMENT '零件型号',
    system_quantity INT NOT NULL COMMENT '系统库存数量（基于快照）',
    actual_quantity INT NOT NULL COMMENT '实际库存数量',
    diff_quantity INT NOT NULL COMMENT '差异数量',
    shelf_no VARCHAR(50) NOT NULL COMMENT '货架编号',
    check_person VARCHAR(50) NOT NULL COMMENT '盘点人',
    remark VARCHAR(500) COMMENT '差异原因备注',
    quarter VARCHAR(20) NOT NULL COMMENT '盘点季度：2024-Q1',
    snapshot_id BIGINT COMMENT '关联快照ID',
    confirm_status TINYINT NOT NULL DEFAULT 0 COMMENT '差异确认状态：0-未确认，1-已闭环',
    handle_conclusion VARCHAR(500) COMMENT '处理结论',
    confirm_person VARCHAR(50) COMMENT '确认人',
    confirm_time DATETIME COMMENT '确认时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_part_id (part_id),
    INDEX idx_quarter (quarter),
    INDEX idx_confirm_status (confirm_status),
    INDEX idx_part_model_create_time (part_model, create_time DESC),
    UNIQUE KEY uk_part_model_quarter (part_model, quarter)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存盘点记录表';

CREATE TABLE IF NOT EXISTS scrap_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    part_id BIGINT NOT NULL COMMENT '小件ID',
    part_model VARCHAR(100) NOT NULL COMMENT '零件型号',
    quantity INT NOT NULL COMMENT '报废数量',
    scrap_reason VARCHAR(200) NOT NULL COMMENT '报废原因：变形/断裂/磨损/其他',
    operator VARCHAR(50) NOT NULL COMMENT '操作人',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_part_id (part_id),
    INDEX idx_create_time (create_time),
    INDEX idx_part_model_create_time (part_model, create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报废记录表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货架容量表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产线领用配额表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='破损原因字典表';

INSERT INTO scrap_reason (reason_code, reason_name, part_type, sort, status) VALUES
('PIN_BEND', '顶针弯曲', '顶针', 1, 1),
('PIN_WEAR', '针尖磨损', '顶针', 2, 1),
('PIN_BREAK', '顶针断裂', '顶针', 3, 1),
('PIN_OTHER', '顶针其他', '顶针', 4, 1),
('SHIM_DEFORM', '垫片变形', '限位垫片', 5, 1),
('HOLE_DEVIATION', '孔径偏差', '限位垫片', 6, 1),
('SHIM_WEAR', '垫片磨损', '限位垫片', 7, 1),
('SHIM_OTHER', '垫片其他', '限位垫片', 8, 1),
('COMMON_OTHER', '其他', '全部', 99, 1);

CREATE TABLE IF NOT EXISTS pin_box (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    box_no VARCHAR(100) NOT NULL COMMENT '盒号',
    part_id BIGINT NOT NULL COMMENT '零件ID',
    part_model VARCHAR(100) NOT NULL COMMENT '零件型号',
    status VARCHAR(30) NOT NULL COMMENT '状态：IN_STOCK-在库，OUT_OF_STOCK-已出库，SCRAPPED-已报废',
    stock_in_record_id BIGINT COMMENT '入库记录ID',
    stock_out_record_id BIGINT COMMENT '出库记录ID',
    production_line VARCHAR(100) COMMENT '领用产线',
    shelf_no VARCHAR(50) NOT NULL COMMENT '货架编号',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_box_no (box_no),
    INDEX idx_part_id (part_id),
    INDEX idx_status (status),
    INDEX idx_part_model (part_model),
    INDEX idx_stock_in_record_id (stock_in_record_id),
    INDEX idx_stock_out_record_id (stock_out_record_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='顶针盒号表';
