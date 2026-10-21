package io.github.hatsu3140.pianotab.converter.fret;

import io.github.hatsu3140.pianotab.converter.model.TabNote;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * よく使うコードの押さえ方。ギターを弾く人なら手が覚えている形なので、
 * {@link Hand} のルールに当てはまらなくても押さえられるものとし、押さえやすさの評価でも易しいとする。
 *
 * <p>TAB 譜ではコードの全部の弦を鳴らすとは限らないので、コードの形の一部の弦だけを鳴らす場合も
 * 「その形を押さえている」とみなす。フレットはカポを基準に数える。
 * <ul>
 *   <li>開放弦を使うコード（C、G、D、A、E、Am、Em、Dm など）: 決まった場所で押さえる</li>
 *   <li>セーハのコード（6弦がルートの F 型、5弦がルートの B♭ 型など）: どのフレットにずらしてもよい</li>
 * </ul>
 */
public final class ChordShapes {

    /** 押さえない弦 */
    private static final int X = Integer.MIN_VALUE;

    /**
     * コードの形。弦は1弦から6弦の順。
     *
     * @param name    名前
     * @param frets   フレット（X は鳴らさない弦）。movable なら、セーハのフレットからの差
     * @param fingers 指（0 = 開放弦）
     * @param movable true ならセーハのコードで、どのフレットにずらしてもよい
     */
    record Shape(String name, int[] frets, int[] fingers, boolean movable) {
    }

    static final List<Shape> SHAPES = List.of(
            open("C", new int[] {0, 1, 0, 2, 3, X}, new int[] {0, 1, 0, 2, 3, 0}),
            open("C7", new int[] {0, 1, 3, 2, 3, X}, new int[] {0, 1, 4, 2, 3, 0}),
            open("D", new int[] {2, 3, 2, 0, X, X}, new int[] {2, 3, 1, 0, 0, 0}),
            open("Dm", new int[] {1, 3, 2, 0, X, X}, new int[] {1, 3, 2, 0, 0, 0}),
            open("D7", new int[] {2, 1, 2, 0, X, X}, new int[] {3, 1, 2, 0, 0, 0}),
            open("E", new int[] {0, 0, 1, 2, 2, 0}, new int[] {0, 0, 1, 3, 2, 0}),
            open("Em", new int[] {0, 0, 0, 2, 2, 0}, new int[] {0, 0, 0, 3, 2, 0}),
            open("E7", new int[] {0, 0, 1, 0, 2, 0}, new int[] {0, 0, 1, 0, 2, 0}),
            open("Fmaj7", new int[] {0, 1, 2, 3, X, X}, new int[] {0, 1, 2, 3, 0, 0}),
            open("G", new int[] {3, 0, 0, 0, 2, 3}, new int[] {3, 0, 0, 0, 1, 2}),
            open("G", new int[] {3, 0, 0, 0, 2, 3}, new int[] {4, 0, 0, 0, 2, 3}),
            open("G", new int[] {3, 3, 0, 0, 2, 3}, new int[] {4, 3, 0, 0, 1, 2}),
            open("G7", new int[] {1, 0, 0, 0, 2, 3}, new int[] {1, 0, 0, 0, 2, 3}),
            open("A", new int[] {0, 2, 2, 2, 0, X}, new int[] {0, 3, 2, 1, 0, 0}),
            open("Am", new int[] {0, 1, 2, 2, 0, X}, new int[] {0, 1, 3, 2, 0, 0}),
            open("A7", new int[] {0, 2, 0, 2, 0, X}, new int[] {0, 3, 0, 2, 0, 0}),
            open("Am7", new int[] {0, 1, 0, 2, 0, X}, new int[] {0, 1, 0, 2, 0, 0}),
            open("B7", new int[] {2, 0, 2, 1, 2, X}, new int[] {4, 0, 3, 1, 2, 0}),
            barre("F型セーハ", new int[] {0, 0, 1, 2, 2, 0}, new int[] {1, 1, 2, 4, 3, 1}),
            barre("Fm型セーハ", new int[] {0, 0, 0, 2, 2, 0}, new int[] {1, 1, 1, 4, 3, 1}),
            barre("F7型セーハ", new int[] {0, 0, 1, 0, 2, 0}, new int[] {1, 1, 2, 1, 3, 1}),
            barre("Fm7型セーハ", new int[] {0, 0, 0, 0, 2, 0}, new int[] {1, 1, 1, 1, 3, 1}),
            barre("B♭型セーハ", new int[] {0, 2, 2, 2, 0, X}, new int[] {1, 4, 3, 2, 1, 0}),
            barre("B♭m型セーハ", new int[] {0, 1, 2, 2, 0, X}, new int[] {1, 2, 4, 3, 1, 0}),
            barre("B♭7型セーハ", new int[] {0, 2, 0, 2, 0, X}, new int[] {1, 3, 1, 2, 1, 0}),
            barre("B♭m7型セーハ", new int[] {0, 1, 0, 2, 0, X}, new int[] {1, 2, 1, 3, 1, 0}));

