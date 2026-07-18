package com.example.tournaments_backend.instance;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring for {@link InstanceIdFilter}: reads the instance id from the
 * {@code INSTANCE_ID} env var and prevents Spring Boot from auto-registering
 * the filter as a servlet-level filter — it is added explicitly inside the
 * Spring Security chain instead, the same way as {@code RateLimitFilter}.
 */
@Configuration
public class InstanceIdConfig {

    @Value("${INSTANCE_ID:" + InstanceIdFilter.DEFAULT_INSTANCE_ID + "}")
    private String instanceId;

    @Bean
    public InstanceIdFilter instanceIdFilter() {
        return new InstanceIdFilter(instanceId);
    }

    @Bean
    public FilterRegistrationBean<InstanceIdFilter> instanceIdFilterRegistration(InstanceIdFilter filter) {
        FilterRegistrationBean<InstanceIdFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
