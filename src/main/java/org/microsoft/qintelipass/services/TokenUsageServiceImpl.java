package org.microsoft.qintelipass.services;

import lombok.extern.slf4j.Slf4j;
import org.microsoft.qintelipass.dtos.TokenUsageRankDTO;
import org.microsoft.qintelipass.dtos.UserTokenUsageDTO;
import org.microsoft.qintelipass.models.User;
import org.microsoft.qintelipass.util.ExpirationTimeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class TokenUsageServiceImpl implements TokenUsageService {
    private static final String USAGE_KEY_PREFIX = "usage:daily:";
    private static final String RANK_KEY_PREFIX = "usage:daily:rank:";
    private static final String LIMIT_KEY_PREFIX = "user:token:limit:";
    private static final String TOTAL_TOKENS_KEY = "models:daily:total:tokens";
    private static long DEFAULT_TOKEN_LIMIT = 100000L;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserService userService;
    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    @Autowired
    public TokenUsageServiceImpl(RedisTemplate<String, String> redisTemplate, UserService userService) {
        this.redisTemplate = redisTemplate;
        this.userService = userService;
    }

    @Override
    public boolean recordTokenUsage(Long userId, int tokensUsed) {
        if (tokensUsed <= 0) {
            return false;
        }

        String today = getTodayDateString();
        String usageKey = getUsageKey(today, userId);
        String rankKey = getRankKey(today);

        Long currentUsage = redisTemplate.opsForValue().increment(usageKey, tokensUsed);

        if (currentUsage != null && currentUsage == tokensUsed) {
            redisTemplate.expireAt(usageKey, ExpirationTimeHelper.getNextDayTime());
        }

        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        zSetOps.incrementScore(rankKey, String.valueOf(userId), tokensUsed);

        if (zSetOps.size(rankKey) != null && zSetOps.size(rankKey) == 1) {
            redisTemplate.expireAt(usageKey, ExpirationTimeHelper.getNextDayTime());
        }

        log.debug("Recorded token usage: userId={}, tokens={}, total={}", userId, tokensUsed, currentUsage);
        return true;
    }



    @Override
    public boolean checkTokenLimit(Long userId) {
        long limit = getUserTokenLimit(userId);
        long currentUsage = getCurrentTokenUsage(userId);
        boolean exceeded = currentUsage >= limit;

        if (exceeded) {
            log.warn("User {} exceeded token limit: usage={}, limit={}", userId, currentUsage, limit);
        }

        return !exceeded;
    }

    @Override
    public UserTokenUsageDTO getUserTokenUsage(Long userId) {
        User user = userService.getUserById(userId);
        String userName = user != null ? user.getName() : "Unknown";

        long currentUsage = getCurrentTokenUsage(userId);
        long limit = getUserTokenLimit(userId);

        return UserTokenUsageDTO.builder()
                .userId(userId)
                .userName(userName)
                .tokenUsed(currentUsage)
                .tokenLimit(limit)
                .isExceeded(currentUsage >= limit)
                .build();
    }

    @Override
    public List<TokenUsageRankDTO> getDailyTokenRank(int topN) {
        String today = getTodayDateString();
        String rankKey = getRankKey(today);

        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(rankKey, 0, topN - 1);

        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }

        int rank = 1;
        List<TokenUsageRankDTO> result = List.of();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Long userId = Long.valueOf(tuple.getValue());
            Double score = tuple.getScore();
            Long totalTokens = score != null ? score.longValue() : 0L;

            User user = userService.getUserById(userId);
            String userName = user != null ? user.getName() : "Unknown";

            result = new java.util.ArrayList<>(result);
            result.add(TokenUsageRankDTO.builder()
                    .userId(userId)
                    .userName(userName)
                    .totalTokens(totalTokens)
                    .rank(rank++)
                    .build());
        }

        return result;
    }

    @Override
    public long getUserTokenLimit(Long userId) {
        String limitKey = LIMIT_KEY_PREFIX + userId;
        String limitStr = redisTemplate.opsForValue().get(limitKey);

        if (limitStr != null) {
            try {
                return Long.parseLong(limitStr);
            } catch (NumberFormatException e) {
                log.error("Invalid token limit format for user: {}", userId);
            }
        }

        return DEFAULT_TOKEN_LIMIT;
    }

    @Override
    public void setUserTokenLimit(Long userId, long limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Token limit must be positive");
        }
        String limitKey = LIMIT_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(limitKey, String.valueOf(limit));
        log.info("Set token limit: userId={}, limit={}", userId, limit);
    }

    @Override
    public String getTodayTotalTokens() {
        return this.redisTemplate.opsForValue().get(TOTAL_TOKENS_KEY);
    }

    @Override
    public void increaseDailyTotalTokens(Integer tokens) {
        this.redisTemplate.opsForValue().increment(TOTAL_TOKENS_KEY, tokens);
        this.redisTemplate.expireAt(TOTAL_TOKENS_KEY, ExpirationTimeHelper.getNextDayTime());
    }

    @Override
    public Long getOveruseUsers() {
        return redisTemplate.opsForZSet().count(getRankKey(),0, DEFAULT_TOKEN_LIMIT);
    }
    @Override
    public Long getDailyTokenLimit() {
        return DEFAULT_TOKEN_LIMIT;
    }
    @Override
    public void setDailyTokenLimit(Long value) {
        DEFAULT_TOKEN_LIMIT = value;
    }
    private long getCurrentTokenUsage(Long userId) {
        String today = getTodayDateString();
        String usageKey = getUsageKey(today, userId);
        String usageStr = redisTemplate.opsForValue().get(usageKey);

        if (usageStr == null) {
            return 0L;
        }

        try {
            return Long.parseLong(usageStr);
        } catch (NumberFormatException e) {
            log.error("Invalid token usage format for user: {}", userId);
            return 0L;
        }
    }

    private String getTodayDateString() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    private String getUsageKey(String date, Long userId) {
        return USAGE_KEY_PREFIX + date + ":" + userId;
    }

    private String getRankKey(String date) {
        return RANK_KEY_PREFIX + date;
    }

    private String getRankKey() {
        return RANK_KEY_PREFIX + getTodayDateString();
    }

    private long getSecondsUntilMidnight() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        java.time.LocalDateTime midnight = tomorrow.atStartOfDay();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        return java.time.Duration.between(now, midnight).getSeconds();
    }
}
