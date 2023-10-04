package it.devchallenge.excel.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class CellResponse {
    private String value;
    private String result;

}
