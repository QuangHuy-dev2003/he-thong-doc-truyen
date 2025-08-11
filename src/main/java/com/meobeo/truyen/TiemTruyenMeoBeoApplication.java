package com.meobeo.truyen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TiemTruyenMeoBeoApplication {

	public static void main(String[] args) {
		SpringApplication.run(TiemTruyenMeoBeoApplication.class, args);
	}

}
