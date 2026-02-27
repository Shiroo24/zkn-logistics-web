package com.logistic.app;

public class ArtikelModel {
    public String bin, sku, size, brand, nama, hb, hj, cat, qty;

    // Constructor Kosong
    public ArtikelModel() {}

    // Constructor Lengkap
    public ArtikelModel(String bin, String sku, String size, String brand, String nama, String hb, String hj, String cat, String qty) {
        this.bin = bin; this.sku = sku; this.size = size; this.brand = brand;
        this.nama = nama; this.hb = hb; this.hj = hj; this.cat = cat; this.qty = qty;
    }
}
