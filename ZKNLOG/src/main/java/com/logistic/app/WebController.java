package com.logistic.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@Controller
public class WebController {

    @Autowired
    private GoogleSheetsService sheetService;

    @GetMapping("/resiscreen")
    public String dashboard(@RequestParam(name = "tab", defaultValue = "SHOPEE EXPRESS") String tab, Model model) {
        try {
            // Ambil data berdasarkan Tab yang dipilih
            List<Resi> data = sheetService.getResiFromSheet(tab);

            model.addAttribute("dataResi", data);
            model.addAttribute("activeTab", tab);

            // Opsional: Untuk memastikan sidebar tahu kita di halaman resi
            model.addAttribute("currentMenu", "resi");

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Gagal mengambil data dari Google Sheets: " + e.getMessage());
        }

        // Return ke resiscreen.html yang sudah dibungkus layout:decorate="~{layout}"
        return "resiscreen";
    }
    @GetMapping("/mobile-app")
    public String mobileAppPage(Model model) {
        model.addAttribute("currentMenu", "mobile"); // Supaya sidebar tahu menu ini aktif
        return "mobile-app";
    }
}
