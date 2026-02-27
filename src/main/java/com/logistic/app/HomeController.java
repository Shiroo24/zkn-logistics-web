package com.logistic.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private StockService stockService;

    @GetMapping("/")
    public String home(Model model) {
        // Step 1: Inisialisasi variabel dengan nilai default agar TIDAK ERROR 500
        int totalQty = 0;
        int totalModels = 0;
        String systemStatus = "OFFLINE";

        try {
            // Step 2: Ambil data dari Service
            List<ArtikelModel> allData = stockService.getStockOnDemand();

            if (allData != null && !allData.isEmpty()) {
                // Step 3: Hitung total Qty (Konversi String ke Integer secara aman)
                totalQty = allData.stream()
                        .mapToInt(item -> {
                            try {
                                if (item.qty == null || item.qty.trim().isEmpty()) return 0;
                                return Integer.parseInt(item.qty.trim());
                            } catch (Exception e) {
                                return 0; // Abaikan jika bukan angka
                            }
                        })
                        .sum();

                // Step 4: Hitung model unik berdasarkan nama
                totalModels = (int) allData.stream()
                        .filter(item -> item.nama != null)
                        .map(item -> item.nama)
                        .distinct()
                        .count();

                systemStatus = "OPERATIONAL";
            }
        } catch (Exception e) {
            // Cetak error di console IntelliJ untuk mempermudah debug
            System.err.println("Gagal memuat data dashboard: " + e.getMessage());
            e.printStackTrace();
        }

        // Step 5: Kirim data ke HTML
        model.addAttribute("totalQty", totalQty);
        model.addAttribute("totalModels", totalModels);
        model.addAttribute("systemStatus", systemStatus);

        return "home";
    }
}