package com.youkong.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.youkong.dto.BookingDetailVO;
import com.youkong.entity.Booking;
import com.youkong.entity.ServiceItem;
import com.youkong.entity.Staff;
import com.youkong.mapper.BookingMapper;
import com.youkong.mapper.ServiceItemMapper;
import com.youkong.mapper.StaffMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 预约列表查询。员工名、服务名在内存里回填，避免为两个简单关联写 JOIN。
 */
@Service
@RequiredArgsConstructor
public class BookingQueryService {

    private final BookingMapper bookingMapper;
    private final StaffMapper staffMapper;
    private final ServiceItemMapper serviceItemMapper;

    /** 店铺某天的全部预约，按时间排序 */
    public List<BookingDetailVO> listByShopAndDate(Long shopId, LocalDate date) {
        LocalDateTime from = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime to = LocalDateTime.of(date.plusDays(1), LocalTime.MIN);
        List<Booking> rows = bookingMapper.selectList(Wrappers.<Booking>lambdaQuery()
                .eq(Booking::getShopId, shopId)
                .ge(Booking::getStartAt, from)
                .lt(Booking::getStartAt, to)
                .orderByAsc(Booking::getStartAt));
        return enrich(rows);
    }

    /** 顾客用手机号查自己的预约 */
    public List<BookingDetailVO> listByPhone(Long shopId, String phone) {
        List<Booking> rows = bookingMapper.selectList(Wrappers.<Booking>lambdaQuery()
                .eq(Booking::getShopId, shopId)
                .eq(Booking::getCustomerPhone, phone)
                .orderByDesc(Booking::getStartAt));
        return enrich(rows);
    }

    /** 单条预约详情，供预约成功页使用 */
    public BookingDetailVO findDetail(Long bookingId) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            return null;
        }
        return enrich(List.of(booking)).get(0);
    }

    private List<BookingDetailVO> enrich(List<Booking> rows) {
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> staffIds = rows.stream().map(Booking::getStaffId).distinct().toList();
        List<Long> serviceIds = rows.stream().map(Booking::getServiceId).distinct().toList();

        Map<Long, Staff> staffMap = staffMapper.selectBatchIds(staffIds).stream()
                .collect(Collectors.toMap(Staff::getId, Function.identity()));
        Map<Long, ServiceItem> serviceMap = serviceItemMapper.selectBatchIds(serviceIds).stream()
                .collect(Collectors.toMap(ServiceItem::getId, Function.identity()));

        return rows.stream().map(b -> {
            BookingDetailVO vo = BookingDetailVO.from(b);
            Staff staff = staffMap.get(b.getStaffId());
            ServiceItem item = serviceMap.get(b.getServiceId());
            vo.setStaffName(staff == null ? "" : staff.getName());
            vo.setServiceName(item == null ? "" : item.getName());
            return vo;
        }).toList();
    }
}
