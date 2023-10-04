package it.devchallenge.excel.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Builder
@Getter
@Setter
@Document(collection = "cells")
public class CellEntity {
    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();
    private String name;
    private String sheetName;
    private String value;
    private CellType type;

    public enum CellType {
        STRING, DIGIT, FORMULA
    }
}
