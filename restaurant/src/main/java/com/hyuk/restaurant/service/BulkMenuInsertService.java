package com.hyuk.restaurant.service;

import com.hyuk.common.Snowflake;
import com.hyuk.restaurant.entity.MenuEntity;
import com.hyuk.restaurant.entity.RestaurantEntity;
import com.hyuk.restaurant.repository.MenuRepository;
import com.hyuk.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkMenuInsertService {
    private final MenuRepository menuRepository;
    private final Snowflake snowflake;
    private final RestaurantRepository restaurantRepository;

    @Transactional
    public void processBulkMenuInsert() {
        // 1. 카테고리 템플릿 정의
        Map<String, Map<String, Integer>> categoryTemplate = Map.of(
                "한식", Map.of("김치찌개", 8000, "된장찌개", 8000, "제육볶음", 9500, "비빔밥", 9000, "불고기", 12000),
                "일식", Map.of("돈카츠", 11000, "라멘", 10000, "연어덮밥", 13000, "초밥세트", 15000, "가츠동", 9500),
                "서양식", Map.of("까르보나라", 13000, "마르게리따 피자", 15000, "비프 스테이크", 25000, "봉골레 파스타", 14000, "리조또", 13500),
                "중식", Map.of("짜장면", 7000, "짬뽕", 8000, "탕수육", 18000, "마파두부", 12000, "볶음밥", 7500),
                "동남아시아", Map.of("쌀국수", 9500, "나시고랭", 11000, "팟타이", 11000, "뿌팟퐁커리", 22000, "월남쌈", 15000)
        );

        List<RestaurantEntity> restaurants = restaurantRepository.findAll();
        List<MenuEntity> allMenus = new ArrayList<>();
        int skipCount = 0;

        for (RestaurantEntity restaurant : restaurants) {
            if (menuRepository.existsByRestaurantId(restaurant.getId())) {
                skipCount++;
                continue;
            }

            String category = restaurant.getCategory();
            if (categoryTemplate.containsKey(category)) {
                Map<String, Integer> menus = categoryTemplate.get(category);

                menus.forEach((menuName, price) -> {
                    allMenus.add(MenuEntity.create(
                            snowflake.nextId(),
                            menuName,
                            price,
                            restaurant
                    ));
                });
            }

            if (allMenus.size() >= 500) {
                menuRepository.saveAll(allMenus);
                allMenus.clear();
                log.info("중간 저장 완료...");
            }
        }

        if (!allMenus.isEmpty()) {
            menuRepository.saveAll(allMenus);
        }

        log.info("전체 작업 완료. 건너뛴 식당 수: {}, 새로 추가된 메뉴 세트 보유 식당 수: {}", skipCount, (restaurants.size() - skipCount));
    }
}
