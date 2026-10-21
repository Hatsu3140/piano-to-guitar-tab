package io.github.hatsu3140.pianotab.converter.arrange;

import io.github.hatsu3140.pianotab.converter.Difficulty.BassRhythm;
import io.github.hatsu3140.pianotab.converter.model.Arrangement;
import io.github.hatsu3140.pianotab.converter.model.Measure;
import io.github.hatsu3140.pianotab.converter.model.Note;

import java.util.ArrayList;
import java.util.List;

/**
 * 難易度に合わせてベースの音を減らす。
 * 入門はベースなし、初級は小節の頭だけ、中級は拍の頭だけ、上級はすべて残す。
 */
public class BassSimplifier {

    private final BassRhythm rhythm;

    public BassSimplifier(BassRhythm rhythm) {
        this.rhythm = rhythm;
    }

    public Arrangement simplify(Arrangement arrangement) {
        if (rhythm == BassRhythm.ALL) {
            return arrangement;
        }
        List<Note> bass = new ArrayList<>();
        if (rhythm != BassRhythm.NONE) {
            List<Measure> measures = arrangement.info().measures();
            int ticksPerQuarter = arrangement.info().ticksPerQuarter();
            for (Note note : arrangement.bass()) {
                Measure measure = measureAt(measures, note.startTick());
                long offset = note.startTick() - measure.startTick();
                boolean keep = rhythm == BassRhythm.MEASURE
                        ? offset == 0
                        : offset % beatTicks(measure, ticksPerQuarter) == 0;
                if (keep) {
                    bass.add(note);
                }
            }
        }
        return new Arrangement(arrangement.info(), arrangement.melody(), bass);
    }

    /** 1拍の長さ。6/8 拍子のような複合拍子は付点4分音符を1拍とする */
    static long beatTicks(Measure measure, int ticksPerQuarter) {
        long unit = (long) ticksPerQuarter * 4 / measure.beatType();
        boolean compound = measure.beatType() == 8 && measure.beats() % 3 == 0 && measure.beats() > 3;
        return compound ? unit * 3 : unit;
    }

    private static Measure measureAt(List<Measure> measures, long tick) {
        Measure found = measures.get(0);
        for (Measure measure : measures) {
            if (measure.startTick() > tick) {
                break;
            }
            found = measure;
        }
        return found;
    }
}
