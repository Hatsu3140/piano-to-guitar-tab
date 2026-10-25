package io.github.hatsu3140.pianotab.web;

import java.util.List;

/**
 * チューニングとカポの提案を画面に返す JSON。良い順に並べる。
 *
 * @param suggestions 提案の一覧（先頭が一番のおすすめ）
 */
public record SuggestResponse(List<Item> suggestions) {

    /**
     * @param tuning  チューニングの名前（画面の選択肢の value と同じ）
     * @param capo    カポの位置（0 = カポなし）
     * @param reasons おすすめする理由
     */
    public record Item(String tuning, int capo, List<String> reasons) {
    }
}
