package lab4.tip;

// const i niz
public class KompozitniTip extends Tip {
    public Tip subTip;

    public KompozitniTip(TipEnum tip, Tip subTip) {
        super(tip);
        if (!(tip == TipEnum.NIZ || tip == TipEnum.CONST)) {
            throw new Error("Unsupported composite type");
        }
        this.tip = tip;
        this.subTip = subTip;
    }

    public KompozitniTip(String tip, String subTip) {
        super(tip);
        if (!(this.tip == TipEnum.NIZ || this.tip == TipEnum.CONST)) {
            throw new Error("Unsupported composite type");
        }
        this.subTip = new Tip(subTip);
        // System.err.println(this.tip.toString() + " " + this.subTip.tip.toString());
    }

    public KompozitniTip(String nadTip, String tip, String subTip) {
        super(nadTip);
        if ((this.tip != TipEnum.NIZ)) {
            throw new Error("Unsupported composite type");
        }
        this.subTip = new KompozitniTip(tip, subTip);
        // System.err.println(
        //         this.tip.toString() + " " + this.subTip.tip.toString() + " "
        //                 + ((KompozitniTip) this.subTip).subTip.tip.toString());
    }

    @Override
    public boolean equals(Tip tip) {
        if (!(tip instanceof KompozitniTip)) {
            return false;
        }
        else {
            return this.tip == tip.tip && ((KompozitniTip) tip).subTip.equals(this.subTip);
        }
    }
}
