package com.logistic.app;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Membuat Getter & Setter otomatis
@AllArgsConstructor // Membuat Constructor lengkap otomatis
@NoArgsConstructor // Membuat Constructor kosong otomatis
public class Resi {
    private String uniqueId;
    private String date;
    private String resi;
    private String notes;
    private String ekspedisi;
}