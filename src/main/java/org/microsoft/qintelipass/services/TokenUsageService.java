package org.microsoft.qintelipass.services;

import org.microsoft.qintelipass.dtos.TokenUsageRankDTO;
import org.microsoft.qintelipass.dtos.UserTokenUsageDTO;

import java.util.List;

public interface TokenUsageService {
    boolean recordTokenUsage(Long userId, int tokensUsed);
    boolean checkTokenLimit(Long userId);
    UserTokenUsageDTO getUserTokenUsage(Long userId);
    List<TokenUsageRankDTO> getDailyTokenRank(int topN);
    long getUserTokenLimit(Long userId);
    void setUserTokenLimit(Long userId, long limit);
    String getTodayTotalTokens();
    void increaseDailyTotalTokens(Integer tokens);
    Long getOveruseUsers();
    Long getDailyTokenLimit();

    void setDailyTokenLimit(Long value);
}
