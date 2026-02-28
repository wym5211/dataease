package io.dataease.utils;

import io.dataease.model.DeModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ModelUtils implements ApplicationContextAware {

    private static String modelValue;

    private static Environment environment;

    @Value("${spring.profiles.active:standalone}")
    public void setModelValue(String modelValue) {
        ModelUtils.modelValue = modelValue;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) {
        ModelUtils.environment = context.getEnvironment();
    }

    public static DeModel get() {
        return DeModel.valueOf(modelValue.toUpperCase());
    }

    public static boolean isDesktop() {
        // Try environment first, fallback to modelValue
        if (environment != null) {
            String[] activeProfiles = environment.getActiveProfiles();
            if (activeProfiles != null && activeProfiles.length > 0) {
                for (String profile : activeProfiles) {
                    if ("desktop".equalsIgnoreCase(profile)) {
                        return true;
                    }
                }
            }
        }
        // Fallback to modelValue
        return modelValue != null && "desktop".equalsIgnoreCase(modelValue);
    }
}
