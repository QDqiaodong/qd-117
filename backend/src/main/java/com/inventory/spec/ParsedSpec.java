package com.inventory.spec;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ParsedSpec implements Serializable {

    private String partType;

    private String diameter;

    private String length;

    private String thickness;

    private String outerDiameter;

    private String innerDiameter;

    private String material;

    private String rawSpecParams;

    public boolean isPin() {
        return "顶针".equals(partType);
    }

    public boolean isShim() {
        return "限位垫片".equals(partType);
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (partType == null || partType.trim().isEmpty()) {
            errors.add("零件类型不能为空");
            return errors;
        }
        if (isPin()) {
            if (diameter == null || diameter.trim().isEmpty()) {
                errors.add("顶针规格缺少【直径】参数");
            } else if (!isValidNumber(diameter)) {
                errors.add("顶针【直径】参数格式不正确: " + diameter);
            }
            if (length == null || length.trim().isEmpty()) {
                errors.add("顶针规格缺少【长度】参数");
            } else if (!isValidNumber(length)) {
                errors.add("顶针【长度】参数格式不正确: " + length);
            }
        } else if (isShim()) {
            if (thickness == null || thickness.trim().isEmpty()) {
                errors.add("垫片规格缺少【厚度】参数");
            } else if (!isValidNumber(thickness)) {
                errors.add("垫片【厚度】参数格式不正确: " + thickness);
            }
            if (outerDiameter == null || outerDiameter.trim().isEmpty()) {
                errors.add("垫片规格缺少【外径】参数");
            } else if (!isValidNumber(outerDiameter)) {
                errors.add("垫片【外径】参数格式不正确: " + outerDiameter);
            }
            if (innerDiameter != null && !innerDiameter.trim().isEmpty() && !isValidNumber(innerDiameter)) {
                errors.add("垫片【孔径】参数格式不正确: " + innerDiameter);
            }
        }
        return errors;
    }

    public boolean isValid() {
        return validate().isEmpty();
    }

    private boolean isValidNumber(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(text.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public Double getDiameterAsDouble() {
        return parseDouble(diameter);
    }

    public Double getLengthAsDouble() {
        return parseDouble(length);
    }

    public Double getThicknessAsDouble() {
        return parseDouble(thickness);
    }

    public Double getOuterDiameterAsDouble() {
        return parseDouble(outerDiameter);
    }

    public Double getInnerDiameterAsDouble() {
        return parseDouble(innerDiameter);
    }

    private Double parseDouble(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
