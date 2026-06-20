package com.petshop.order.controller.app;

import com.petshop.order.common.R;
import com.petshop.order.entity.AppUser;
import com.petshop.order.entity.UserAddress;
import com.petshop.order.service.AppAuthService;
import com.petshop.order.service.UserAddressService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app/addresses")
@RequiredArgsConstructor
public class AppUserAddressController {

    private final UserAddressService userAddressService;
    private final AppAuthService appAuthService;

    @GetMapping
    public R<List<Map<String, Object>>> list() {
        Long userId = currentUserId();
        List<UserAddress> list = userAddressService.listByUser(userId);
        return R.ok(list.stream().map(this::toMap).toList());
    }

    @PostMapping
    public R<Map<String, Object>> create(@Validated @RequestBody AddressRequest req) {
        Long userId = currentUserId();
        UserAddress addr = new UserAddress();
        addr.setLabel(req.getLabel());
        addr.setAddress(req.getAddress());
        addr.setDetail(req.getDetail());
        addr.setLat(new BigDecimal(req.getLat()));
        addr.setLng(new BigDecimal(req.getLng()));
        UserAddress created = userAddressService.create(userId, addr);
        return R.ok(toMap(created));
    }

    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable Long id, @Validated @RequestBody AddressRequest req) {
        Long userId = currentUserId();
        UserAddress addr = new UserAddress();
        addr.setLabel(req.getLabel());
        addr.setAddress(req.getAddress());
        addr.setDetail(req.getDetail());
        addr.setLat(new BigDecimal(req.getLat()));
        addr.setLng(new BigDecimal(req.getLng()));
        UserAddress updated = userAddressService.update(userId, id, addr);
        return R.ok(toMap(updated));
    }

    @PutMapping("/{id}/default")
    public R<Void> setDefault(@PathVariable Long id) {
        Long userId = currentUserId();
        userAddressService.setDefault(userId, id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        Long userId = currentUserId();
        userAddressService.delete(userId, id);
        return R.ok();
    }

    private Long currentUserId() {
        AppUser user = appAuthService.getCurrentUser();
        return user.getId();
    }

    private Map<String, Object> toMap(UserAddress addr) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", addr.getId());
        m.put("label", addr.getLabel());
        m.put("address", addr.getAddress());
        m.put("detail", addr.getDetail());
        m.put("lat", addr.getLat() != null ? addr.getLat().toPlainString() : null);
        m.put("lng", addr.getLng() != null ? addr.getLng().toPlainString() : null);
        m.put("isDefault", addr.getIsDefault() != null && addr.getIsDefault() == 1);
        m.put("createTime", addr.getCreateTime());
        return m;
    }

    @Data
    public static class AddressRequest {
        private String label;
        @NotBlank
        private String address;
        private String detail;
        @NotBlank
        private String lat;
        @NotBlank
        private String lng;
    }
}
