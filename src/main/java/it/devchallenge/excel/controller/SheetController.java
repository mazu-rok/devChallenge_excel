package it.devchallenge.excel.controller;

import it.devchallenge.excel.dto.AddCellRequest;
import it.devchallenge.excel.dto.CellResponse;
import it.devchallenge.excel.exceptions.CalculationException;
import it.devchallenge.excel.exceptions.NotFoundException;
import it.devchallenge.excel.service.SheetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(path = "/api/v1/{sheetName}")
public class SheetController {
    private final SheetService sheetService;

    @Autowired
    public SheetController(SheetService sheetService) {
        this.sheetService = sheetService;
    }

    @PostMapping("/{cellName}")
    public ResponseEntity<?> addCell(@PathVariable String sheetName,
                                     @PathVariable String cellName,
                                     @RequestBody AddCellRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(sheetService.addCell(sheetName, cellName, request.getValue()));
        } catch (CalculationException e) {
            log.error("Add cell calculation error", e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(CellResponse.builder()
                            .value(request.getValue())
                            .result("ERROR")
                            .build());
        }
    }

    @GetMapping
    public ResponseEntity<?> getSheet(@PathVariable String sheetName) {
        try {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(sheetService.getSheet(sheetName));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{cellName}")
    public ResponseEntity<?> getCell(@PathVariable String sheetName,
                                     @PathVariable String cellName) {
        try {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(sheetService.getCellResponse(sheetName, cellName));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
