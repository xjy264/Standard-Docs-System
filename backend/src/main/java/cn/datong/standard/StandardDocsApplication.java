package cn.datong.standard;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cn.datong.standard.mapper")
public class StandardDocsApplication {

    public static void main(String[] args) {
        SpringApplication.run(StandardDocsApplication.class, args);
    }
}
