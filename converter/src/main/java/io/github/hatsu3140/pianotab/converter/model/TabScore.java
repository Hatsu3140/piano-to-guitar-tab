package io.github.hatsu3140.pianotab.converter.model;

import java.util.List;

/**
 * フレット割り当てが終わった TAB 譜。
 */
public record TabScore(int ticksPerQuarter, Tuning tuning, List<TabNote> notes) {
    public TabScore {
        notes = List.copyOf(notes);
    }
}
