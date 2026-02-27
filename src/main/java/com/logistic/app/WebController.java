package com.logistic.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.util.Collections;
import java.util.List;

@Controller
public class WebController {

    @Autowired
    private GoogleSheetsService sheetService;

    @Autowired
    private StockMin stockMin;

    /**
     * Dashboard Resi Screen
     * Mengambil data dari Google Sheets berdasarkan tab kurir.
     */
    @GetMapping("/resiscreen")
    public String dashboard(@RequestParam(name = "tab", defaultValue = "SHOPEE EXPRESS") String tab, Model model) {
        try {
            List<Resi> data = sheetService.getResiFromSheet(tab);
            model.addAttribute("dataResi", data);
            model.addAttribute("activeTab", tab);
            model.addAttribute("currentMenu", "resi");
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Gagal mengambil data dari Google Sheets: " + e.getMessage());
        }
        return "resiscreen";
    }

    /**
     * Halaman Informasi Mobile App
     */
    @GetMapping("/mobile-app")
    public String mobileAppPage(Model model) {
        model.addAttribute("currentMenu", "mobile");
        return "mobile-app";
    }

    /**
     * HALAMAN UTAMA IMPORT STOCK
     * Menampilkan form upload file Excel.
     */
    @GetMapping("/import-stock")
    public String pageImport(Model model) {
        model.addAttribute("currentMenu", "stock");
        return "import_form";
    }

    /**
     * PROSES STEP 1: FILTER STOCK MINUS
     * Memfilter baris yang memiliki QTY SYSTEM < 0.
     */
    @PostMapping("/import-stock/process")
    public String processImport(@RequestParam("file") MultipartFile file, Model model) {
        try {
            model.addAttribute("currentMenu", "stock");

            // Memanggil logika filter di Service StockMin
            List<List<String>> result = stockMin.filterNegativeStock(file);

            if (result == null || result.isEmpty()) {
                model.addAttribute("error", "File kosong atau format 'QTY SYSTEM' tidak ditemukan.");
            } else {
                // Baris 0 adalah Header, sisanya adalah Data
                model.addAttribute("headers", result.get(0));
                model.addAttribute("rows", result.size() > 1 ? result.subList(1, result.size()) : Collections.emptyList());
            }
        } catch (Exception e) {
            model.addAttribute("error", "Kesalahan Step 1: " + e.getMessage());
        }
        // Mengembalikan ke view yang sama (Hasil akan muncul di Modal via AJAX/Logic HTML)
        return "import_form";
    }

    /**
     * PROSES STEP 2: ALOKASI BIN (LOGIKA MACRO)
     * Mencari stok positif di BIN lain untuk menutup stok minus.
     */
    @PostMapping("/import-stock/step2")
    public String processStep2(@RequestParam("file") MultipartFile file, Model model) {
        try {
            model.addAttribute("currentMenu", "stock");

            // Memanggil logika alokasi berdasarkan prioritas BIN
            List<List<String>> result = stockMin.processStep2(file);

            if (result == null || result.isEmpty()) {
                model.addAttribute("error", "Gagal memproses alokasi stok. Cek kembali format SKU/BIN.");
            } else {
                model.addAttribute("headers", result.get(0));
                model.addAttribute("rows", result.size() > 1 ? result.subList(1, result.size()) : Collections.emptyList());
            }
        } catch (Exception e) {
            model.addAttribute("error", "Kesalahan Step 2: " + e.getMessage());
        }
        // Mengembalikan fragment tabel untuk di-render ulang di modal
        return "import_form";
    }
}