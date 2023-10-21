package it.devchallenge.excel.service;


import it.devchallenge.excel.exceptions.CalculationException;
import it.devchallenge.excel.exceptions.NotFoundException;
import it.devchallenge.excel.model.CellEntity;
import it.devchallenge.excel.repository.CellRepository;
import org.junit.jupiter.api.Test;
import org.mariuszgromada.math.mxparser.License;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class SheetServiceTest {
    private static final String SHEET_NAME = "testSheet";
    private static final String CELL_NAME = "testCell";
    private static final String STRING_VALUE = "stringValue";
    private static final String DIGIT_VALUE = "12";
    private static final String CALCULATION_VALUE = "=1+2";
    private final CellRepository cellRepository = mock(CellRepository.class);
    private final SheetService sheetService;

    public SheetServiceTest() {
        License.iConfirmNonCommercialUse("testUsage");
        sheetService = new SheetService(cellRepository);
    }

    /**
     * Add cell tests
     */
    @Test
    public void addCellWithStringType() throws CalculationException {
        when(cellRepository.findBySheetNameIgnoreCaseAndNameIgnoreCase(anyString(), anyString())).thenReturn(Optional.empty());
        ArgumentCaptor<CellEntity> argumentCaptor = ArgumentCaptor.forClass(CellEntity.class);
        var result = sheetService.addCell(SHEET_NAME, CELL_NAME, STRING_VALUE);
        verify(cellRepository).save(argumentCaptor.capture());

        CellEntity cell = argumentCaptor.getValue();

        assertThat(cell.getSheetName()).isEqualTo(SHEET_NAME);
        assertThat(cell.getName()).isEqualTo(CELL_NAME);
        assertThat(cell.getValue()).isEqualTo(STRING_VALUE);
        assertThat(cell.getType()).isEqualTo(CellEntity.CellType.STRING);

        assertThat(result.getValue()).isEqualTo(STRING_VALUE);
        assertThat(result.getResult()).isEqualTo(STRING_VALUE);
    }

    @Test
    public void addCellWithDigitType() throws CalculationException {
        when(cellRepository.findBySheetNameIgnoreCaseAndNameIgnoreCase(anyString(), anyString())).thenReturn(Optional.empty());
        ArgumentCaptor<CellEntity> argumentCaptor = ArgumentCaptor.forClass(CellEntity.class);
        var result = sheetService.addCell(SHEET_NAME, CELL_NAME, DIGIT_VALUE);
        verify(cellRepository).save(argumentCaptor.capture());

        CellEntity cell = argumentCaptor.getValue();

        assertThat(cell.getSheetName()).isEqualTo(SHEET_NAME);
        assertThat(cell.getName()).isEqualTo(CELL_NAME);
        assertThat(cell.getValue()).isEqualTo(DIGIT_VALUE);
        assertThat(cell.getType()).isEqualTo(CellEntity.CellType.DIGIT);

        assertThat(result.getValue()).isEqualTo(DIGIT_VALUE);
        assertThat(result.getResult()).isEqualTo(DIGIT_VALUE);
    }

    @Test
    public void addCellWithCalculationType() throws CalculationException {
        when(cellRepository.findBySheetNameIgnoreCaseAndNameIgnoreCase(anyString(), anyString())).thenReturn(Optional.empty());
        ArgumentCaptor<CellEntity> argumentCaptor = ArgumentCaptor.forClass(CellEntity.class);
        var result = sheetService.addCell(SHEET_NAME, CELL_NAME, CALCULATION_VALUE);
        verify(cellRepository).save(argumentCaptor.capture());

        CellEntity cell = argumentCaptor.getValue();

        assertThat(cell.getSheetName()).isEqualTo(SHEET_NAME);
        assertThat(cell.getName()).isEqualTo(CELL_NAME);
        assertThat(cell.getValue()).isEqualTo(CALCULATION_VALUE);
        assertThat(cell.getType()).isEqualTo(CellEntity.CellType.FORMULA);

        assertThat(result.getValue()).isEqualTo(CALCULATION_VALUE);
        assertThat(result.getResult()).isEqualTo("3");
    }

    @Test
    public void addCellWithCalculationError() {
        String cellValue = "=1+cell2";

        when(cellRepository.findBySheetNameIgnoreCaseAndNameIgnoreCase(anyString(), anyString())).thenReturn(Optional.empty());
        Exception exception = assertThrows(CalculationException.class, () -> sheetService.addCell(SHEET_NAME, CELL_NAME, cellValue));
        assertThat(exception.getMessage()).isEqualTo("Failed to fill formula");
        verify(cellRepository, times(0)).save(any());
    }

    @Test
    public void addCellWithRecursionCalculationError() {
        String cellValue = "=" + CELL_NAME;

        when(cellRepository.findBySheetNameIgnoreCaseAndNameIgnoreCase(anyString(), anyString())).thenReturn(Optional.empty());
        Exception exception = assertThrows(CalculationException.class, () -> sheetService.addCell(SHEET_NAME, CELL_NAME, cellValue));
        assertThat(exception.getMessage()).isEqualTo("Recursive formula");

        verify(cellRepository, times(0)).save(any());
    }

    @Test
    public void editCellValueShouldRecalculateResult() throws CalculationException {

        when(cellRepository.findBySheetNameIgnoreCaseAndNameIgnoreCase(anyString(), anyString())).thenReturn(Optional.empty());
        var result = sheetService.addCell(SHEET_NAME, CELL_NAME, DIGIT_VALUE);

        assertThat(result.getValue()).isEqualTo(DIGIT_VALUE);
        assertThat(result.getResult()).isEqualTo(DIGIT_VALUE);
        result = sheetService.addCell(SHEET_NAME, CELL_NAME, STRING_VALUE);

        assertThat(result.getValue()).isEqualTo(STRING_VALUE);
        assertThat(result.getResult()).isEqualTo(STRING_VALUE);
    }

    @Test
    public void getResultForStringCell() throws CalculationException, NotFoundException {
        CellEntity cell = getCell("stringValue", CellEntity.CellType.STRING);

        String result = sheetService.getResult(cell);
        assertThat(result).isEqualTo(cell.getValue());
    }

    @Test
    public void getResultForDigitCell() throws CalculationException, NotFoundException {
        CellEntity cell = getCell("1", CellEntity.CellType.STRING);

        String result = sheetService.getResult(cell);
        assertThat(result).isEqualTo(cell.getValue());
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
