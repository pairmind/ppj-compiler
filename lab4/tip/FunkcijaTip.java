package lab4.tip;


// TODO: pripazit da se void tip predstavlja praznim arrayem
public class FunkcijaTip extends Tip {
    
    public Tip[] args;
    public Tip rval;

    public FunkcijaTip(Tip[] args, Tip rval){
        super(TipEnum.FUNKCIJA);
        if(args.length == 1 && args[0].equals(new Tip(TipEnum.VOID))){
            this.args = new Tip[0];
        }
        else {
            this.args = args;
        }
        this.rval = rval;
    }

    // TODO: reimplement if needed
    public boolean isVoidFunction(){
        return this.args.length == 0;
    }

    @Override
    public boolean equals(Tip tip){
        if( ! (tip instanceof FunkcijaTip))
            return false;
        throw new UnsupportedOperationException();
    }

}
