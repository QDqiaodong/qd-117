package com.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("shelf_capacity")
public class ShelfCapacity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "货架编号不能为空")
    private String shelfNo;

    @NotNull(message = "顶针最大盒数不能为空")
    private Integer maxPinBoxes;

    @NotNull(message = "垫片最大包数不能为空")
    private Integer maxShimPacks;

    private Integer currentPinBoxes;

    private Integer currentShimPacks;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public Integer getRemainingPinBoxes() {
        if (maxPinBoxes == null) return 0;
        int current = currentPinBoxes == null ? 0 : currentPinBoxes;
        return Math.max(0, maxPinBoxes - current);
    }

    public Integer getRemainingShimPacks() {
        if (maxShimPacks == null) return 0;
        int current = currentShimPacks == null ? 0 : currentShimPacks;
        return Math.max(0, maxShimPacks - current);
    }

    public boolean canAddPinBoxes(int quantity) {
        return getRemainingPinBoxes() >= quantity;
    }

    public boolean canAddShimPacks(int quantity) {
        return getRemainingShimPacks() >= quantity;
    }
}
