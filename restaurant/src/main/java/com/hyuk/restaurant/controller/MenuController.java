package com.hyuk.restaurant.controller;

import com.hyuk.restaurant.service.BulkMenuInsertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MenuController {
    private final BulkMenuInsertService bulkMenuInsertService;
    @PostMapping("/restaurant-service/bulk-insert")
    public void bulkInsert() {
        bulkMenuInsertService.processBulkMenuInsert();
    }
}
