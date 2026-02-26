package com.logistic.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpServletRequest; // Tambahkan ini jika perlu cek manual

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class StockController {

    @Autowired
    private StockService stockService;

    @GetMapping("/datastock")
    public String showStock(Model model) {
        try {
            // Ambil data (otomatis sync via service)
            List<ArtikelModel> allData = stockService.getStockOnDemand();

            // Grouping berdasarkan Nama & Sorting berdasarkan Size
            Map<String, List<ArtikelModel>> grouped = allData.stream()
                    .collect(Collectors.groupingBy(
                            item -> item.nama,
                            Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    list -> {
                                        // Sorting size secara cerdas (Alfabetis/Numerik)
                                        list.sort(Comparator.comparing(v -> v.size));
                                        return list;
                                    }
                            )
                    ));

            model.addAttribute("groupedStock", grouped);

            // Kirim waktu terakhir sync untuk tampilan di header
            String lastUpdate = new SimpleDateFormat("HH:mm:ss").format(new Date(stockService.getLastSyncTime()));
            model.addAttribute("lastSync", lastUpdate);

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Koneksi ke database logistik terputus: " + e.getMessage());
        }

        // Return ke file datastock.html yang sudah memakai layout:decorate="~{layout}"
        return "datastock";
    }
}