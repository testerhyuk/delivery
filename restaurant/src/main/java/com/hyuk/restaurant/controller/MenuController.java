package com.hyuk.restaurant.controller;

import com.hyuk.restaurant.dto.ResponseMenu;
import com.hyuk.restaurant.service.BulkMenuInsertService;
import com.hyuk.restaurant.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/restaurant-service")
public class MenuController {
    private final BulkMenuInsertService bulkMenuInsertService;
    private final MenuService menuService;

    @PostMapping("/bulk-insert")
    public void bulkInsert() {
        bulkMenuInsertService.processBulkMenuInsert();
    }

    @GetMapping("/menu/{restaurantId}")
    public ResponseEntity<List<ResponseMenu>> getMenu(@PathVariable("restaurantId") Long id) {
        List<ResponseMenu> response = menuService.getMenu(id);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
