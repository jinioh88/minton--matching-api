package org.app.mintonmatchapi.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.app.mintonmatchapi.notification.push.FcmPushClient;
import org.app.mintonmatchapi.notification.push.FcmPushClientImpl;
import org.app.mintonmatchapi.notification.push.FcmPushClientNoop;
import org.app.mintonmatchapi.notification.push.FcmPushProperties;
import org.app.mintonmatchapi.notification.service.PushTokenService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@EnableConfigurationProperties(FcmPushProperties.class)
public class FcmPushConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "minton.push.fcm", name = "enabled", havingValue = "true")
    FirebaseApp firebaseApp(FcmPushProperties properties) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        FirebaseOptions.Builder builder = FirebaseOptions.builder();
        if (properties.hasCredentialsPath()) {
            try (InputStream in = new FileInputStream(properties.getCredentialsPath())) {
                builder.setCredentials(GoogleCredentials.fromStream(in));
            }
        } else {
            builder.setCredentials(GoogleCredentials.getApplicationDefault());
        }
        return FirebaseApp.initializeApp(builder.build());
    }

    @Bean
    FcmPushClient fcmPushClient(FcmPushProperties properties,
                               ObjectProvider<FirebaseApp> firebaseApp,
                               PushTokenService pushTokenService) {
        if (!properties.isEnabled()) {
            return new FcmPushClientNoop();
        }
        if (firebaseApp.getIfAvailable() == null) {
            return new FcmPushClientNoop();
        }
        return new FcmPushClientImpl(pushTokenService);
    }
}
