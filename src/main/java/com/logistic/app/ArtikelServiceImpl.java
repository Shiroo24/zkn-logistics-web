package com.logistic.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ArtikelServiceImpl implements ArtikelService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<ArtikelModel> getAllArtikel() {
        // Ganti 'master_artikel' dengan nama tabel database kamu yang sebenarnya
        String sql = "SELECT * FROM master_artikel";
        try {
            return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(ArtikelModel.class));
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
}