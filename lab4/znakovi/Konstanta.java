package lab4.znakovi;

public class Konstanta extends Znak {
    public KonstantaEnum konstantaTip;
    public int lineN;
    public String vrijednost;

    public Konstanta(KonstantaEnum tip, int lineN, String vrijednost) {
        this.konstantaTip = tip;
        this.lineN = lineN;
        this.vrijednost = vrijednost;
    }

    // actually useful
    public Konstanta(String tip, int lineN, String vrijednost) {
        switch (tip) {
            case "IDN":
                this.konstantaTip = KonstantaEnum.IDN;
                break;
            case "BROJ":
                this.konstantaTip = KonstantaEnum.BROJ;
                break;
            case "ZNAK":
                this.konstantaTip = KonstantaEnum.ZNAK;
                break;
            case "NIZ_ZNAKOVA":
                this.konstantaTip = KonstantaEnum.NIZ_ZNAKOVA;
                break;
            case "KR_BREAK":
                this.konstantaTip = KonstantaEnum.KR_BREAK;
                break;
            case "KR_CHAR":
                this.konstantaTip = KonstantaEnum.KR_CHAR;
                break;
            case "KR_CONST":
                this.konstantaTip = KonstantaEnum.KR_CONST;
                break;
            case "KR_CONTINUE":
                this.konstantaTip = KonstantaEnum.KR_CONTINUE;
                break;
            case "KR_ELSE":
                this.konstantaTip = KonstantaEnum.KR_ELSE;
                break;
            case "KR_FOR":
                this.konstantaTip = KonstantaEnum.KR_FOR;
                break;
            case "KR_IF":
                this.konstantaTip = KonstantaEnum.KR_IF;
                break;
            case "KR_INT":
                this.konstantaTip = KonstantaEnum.KR_INT;
                break;
            case "KR_RETURN":
                this.konstantaTip = KonstantaEnum.KR_RETURN;
                break;
            case "KR_VOID":
                this.konstantaTip = KonstantaEnum.KR_VOID;
                break;
            case "KR_WHILE":
                this.konstantaTip = KonstantaEnum.KR_WHILE;
                break;
            case "PLUS":
                this.konstantaTip = KonstantaEnum.PLUS;
                break;
            case "OP_INC":
                this.konstantaTip = KonstantaEnum.OP_INC;
                break;
            case "MINUS":
                this.konstantaTip = KonstantaEnum.MINUS;
                break;
            case "OP_DEC":
                this.konstantaTip = KonstantaEnum.OP_DEC;
                break;
            case "OP_PUTA":
                this.konstantaTip = KonstantaEnum.OP_PUTA;
                break;
            case "OP_DIJELI":
                this.konstantaTip = KonstantaEnum.OP_DIJELI;
                break;
            case "OP_MOD":
                this.konstantaTip = KonstantaEnum.OP_MOD;
                break;
            case "OP_PRIDRUZI":
                this.konstantaTip = KonstantaEnum.OP_PRIDRUZI;
                break;
            case "OP_LT":
                this.konstantaTip = KonstantaEnum.OP_LT;
                break;
            case "OP_LTE":
                this.konstantaTip = KonstantaEnum.OP_LTE;
                break;
            case "OP_GT":
                this.konstantaTip = KonstantaEnum.OP_GT;
                break;
            case "OP_GTE":
                this.konstantaTip = KonstantaEnum.OP_GTE;
                break;
            case "OP_EQ":
                this.konstantaTip = KonstantaEnum.OP_EQ;
                break;
            case "OP_NEQ":
                this.konstantaTip = KonstantaEnum.OP_NEQ;
                break;
            case "OP_NEG":
                this.konstantaTip = KonstantaEnum.OP_NEG;
                break;
            case "OP_TILDA":
                this.konstantaTip = KonstantaEnum.OP_TILDA;
                break;
            case "OP_I":
                this.konstantaTip = KonstantaEnum.OP_I;
                break;
            case "OP_ILI":
                this.konstantaTip = KonstantaEnum.OP_ILI;
                break;
            case "OP_BIN_I":
                this.konstantaTip = KonstantaEnum.OP_BIN_I;
                break;
            case "OP_BIN_ILI":
                this.konstantaTip = KonstantaEnum.OP_BIN_ILI;
                break;
            case "OP_BIN_XILI":
                this.konstantaTip = KonstantaEnum.OP_BIN_XILI;
                break;
            case "ZAREZ":
                this.konstantaTip = KonstantaEnum.ZAREZ;
                break;
            case "TOCKAZAREZ":
                this.konstantaTip = KonstantaEnum.TOCKAZAREZ;
                break;
            case "L_ZAGRADA":
                this.konstantaTip = KonstantaEnum.L_ZAGRADA;
                break;
            case "D_ZAGRADA":
                this.konstantaTip = KonstantaEnum.D_ZAGRADA;
                break;
            case "L_UGL_ZAGRADA":
                this.konstantaTip = KonstantaEnum.L_UGL_ZAGRADA;
                break;
            case "D_UGL_ZAGRADA":
                this.konstantaTip = KonstantaEnum.D_UGL_ZAGRADA;
                break;
            case "L_VIT_ZAGRADA":
                this.konstantaTip = KonstantaEnum.L_VIT_ZAGRADA;
                break;
            case "D_VIT_ZAGRADA":
                this.konstantaTip = KonstantaEnum.D_VIT_ZAGRADA;
                break;
            default:
                this.konstantaTip = null;
                break;
        }

        this.lineN = lineN;
        this.vrijednost = vrijednost;
    }

    @Override
    public String toString() {
        return this.konstantaTip.toString() + "(" + this.lineN + "," + this.vrijednost + ")";
    }

}
