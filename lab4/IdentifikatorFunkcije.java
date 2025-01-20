package lab4;


import lab4.tip.FunkcijaTip;

public class IdentifikatorFunkcije extends Identifikator {

   public boolean definirana = false;

   public String kodTjelaFunkcije;    // TODO Jel to opce pametno tu stavit il napravit nekak drukcije

   public IdentifikatorFunkcije(FunkcijaTip tip, String ime) {
      super(tip, ime);
   }

   

}
