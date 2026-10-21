package io.github.hatsu3140.pianotab.web;

import io.github.hatsu3140.pianotab.converter.GuitarTabConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * converter モジュールの部品を Spring に登録する設定。
 * converter は Spring に依存しない普通の Java なので、ここで橋渡しする。
 */
@Configuration
public class ConverterConfig {

    @Bean
    public GuitarTabConverter guitarTabConverter() {
        return new GuitarTabConverter();
    }
}
