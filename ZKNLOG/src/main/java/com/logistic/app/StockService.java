package com.logistic.app;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StockService {

    private List<ArtikelModel> cachedStock = new ArrayList<>();
    private long lastSyncTime = 0;
    private final long SYNC_INTERVAL = 5 * 60 * 1000; // 5 Menit

    // Fungsi Utama yang dipanggil Controller
    public List<ArtikelModel> getStockOnDemand() throws Exception {
        long currentTime = System.currentTimeMillis();

        // Sync hanya jika cache kosong ATAU sudah lewat 5 menit
        if (cachedStock.isEmpty() || (currentTime - lastSyncTime > SYNC_INTERVAL)) {
            this.cachedStock = fetchStockData();
            this.lastSyncTime = currentTime;
        }
        return cachedStock;
    }

    public long getLastSyncTime() {
        return lastSyncTime;
    }

    // Logika Inti Scrapping & Excel
    private List<ArtikelModel> fetchStockData() throws Exception {
        String urlLoginPage = "https://jezpro.id/";
        String urlActionLogin = "https://jezpro.id/user_login";
        String urlDownload = "https://jezpro.id/export_mass_adjustment_template?st_id=3&psc_id=all&br_id=all&pl_id=&qty_filter=1";

        // Login
        Connection.Response loginPageRes = Jsoup.connect(urlLoginPage).method(Connection.Method.GET).execute();
        String csrfToken = loginPageRes.parse().select("input[name=_token]").first().attr("value");

        Connection.Response loginRes = Jsoup.connect(urlActionLogin)
                .cookies(loginPageRes.cookies())
                .data("_token", csrfToken, "u_email", "andysilvano2406@jez.co.id", "password", "999")
                .method(Connection.Method.POST)
                .execute();

        // Download
        Connection.Response downloadRes = Jsoup.connect(urlDownload)
                .cookies(loginRes.cookies())
                .timeout(60000)
                .ignoreContentType(true)
                .execute();

        List<ArtikelModel> list = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(downloadRes.bodyStream());
        Sheet sheet = workbook.getSheetAt(0);
        DataFormatter formatter = new DataFormatter();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            list.add(new ArtikelModel(
                    formatter.formatCellValue(row.getCell(1)), // bin
                    formatter.formatCellValue(row.getCell(2)), // sku
                    formatter.formatCellValue(row.getCell(5)), // size
                    formatter.formatCellValue(row.getCell(3)), // brand
                    formatter.formatCellValue(row.getCell(4)), // nama
                    formatter.formatCellValue(row.getCell(7)), // hb
                    formatter.formatCellValue(row.getCell(8)), // hj
                    formatter.formatCellValue(row.getCell(6)), // cat
                    formatter.formatCellValue(row.getCell(9))  // qty
            ));
        }
        workbook.close();
        return list;
    }
}