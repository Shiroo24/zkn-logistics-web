package com.logistic.app;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleSheetsService {

    private final String SPREADSHEET_ID = "14Y3utBCfpZk3XFJnYD7eXQTYW1bdWw39KZKc2FlfXks";
    // Pastikan nama file JSON di bawah ini sama persis dengan yang ada di folder resources Anda
    private final String KEY_FILE = "/zknlog-8b5443b3bd83.json";

    public List<Resi> getResiFromSheet(String sheetName) throws Exception {
        InputStream in = GoogleSheetsService.class.getResourceAsStream(KEY_FILE);

        Sheets service = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new com.google.auth.http.HttpCredentialsAdapter(
                        com.google.auth.oauth2.ServiceAccountCredentials.fromStream(in)
                                .createScoped(java.util.Collections.singleton("https://www.googleapis.com/auth/spreadsheets.readonly"))
                )
        ).setApplicationName("ZKN LOG").build();

        // Ambil sampai kolom G agar tab INSTANT juga terbaca
        String range = sheetName + "!A2:G";
        ValueRange response = service.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute();

        List<List<Object>> values = response.getValues();
        List<Resi> listResi = new ArrayList<>();

        if (values != null) {
            for (List<Object> row : values) {
                // Logika cerdas: Jika tab INSTANT, ambil kolom C (Resi) dan E (No Pesanan)
                if (sheetName.equalsIgnoreCase("INSTANT")) {
                    listResi.add(new Resi(
                            row.size() > 0 ? row.get(0).toString() : "", // ID
                            row.size() > 1 ? row.get(1).toString() : "", // Date
                            row.size() > 4 ? row.get(4).toString() : "", // No Pesanan jadi Nomor Resi
                            row.size() > 3 ? row.get(3).toString() : "", // Expedition jadi Notes
                            row.size() > 2 ? row.get(2).toString() : ""  // Courier Name
                    ));
                } else {
                    // Untuk tab standard (SPX, J&T, JNE, ANTERAJA)
                    listResi.add(new Resi(
                            row.size() > 0 ? row.get(0).toString() : "",
                            row.size() > 1 ? row.get(1).toString() : "",
                            row.size() > 2 ? row.get(2).toString() : "",
                            row.size() > 3 ? row.get(3).toString() : "",
                            row.size() > 4 ? row.get(4).toString() : ""
                    ));
                }
            }
        }
        return listResi;
    }
}