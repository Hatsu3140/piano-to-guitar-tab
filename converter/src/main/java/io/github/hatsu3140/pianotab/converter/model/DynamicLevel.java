package io.github.hatsu3140.pianotab.converter.model;

/**
 * 強弱記号の強さ（弱い順）。
 */
public enum DynamicLevel {
    PPP, PP, P, MP, MF, F, FF, FFF;

    /** 強弱記号の文字（"mf" など）から強さを求める。分からない記号なら null */
    public static DynamicLevel fromMark(String mark) {
        return switch (mark) {
            case "ppp", "pppp", "ppppp", "pppppp" -> PPP;
            case "pp" -> PP;
            case "p", "pf" -> P;
            case "mp" -> MP;
            case "mf" -> MF;
            case "f", "fp", "sf", "sfp", "sfpp", "sfz", "sffz", "fz", "rf", "rfz" -> F;
            case "ff", "sfzp" -> FF;
            case "fff", "ffff", "fffff", "ffffff" -> FFF;
            default -> null;
        };
    }
}