    /** 易しいとみなすのは、3音以上をコードの形で押さえるとき（1〜2音ならどの形にも当てはまってしまう） */
    static final int MIN_NOTES_FOR_SHAPE = 3;

    private ChordShapes() {
    }

    private static Shape open(String name, int[] frets, int[] fingers) {
        return new Shape(name, frets, fingers, false);
    }

    private static Shape barre(String name, int[] frets, int[] fingers) {
        return new Shape(name, frets, fingers, true);
    }

    /**
     * notes がどれかのコードの形の一部なら、その形どおりの押さえ方を返す。
     *
     * @param capo         カポ
     * @param barreAllowed セーハを使ってよいか
     * @param maxFingers   使ってよい指の数
     */
    static List<Fingering> fingerings(List<TabNote> notes, int capo, boolean barreAllowed, int maxFingers) {
        List<Fingering> result = new ArrayList<>();
        for (Shape shape : SHAPES) {
            for (int base : bases(shape, notes, capo)) {
                Fingering fingering = place(shape, base, notes, capo);
                if (fingering == null || fingering.fingerCount() > maxFingers
                        || (fingering.usesBarre() && !barreAllowed)) {
                    continue;
                }
                if (result.stream().noneMatch(found -> found.placements().equals(fingering.placements()))) {
                    result.add(fingering);
                }
            }
        }
        return result;
    }

    /**
     * 押さえ方がコードの形どおりなら、その形の名前。1音だけ違う「似た形」なら、名前の後ろに「に近い」を付ける。
     * どちらでもなければ null
     */
    static String match(Fingering fingering, int capo) {
        List<TabNote> notes = fingering.placements().stream().map(Fingering.Placement::note).toList();
        if (notes.size() < MIN_NOTES_FOR_SHAPE) {
            return null;
        }
        String similar = null;
        for (Shape shape : SHAPES) {
            for (int base : bases(shape, notes, capo)) {
                int misses = 0;
                for (Fingering.Placement placement : fingering.placements()) {
                    if (!fits(shape, base, placement, capo)) {
                        misses++;
                    }
                }
                if (misses == 0) {
                    return shape.name();
                }
                if (misses == 1 && similar == null && notes.size() > MIN_NOTES_FOR_SHAPE) {
                    similar = shape.name() + "に近い";
                }
            }
        }
        return similar;
    }

    /** 形を置く場所の候補（セーハのコードはセーハのフレット、開放弦のコードは 0） */
    private static Set<Integer> bases(Shape shape, List<TabNote> notes, int capo) {
        Set<Integer> bases = new HashSet<>();
        if (!shape.movable()) {
            bases.add(0);
            return bases;
        }
        for (TabNote note : notes) {
            int offset = shape.frets()[note.string() - 1];
            int fret = note.fret() - capo;
            if (offset != X && fret > 0 && fret - offset >= 1) {
                bases.add(fret - offset);
            }
        }
        return bases;
    }

    private static boolean fits(Shape shape, int base, Fingering.Placement placement, int capo) {
        TabNote note = placement.note();
        int string = note.string() - 1;
        int offset = shape.frets()[string];
        return offset != X && note.fret() - capo == base + offset && placement.finger() == shape.fingers()[string];
    }

    private static Fingering place(Shape shape, int base, List<TabNote> notes, int capo) {
        List<Fingering.Placement> placements = new ArrayList<>();
        int barreTo = 0;
        int indexNotes = 0;
        for (TabNote note : notes) {
            int string = note.string() - 1;
            int offset = shape.frets()[string];
            if (offset == X || note.fret() - capo != base + offset) {
                return null;
            }
            int finger = shape.fingers()[string];
            if (finger == Fingering.INDEX) {
                indexNotes++;
                barreTo = Math.max(barreTo, note.string());
            }
            placements.add(new Fingering.Placement(note, finger));
        }
        boolean barred = shape.movable() && indexNotes > 1;
        String name = notes.size() >= MIN_NOTES_FOR_SHAPE ? shape.name() : null;
        return new Fingering(List.copyOf(placements), barred ? base : 0, barred ? barreTo : 0, name);
    }
}
