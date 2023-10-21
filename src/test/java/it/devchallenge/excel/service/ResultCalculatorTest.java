package it.devchallenge.excel.service;

import it.devchallenge.excel.exceptions.CalculationException;
import it.devchallenge.excel.exceptions.NotFoundException;
import it.devchallenge.excel.model.CellEntity;
import it.devchallenge.excel.repository.CellRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResultCalculatorTest {
    private final CellRepository cellRepository = mock(CellRepository.class);


    /**
     * Parse command tests
     */
    @Test
    public void parseCommandWithDigits() throws CalculationException, NotFoundException {
        CellEntity cell = getCell("=1+2*3/4-(5+6)", CellEntity.CellType.FORMULA);

        var strings = new ResultCalculator(cell, cellRepository).parseFormula(cell.getValue());
        assertThat(strings).isEqualTo(List.of("1","+","2","*","3","/","4","-","(","5","+","6",")"));
    }

    @Test
    public void parseCommandWithSpacesWithDigits() throws CalculationException, NotFoundException {
        CellEntity cell = getCell("=1 +  2 *  3 / 4  -  ( 5 +  6 ) ", CellEntity.CellType.FORMULA);

        var strings = new ResultCalculator(cell, cellRepository).parseFormula(cell.getValue());
        assertThat(strings).isEqualTo(List.of("1","+","2","*","3","/","4","-","(","5","+","6",")"));
    }

    @Test
    public void fillFormulaWithCellNames() throws CalculationException, NotFoundException {
        CellEntity cell = getCell("=cell1+cell1*(cell1+cell1)", CellEntity.CellType.FORMULA);
        when(cellRepository.findBySheetNameIgnoreCaseAndNameIgnoreCase(cell.getSheetName(), "cell1"))
                .thenReturn(Optional.ofNullable(getCell("9", CellEntity.CellType.DIGIT)));

        var strings = new ResultCalculator(cell, cellRepository).fillFormula(cell.getValue());
        assertThat(strings).isEqualTo(List.of("9","+","9","*","(","9","+","9",")"));
    }

    @Test
    public void fillFormulaWithSpacesWithCellNames() throws CalculationException, NotFoundException {
        CellEntity cell = getCell("=cell1 + cell1 * (cell1 + cell1)", CellEntity.CellType.FORMULA);
        when(cellRepository.findBySheetNameIgnoreCaseAndNameIgnoreCase(cell.getSheetName(), "cell1"))
                .thenReturn(Optional.ofNullable(getCell("9", CellEntity.CellType.DIGIT)));

        var strings = new ResultCalculator(cell, cellRepository).fillFormula(cell.getValue());
        assertThat(strings).isEqualTo(List.of("9","+","9","*","(","9","+","9",")"));
    }

    /**
     * Calculation tests
     */
    @Test
    public void calculateDigitExpression() throws CalculationException {
        CellEntity cell = getCell("=1+2*3/4-(5+6)", CellEntity.CellType.FORMULA);

        var result = new ResultCalculator(cell, cellRepository).calculateFormula();
        assertThat(result).isEqualTo("-8.5");
    }

    @Test
    public void calculateShouldReturnValueFromAnotherCell() throws CalculationException {
        CellEntity anotherCell = getCell("cell1", "2", CellEntity.CellType.DIGIT);
        CellEntity cell = getCell("=" + anotherCell.getName(), CellEntity.CellType.FORMULA);

        when(cellRepository.findBySheetNameIgnoreCaseAndNameIgnoreCase(anotherCell.getSheetName(), anotherCell.getName()))
                .thenReturn(Optional.of(anotherCell));

        var result = new ResultCalculator(cell, cellRepository).calculateFormula();
        assertThat(result).isEqualTo(anotherCell.getValue());
    }

    @Test
    public void calculateShouldSolveExpressionWithOtherCellsValue() throws CalculationException {
        CellEntity anotherCell = getCell("cell1", "2", CellEntity.CellType.DIGIT);
        CellEntity anotherCell2 = getCell("cell2", "3", CellEntity.CellType.DIGIT);
        CellEntity cell = getCell("=%s+%s".formatted(anotherCell.getName(), anotherCell2.getName()), CellEntity.CellType.FORMULA);

        when(cellRepository.findBySheetNameIgnoreCaseAndNameIgnoreCase(anotherCell.getSheetName(), anotherCell.getName()))
                .thenReturn(Optional.of(anotherCell));
        when(cellRepository.findBySheetNameIgnoreCaseAndNameIgnoreCase(anotherCell2.getSheetName(), anotherCell2.getName()))
                .thenReturn(Optional.of(anotherCell2));

        var result = new ResultCalculator(cell, cellRepository).calculateFormula();
        assertThat(result).isEqualTo("5");
    }

    @Test
    public void calculateShouldReturnErrorForNotExistingCell() {
        String cell2Name = "cell2";
        CellEntity anotherCell = getCell("cell1", "2", CellEntity.CellType.DIGIT);
        CellEntity cell = getCell("=%s+%s".formatted(anotherCell.getName(), cell2Name), CellEntity.CellType.FORMULA);

        when(cellRepository.findBySheetNameIgnoreCaseAndNameIgnoreCase(anotherCell.getSheetName(), anotherCell.getName()))
                .thenReturn(Optional.of(anotherCell));
        when(cellRepository.findBySheetNameIgnoreCaseAndNameIgnoreCase(anotherCell.getSheetName(), cell2Name))
                .thenReturn(Optional.empty());

        var calculator = new ResultCalculator(cell, cellRepository);
        Exception exception = assertThrows(CalculationException.class, calculator::calculateFormula);
        assertThat(exception.getMessage()).isEqualTo("Failed to fill formula");
    }

    private CellEntity getCell(String value, CellEntity.CellType type) {
        return getCell("testCell", value, type);
    }

    private CellEntity getCell(String name, String value, CellEntity.CellType type) {
        return CellEntity.builder()
                .sheetName("testSheet")
                .name(name)
                .value(value)
                .type(type)
                .build();
    }
}
