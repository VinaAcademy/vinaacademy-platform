package com.vinaacademy.platform;

import com.vinaacademy.platform.feature.common.constant.AppConstants;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import vn.vinaacademy.kafka.KafkaProducerConfig;

import java.util.TimeZone;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.vinaacademy.platform",
        "vn.vinaacademy.common"
})
@EnableFeignClients
@Import({KafkaProducerConfig.class})
public class VinaAcademyApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(AppConstants.TIME_ZONE));
        SpringApplication.run(VinaAcademyApplication.class, args);
    }

//    @Bean
//    public CommandLineRunner createTestingData(TestingDataService testingDataService) {
//        return args -> {
//            // Create testing data here
//            testingDataService.createTestingAuthData(); // Create roles and users
//            testingDataService.createSeedDataFromJson(); // Create categories and courses from JSON file
//        };
//    }
}
