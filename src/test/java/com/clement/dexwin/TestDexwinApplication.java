package com.clement.dexwin;

import org.springframework.boot.SpringApplication;

public class TestDexwinApplication {

    public static void main(String[] args) {
        SpringApplication.from(DexwinApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
