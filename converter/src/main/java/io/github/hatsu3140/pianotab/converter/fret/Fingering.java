package io.github.hatsu3140.pianotab.converter.fret;

import io.github.hatsu3140.pianotab.converter.model.TabNote;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 同時に鳴る音を左手でどう押さえるか。音ごとに、どの指で押さえるか（0 = 開放弦）を決めたもの。
 *
 * @param placements 音ごとの指
 * @param barreFret  セーハするフレット（カポ基準）。セーハしないなら 0
 * @param barreTo    セーハで押さえる一番太い弦（1弦からこの弦まで）。セーハしないなら 0
 */
public record Fingering(List<Placement> placements, int barreFret, int barreTo) {

    /** 人差し指 */
    public static final int INDEX = 1;
    /** 小指 */
    public static final int PINKY = 4;

    /**
     * 1音の押さえ方。
     *
     * @param note   音
     * @param finger 1 = 人差し指、2 = 中指、3 = 薬指、4 = 小指、0 = 開放弦
     */
    public record Placement(TabNote note, int finger) {
    }

    public boolean usesBarre() {
        return barreFret > 0;
    }

    /** 使う指の本数（セーハは1本） */
    public int fingerCount() {
        return (int) placements.stream().mapToInt(Placement::finger).filter(finger -> finger > 0).distinct().count();
    }

    /** 確認用の説明。例: 「人差し指 1フレット セーハ(1〜6弦) / 中指 3弦2 / 薬指 5弦3 / 小指 4弦3」 */
    public String describe(int capo) {
        String[] names = {"開放", "人差し指", "中指", "薬指", "小指"};
        String notes = placements.stream()
                .filter(placement -> !(usesBarre() && placement.finger() == INDEX
                        && placement.note().fret() - capo == barreFret))
                .sorted((a, b) -> a.finger() != b.finger() ? a.finger() - b.finger()
                        : a.note().string() - b.note().string())
                .map(placement -> names[placement.finger()] + " " + placement.note().string() + "弦"
                        + placement.note().fret())
                .collect(Collectors.joining(" / "));
        if (!usesBarre()) {
            return notes;
        }
        String barre = "人差し指 " + (barreFret + capo) + "フレット セーハ(1〜" + barreTo + "弦)";
        return notes.isEmpty() ? barre : barre + " / " + notes;
    }
}
