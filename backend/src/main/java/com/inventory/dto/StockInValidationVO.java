package com.inventory.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class StockInValidationVO implements Serializable {

    private boolean valid;

    private List<String> errors = new ArrayList<>();

    private List<ShelfSuggestionVO> suggestions = new ArrayList<>();

    public static StockInValidationVO success() {
        StockInValidationVO vo = new StockInValidationVO();
        vo.setValid(true);
        return vo;
    }

    public static StockInValidationVO fail(List<String> errors) {
        StockInValidationVO vo = new StockInValidationVO();
        vo.setValid(false);
        vo.setErrors(errors);
        return vo;
    }

    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }

    public void addSuggestion(ShelfSuggestionVO suggestion) {
        this.suggestions.add(suggestion);
    }
}
