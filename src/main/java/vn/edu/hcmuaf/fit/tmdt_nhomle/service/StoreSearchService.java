package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.StoreStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Store;

import java.util.List;

public class StoreSearchService {
    public List<Store> search(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        return StoreStore.findAll().stream()
                .filter(store -> normalizedQuery.isEmpty()
                        || store.getName().toLowerCase().contains(normalizedQuery)
                        || store.getDescription().toLowerCase().contains(normalizedQuery)
                        || store.getSpecialty().toLowerCase().contains(normalizedQuery)
                        || store.getLocation().toLowerCase().contains(normalizedQuery))
                .toList();
    }
}
