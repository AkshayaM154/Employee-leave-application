package com.wenxt.leavemanagement.util;

import org.springframework.stereotype.Component;
import java.time.*;

@Component
public class HolidayChecker {

    public boolean isNonWorkingDay(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY
                || MonthDay.from(date).equals(MonthDay.of(1,1))
                || MonthDay.from(date).equals(MonthDay.of(1,26))
                || MonthDay.from(date).equals(MonthDay.of(8,15))
                || MonthDay.from(date).equals(MonthDay.of(10,2))
                || MonthDay.from(date).equals(MonthDay.of(12,25));
    }
}
