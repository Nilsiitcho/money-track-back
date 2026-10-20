package com.moneytrack.dashboard;

import com.moneytrack.common.Months;
import com.moneytrack.security.AuthContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public DashboardResponse dashboard(@RequestParam String month) {
        YearMonth ym = Months.parse(month);
        return service.build(AuthContext.currentUserId(), ym);
    }
}
