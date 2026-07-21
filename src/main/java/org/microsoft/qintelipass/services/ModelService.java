package org.microsoft.qintelipass.services;

import org.microsoft.qintelipass.entity.AiModelConfig;

import java.util.Optional;

public interface ModelService {
    Optional<AiModelConfig> findModelById(Long id);
}
