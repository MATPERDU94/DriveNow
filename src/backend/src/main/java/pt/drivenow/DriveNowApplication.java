package pt.drivenow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DriveNowApplication {

    public static void main(String[] args) {
        SpringApplication.run(DriveNowApplication.class, args);
    }
}
