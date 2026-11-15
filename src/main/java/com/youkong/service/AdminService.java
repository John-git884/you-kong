package com.youkong.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.youkong.common.BizException;
import com.youkong.common.ErrorCode;
import com.youkong.dto.ScheduleEntry;
import com.youkong.dto.ServiceItemRequest;
import com.youkong.dto.StaffRequest;
import com.youkong.entity.Booking;
import com.youkong.entity.ServiceItem;
import com.youkong.entity.Shop;
import com.youkong.entity.Staff;
import com.youkong.entity.StaffSchedule;
import com.youkong.mapper.BookingMapper;
import com.youkong.mapper.ServiceItemMapper;
import com.youkong.mapper.ShopMapper;
import com.youkong.mapper.StaffMapper;
import com.youkong.mapper.StaffScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 店主维护店铺资料的用例：服务项目、员工、周排班。
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final ShopMapper shopMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final StaffMapper staffMapper;
    private final StaffScheduleMapper staffScheduleMapper;
    private final BookingMapper bookingMapper;

    public List<ServiceItem> listServices(Long shopId) {
        return serviceItemMapper.selectList(Wrappers.<ServiceItem>lambdaQuery()
                .eq(ServiceItem::getShopId, shopId)
                .orderByAsc(ServiceItem::getId));
    }

    public ServiceItem createService(Long shopId, ServiceItemRequest req) {
        requireShop(shopId);
        ServiceItem item = new ServiceItem();
        item.setShopId(shopId);
        applyFields(item, req);
        serviceItemMapper.insert(item);
        return item;
    }

    public ServiceItem updateService(Long serviceId, ServiceItemRequest req) {
        ServiceItem item = serviceItemMapper.selectById(serviceId);
        if (item == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "service " + serviceId);
        }
        applyFields(item, req);
        serviceItemMapper.updateById(item);
        return item;
    }

    /**
     * 只允许删除从未被预约过的服务，历史订单要能查回服务名。
     */
    public void deleteService(Long serviceId) {
        ServiceItem item = serviceItemMapper.selectById(serviceId);
        if (item == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "service " + serviceId);
        }
        Long used = bookingMapper.selectCount(Wrappers.<Booking>lambdaQuery()
                .eq(Booking::getServiceId, serviceId));
        if (used != null && used > 0) {
            throw new BizException(ErrorCode.SERVICE_IN_USE);
        }
        serviceItemMapper.deleteById(serviceId);
    }

    public List<Staff> listStaff(Long shopId) {
        return staffMapper.selectList(Wrappers.<Staff>lambdaQuery()
                .eq(Staff::getShopId, shopId)
                .orderByAsc(Staff::getId));
    }

    public Staff createStaff(Long shopId, StaffRequest req) {
        requireShop(shopId);
        Staff staff = new Staff();
        staff.setShopId(shopId);
        staff.setName(req.getName().trim());
        staff.setTitle(req.getTitle() == null ? "" : req.getTitle().trim());
        staff.setActive(req.getActive() == null || req.getActive());
        staffMapper.insert(staff);
        return staff;
    }

    public Staff updateStaff(Long staffId, StaffRequest req) {
        Staff staff = staffMapper.selectById(staffId);
        if (staff == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "staff " + staffId);
        }
        staff.setName(req.getName().trim());
        staff.setTitle(req.getTitle() == null ? "" : req.getTitle().trim());
        if (req.getActive() != null) {
            staff.setActive(req.getActive());
        }
        staffMapper.updateById(staff);
        return staff;
    }

    public List<StaffSchedule> listSchedules(Long staffId) {
        return staffScheduleMapper.selectList(Wrappers.<StaffSchedule>lambdaQuery()
                .eq(StaffSchedule::getStaffId, staffId)
                .orderByAsc(StaffSchedule::getDayOfWeek));
    }

    /**
     * 整体覆盖某员工的周排班。传入空数组表示这一周都不排班。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<StaffSchedule> saveSchedules(Long staffId, List<ScheduleEntry> entries) {
        Staff staff = staffMapper.selectById(staffId);
        if (staff == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "staff " + staffId);
        }

        Set<Integer> days = new HashSet<>();
        for (ScheduleEntry entry : entries) {
            if (entry == null || entry.getDayOfWeek() == null
                    || entry.getDayOfWeek() < 1 || entry.getDayOfWeek() > 7
                    || entry.getStartTime() == null || entry.getEndTime() == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "排班条目不完整");
            }
            if (!days.add(entry.getDayOfWeek())) {
                throw new BizException(ErrorCode.PARAM_INVALID,
                        "星期 " + entry.getDayOfWeek() + " 重复");
            }
            if (!entry.getStartTime().isBefore(entry.getEndTime())) {
                throw new BizException(ErrorCode.PARAM_INVALID, "开始时间必须早于结束时间");
            }
        }

        staffScheduleMapper.delete(Wrappers.<StaffSchedule>lambdaQuery()
                .eq(StaffSchedule::getStaffId, staffId));
        for (ScheduleEntry entry : entries) {
            StaffSchedule schedule = new StaffSchedule();
            schedule.setStaffId(staffId);
            schedule.setDayOfWeek(entry.getDayOfWeek());
            schedule.setStartTime(entry.getStartTime());
            schedule.setEndTime(entry.getEndTime());
            staffScheduleMapper.insert(schedule);
        }
        return listSchedules(staffId);
    }

    private void applyFields(ServiceItem item, ServiceItemRequest req) {
        item.setName(req.getName().trim());
        item.setDurationMinutes(req.getDurationMinutes());
        item.setPriceCent(req.getPriceCent());
    }

    private void requireShop(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "shop " + shopId);
        }
    }
}
