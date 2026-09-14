package com.main.service;

import com.main.model.dto.ExcelRowRecord;
import com.main.model.dto.UserRequestDto;
import com.main.shared.exception.InvalidFileException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class ExcelStreamProcessor {

    private static final int BATCH_SIZE = 1000;

    public static void process(File stagedFile, Consumer<List<ExcelRowRecord>> batchConsumer) {
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

            List<ExcelRowRecord> currentBatch = new ArrayList<>(BATCH_SIZE);

            for (int r = firstRowNum + 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }

                String firstName = getCellValue(row, colMap.get("firstName"), dataFormatter);
                String lastName = getCellValue(row, colMap.get("lastName"), dataFormatter);
                String email = getCellValue(row, colMap.get("email"), dataFormatter);
                String password = getCellValue(row, colMap.get("password"), dataFormatter);
                String phone = getCellValue(row, colMap.get("phone"), dataFormatter);
                String role = getCellValue(row, colMap.get("role"), dataFormatter);

                // If all cells in this row are blank, ignore it
                if (isBlank(firstName) && isBlank(lastName) && isBlank(email) &&
                        isBlank(password) && isBlank(phone) && isBlank(role)) {
                    continue;
                }

                UserRequestDto dto = UserRequestDto.builder()
                        .firstName(firstName)
                        .lastName(lastName)
                        .email(email)
                        .password(password)
                        .phone(phone)
                        .role(role)
                        .build();

                int excelRowNumber = r + 1; // 1-based row index for user readability
                currentBatch.add(ExcelRowRecord.builder()
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
        // Set default 0-indexed column order fallback
        colMap.put("firstName", 0);
        colMap.put("lastName", 1);
        colMap.put("email", 2);
        colMap.put("password", 3);
        colMap.put("phone", 4);
        colMap.put("role", 5);

        boolean matchedAny = false;
        short minCol = headerRow.getFirstCellNum();
        short maxCol = headerRow.getLastCellNum();

        if (minCol >= 0 && maxCol >= minCol) {
            for (int c = minCol; c < maxCol; c++) {
                Cell cell = headerRow.getCell(c);
                if (cell == null) continue;
                String header = formatter.formatCellValue(cell).trim().toLowerCase().replaceAll("[^a-z0-9]", "");
                if (header.contains("firstname") || header.equals("first")) {
                    colMap.put("firstName", c);
                    matchedAny = true;
                } else if (header.contains("lastname") || header.equals("last")) {
                    colMap.put("lastName", c);
                    matchedAny = true;
                } else if (header.contains("email")) {
                    colMap.put("email", c);
                    matchedAny = true;
                } else if (header.contains("password")) {
                    colMap.put("password", c);
                    matchedAny = true;
                } else if (header.contains("phone") || header.contains("mobile")) {
                    colMap.put("phone", c);
                    matchedAny = true;
                } else if (header.contains("role")) {
                    colMap.put("role", c);
                    matchedAny = true;
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

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
