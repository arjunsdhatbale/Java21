package com.main.service;

import com.main.model.dto.ProductExcelRowRecord;
import com.main.model.dto.ProductRequestDto;
import com.main.shared.exception.InvalidFileException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class ProductExcelStreamProcessor {

    private static final int BATCH_SIZE = 1000;

    public static void process(File stagedFile, Consumer<List<ProductExcelRowRecord>> batchConsumer) {
        if (stagedFile == null || !stagedFile.exists()) {
            throw new InvalidFileException("Staged file not found");
        }

        DataFormatter dataFormatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(stagedFile)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new InvalidFileException("Excel workbook has no sheets");
            }

            int firstRowNum = sheet.getFirstRowNum();
            int lastRowNum = sheet.getLastRowNum();

            if (firstRowNum < 0 || lastRowNum < firstRowNum) {
                throw new InvalidFileException("Excel sheet is empty");
            }

            Row headerRow = sheet.getRow(firstRowNum);
            if (headerRow == null) {
                throw new InvalidFileException("Header row is missing");
            }

            Map<String, Integer> colMap = resolveColumns(headerRow, dataFormatter);

            List<ProductExcelRowRecord> currentBatch = new ArrayList<>(BATCH_SIZE);

            for (int r = firstRowNum + 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }

                String name = getCellValue(row, colMap.get("name"), dataFormatter);
                String description = getCellValue(row, colMap.get("description"), dataFormatter);
                String priceStr = getCellValue(row, colMap.get("price"), dataFormatter);
                String stockStr = getCellValue(row, colMap.get("stock"), dataFormatter);
                String category = getCellValue(row, colMap.get("category"), dataFormatter);
                String imageUrl = getCellValue(row, colMap.get("imageUrl"), dataFormatter);
                String status = getCellValue(row, colMap.get("status"), dataFormatter);

                // If all cells in this row are blank, ignore it
                if (isBlank(name) && isBlank(description) && isBlank(priceStr) &&
                        isBlank(stockStr) && isBlank(category) && isBlank(imageUrl) && isBlank(status)) {
                    continue;
                }

                BigDecimal price = parseBigDecimal(priceStr);
                Integer stock = parseInteger(stockStr);

                ProductRequestDto dto = ProductRequestDto.builder()
                        .name(name)
                        .description(description)
                        .price(price)
                        .stock(stock)
                        .category(category)
                        .imageUrl(imageUrl)
                        .status(isBlank(status) ? "ACTIVE" : status.trim().toUpperCase())
                        .build();

                int excelRowNumber = r + 1; // 1-based row index for user readability
                currentBatch.add(ProductExcelRowRecord.builder()
                        .rowNumber(excelRowNumber)
                        .dto(dto)
                        .build());

                if (currentBatch.size() >= BATCH_SIZE) {
                    batchConsumer.accept(new ArrayList<>(currentBatch));
                    currentBatch.clear();
                }
            }

            if (!currentBatch.isEmpty()) {
                batchConsumer.accept(new ArrayList<>(currentBatch));
                currentBatch.clear();
            }

        } catch (InvalidFileException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to parse Excel file", ex);
            throw new InvalidFileException("Failed to read Excel file: " + ex.getMessage());
        }
    }

    private static Map<String, Integer> resolveColumns(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> colMap = new HashMap<>();
        // Default 0-indexed fallback column order
        colMap.put("name", 0);
        colMap.put("description", 1);
        colMap.put("price", 2);
        colMap.put("stock", 3);
        colMap.put("category", 4);
        colMap.put("imageUrl", 5);
        colMap.put("status", 6);

        short minCol = headerRow.getFirstCellNum();
        short maxCol = headerRow.getLastCellNum();

        if (minCol >= 0 && maxCol >= minCol) {
            for (int c = minCol; c < maxCol; c++) {
                Cell cell = headerRow.getCell(c);
                if (cell == null) continue;
                String header = formatter.formatCellValue(cell).trim().toLowerCase().replaceAll("[^a-z0-9]", "");
                if (header.contains("productname") || header.equals("name") || header.equals("title")) {
                    colMap.put("name", c);
                } else if (header.contains("description") || header.equals("desc")) {
                    colMap.put("description", c);
                } else if (header.contains("price") || header.contains("cost") || header.contains("amount")) {
                    colMap.put("price", c);
                } else if (header.contains("stock") || header.contains("quantity") || header.equals("qty")) {
                    colMap.put("stock", c);
                } else if (header.contains("category") || header.contains("cat")) {
                    colMap.put("category", c);
                } else if (header.contains("image") || header.contains("img") || header.contains("picture")) {
                    colMap.put("imageUrl", c);
                } else if (header.contains("status")) {
                    colMap.put("status", c);
                }
            }
        }

        return colMap;
    }

    private static String getCellValue(Row row, Integer colIdx, DataFormatter formatter) {
        if (colIdx == null || colIdx < 0) {
            return null;
        }
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            return null;
        }
        String text = formatter.formatCellValue(cell);
        return text != null ? text.trim() : null;
    }

    private static BigDecimal parseBigDecimal(String str) {
        if (isBlank(str)) return null;
        try {
            // Remove currency symbols, commas and spaces
            String cleaned = str.replaceAll("[^0-9.-]", "");
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer parseInteger(String str) {
        if (isBlank(str)) return null;
        try {
            // If parsed as a double like 10.0 from Excel numeric formatting
            String cleaned = str.replaceAll("[^0-9.-]", "");
            if (cleaned.contains(".")) {
                return (int) Double.parseDouble(cleaned);
            }
            return Integer.parseInt(cleaned);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
