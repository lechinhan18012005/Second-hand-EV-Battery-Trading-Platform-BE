package com.evdealer.evdealermanagement.controller.revenue;

import com.evdealer.evdealermanagement.dto.revenue.MonthlyRevenue;
import com.evdealer.evdealermanagement.dto.revenue.YearlyRevenue;
import com.evdealer.evdealermanagement.service.implement.RevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/revenue")
@RequiredArgsConstructor
public class RevenueController {

    private final RevenueService revenueService;

    // Lấy doanh thu theo tháng trong từng năm
    @GetMapping("/month")
    public List<MonthlyRevenue> getRevenueByMonth(@RequestParam String month) {
        return revenueService.getTotalFeeDuringMonth(month);
    }

    // Lấy tổng doanh thu theo từng năm
    @GetMapping("/year")
    public List<YearlyRevenue> getRevenueByYear() {
        return revenueService.getTotalRevenueByYear();
    }

    @GetMapping("/year/{year}/monthly")
    public List<MonthlyRevenue> getMonthlyRevenueOfYear(@PathVariable String year) {
        return revenueService.getMonthlyRevenueOfYear(year);
    }
}
