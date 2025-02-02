package lab3;

import lab3.tip.*;
import lab3.tip.TipEnum;

public class Identifikator {
   public Tip tip;
   public String ime;
   public boolean l_izraz;

   public Identifikator(Tip tip, String ime) {
      this.tip = tip;
      this.ime = ime;
      if(tip instanceof KompozitniTip || tip instanceof FunkcijaTip){
         l_izraz = false;
      }
      else {
         l_izraz = true;
      }
   }



}
