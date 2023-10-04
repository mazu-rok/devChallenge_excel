package it.devchallenge.excel;

import org.mariuszgromada.math.mxparser.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DevChallengeApplication {

    public static void main(String[] args) {
        License.iConfirmNonCommercialUse("devChallenge");
        SpringApplication.run(DevChallengeApplication.class, args);
    }

}
