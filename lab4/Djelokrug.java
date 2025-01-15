package lab4;

import java.util.HashMap;
import java.util.Map;

import lab4.tip.FunkcijaTip;
import lab4.tip.Tip;
import lab4.tip.TipEnum;

/// TODO: mozda tu ne trebaju ic identifikatori neg neka slicna nova klasa (Deklaracija?)
/// Deklaracija
/// Varijabla extends Deklaracija
/// 
public class Djelokrug {
   public Map<String, Identifikator> varijable;          // TODO change to public
   public Map<String, IdentifikatorFunkcije> funkcije;   // TODO change to public
   public Djelokrug ugnjezdujuciDjelokrug;

   public TipDjelokruga tipDjelokruga;

   public Tip povratniTip; // samo za tipDjelokruga == FUNKCIJA

   public Djelokrug(Djelokrug ugnjezdujuciDjelokrug) {
      varijable = new HashMap<String, Identifikator>();
      funkcije = new HashMap<String, IdentifikatorFunkcije>();
      this.ugnjezdujuciDjelokrug = ugnjezdujuciDjelokrug;
      this.povratniTip = new Tip(TipEnum.VOID);
      this.tipDjelokruga = TipDjelokruga.OBICNI_BLOK;
   }

   public Djelokrug() {
      varijable = new HashMap<String, Identifikator>();
      funkcije = new HashMap<String, IdentifikatorFunkcije>();
      this.ugnjezdujuciDjelokrug = null;
      this.povratniTip = new Tip(TipEnum.VOID);
      this.tipDjelokruga = TipDjelokruga.OBICNI_BLOK;
   }

   public void setFunkcija(FunkcijaTip fTip){
      tipDjelokruga = TipDjelokruga.FUNKCIJA;
      povratniTip = fTip.rval;
   }

   public boolean sadrziLokalnuVarijablu(String ime){
      return lokalnaVarijabla(ime) != null;
   }

   public boolean sadrziVarijablu(String ime){
      return varijabla(ime) != null;
   }

   public boolean sadrziDeklaraciju(String ime){
      return sadrziVarijablu(ime) || funkcija(ime) != null;
   }

   /// tu je zbog produkcije <primarni_izraz> ::= IDN
   /// onemogucava razlikovanje izmedju varijable i istoimene funkcije
   public Identifikator deklaracija(String ime) {
      if(funkcija(ime) != null) {
         return funkcija(ime);
      }
      return varijabla(ime);
   }

   public Identifikator varijabla(String ime) {
      Identifikator id = varijable.get(ime);
      if(id == null) {
         if(ugnjezdujuciDjelokrug == null) {
            return null;
         }
         else {
            return ugnjezdujuciDjelokrug.varijabla(ime);
         }
      }
      return id;
   }

   public IdentifikatorFunkcije funkcija(String ime) {
      IdentifikatorFunkcije id = funkcije.get(ime);
      if(id == null) {
         if(ugnjezdujuciDjelokrug == null) {
            return null;
         }
         else {
            return ugnjezdujuciDjelokrug.funkcija(ime);
         }
      }
      return id;
   }

   public Identifikator lokalnaVarijabla(String ime) {
      return varijable.get(ime);
   }
   
   public boolean jeUnutarFunkcijePovratneVrijednosti(Tip rval) {
      if(tipDjelokruga == TipDjelokruga.FUNKCIJA && povratniTip.equals(rval)) {
         return true;
      }
      else if(ugnjezdujuciDjelokrug != null){
         return ugnjezdujuciDjelokrug.jeUnutarFunkcijePovratneVrijednosti(rval);
      }
      else {
         return false;
      }
   }

   public Tip povratniTipUgnjezdujuceFunkcije() {
      if(tipDjelokruga == TipDjelokruga.FUNKCIJA) {
         return povratniTip;
      }
      else if(ugnjezdujuciDjelokrug != null){
         return ugnjezdujuciDjelokrug.povratniTipUgnjezdujuceFunkcije();
      }
      else {
         return null;
      }
   }

   public boolean jeUnutarPetlje(){
      if(tipDjelokruga == TipDjelokruga.PETLJA){
         return true;
      }
      else if(ugnjezdujuciDjelokrug != null){
         return ugnjezdujuciDjelokrug.jeUnutarPetlje();
      }
      else {
         return false;
      }
   }
   
   public void zabiljeziIdentifikator(String ime, Tip tip){
      if(tip instanceof FunkcijaTip) {
         funkcije.put(ime, new IdentifikatorFunkcije((FunkcijaTip) tip, ime));
      }
      else {
         varijable.put(ime, new Identifikator(tip, ime));
      }
   }
}
