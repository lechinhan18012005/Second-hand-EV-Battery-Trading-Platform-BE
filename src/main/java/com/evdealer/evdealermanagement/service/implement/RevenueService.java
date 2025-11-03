package com.evdealer.evdealermanagement.service.implement;

import com.evdealer.evdealermanagement.dto.revenue.MonthlyRevenue;
import com.evdealer.evdealermanagement.dto.revenue.YearlyRevenue;
import com.evdealer.evdealermanagement.entity.post.PostPayment;
import com.evdealer.evdealermanagement.repository.PostPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.comparator.Comparators;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueService {

    private final PostPaymentRepository postPaymentRepository;

    public List<MonthlyRevenue> getTotalFeeDuringMonth(String monthStr) {
        try {
            int month = Integer.parseInt(monthStr);

            if (month < 1 || month > 12) {
                log.warn("Invalid input {}", month);
                return List.of();
            }

            List<PostPayment> paymentList = postPaymentRepository.findAll();

            Map<Integer, BigDecimal> revenueByYear = paymentList.stream()
                    .filter(p -> p.getCreatedAt() != null)
                    .filter(p -> p.getCreatedAt().getMonthValue() == month)
                    .collect(Collectors.groupingBy(
                            p-> p.getCreatedAt().getYear(),
                            Collectors.reducing(BigDecimal.ZERO, PostPayment::getAmount, BigDecimal::add)
                    ));

            List<MonthlyRevenue> revenueList = revenueByYear.entrySet().stream()
                    .map(entry -> new MonthlyRevenue(entry.getKey(), month, entry.getValue()))
                    .sorted(Comparator.comparing(MonthlyRevenue::getYear))
                    .toList();

            log.debug("Revenue by year for month {}: {}", month, revenueList);
            return revenueList;

        }
        catch (NumberFormatException e) {
            log.error("Invalid format: {}", monthStr, e);
            return List.of();
        } catch (Exception e) {
            log.error("Error calculating total fee for month: {}", monthStr, e);
            return List.of();
        }
    }

    public List<YearlyRevenue> getTotalRevenueByYear() {
        try {
            List<PostPayment> paymentList = postPaymentRepository.findAll();

            Map<Integer, BigDecimal> revenueByYear = paymentList.stream()
                    .filter(p -> p.getCreatedAt() != null)
                    .collect(Collectors.groupingBy(
                            p -> p.getCreatedAt().getYear(),
                            Collectors.reducing(BigDecimal.ZERO, PostPayment::getAmount, BigDecimal::add)
                    ));

            List<YearlyRevenue> revenueList = revenueByYear.entrySet().stream()
                    .map(entry -> new YearlyRevenue(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparing(YearlyRevenue::getYear))
                    .toList();

            return revenueList;
        } catch (Exception e) {
            log.error("Error calculating yearly revenue", e);
            return List.of();
        }
    }

    public List<MonthlyRevenue> getMonthlyRevenueOfYear(String yearStr) {
        try {
            int year = Integer.parseInt(yearStr);

            if (year < 1900 || year > 3000) {
                log.warn("Invalid year input: {}", year);
                return List.of();
            }

            List<PostPayment> paymentList = postPaymentRepository.findAll();

            Map<Integer, BigDecimal> revenueMonth = paymentList.stream()
                    .filter(p -> p.getCreatedAt() != null)
                    .filter(p -> p.getCreatedAt().getYear() == year)
                    .collect(Collectors.groupingBy(
                            p -> p.getCreatedAt().getMonthValue(),
                            Collectors.reducing(BigDecimal.ZERO, PostPayment::getAmount, BigDecimal::add)
                    ));
            List<MonthlyRevenue> revenueList =
                    java.util.stream.IntStream.rangeClosed(1, 12)
                            .mapToObj(month -> new MonthlyRevenue(
                                    year,
                                    month,
                                    revenueMonth.getOrDefault(month, BigDecimal.ZERO)
                            ))
                            .toList();
            log.debug("Monthly revenue for year {}: {}", year, revenueList);
            return revenueList;

        } catch (NumberFormatException e) {
            log.error("Invalid year format: {}", yearStr, e);
            return List.of();
        } catch (Exception e) {
            log.error("Error calculating monthly revenue for year: {}", yearStr, e);
            return List.of();
        }
    }
}
