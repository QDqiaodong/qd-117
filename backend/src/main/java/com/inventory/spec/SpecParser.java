package com.inventory.spec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpecParser {

    private final ObjectMapper objectMapper;

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final Pattern MATERIAL_PATTERN = Pattern.compile("材质\\s*[:：]?\\s*([A-Za-z0-9\\u4e00-\\u9fa5_\\-]+)");

    public ParsedSpec parse(String partType, String specParams) {
        ParsedSpec spec = new ParsedSpec();
        spec.setPartType(partType);
        spec.setRawSpecParams(specParams);

        if (specParams == null || specParams.trim().isEmpty()) {
            return spec;
        }

        String raw = specParams.trim();

        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node != null && node.isObject()) {
                if ("顶针".equals(partType)) {
                    spec.setDiameter(stripToNumber(getJsonAny(node, "diameter", "直径", "Diameter", "D")));
                    spec.setLength(stripToNumber(getJsonAny(node, "length", "长度", "Length", "L")));
                    String material = getJsonAny(node, "material", "材质", "Material", "M");
                    spec.setMaterial(material == null ? null : material.trim());
                } else if ("限位垫片".equals(partType)) {
                    spec.setThickness(stripToNumber(getJsonAny(node, "thickness", "厚度", "Thickness", "T")));
                    spec.setOuterDiameter(stripToNumber(getJsonAny(node, "outerDiameter", "外径", "OuterDiameter", "OD", "外直径")));
                    spec.setInnerDiameter(stripToNumber(getJsonAny(node, "innerDiameter", "孔径", "InnerDiameter", "ID", "内直径", "holeDiameter", "hole")));
                    String material = getJsonAny(node, "material", "材质", "Material", "M");
                    spec.setMaterial(material == null ? null : material.trim());
                }
            }
        } catch (Exception e) {
            log.debug("JSON 解析失败，使用正则回退: {}", raw);
        }

        if ("顶针".equals(partType)) {
            if (spec.getDiameter() == null) {
                spec.setDiameter(extractNumber(raw, "直径"));
            }
            if (spec.getLength() == null) {
                spec.setLength(extractNumber(raw, "长度"));
            }
            if (spec.getMaterial() == null || spec.getMaterial().isEmpty()) {
                String material = extractMaterial(raw);
                spec.setMaterial(material == null ? null : material.trim());
            }
        } else if ("限位垫片".equals(partType)) {
            if (spec.getThickness() == null) {
                spec.setThickness(extractNumber(raw, "厚度"));
            }
            if (spec.getOuterDiameter() == null) {
                spec.setOuterDiameter(extractNumber(raw, "外径"));
            }
            if (spec.getOuterDiameter() == null) {
                spec.setOuterDiameter(extractNumber(raw, "外直径"));
            }
            if (spec.getInnerDiameter() == null) {
                spec.setInnerDiameter(extractNumber(raw, "孔径"));
            }
            if (spec.getInnerDiameter() == null) {
                spec.setInnerDiameter(extractNumber(raw, "内直径"));
            }
            if (spec.getMaterial() == null || spec.getMaterial().isEmpty()) {
                String material = extractMaterial(raw);
                spec.setMaterial(material == null ? null : material.trim());
            }
        }

        return spec;
    }

    private String getJsonAny(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull()) {
                String text = value.isTextual() ? value.asText() : value.toString();
                if (text != null && !text.trim().isEmpty()) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private String stripToNumber(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : (text.trim().isEmpty() ? null : text.trim());
    }

    private String extractNumber(String text, String keyword) {
        Matcher matcher = Pattern.compile(keyword + "\\s*[:：]?\\s*(\\d+(?:\\.\\d+)?)").matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractMaterial(String text) {
        Matcher matcher = MATERIAL_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }
}
