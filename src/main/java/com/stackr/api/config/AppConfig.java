package com.stackr.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableScheduling
public class AppConfig {

    /** Domain LocalDateTime values are UTC; the wire format always has an explicit Z. */
    @Bean
    public org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer utcJson() {
        return builder -> {
            builder.serializerByType(java.time.LocalDateTime.class, new com.fasterxml.jackson.databind.JsonSerializer<java.time.LocalDateTime>() {
                @Override public void serialize(java.time.LocalDateTime value, com.fasterxml.jackson.core.JsonGenerator gen,
                        com.fasterxml.jackson.databind.SerializerProvider provider) throws java.io.IOException {
                    gen.writeString(value.atOffset(java.time.ZoneOffset.UTC).toString());
                }
            });
            builder.deserializerByType(java.time.LocalDateTime.class, new com.fasterxml.jackson.databind.JsonDeserializer<java.time.LocalDateTime>() {
                @Override public java.time.LocalDateTime deserialize(com.fasterxml.jackson.core.JsonParser parser,
                        com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
                    String value = parser.getText();
                    try { return java.time.OffsetDateTime.parse(value).withOffsetSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime(); }
                    catch (java.time.format.DateTimeParseException e) { return java.time.LocalDateTime.parse(value); }
                }
            });
        };
    }

    @Bean
    public RestTemplate restTemplate() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(120000);
        return new RestTemplate(factory);
    }
}
