package cn.datong.standard;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("cn.datong.standard.mapper")
public class StandardDocsApplication {

    public static void main(String[] args) {
        SpringApplication.run(StandardDocsApplication.class, args);
    }
}
