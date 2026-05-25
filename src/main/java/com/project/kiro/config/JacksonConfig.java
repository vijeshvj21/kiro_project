package com.project.kiro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class JacksonConfig {

    /**
     * Configures the Jackson ObjectMapper with:
     * - BigDecimal serialized as plain decimal strings (no scientific notation)
     * - ISO-8601 date formats (dates as strings, not timestamps)
     * Java time support is built into Jackson 3's jackson-databind.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                // Serialize dates as ISO-8601 strings, not numeric timestamps
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                // Serialize BigDecimal as plain decimal strings (no scientific notation)
                .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                .build();
    }
}
