package com.example.ridehailing.repository;

import com.example.ridehailing.domain.Coupon;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCouponRepository implements CouponRepository {

    private final ConcurrentHashMap<String, Coupon> store = new ConcurrentHashMap<>();

    @Override
    public Coupon save(Coupon coupon) {
        store.put(coupon.code(), coupon);
        return coupon;
    }

    @Override
    public Optional<Coupon> findById(String code) {
        return Optional.ofNullable(store.get(Coupon.normalizeCode(code)));
    }

    @Override
    public List<Coupon> findAll() {
        return List.copyOf(store.values());
    }
}
