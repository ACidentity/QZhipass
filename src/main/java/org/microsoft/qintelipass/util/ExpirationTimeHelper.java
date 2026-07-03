package org.microsoft.qintelipass.util;

import java.time.Instant;
import java.time.LocalDate;

public class ExpirationTimeHelper {
    public static Instant getNextDayTime(){
        return Instant
                .from(LocalDate
                        .now()
                        .plusDays(1)
                        .atStartOfDay());
    }
}
