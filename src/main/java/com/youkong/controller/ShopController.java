package com.youkong.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.youkong.common.ApiResponse;
import com.youkong.entity.ServiceItem;
import com.youkong.entity.Shop;
import com.youkong.entity.Staff;
import com.youkong.mapper.ServiceItemMapper;
import com.youkong.mapper.ShopMapper;
import com.youkong.mapper.StaffMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopMapper shopMapper;
    private final StaffMapper staffMapper;
    private final ServiceItemMapper serviceItemMapper;

    @GetMapping("/{shopId}")
    public ApiResponse<Shop> detail(@PathVariable Long shopId) {
        return ApiResponse.ok(shopMapper.selectById(shopId));
    }

    @GetMapping("/{shopId}/staff")
    public ApiResponse<List<Staff>> staff(@PathVariable Long shopId) {
        return ApiResponse.ok(staffMapper.selectList(Wrappers.<Staff>lambdaQuery()
                .eq(Staff::getShopId, shopId)
                .eq(Staff::getActive, true)));
    }

    @GetMapping("/{shopId}/services")
    public ApiResponse<List<ServiceItem>> services(@PathVariable Long shopId) {
        return ApiResponse.ok(serviceItemMapper.selectList(Wrappers.<ServiceItem>lambdaQuery()
                .eq(ServiceItem::getShopId, shopId)));
    }
}
