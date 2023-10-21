package it.devchallenge.excel.service;

import com.mongodb.internal.VisibleForTesting;
import it.devchallenge.excel.exceptions.CalculationException;
import it.devchallenge.excel.exceptions.NotFoundException;
import it.devchallenge.excel.model.CellEntity;
import it.devchallenge.excel.repository.CellRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.mariuszgromada.math.mxparser.Expression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ResultCalculator {
    private final CellEntity cell;
    private final CellRepository cellRepository;

    @Setter
    private CellEntity newCell;


    private CellEntity getCell(String cellName) throws CalculationException, NotFoundException {
        if (cell.getName().equalsIgnoreCase(cellName)) {
            throw new CalculationException("Recursive formula");
        } else if (newCell != null && newCell.getName().equalsIgnoreCase(cellName)) {
            return newCell;
        } else {
            return cellRepository.findBySheetNameIgnoreCaseAndNameIgnoreCase(cell.getSheetName(), cellName)
                    .orElseThrow(() -> new NotFoundException("Cell %s not found".formatted(cellName)));
        }
    }

    public String calculateFormula() throws CalculationException {
        String formula;
        try {
            formula = String.join("", fillFormula(cell.getValue()));
            if (!formula.matches(".*[-+*/()].*")) {
                log.info("Formula doesn't have math operators, returning result: {}", formula);
                return formula;
            }
        } catch (NotFoundException e) {
            log.error("Failed to fill formula {}", cell.getValue(), e);
            throw new CalculationException("Failed to fill formula", e);
        }

        Expression e = new Expression(formula);
        double expressionResult = e.calculate();
        if (Double.isNaN(expressionResult)) {
            throw new CalculationException("Failed to calculate expression %s".formatted(cell.getValue()));
        }
        log.debug("Formula {} calculation result {}", formula, expressionResult);
        return String.valueOf(expressionResult).replaceAll("\\.?0*$", "");
    }

    @VisibleForTesting(otherwise = VisibleForTesting.AccessModifier.PRIVATE)
    protected List<String> fillFormula(String formula) throws NotFoundException, CalculationException {
        List<String> formulaElems = parseFormula(formula);
        List<String> result = new ArrayList<>();
        for (String elem : formulaElems) {
            if ((elem.length() == 1 && isOperator(elem.charAt(0))) || isDigit(elem)) {
                result.add(elem);
            } else {
                CellEntity nexCell = getCell(elem);
                if (nexCell.getType().equals(CellEntity.CellType.FORMULA)) {
                    result.addAll(fillFormula(nexCell.getValue()));
                } else {
                    result.add(nexCell.getValue());
                }
            }
        }
        log.debug("Filled formula {} result: {}", formula, result);
        return result;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.AccessModifier.PRIVATE)
    protected List<String> parseFormula(String formula) throws CalculationException {
        if (!formula.startsWith("=")) {
            throw new CalculationException("Not valid formula");
        }
        String input = formula.substring(1);
        String[] elems = input.split("((?=[+\\-*/()])|(?<=[+\\-*/()]))");
        var resultElems = Arrays.stream(elems).map(String::trim).filter(e -> !e.isEmpty()).toList();
        log.debug("Parsed formula {} to elems: {}", formula, resultElems);
        return resultElems;
    }

    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == ')';
    }

    public static boolean isDigit(String val) {
        return val.matches("-?\\d+(\\.\\d+)?");
    }
}
