package it.devchallenge.excel.service;

import com.mongodb.internal.VisibleForTesting;
import it.devchallenge.excel.dto.CellResponse;
import it.devchallenge.excel.exceptions.CalculationException;
import it.devchallenge.excel.exceptions.NotFoundException;
import it.devchallenge.excel.model.CellEntity;
import it.devchallenge.excel.repository.CellRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class SheetService {
    private final CellRepository cellRepository;

    @Autowired
    public SheetService(CellRepository cellRepository) {
        this.cellRepository = cellRepository;
    }

    public CellResponse addCell(String sheetName, String cellName, String cellValue) throws CalculationException {
        CellEntity.CellType type;
        if (cellValue.startsWith("=")) {
            type = CellEntity.CellType.FORMULA;
        } else if (ResultCalculator.isDigit(cellValue)) {
            type = CellEntity.CellType.DIGIT;
        } else {
            type = CellEntity.CellType.STRING;
        }

        CellEntity cell = getCell(sheetName, cellName)
                .orElseGet(() ->
                        CellEntity.builder()
                                .name(cellName)
                                .sheetName(sheetName)
                                .build());

        cell.setValue(cellValue);
        cell.setType(type);
        if (type.equals(CellEntity.CellType.FORMULA) || type.equals(CellEntity.CellType.STRING) || cellValue.equals("0")) {
            // calculate the result for all cells that use the current cell
            var cells = cellRepository.findByValueContainingInput(sheetName, cellName);
            // todo: run it async to improve speed
            for (CellEntity c : cells) {
                ResultCalculator resultCalculator = new ResultCalculator(c, cellRepository);
                resultCalculator.setNewCell(cell);
                resultCalculator.calculateFormula();
            }
        }

        String result = getResult(cell);
        cellRepository.save(cell);

        return CellResponse.builder()
                .value(cellValue)
                .result(result)
                .build();
    }

    public Map<String, CellResponse> getSheet(String sheetName) throws NotFoundException {
        List<CellEntity> sheetCells = cellRepository.findAllBySheetNameIgnoreCase(sheetName);
        if (sheetCells.isEmpty()) {
            throw new NotFoundException("Sheet %s not found".formatted(sheetName));
        }
        Map<String, CellResponse> response = new HashMap<>();
        sheetCells.forEach(cell -> {
            String result;
            try {
                result = getResult(cell);
                cellRepository.save(cell);
            } catch (CalculationException e) {
                log.error("Calculation error!", e);
                result = "ERROR";
            }
            response.put(cell.getName(), CellResponse.builder()
                    .value(cell.getValue())
                    .result(result)
                    .build());
        });
        return response;
    }

    public CellResponse getCellResponse(String sheetName, String cellName) throws NotFoundException {
        var cell = getCell(sheetName, cellName)
                .orElseThrow(() -> new NotFoundException("Cell '%s' in sheet %s not found".formatted(cellName, sheetName)));

        String result;
        try {
            result = getResult(cell);
        } catch (CalculationException e) {
            log.error("Calculation error", e);
            result = "ERROR";
        }

        return CellResponse.builder()
                .value(cell.getValue())
                .result(result)
                .build();
    }

    @VisibleForTesting(otherwise = VisibleForTesting.AccessModifier.PRIVATE)
    protected String getResult(CellEntity cell) throws CalculationException {
        return switch (cell.getType()) {
            case FORMULA -> new ResultCalculator(cell, cellRepository).calculateFormula();
            case DIGIT, STRING -> cell.getValue();
        };
    }

    private Optional<CellEntity> getCell(String sheetName, String cellName) {
        return cellRepository.findBySheetNameIgnoreCaseAndNameIgnoreCase(sheetName, cellName);
    }
}
