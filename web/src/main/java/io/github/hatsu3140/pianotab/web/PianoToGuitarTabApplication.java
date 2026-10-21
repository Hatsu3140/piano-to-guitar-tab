package io.github.hatsu3140.pianotab.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * アプリの起動クラス。{@code main} を実行すると http://localhost:8080 で画面が開ける。
 */
@SpringBootApplication
public class PianoToGuitarTabApplication {

    public static void main(String[] args) {
        SpringApplication.run(PianoToGuitarTabApplication.class, args);
    }
}
