package lab4;


import lab4.tip.*;

public class Identifikator {
   public Tip tip;
   public String ime;
   public boolean l_izraz;

   public String labela;
   public String initalValue;

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

   public void setLabela(String labela) {
      this.labela = labela;
   }


}
