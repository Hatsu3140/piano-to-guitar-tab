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
 * 入門はベースなし、初級は小節の頭だけ、中級は拍の頭だけ、上級はほぼすべて残す
 * （上級でも、同じ拍の中でベースより上がる音は伴奏の刻みなので除く）。
 * 除いた音は和音の候補（harmony）に回す。
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
        List<Note> harmony = new ArrayList<>(arrangement.harmony());
        List<Measure> measures = arrangement.info().measures();
        int ticksPerQuarter = arrangement.info().ticksPerQuarter();
        Note previous = null;
        long previousBeatStart = -1;
        List<Note> sorted = new ArrayList<>(arrangement.bass());
        sorted.sort(Comparator.comparingLong(Note::startTick));
        for (Note note : sorted) {
            Measure measure = measureAt(measures, note.startTick());
            long offset = note.startTick() - measure.startTick();
            long beat = measure.beatTicks(ticksPerQuarter);
            long beatStart = measure.startTick() + offset / beat * beat;
            boolean keep = switch (rhythm) {
                case NONE -> false;
                case MEASURE -> offset == 0;
                case BEAT -> offset % beat == 0;
                case ALL -> previous == null || beatStart != previousBeatStart || note.pitch() <= previous.pitch();
            };
            if (keep) {
                bass.add(note);
                previous = note;
                previousBeatStart = beatStart;
            } else {
                harmony.add(note);
            }
        }
        return new Arrangement(arrangement.info(), arrangement.melody(), letRing(bass, measures), harmony);
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
