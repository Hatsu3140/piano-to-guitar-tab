package io.github.hatsu3140.pianotab.converter.arrange;

import io.github.hatsu3140.pianotab.converter.Difficulty.BassRhythm;
import io.github.hatsu3140.pianotab.converter.model.Arrangement;
import io.github.hatsu3140.pianotab.converter.model.Measure;
import io.github.hatsu3140.pianotab.converter.model.Note;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 難易度に合わせてベースの音を減らす。
 * 入門はベースなし、初級は小節の頭だけ、中級は拍の頭だけ、上級はすべて残す。
 *
 * <p>ギターのベースは響かせるのが大事なので、残した音は次のベース音（または小節の終わり）まで
 * 伸ばす。ピアノの左手が短く刻んでいても、ギターではベースを押さえたままメロディを弾く形になる。
 */
public class BassSimplifier {

    private final BassRhythm rhythm;

    public BassSimplifier(BassRhythm rhythm) {
        this.rhythm = rhythm;
    }

    public Arrangement simplify(Arrangement arrangement) {
        List<Note> bass = new ArrayList<>();
        if (rhythm != BassRhythm.NONE) {
            List<Measure> measures = arrangement.info().measures();
            int ticksPerQuarter = arrangement.info().ticksPerQuarter();
            for (Note note : arrangement.bass()) {
                Measure measure = measureAt(measures, note.startTick());
                long offset = note.startTick() - measure.startTick();
                boolean keep = switch (rhythm) {
                    case MEASURE -> offset == 0;
                    case BEAT -> offset % beatTicks(measure, ticksPerQuarter) == 0;
                    default -> true;
                };
                if (keep) {
                    bass.add(note);
                }
            }
        }
        return new Arrangement(arrangement.info(), arrangement.melody(), letRing(bass, arrangement.info().measures()));
    }

    /** 各ベース音を、次のベース音が鳴るまで（ただし小節の終わりまで）伸ばす */
    static List<Note> letRing(List<Note> bass, List<Measure> measures) {
        List<Note> sorted = new ArrayList<>(bass);
        sorted.sort(Comparator.comparingLong(Note::startTick));
        List<Note> result = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Note note = sorted.get(i);
            Measure measure = measureAt(measures, note.startTick());
            long end = measure.startTick() + measure.lengthTicks();
            for (int j = i + 1; j < sorted.size(); j++) {
                if (sorted.get(j).startTick() > note.startTick()) {
                    end = Math.min(end, sorted.get(j).startTick());
                    break;
                }
            }
            long duration = Math.max(note.durationTicks(), end - note.startTick());
            result.add(new Note(note.pitch(), note.startTick(), duration));
        }
        return result;
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
