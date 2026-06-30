package org.microsoft.qintelipass.services;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Service
public interface ISmsService {
    @Nullable String sendSmsCode(String phoneNumber);
}
