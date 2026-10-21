package io.github.hatsu3140.pianotab.converter.gp5;

import io.github.hatsu3140.pianotab.converter.model.TabScore;

/**
 * 手順4: TAB 譜を Guitar Pro 5 形式（.gp5）のバイト列にする。
 */
public class Gp5Writer {

    public byte[] write(TabScore tabScore) {
        // TODO: GP5 のバイナリ形式（ヘッダ、曲情報、小節、トラック、拍、音）を書き出す。
        //       TuxGuitar で開けることを確認しながら実装する。
        return new byte[0];
    }
}
