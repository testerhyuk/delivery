export interface Menu {
  id: string;
  menuId?: string;
  name: string;
  price: number;
}

export interface Restaurant {
  id: string; // 엔티티의 ID
  restaurantId?: string;
  name: string;
}