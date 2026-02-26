package com.logistic.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.apache.poi.ss.usermodel.*;
import java.util.*;

@Controller
public class StockInController {

    @Autowired
    private StockService stockService;

    @GetMapping("/stock-in")
    public String stockInPage(Model model) {
        model.addAttribute("activeTab", "STOCK IN");
        return "pembagian-stock";
    }

    @PostMapping("/api/stock-in/proses")
    @ResponseBody
    public ResponseEntity<?> prosesStockIn(@RequestParam("files") MultipartFile[] files) {
        try {
            // 1. AMBIL DATA DARI SCRAPPING (Pengganti DatabaseArtikelPrefs di Android)
            List<ArtikelModel> artikelList = stockService.getStockOnDemand();

            Map<String, Integer> dictStockStore = new HashMap<>();
            Map<String, Integer> dictL3 = new HashMap<>();
            Map<String, Boolean> dictTokoExist = new HashMap<>();

            for (ArtikelModel artikel : artikelList) {
                String sku = normalizeSku(artikel.sku);
                int qty = 0;
                try {
                    qty = Integer.parseInt(artikel.qty.trim());
                } catch (Exception e) { qty = 0; }

                String bin = (artikel.bin != null) ? artikel.bin.toUpperCase() : "";

                if (sku.isEmpty() || qty <= 0) continue;

                // FILTER BIN (Persis Logika Android)
                if (bin.equals("TOKO") || bin.equals("GUDANG LT.2") || bin.startsWith("GL2-STORE")) {
                    dictStockStore.put(sku, dictStockStore.getOrDefault(sku, 0) + qty);
                    if (bin.equals("TOKO")) {
                        dictTokoExist.put(artikel.nama, true);
                    }
                }

                if (bin.startsWith("GL3-DC")) {
                    dictL3.put(sku, dictL3.getOrDefault(sku, 0) + qty);
                }
            }

            // 2. PROSES EXCEL PO
            Map<String, Map<String, Object>> poDataMap = new LinkedHashMap<>();
            DataFormatter formatter = new DataFormatter();

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                Workbook wb = WorkbookFactory.create(file.getInputStream());
                Sheet sheet = wb.getSheetAt(0);

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    String nama = getCellValue(row.getCell(0));
                    String kat = getCellValue(row.getCell(2)).toUpperCase().trim();
                    String size = getCellValue(row.getCell(3));
                    String sku = normalizeSku(formatter.formatCellValue(row.getCell(5)));

                    int qtyIn = 0;
                    Cell qtyCell = row.getCell(6);
                    if (qtyCell != null) {
                        if (qtyCell.getCellType() == CellType.NUMERIC) {
                            qtyIn = (int) qtyCell.getNumericCellValue();
                        } else {
                            try { qtyIn = Integer.parseInt(getCellValue(qtyCell)); } catch (Exception e) {}
                        }
                    }

                    if (!Arrays.asList("SHOES", "SANDALS").contains(kat) || qtyIn <= 0) continue;

                    String key = sku + "|" + size;
                    if (!poDataMap.containsKey(key)) {
                        Map<String, Object> val = new HashMap<>();
                        val.put("nama", nama);
                        val.put("kat", kat);
                        val.put("qtyIn", qtyIn);
                        poDataMap.put(key, val);
                    } else {
                        int currentPoQty = (int) poDataMap.get(key).get("qtyIn");
                        poDataMap.get(key).put("qtyIn", currentPoQty + qtyIn);
                    }
                }
                wb.close();
            }

            // 3. ALOKASI FINAL (MIRROR LOGIKA KOTLIN)
            List<StockItemDTO> hasilToko = new ArrayList<>();
            List<StockItemDTO> hasilL4 = new ArrayList<>();

            // Map pelacak agar baris selanjutnya jadi REPLENISH
            Map<String, Boolean> currentProcessTokoExist = new HashMap<>(dictTokoExist);

            for (String key : poDataMap.keySet()) {
                String[] parts = key.split("\\|");
                String sku = parts[0];
                String size = parts[1];

                Map<String, Object> data = poDataMap.get(key);
                String nama = (String) data.get("nama");
                String kat = (String) data.get("kat");
                int qtyIn = (int) data.get("qtyIn");

                int stockStoreQty = dictStockStore.getOrDefault(sku, 0);

                // Rumus Target Display
                int targetDisplay = (qtyIn > 10) ? 3 : (qtyIn > 3) ? 2 : 1;
                if ((qtyIn == 2 || qtyIn == 3) && stockStoreQty == 1) {
                    targetDisplay = 2;
                }

                // Penentuan NEW DISPLAY vs REPLENISH
                boolean isNewDisplay = !currentProcessTokoExist.getOrDefault(nama, false);
                if (isNewDisplay) {
                    currentProcessTokoExist.put(nama, true); // Baris berikutnya dengan nama sama jadi false
                }

                int qtyStoreAlloc = 0;
                if (stockStoreQty < targetDisplay) {
                    qtyStoreAlloc = Math.min(qtyIn, targetDisplay - stockStoreQty);
                    hasilToko.add(new StockItemDTO(
                            "T-" + sku + "-" + size, nama, kat, sku, size,
                            qtyIn, stockStoreQty, qtyStoreAlloc,
                            isNewDisplay ? "NEW DISPLAY" : "REPLENISH",
                            true
                    ));
                } else {
                    hasilToko.add(new StockItemDTO(
                            "T-" + sku + "-" + size, nama, kat, sku, size,
                            qtyIn, stockStoreQty, 0,
                            "FULL (STOK ADA)", true
                    ));
                }

                // Logika Lantai 4 & Koli
                int sisa = qtyIn - qtyStoreAlloc;
                if (sisa > 0) {
                    int koli = (sku.startsWith("SPS") || sku.startsWith("N")) ? 6 : 12;
                    int qtyL3 = dictL3.getOrDefault(sku, 0);
                    int qtyL4 = (sisa / koli) * koli;
                    int sisaL3 = (qtyL3 > 0) ? (sisa % koli) : (6 + ((sisa - 6) % koli));

                    if (qtyL4 > 0) {
                        hasilL4.add(new StockItemDTO(
                                "L-" + sku + "-" + size, nama, kat, sku, size,
                                qtyIn, stockStoreQty, qtyL4,
                                "Koli " + koli + " (Sisa " + sisaL3 + " ke LT.3)", false
                        ));
                    }
                }
            }

            hasilToko.sort(Comparator.comparing(StockItemDTO::getSku));
            hasilL4.sort(Comparator.comparing(StockItemDTO::getSku));

            return ResponseEntity.ok(Map.of("hasilToko", hasilToko, "hasilL4", hasilL4));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    private String normalizeSku(String raw) {
        if (raw == null) return "";
        return raw.trim().toUpperCase().replace("]C1", "").replace("C1", "").replaceAll("[^A-Z0-9/]", "");
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return new DataFormatter().formatCellValue(cell).trim();
    }
}