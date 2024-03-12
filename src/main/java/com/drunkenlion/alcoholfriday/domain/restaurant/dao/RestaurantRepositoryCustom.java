package com.drunkenlion.alcoholfriday.domain.restaurant.dao;

import com.drunkenlion.alcoholfriday.domain.member.entity.Member;
import com.drunkenlion.alcoholfriday.domain.restaurant.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
public interface RestaurantRepositoryCustom {
    Page<Restaurant> findAllBasedAuth(Member member, Pageable pageable);
    List<Restaurant> getRestaurant(double neLatitude, double neLongitude, double swLatitude, double swLongitude);
}
