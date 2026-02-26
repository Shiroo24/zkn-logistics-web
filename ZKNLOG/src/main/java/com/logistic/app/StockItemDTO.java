package com.logistic.app;

public class StockItemDTO {
    private String id;
    private String namaArt;
    private String kategori;
    private String sku;
    private String size;
    private int poTotal;
    private int stockStore;
    private int target;
    private int current = 0;
    private String keterangan;
    private boolean isToko;

    public StockItemDTO(String id, String namaArt, String kategori, String sku, String size,
                        int poTotal, int stockStore, int target, String keterangan, boolean isToko) {
        this.id = id;
        this.namaArt = namaArt;
        this.kategori = kategori;
        this.sku = sku;
        this.size = size;
        this.poTotal = poTotal;
        this.stockStore = stockStore;
        this.target = target;
        this.keterangan = keterangan;
        this.isToko = isToko;
    }

    // Getters
    public String getSku() { return sku; }
    public String getNamaArt() { return namaArt; }
    public String getKategori() { return kategori; }
    public String getSize() { return size; }
    public int getPoTotal() { return poTotal; }
    public int getStockStore() { return stockStore; }
    public int getTarget() { return target; }
    public String getKeterangan() { return keterangan; }
}