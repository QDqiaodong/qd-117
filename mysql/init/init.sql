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
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_part_id (part_id),
    INDEX idx_create_time (create_time),
    INDEX idx_part_model_create_time (part_model, create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库领用记录表';

CREATE TABLE IF NOT EXISTS stock_check_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    part_id BIGINT NOT NULL COMMENT '小件ID',
    part_model VARCHAR(100) NOT NULL COMMENT '零件型号',
    system_quantity INT NOT NULL COMMENT '系统库存数量',
    actual_quantity INT NOT NULL COMMENT '实际库存数量',
    diff_quantity INT NOT NULL COMMENT '差异数量',
    shelf_no VARCHAR(50) NOT NULL COMMENT '货架编号',
    check_person VARCHAR(50) NOT NULL COMMENT '盘点人',
    remark VARCHAR(500) COMMENT '差异原因备注',
    quarter VARCHAR(20) NOT NULL COMMENT '盘点季度：2024-Q1',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_part_id (part_id),
    INDEX idx_quarter (quarter),
    INDEX idx_part_model_create_time (part_model, create_time DESC)
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
