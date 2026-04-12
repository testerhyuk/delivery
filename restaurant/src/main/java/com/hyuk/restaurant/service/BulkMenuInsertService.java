package com.hyuk.restaurant.service;

import com.hyuk.common.Snowflake;
import com.hyuk.restaurant.entity.MenuEntity;
import com.hyuk.restaurant.entity.RestaurantEntity;
import com.hyuk.restaurant.repository.MenuRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BulkMenuInsertService {
    private static final Logger log = LoggerFactory.getLogger(BulkMenuInsertService.class);

    private final MenuRepository menuRepository;
    private final Snowflake snowflake;
    
    @PersistenceContext
    private EntityManager entityManager;

    private static final int RESTAURANT_FETCH_SIZE = 5000;
    private static final int MENU_INSERT_BATCH_SIZE = 2000;

    public BulkMenuInsertService(MenuRepository menuRepository, Snowflake snowflake) {
        this.menuRepository = menuRepository;
        this.snowflake = snowflake;
    }

    @Transactional
    public void processBulkMenuInsert() {
        Map<String, Map<String, Integer>> categoryTemplate = Map.of(
                "한식", Map.of("김치찌개", 8000, "된장찌개", 8000, "제육볶음", 9500, "비빔밥", 9000, "불고기", 12000),
                "일식", Map.of("돈카츠", 11000, "라멘", 10000, "연어덮밥", 13000, "초밥세트", 15000, "가츠동", 9500),
                "서양식", Map.of("까르보나라", 13000, "마르게리따 피자", 15000, "비프 스테이크", 25000, "봉골레 파스타", 14000, "리조또", 13500),
                "중식", Map.of("짜장면", 7000, "짬뽕", 8000, "탕수육", 18000, "마파두부", 12000, "볶음밥", 7500),
                "동남아시아", Map.of("쌀국수", 9500, "나시고랭", 11000, "팟타이", 11000, "뿌팟퐁커리", 22000, "월남쌈", 15000)
        );

        List<MenuEntity> allMenus = new ArrayList<>();
        int processedRestaurantCount = 0;
        int insertedRestaurantCount = 0;
        long lastId = 0L;

        while (true) {
            List<RestaurantEntity> restaurants = entityManager.createQuery(
                            "select r from RestaurantEntity r where r.id > :lastId order by r.id asc",
                            RestaurantEntity.class
                    )
                    .setParameter("lastId", lastId)
                    .setMaxResults(RESTAURANT_FETCH_SIZE)
                    .getResultList();

            if (restaurants.isEmpty()) {
                break;
            }

            for (RestaurantEntity restaurant : restaurants) {
                processedRestaurantCount++;

                String category = restaurant.getCategory();
                if (categoryTemplate.containsKey(category)) {
                    insertedRestaurantCount++;
                    Map<String, Integer> menus = categoryTemplate.get(category);

                    Long restaurantPk = restaurant.getId();

                    menus.forEach((menuName, price) -> allMenus.add(MenuEntity.create(
                            snowflake.nextId(),
                            menuName,
                            price,
                            entityManager.getReference(RestaurantEntity.class, restaurantPk)
                    )));
                }

                if (allMenus.size() >= MENU_INSERT_BATCH_SIZE) {
                    menuRepository.saveAll(allMenus);
                    entityManager.flush();
                    entityManager.clear();
                    allMenus.clear();
                    log.info("중간 저장 완료... processedRestaurants={}", processedRestaurantCount);
                }
            }

            lastId = restaurants.getLast().getId();
        }

        if (!allMenus.isEmpty()) {
            menuRepository.saveAll(allMenus);
            entityManager.flush();
            entityManager.clear();
        }

        log.info(
                "전체 작업 완료. 처리 식당 수: {}, 새로 추가된 메뉴 세트 보유 식당 수: {}",
                processedRestaurantCount,
                insertedRestaurantCount
        );
    }
}
