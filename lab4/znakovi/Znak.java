package lab4.znakovi;

import lab4.Node;

public abstract class Znak extends Node {
    
   // TODO check :)
   // gernerira isklucivo
   public Konstanta generira(KonstantaEnum k) {
      if(children.size() > 1)
         return null;
      if(children.size() == 0 ){
         if(this instanceof Konstanta){
            if( ((Konstanta) this ).konstantaTip == k ) {
               return (Konstanta) this;
            }
            else {
               return null;
            }
         }
         // vjv unreachable
         return null;
      }
      
      return ( (Znak) this.children.get(0) ).generira(k);
   }
}
