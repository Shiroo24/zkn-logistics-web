package com.logistic.app;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Service
public class StockMin {

    // Daftar Prioritas BIN (Huruf Besar)
    private final List<String> PRIOR_BINS = Arrays.asList(
            "RAK ACC LT.1", "STAGGING INBOUND", "STAGGING OUTBOUND", "KARANTINA DC",
            "KARANTINA STORE 02", "STAGGING REFUND", "STAGING GAGAL QC", "STAGGING LT.3",
            "STAGGING OUTBOUND SEMARANG", "STAGGING OUTBOUND SIDOARJO", "STAGGING LT.2", "LT.4"
    );

    /**
     * STEP 1: Filter data QTY SYSTEM < 0
     * Perbaikan: Menangani berbagai tipe data Cell (Numeric & String)
     */
    public List<List<String>> filterNegativeStock(MultipartFile file) throws Exception {
        List<List<String>> filteredData = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) return filteredData;

            // Cari Index Kolom (Case Insensitive & Trim)
            int qtyColIndex = findColumnIndex(headerRow, "QTY SYSTEM");
            if (qtyColIndex == -1) throw new Exception("Kolom 'QTY SYSTEM' tidak ditemukan!");

            // Masukkan Header (A-K)
            filteredData.add(getRowData(headerRow, 10));

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                double qtyVal = getNumericValue(row.getCell(qtyColIndex));

                // Filter hanya yang minus
                if (qtyVal < 0) {
                    filteredData.add(getRowData(row, 10));
                }
            }
        }
        return filteredData;
    }

    /**
     * STEP 2: Logika Alokasi
     * Perbaikan: Sinkronisasi pengambilan nilai angka agar tidak 0 terus
     */
    public List<List<String>> processStep2(MultipartFile file) throws Exception {
        List<List<String>> finalResult = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            int colSKU = findColumnIndex(headerRow, "SKU");
            int colBIN = findColumnIndex(headerRow, "BIN");
            int colQty = findColumnIndex(headerRow, "QTY SYSTEM");

            Map<String, Map<String, Double>> dict = new HashMap<>();
            List<List<String>> minusRows = new ArrayList<>();

            // 1. Load Data
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String sku = getCellValue(row.getCell(colSKU)).toUpperCase().trim();
                String bin = getCellValue(row.getCell(colBIN)).toUpperCase().trim();
                double qty = getNumericValue(row.getCell(colQty));

                if (!sku.isEmpty() && qty > 0) {
                    dict.computeIfAbsent(sku, k -> new LinkedHashMap<>()).put(bin, qty);
                } else if (!sku.isEmpty() && qty < 0) {
                    minusRows.add(getRowData(row, 10));
                }
            }

            // 2. Header Baru
            List<String> headers = getRowData(headerRow, 10);
            headers.add("BIN PENYELESAIAN");
            headers.add("QTY BIN");
            headers.add("STATUS");
            finalResult.add(headers);

            // 3. Alokasi
            for (List<String> rowData : minusRows) {
                String sku = rowData.get(colSKU).toUpperCase().trim();
                double sisaDibutuhkan = Math.abs(getNumericValueFromString(rowData.get(colQty)));

                if (!dict.containsKey(sku)) {
                    finalResult.add(createFinalRow(rowData, "TIDAK ADA", "0", "NEED ADJUSMENT"));
                    continue;
                }

                Map<String, Double> skuDict = dict.get(sku);

                while (sisaDibutuhkan > 0.0001) {
                    String binSolusi = "";
                    double qtyTersedia = 0;

                    for (String pBin : PRIOR_BINS) {
                        if (skuDict.getOrDefault(pBin, 0.0) > 0) {
                            binSolusi = pBin;
                            qtyTersedia = skuDict.get(pBin);
                            break;
                        }
                    }

                    if (binSolusi.isEmpty()) {
                        for (Map.Entry<String, Double> entry : skuDict.entrySet()) {
                            if (!entry.getKey().contains("REJECT") && entry.getValue() > 0) {
                                binSolusi = entry.getKey();
                                qtyTersedia = entry.getValue();
                                break;
                            }
                        }
                    }

                    if (binSolusi.isEmpty()) {
                        finalResult.add(createFinalRow(rowData, "STOK HABIS", "0", "NEED ADJUSMENT"));
                        break;
                    } else {
                        double ambil = Math.min(sisaDibutuhkan, qtyTersedia);
                        finalResult.add(createFinalRow(rowData, binSolusi, String.valueOf(ambil), "DONE SET UP"));

                        skuDict.put(binSolusi, qtyTersedia - ambil);
                        sisaDibutuhkan -= ambil;
                    }
                }
            }
        }
        return finalResult;
    }

    // --- HELPER METHODS (DIBETULKAN) ---

    private int findColumnIndex(Row header, String name) {
        for (int i = 0; i < header.getLastCellNum(); i++) {
            Cell cell = header.getCell(i);
            if (cell != null && cell.toString().trim().equalsIgnoreCase(name)) return i;
        }
        return -1;
    }

    // Mengambil angka dari Cell, apapun tipenya (Numeric atau String)
    private double getNumericValue(Cell cell) {
        if (cell == null) return 0;
        try {
            if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
            if (cell.getCellType() == CellType.STRING) return Double.parseDouble(cell.getStringCellValue().trim());
        } catch (Exception e) {
            return 0;
        }
        return 0;
    }

    private double getNumericValueFromString(String val) {
        try { return Double.parseDouble(val); } catch (Exception e) { return 0; }
    }

    private List<String> getRowData(Row row, int maxCol) {
        List<String> data = new ArrayList<>();
        for (int j = 0; j <= maxCol; j++) {
            data.add(getCellValue(row.getCell(j)));
        }
        return data;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            // Hilangkan .0 jika itu bilangan bulat
            double val = cell.getNumericCellValue();
            if (val == (long) val) return String.format("%d", (long) val);
            return String.valueOf(val);
        }
        return cell.toString().trim();
    }

    private List<String> createFinalRow(List<String> base, String bin, String qty, String status) {
        List<String> newRow = new ArrayList<>(base);
        newRow.add(bin);
        newRow.add(qty);
        newRow.add(status);
        return newRow;
    }
}