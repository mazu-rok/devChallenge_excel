package it.devchallenge.excel.service;

import com.mongodb.internal.VisibleForTesting;
import it.devchallenge.excel.dto.CellResponse;
import it.devchallenge.excel.exceptions.CalculationException;
import it.devchallenge.excel.exceptions.NotFoundException;
import it.devchallenge.excel.model.CellEntity;
import it.devchallenge.excel.repository.CellRepository;
import lombok.extern.slf4j.Slf4j;
import org.mariuszgromada.math.mxparser.Expression;
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
        CellEntity cell = cellRepository.findBySheetNameIgnoreCaseAndNameIgnoreCase(sheetName, cellName)
                .orElseGet(() -> CellEntity.builder()
                        .name(cellName)
                        .sheetName(sheetName)
                        .build());

        CellEntity.CellType type;
        if (cellValue.startsWith("=")) {
            type = CellEntity.CellType.FORMULA;
        } else if (isDigit(cellValue)) {
            type = CellEntity.CellType.DIGIT;
        } else {
            type = CellEntity.CellType.STRING;
            // we can't save string data if this cell used in math formula
            long usedCount = cellRepository.findByValueContainingInput(cellName);
            if (usedCount != 0) {
                long stringAcceptableFormula = cellRepository.countByValueIgnoreCase("=" + cellName);
                if (usedCount != stringAcceptableFormula) {
                    throw new CalculationException("Cell used math in formula");
                }
            }
        }

        cell.setValue(cellValue);
        cell.setType(type);
        String result;
        result = getResult(cell);
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

    public CellResponse getCell(String sheetName, String cellName) throws NotFoundException {
        var cell = getCellEntity(sheetName, cellName);

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
            case FORMULA -> calculateResult(cell);
            case DIGIT, STRING -> cell.getValue();
        };
    }

    @VisibleForTesting(otherwise = VisibleForTesting.AccessModifier.PRIVATE)
    protected String calculateResult(CellEntity cell) throws CalculationException {
        String expression;
        try {
            expression = String.join("", parseCommand(cell));
        } catch (NotFoundException e) {
            throw new CalculationException("Failed to parse expression %s".formatted(cell.getValue()), e);
        }
        Expression e = new Expression(expression);
        double expressionResult = e.calculate();
        if (Double.isNaN(expressionResult)) {
            throw new CalculationException("Failed to calculate expression %s".formatted(cell.getValue()));
        }
        return String.valueOf(expressionResult).replaceAll("\\.?0*$", "");
    }

    @VisibleForTesting(otherwise = VisibleForTesting.AccessModifier.PRIVATE)
    protected List<String> parseCommand(CellEntity cell) throws NotFoundException, CalculationException {
        String input = cell.getValue().substring(1);
        List<String> result = new ArrayList<>();
        var tokens = input.split("((?=[+\\-*/()])|(?<=[+\\-*/()]))");

        for (String elem : tokens) {
            elem = elem.trim();
            if (elem.isEmpty()) {
                continue;
            }
            if (isDigit(elem) || (elem.length() == 1 && isOperator(elem.charAt(0)))) {
                result.add(elem);
            } else {
                if (cell.getName().equals(elem)) {
                    throw new CalculationException("Recursive reference");
                }
                CellEntity cellEntity = getCellEntity(cell.getSheetName(), elem);
                if (cellEntity.getType().equals(CellEntity.CellType.FORMULA)) {
                    result.addAll(parseCommand(cellEntity));
                } else {
                    result.add(cellEntity.getValue());
                }
            }
        }
        return result;
    }

    private CellEntity getCellEntity(String sheetName, String cellName) throws NotFoundException {
        return cellRepository.findBySheetNameIgnoreCaseAndNameIgnoreCase(sheetName, cellName)
                .orElseThrow(() -> new NotFoundException("Cell '%s' in sheet %s not found".formatted(cellName, sheetName)));
    }

    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == ')';
    }

    private boolean isDigit(String val) {
        return val.matches("-?\\d+(\\.\\d+)?");
    }

}
