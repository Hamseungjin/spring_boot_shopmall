package com.hsj.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class DailySalesResponse {

    private LocalDate date;
    private BigDecimal revenue;
    private long orderCount;
}
