package io.github.hatsu3140.pianotab.converter.model;

import java.util.ArrayList;
import java.util.List;

/**
 * MusicXML から読み込んだ楽譜。音符を、どの段にあったかで分けて持つ。
 *
 * @param info  曲全体の情報
 * @param vocal 歌の段の音符（弾き語り譜でなければ空）
 * @param upper ピアノの上段（右手）の音符。段の区別がない楽譜では、歌以外のすべての音符
 * @param lower ピアノの下段（左手）の音符（段の区別がない楽譜では空）
 */
public record Score(SongInfo info, List<Note> vocal, List<Note> upper, List<Note> lower) {
    public Score {
        vocal = List.copyOf(vocal);
        upper = List.copyOf(upper);
        lower = List.copyOf(lower);
    }

    /** 段の区別がない楽譜 */
    public Score(SongInfo info, List<Note> notes) {
        this(info, List.of(), notes, List.of());
    }

    /** すべての音符（歌・上段・下段の順） */
    public List<Note> notes() {
        List<Note> all = new ArrayList<>(vocal);
        all.addAll(upper);
        all.addAll(lower);
        return all;
    }
}
