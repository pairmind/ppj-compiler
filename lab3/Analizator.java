package lab3;
import java.util.Arrays;

import lab3.tip.*;
import lab3.znakovi.*;

public class Analizator {
   
    private Djelokrug lokalniDjelokrug;
    private Djelokrug globalniDjelokrug;

    public Analizator(){
        globalniDjelokrug = new Djelokrug();
        lokalniDjelokrug = globalniDjelokrug;

    }

    public void analiziraj(PrijevodnaJedinica prijevodnaJedinica) {
        // System.out.println("ANALIZIRAM");
        provjeri(prijevodnaJedinica);
        // TODO provjere nakon stabla
        // TODO zaustaviti program nakon greske
    }


    private void deklarirajFunkciju(String ime, FunkcijaTip tip){
        lokalniDjelokrug.zabiljeziIdentifikator(ime, tip);
    }

    private void definirajFunkciju(String ime, FunkcijaTip tip){
        deklarirajFunkciju(ime, tip);
        /// definirajFunkciju ce se jedino ikad zvati iz globalnog djelokruga 
        /// tako da ce globalniDjelokrug.funkcija(ime) zasigurno postojati 
        /// iako deklarirajFunkciju radi nad lokalnim djelokrugom
        globalniDjelokrug.funkcija(ime).definirana = true;
    }

    public boolean postojiDefiniranaFunkcija(String ime) {
        IdentifikatorFunkcije funkcija = globalniDjelokrug.funkcija(ime);
        if( funkcija == null ) {
            return false;
        }
        return globalniDjelokrug.funkcija(ime).definirana;
    }

    public boolean postojiDeklariranaFunkcija(String ime) {
        return lokalniDjelokrug.funkcija(ime) != null;
    }

    private void assertOrError(boolean condition, Node mistake) {
        if (!condition) {
            ispisiError(mistake);
            // TODO: stop everything
        }
    }

    // TODO: check if works
    private void ispisiError(Node mistake) {

        System.out.printf(mistake.toString() + " ::=");

        for (Node child : mistake.children) {
            System.out.printf(" " + child.toString());
        }

        System.out.printf("\n");

        // izadji iz programa
        System.exit(0);
    }

    private void provjeri(PrimarniIzraz iz) {
        if (iz.children.get(0) instanceof Konstanta) {
            Konstanta c = (Konstanta) iz.children.get(0);

            if (c.konstantaTip == KonstantaEnum.IDN) {
                // <primarni_izraz> ::= IDN
                Konstanta idn = (Konstanta) iz.children.get(0);
                assertOrError(lokalniDjelokrug.sadrziDeklaraciju(idn.vrijednost), iz);
                Identifikator identifikator = lokalniDjelokrug.deklaracija(idn.vrijednost);
                iz.tip = identifikator.tip;
                iz.l_izraz = identifikator.l_izraz;
            } else if (c.konstantaTip == KonstantaEnum.BROJ) {
                // <primarni_izraz> ::= BROJ
                try {
                    Integer.parseInt(c.vrijednost);
                }
                catch (Exception e) {
                    ispisiError(iz);  // integer izvan range-a (32 bit)
                }

                iz.tip = new Tip(TipEnum.INT);
                iz.l_izraz = false;
            } else if (c.konstantaTip == KonstantaEnum.ZNAK) {
                // <primarni_izraz> ::= ZNAK
                // TODO: provjerit metodu
                String chr = c.vrijednost.substring(1, c.vrijednost.length() - 1);
                if(chr.equals("\\t") || chr.equals("\\n") || chr.equals("\\0") 
                || chr.equals("\\'") || chr.equals("\\\"") || chr.equals("\\\\")
                || chr.length() == 1 && ((int)chr.charAt(0)) >= 0 && ((int)chr.charAt(0)) < 128){
                    iz.tip = new Tip(TipEnum.CHAR);
                    iz.l_izraz = false;
                }
                else {
                    ispisiError(iz);  // invalid char
                }

            } else if (c.konstantaTip == KonstantaEnum.NIZ_ZNAKOVA) {
                // <primarni_izraz> ::= NIZ_ZNAKOVA
                String str = c.vrijednost.substring(1, c.vrijednost.length() - 1);
                for(int i = 0; i < str.length(); i ++){
                    if(str.charAt(i) == '\\') {
                        i++;
                        try {
                            char a = str.charAt(i);
                            if(a != 't' && a != 'n' && a != '0' && a != '\'' && a != '"' && a != '\\') {
                                ispisiError(iz);
                            }
                        }
                        catch (IndexOutOfBoundsException e) {
                            ispisiError(iz);
                        }
                    }
                    if( ! ( ((int)str.charAt(i)) >= 0 && ((int)str.charAt(i)) < 128 ) ){
                        ispisiError(iz);
                    }
                }
                iz.tip = new KompozitniTip(TipEnum.NIZ, new KompozitniTip(TipEnum.CONST, new Tip(TipEnum.CHAR)));
                iz.l_izraz = false;
            } else if (c.konstantaTip == KonstantaEnum.L_ZAGRADA) {
                // <<primarni_izraz> ::= L_ZAGRADA <izraz> D_ZAGRADA
                Izraz izraz = (Izraz) iz.children.get(1);

                provjeri(izraz);

                iz.tip = izraz.tip;
                iz.l_izraz = izraz.l_izraz;
            } else {
                // Unreachable code
            }

        } else {
            // Unreachable code
        }

    }

    public void provjeri(PostfiksIzraz iz) {
        if (iz.children.size() == 1 && iz.children.get(0) instanceof PrimarniIzraz) {
            // <postfiks_izraz> ::= <primarni_izraz>
            PrimarniIzraz izraz = (PrimarniIzraz) iz.children.get(0);
            provjeri(izraz);

            iz.tip = izraz.tip;
            iz.l_izraz = izraz.l_izraz;
        }
        else if (iz.children.get(0) instanceof PostfiksIzraz) {
            PostfiksIzraz postfiksIzraz = (PostfiksIzraz) iz.children.get(0);
            if (iz.children.get(1) instanceof Konstanta) {
                Konstanta k1 = (Konstanta) iz.children.get(1);
                if (k1.konstantaTip == KonstantaEnum.L_UGL_ZAGRADA) {
                    // <postfiks_izraz> ::= <postfiks_izraz> L_UGL_ZAGRADA <izraz> D_UGL_ZAGRADA
                    Izraz izraz = (Izraz) iz.children.get(2);
                    
                    provjeri(postfiksIzraz);
                    assertOrError(Tip.isNizX(postfiksIzraz.tip), iz);
                    provjeri(izraz);
                    assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(izraz.tip, new Tip(TipEnum.INT)), iz);

                    Tip X = ((KompozitniTip) postfiksIzraz.tip).subTip;
                    iz.tip = X;
                    iz.l_izraz = !Tip.isConstT(X);
                } else if (k1.konstantaTip == KonstantaEnum.L_ZAGRADA && iz.children.size() == 3) {
                    // <postfiks_izraz> ::= <postfiks_izraz> L_ZAGRADA D_ZAGRADA

                    provjeri(postfiksIzraz);
                    assertOrError(postfiksIzraz.tip instanceof FunkcijaTip, iz);
                    FunkcijaTip funkcijaTip = (FunkcijaTip) postfiksIzraz.tip;
                    assertOrError(funkcijaTip.isVoidFunction(), iz);

                    iz.tip = funkcijaTip.rval;
                    iz.l_izraz = false;
                } else if (k1.konstantaTip == KonstantaEnum.L_ZAGRADA && iz.children.size() == 4) {
                    // <postfiks_izraz> ::= <postfiks_izraz> L_ZAGRADA <lista_argumenata> D_ZAGRADA

                    ListaArgumenata listaArgumenata = (ListaArgumenata) iz.children.get(2);
                    provjeri(postfiksIzraz);
                    provjeri(listaArgumenata);
                    assertOrError(postfiksIzraz.tip instanceof FunkcijaTip, iz);
                    FunkcijaTip funkcijaTip = (FunkcijaTip) postfiksIzraz.tip;
                    assertOrError(listaArgumenata.tipovi.length == funkcijaTip.args.length, iz);
                    for (int i = 0; i < listaArgumenata.tipovi.length; i++) {
                        assertOrError(listaArgumenata.tipovi[i].equals(funkcijaTip.args[i]), iz);
                    }

                    iz.tip = funkcijaTip.rval;
                    iz.l_izraz = false;

                } else if (k1.konstantaTip == KonstantaEnum.OP_INC || k1.konstantaTip == KonstantaEnum.OP_DEC) {
                    // <postfiks_izraz> ::= <postfiks_izraz> (OP_INC | OP_DEC)
                    provjeri(postfiksIzraz);
                    assertOrError(postfiksIzraz.l_izraz == true, iz);
                    assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(postfiksIzraz.tip, new Tip(TipEnum.INT)), iz);

                    iz.tip = new Tip(TipEnum.INT);
                    iz.l_izraz = false;
                }
            }
        }
    }

    public void provjeri(ListaArgumenata la) {
        if (la.children.get(0) instanceof IzrazPridruzivanja) {
            // <lista_argumenata> ::= <izraz_pridruzivanja>
            IzrazPridruzivanja izrazPridruzivanja = (IzrazPridruzivanja) la.children.get(0);
            provjeri(izrazPridruzivanja);

            Tip[] tipovi = { izrazPridruzivanja.tip };
            la.tipovi = tipovi;
        } else if (la.children.get(0) instanceof ListaArgumenata) {
            // <lista_argumenata> ::= <lista_argumenata> ZAREZ <izraz_pridruzivanja>
            ListaArgumenata listaArgumenata = (ListaArgumenata) la.children.get(0);
            IzrazPridruzivanja izrazPridruzivanja = (IzrazPridruzivanja) la.children.get(2);

            provjeri(listaArgumenata);
            provjeri(izrazPridruzivanja);

            Tip[] tipovi = new Tip[listaArgumenata.tipovi.length + 1];
            for (int i = 0; i < listaArgumenata.tipovi.length; i++) {
                tipovi[i] = listaArgumenata.tipovi[i];
            }
            tipovi[listaArgumenata.tipovi.length] = izrazPridruzivanja.tip;

            la.tipovi = tipovi;
        }
    }

    public void provjeri(UnarniIzraz ui) {
        if (ui.children.get(0) instanceof PostfiksIzraz) {
            // <unarni_izraz> ::= <postfiks_izraz>
            PostfiksIzraz postfiksIzraz = (PostfiksIzraz) ui.children.get(0);

            provjeri(postfiksIzraz);

            ui.tip = postfiksIzraz.tip;
            ui.l_izraz = postfiksIzraz.l_izraz;
        } else if (ui.children.get(0) instanceof Konstanta) {
            // <unarni_izraz> ::= (OP_INC | OP_DEC) <unarni_izraz>
            // zasigurno je OP_INC ili OP_DEC prema gramatickim pravilima pa je provjera
            // suvisna
            UnarniIzraz unarniIzraz = (UnarniIzraz) ui.children.get(1);

            provjeri(unarniIzraz);
            assertOrError(unarniIzraz.l_izraz == true, ui);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(unarniIzraz.tip, new Tip(TipEnum.INT)), ui);

            ui.tip = new Tip(TipEnum.INT);
            ui.l_izraz = false;
        } else if (ui.children.get(0) instanceof UnarniOperator) {
            // <unarni_izraz> ::= <unarni_operator> <cast_izraz>
            // UnarniOperator unarniOperator = (UnarniOperator) ui.children.get(0);
            CastIzraz castIzraz = (CastIzraz) ui.children.get(1);

            provjeri(castIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(castIzraz.tip, new Tip(TipEnum.INT)), ui);

            ui.tip = new Tip(TipEnum.INT);
            ui.l_izraz = false;
        }
    }

    public void provjeri(CastIzraz iz) {
        if (iz.children.get(0) instanceof UnarniIzraz) {
            // <cast_izraz> ::= <unarni_izraz>
            UnarniIzraz unarniIzraz = (UnarniIzraz) iz.children.get(0);

            provjeri(unarniIzraz);

            iz.tip = unarniIzraz.tip;
            iz.l_izraz = unarniIzraz.l_izraz;
        } else if (iz.children.get(0) instanceof Konstanta) {
            // zasigurno L_ZAGRADA
            // <cast_izraz> ::= L_ZAGRADA <ime_tipa> D_ZAGRADA <cast_izraz>
            ImeTipa imeTipa = (ImeTipa) iz.children.get(1);
            CastIzraz castIzraz = (CastIzraz) iz.children.get(3);

            provjeri(imeTipa);
            provjeri(castIzraz);
            assertOrError(Tip.seMozePretvoritiIzU(castIzraz.tip, imeTipa.tip), iz);

            iz.tip = imeTipa.tip;
            iz.l_izraz = false;
        }
    }

    public void provjeri(ImeTipa iz) {
        if (iz.children.get(0) instanceof SpecifikatorTipa) {
            // <ime_tipa> ::= <specifikator_tipa>
            SpecifikatorTipa specifikatorTipa = (SpecifikatorTipa) iz.children.get(0);

            provjeri(specifikatorTipa);

            iz.tip = specifikatorTipa.tip;
        } else if (iz.children.get(0) instanceof Konstanta) { // KR_CONST
            // <ime_tipa> ::= KR_CONST <specifikator_tipa>
            SpecifikatorTipa specifikatorTipa = (SpecifikatorTipa) iz.children.get(1);

            provjeri(specifikatorTipa);
            assertOrError(!specifikatorTipa.tip.equals(new Tip(TipEnum.VOID)), iz);

            iz.tip = new KompozitniTip(TipEnum.CONST, specifikatorTipa.tip);
        }
    }

    public void provjeri(SpecifikatorTipa iz) {
        if (iz.children.get(0) instanceof Konstanta) {
            Konstanta konstanta = (Konstanta) iz.children.get(0);
            switch (konstanta.konstantaTip) {
                case KR_VOID:
                    // <specifikator_tipa> ::= KR_VOID
                    iz.tip = new Tip(TipEnum.VOID);
                    break;
                case KR_CHAR:
                    // <specifikator_tipa> ::= KR_CHAR
                    iz.tip = new Tip(TipEnum.CHAR);
                    break;
                case KR_INT:
                    // <specifikator_tipa> ::= KR_INT
                    iz.tip = new Tip(TipEnum.INT);
                    break;
                default:
                    break;
            }
        }
    }

    public void provjeri(MultiplikativniIzraz iz) {
        if (iz.children.get(0) instanceof CastIzraz) {
            // <multiplikativni_izraz> ::= <cast_izraz>
            CastIzraz castIzraz = (CastIzraz) iz.children.get(0);

            provjeri(castIzraz);

            iz.tip = castIzraz.tip;
            iz.l_izraz = castIzraz.l_izraz;
        } else if (iz.children.get(0) instanceof MultiplikativniIzraz) {
            // <multiplikativni_izraz> ::= <multiplikativni_izraz> (OP_PUTA | OP_DIJELI |
            // OP_MOD) <cast_izraz>
            MultiplikativniIzraz multiplikativniIzraz = (MultiplikativniIzraz) iz.children.get(0);
            CastIzraz castIzraz = (CastIzraz) iz.children.get(2);

            provjeri(multiplikativniIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(multiplikativniIzraz.tip, new Tip(TipEnum.INT)), iz);
            provjeri(castIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(castIzraz.tip, new Tip(TipEnum.INT)), iz);

            iz.tip = new Tip(TipEnum.INT);
            iz.l_izraz = false;
        }
    }

    public void provjeri(AditivniIzraz iz) {
        if (iz.children.get(0) instanceof MultiplikativniIzraz) {
            // <aditivni_izraz> ::= <multiplikativni_izraz>
            MultiplikativniIzraz multiplikativniIzraz = (MultiplikativniIzraz) iz.children.get(0);

            provjeri(multiplikativniIzraz);

            iz.tip = multiplikativniIzraz.tip;
            iz.l_izraz = multiplikativniIzraz.l_izraz;
        } else if (iz.children.get(0) instanceof AditivniIzraz) {
            // <aditivni_izraz> ::= <aditivni_izraz> (PLUS | MINUS) <multiplikativni_izraz>
            AditivniIzraz aditivniIzraz = (AditivniIzraz) iz.children.get(0);
            MultiplikativniIzraz multiplikativniIzraz = (MultiplikativniIzraz) iz.children.get(2);

            provjeri(aditivniIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(aditivniIzraz.tip, new Tip(TipEnum.INT)), iz);
            provjeri(multiplikativniIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(multiplikativniIzraz.tip, new Tip(TipEnum.INT)), iz);

            iz.tip = new Tip(TipEnum.INT);
            iz.l_izraz = false;
        }
    }

    public void provjeri(OdnosniIzraz iz) {
        if (iz.children.get(0) instanceof AditivniIzraz) {
            // <odnosni_izraz> ::= <aditivni_izraz>
            AditivniIzraz aditivniIzraz = (AditivniIzraz) iz.children.get(0);

            provjeri(aditivniIzraz);

            iz.tip = aditivniIzraz.tip;
            iz.l_izraz = aditivniIzraz.l_izraz;
        } else if (iz.children.get(0) instanceof OdnosniIzraz) {
            // <odnosni_izraz> ::= <odnosni_izraz> (OP_LT | OP_GT | OP_LTE | OP_GTE)
            // <aditivni_izraz>
            OdnosniIzraz odnosniIzraz = (OdnosniIzraz) iz.children.get(0);
            AditivniIzraz aditivniIzraz = (AditivniIzraz) iz.children.get(2);

            provjeri(odnosniIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(odnosniIzraz.tip, new Tip(TipEnum.INT)), iz);
            provjeri(aditivniIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(aditivniIzraz.tip, new Tip(TipEnum.INT)), iz);

            iz.tip = new Tip(TipEnum.INT);
            iz.l_izraz = false;
        }
    }

    public void provjeri(JednakosniIzraz iz) {
        if (iz.children.get(0) instanceof OdnosniIzraz) {
            // <jednakosni_izraz> ::= <odnosni_izraz>
            OdnosniIzraz odnosniIzraz = (OdnosniIzraz) iz.children.get(0);

            provjeri(odnosniIzraz);

            iz.tip = odnosniIzraz.tip;
            iz.l_izraz = odnosniIzraz.l_izraz;
        } else if (iz.children.get(0) instanceof JednakosniIzraz) {
            // <jednakosni_izraz> ::= <jednakosni_izraz> (OP_EQ | OP_NEQ) <odnosni_izraz>
            JednakosniIzraz jednakosniIzraz = (JednakosniIzraz) iz.children.get(0);
            OdnosniIzraz odnosniIzraz = (OdnosniIzraz) iz.children.get(2);

            provjeri(jednakosniIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(jednakosniIzraz.tip, new Tip(TipEnum.INT)), iz);
            provjeri(odnosniIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(odnosniIzraz.tip, new Tip(TipEnum.INT)), iz);

            iz.tip = new Tip(TipEnum.INT);
            iz.l_izraz = false;
        }
    }

    public void provjeri(BinIIzraz iz) {
        if (iz.children.get(0) instanceof JednakosniIzraz) {
            // <bin_i_izraz> ::= <jednakosni_izraz>
            JednakosniIzraz jednakosniIzraz = (JednakosniIzraz) iz.children.get(0);

            provjeri(jednakosniIzraz);

            iz.tip = jednakosniIzraz.tip;
            iz.l_izraz = jednakosniIzraz.l_izraz;
        } else if (iz.children.get(0) instanceof BinIIzraz) {
            // <bin_i_izraz> ::= <bin_i_izraz> OP_BIN_I <jednakosni_izraz>
            BinIIzraz binIIzraz = (BinIIzraz) iz.children.get(0);
            JednakosniIzraz jednakosniIzraz = (JednakosniIzraz) iz.children.get(2);

            provjeri(binIIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(binIIzraz.tip, new Tip(TipEnum.INT)), iz);
            provjeri(jednakosniIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(jednakosniIzraz.tip, new Tip(TipEnum.INT)), iz);

            iz.tip = new Tip(TipEnum.INT);
            iz.l_izraz = false;
        }
    }

    public void provjeri(BinXiliIzraz iz) {
        if (iz.children.get(0) instanceof BinIIzraz) {
            // <bin_xili_izraz> ::= <bin_i_izraz>
            BinIIzraz binIIzraz = (BinIIzraz) iz.children.get(0);

            provjeri(binIIzraz);

            iz.tip = binIIzraz.tip;
            iz.l_izraz = binIIzraz.l_izraz;
        } else if (iz.children.get(0) instanceof BinXiliIzraz) {
            // <bin_xili_izraz> ::= <bin_xili_izraz> OP_BIN_XILI <bin_i_izraz>
            BinXiliIzraz binXiliIzraz = (BinXiliIzraz) iz.children.get(0);
            BinIIzraz binIIzraz = (BinIIzraz) iz.children.get(2);

            provjeri(binXiliIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(binXiliIzraz.tip, new Tip(TipEnum.INT)), iz);
            provjeri(binIIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(binIIzraz.tip, new Tip(TipEnum.INT)), iz);

            iz.tip = new Tip(TipEnum.INT);
            iz.l_izraz = false;
        }
    }

    public void provjeri(BinIliIzraz iz) {
        if (iz.children.get(0) instanceof BinXiliIzraz) {
            // <bin_ili_izraz> ::= <bin_xili_izraz>
            BinXiliIzraz binXiliIzraz = (BinXiliIzraz) iz.children.get(0);

            provjeri(binXiliIzraz);

            iz.tip = binXiliIzraz.tip;
            iz.l_izraz = binXiliIzraz.l_izraz;
        } else if (iz.children.get(0) instanceof BinIliIzraz) {
            // <bin_ili_izraz> ::= <bin_ili_izraz> OP_BIN_ILI <bin_xili_izraz>
            BinIliIzraz binIliIzraz = (BinIliIzraz) iz.children.get(0);
            BinXiliIzraz binXiliIzraz = (BinXiliIzraz) iz.children.get(2);

            provjeri(binIliIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(binIliIzraz.tip, new Tip(TipEnum.INT)), iz);
            provjeri(binXiliIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(binXiliIzraz.tip, new Tip(TipEnum.INT)), iz);

            iz.tip = new Tip(TipEnum.INT);
            iz.l_izraz = false;
        }
    }

    public void provjeri(LogIIzraz iz) {
        if (iz.children.get(0) instanceof BinIliIzraz) {
            // <log_i_izraz> ::= <bin_ili_izraz>
            BinIliIzraz binIliIzraz = (BinIliIzraz) iz.children.get(0);

            provjeri(binIliIzraz);

            iz.tip = binIliIzraz.tip;
            iz.l_izraz = binIliIzraz.l_izraz;
        } else if (iz.children.get(0) instanceof LogIIzraz) {
            // <log_i_izraz> ::= <log_i_izraz> OP_I <bin_ili_izraz>
            LogIIzraz logIIzraz = (LogIIzraz) iz.children.get(0);
            BinIliIzraz binIliIzraz = (BinIliIzraz) iz.children.get(2);

            provjeri(logIIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(logIIzraz.tip, new Tip(TipEnum.INT)), iz);
            provjeri(binIliIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(binIliIzraz.tip, new Tip(TipEnum.INT)), iz);

            iz.tip = new Tip(TipEnum.INT);
            iz.l_izraz = false;
        }
    }

    public void provjeri(LogIliIzraz iz) {
        if (iz.children.get(0) instanceof LogIIzraz) {
            // <log_ili_izraz> ::= <log_i_izraz>
            LogIIzraz logIIzraz = (LogIIzraz) iz.children.get(0);

            provjeri(logIIzraz);

            iz.tip = logIIzraz.tip;
            iz.l_izraz = logIIzraz.l_izraz;
        } else if (iz.children.get(0) instanceof LogIliIzraz) {
            // <log_ili_izraz> ::= <log_ili_izraz> OP_ILI <log_i_izraz>
            LogIliIzraz logIliIzraz = (LogIliIzraz) iz.children.get(0);
            LogIIzraz logIIzraz = (LogIIzraz) iz.children.get(2);

            provjeri(logIliIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(logIliIzraz.tip, new Tip(TipEnum.INT)), iz);
            provjeri(logIIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(logIIzraz.tip, new Tip(TipEnum.INT)), iz);

            iz.tip = new Tip(TipEnum.INT);
            iz.l_izraz = false;
        }
    }

    public void provjeri(IzrazPridruzivanja iz) {
        if (iz.children.get(0) instanceof LogIliIzraz) {
            // <izraz_pridruzivanja> ::= <log_ili_izraz>
            LogIliIzraz logIliIzraz = (LogIliIzraz) iz.children.get(0);

            provjeri(logIliIzraz);

            iz.tip = logIliIzraz.tip;
            iz.l_izraz = logIliIzraz.l_izraz;
        } else if (iz.children.get(0) instanceof PostfiksIzraz) {
            // <izraz_pridruzivanja> ::= <postfiks_izraz> OP_PRIDRUZI <izraz_pridruzivanja>
            PostfiksIzraz postfiksIzraz = (PostfiksIzraz) iz.children.get(0);
            IzrazPridruzivanja izrazPridruzivanja = (IzrazPridruzivanja) iz.children.get(2);

            provjeri(postfiksIzraz);
            assertOrError(postfiksIzraz.l_izraz == true, iz);
            provjeri(izrazPridruzivanja);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(izrazPridruzivanja.tip, postfiksIzraz.tip), iz);

            iz.tip = postfiksIzraz.tip;
            iz.l_izraz = false;
        }
    }

    public void provjeri(Izraz iz) {
        if (iz.children.get(0) instanceof IzrazPridruzivanja) {
            // <izraz> ::= <izraz_pridruzivanja>
            IzrazPridruzivanja izrazPridruzivanja = (IzrazPridruzivanja) iz.children.get(0);

            provjeri(izrazPridruzivanja);

            iz.tip = izrazPridruzivanja.tip;
            iz.l_izraz = izrazPridruzivanja.l_izraz;
        } else if (iz.children.get(0) instanceof Izraz) {
            // <izraz> ::= <izraz> ZAREZ <izraz_pridruzivanja>
            Izraz izraz = (Izraz) iz.children.get(0);
            IzrazPridruzivanja izrazPridruzivanja = (IzrazPridruzivanja) iz.children.get(2);

            provjeri(izraz);
            provjeri(izrazPridruzivanja);

            iz.tip = izraz.tip;
            iz.l_izraz = false;
        }
    }

    public void provjeri(SlozenaNaredba na) {

        if (na.children.get(1) instanceof ListaNaredbi) {
            // <slozena_naredba> ::= L_VIT_ZAGRADA <lista_naredbi> D_VIT_ZAGRADA
            ListaNaredbi listaNaredbi = (ListaNaredbi) na.children.get(1);

            provjeri(listaNaredbi);
        } else if (na.children.get(1) instanceof ListaDeklaracija) {
            // <slozena_naredba> ::= L_VIT_ZAGRADA <lista_deklaracija> <lista_naredbi>
            // D_VIT_ZAGRADA
            ListaDeklaracija listaDeklaracija = (ListaDeklaracija) na.children.get(1);
            ListaNaredbi listaNaredbi = (ListaNaredbi) na.children.get(2);

            provjeri(listaDeklaracija);
            provjeri(listaNaredbi);
        }
    }

    public void provjeri(ListaNaredbi na) {
        if (na.children.get(0) instanceof Naredba) {
            // <lista_naredbi> ::= <naredba>
            Naredba naredba = (Naredba) na.children.get(0);

            provjeri(naredba);
        } else if (na.children.get(0) instanceof ListaNaredbi) {
            // <lista_naredbi> ::= <lista_naredbi> <naredba>
            ListaNaredbi listaNaredbi = (ListaNaredbi) na.children.get(0);
            Naredba naredba = (Naredba) na.children.get(1);

            provjeri(listaNaredbi);
            provjeri(naredba);
        }
    }

    public void provjeri(Naredba na) {
        if (na.children.get(0) instanceof SlozenaNaredba) {
            SlozenaNaredba naredba = (SlozenaNaredba) na.children.get(0);
            // slozena naredba u novom djelokrugu
            lokalniDjelokrug = new Djelokrug(lokalniDjelokrug);
            if(na.parent instanceof NaredbaPetlje){
                lokalniDjelokrug.tipDjelokruga = TipDjelokruga.PETLJA;
            }
            provjeri(naredba);
            lokalniDjelokrug = lokalniDjelokrug.ugnjezdujuciDjelokrug;
        } else if (na.children.get(0) instanceof IzrazNaredba) {
            IzrazNaredba naredba = (IzrazNaredba) na.children.get(0);
            provjeri(naredba);
        } else if (na.children.get(0) instanceof NaredbaGrananja) {
            NaredbaGrananja naredba = (NaredbaGrananja) na.children.get(0);
            provjeri(naredba);
        } else if (na.children.get(0) instanceof NaredbaPetlje) {
            NaredbaPetlje naredba = (NaredbaPetlje) na.children.get(0);
            provjeri(naredba);
        } else if (na.children.get(0) instanceof NaredbaSkoka) {
            NaredbaSkoka naredba = (NaredbaSkoka) na.children.get(0);
            provjeri(naredba);
        }
    }

    public void provjeri(IzrazNaredba na) {
        if (na.children.get(0) instanceof Konstanta) {
            // <izraz_naredba> ::= TOCKAZAREZ
            na.tip = new Tip(TipEnum.INT);
        } else if (na.children.get(0) instanceof Izraz) {
            // <izraz_naredba> ::= <izraz> TOCKAZAREZ
            Izraz izraz = (Izraz) na.children.get(0);

            provjeri(izraz);

            na.tip = izraz.tip;
        }
    }

    public void provjeri(NaredbaGrananja na) {
        if (na.children.size() == 5) {
            // <naredba_grananja> ::= KR_IF L_ZAGRADA <izraz> D_ZAGRADA <naredba>
            Izraz izraz = (Izraz) na.children.get(2);
            Naredba naredba = (Naredba) na.children.get(4);

            provjeri(izraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(izraz.tip, new Tip(TipEnum.INT)), na);
            provjeri(naredba);
        } else if (na.children.size() == 7) {
            // <naredba_grananja> ::= KR_IF L_ZAGRADA <izraz> D_ZAGRADA <naredba>1 KR_ELSE
            // <naredba>2
            Izraz izraz = (Izraz) na.children.get(2);
            Naredba naredba1 = (Naredba) na.children.get(4);
            Naredba naredba2 = (Naredba) na.children.get(6);

            provjeri(izraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(izraz.tip, new Tip(TipEnum.INT)), na);
            provjeri(naredba1);
            provjeri(naredba2);
        }
    }

    public void provjeri(NaredbaPetlje na) {
        Konstanta kljucnaRijec = (Konstanta) na.children.get(0);
        if (kljucnaRijec.konstantaTip == KonstantaEnum.KR_WHILE) {
            // <naredba_petlje> ::= KR_WHILE L_ZAGRADA <izraz> D_ZAGRADA <naredba>
            Izraz izraz = (Izraz) na.children.get(2);
            Naredba naredba = (Naredba) na.children.get(4);

            provjeri(izraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(izraz.tip, new Tip(TipEnum.INT)), na);
            provjeri(naredba);
        } else if (kljucnaRijec.konstantaTip == KonstantaEnum.KR_FOR && na.children.size() == 6) {
            // <naredba_petlje> ::= KR_FOR L_ZAGRADA <izraz_naredba>1 <izraz_naredba>2
            // D_ZAGRADA <naredba>
            IzrazNaredba izrazNaredba1 = (IzrazNaredba) na.children.get(2);
            IzrazNaredba izrazNaredba2 = (IzrazNaredba) na.children.get(3);
            Naredba naredba = (Naredba) na.children.get(5);

            provjeri(izrazNaredba1);
            provjeri(izrazNaredba2);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(izrazNaredba2.tip, new Tip(TipEnum.INT)), na);
            provjeri(naredba);
        } else if (kljucnaRijec.konstantaTip == KonstantaEnum.KR_FOR && na.children.size() == 7) {
            // <naredba_petlje> ::= KR_FOR L_ZAGRADA <izraz_naredba>1 <izraz_naredba>2
            // <izraz> D_ZAGRADA <naredba>
            IzrazNaredba izrazNaredba1 = (IzrazNaredba) na.children.get(2);
            IzrazNaredba izrazNaredba2 = (IzrazNaredba) na.children.get(3);
            Izraz izraz = (Izraz) na.children.get(4);
            Naredba naredba = (Naredba) na.children.get(6);

            provjeri(izrazNaredba1);
            provjeri(izrazNaredba2);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(izrazNaredba2.tip, new Tip(TipEnum.INT)), na);
            provjeri(izraz);
            provjeri(naredba);
        }
    }

    public void provjeri(NaredbaSkoka na) {
        KonstantaEnum kljucnaRijec = ((Konstanta) na.children.get(0)).konstantaTip;

        if (kljucnaRijec == KonstantaEnum.KR_CONTINUE || kljucnaRijec == KonstantaEnum.KR_BREAK) {
            // <naredba_skoka> ::= (KR_CONTINUE | KR_BREAK) TOCKAZAREZ

            assertOrError(lokalniDjelokrug.jeUnutarPetlje(), na);

        } else if (kljucnaRijec == KonstantaEnum.KR_RETURN && na.children.size() == 2) {
            // <naredba_skoka> ::= KR_RETURN TOCKAZAREZ
            
            assertOrError(lokalniDjelokrug.jeUnutarFunkcijePovratneVrijednosti(new Tip(TipEnum.VOID)), na);

        } else if (kljucnaRijec == KonstantaEnum.KR_RETURN && na.children.size() == 3) {
            // <naredba_skoka> ::= KR_RETURN <izraz> TOCKAZAREZ
            Izraz izraz = (Izraz) na.children.get(1);

            provjeri(izraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU( izraz.tip, lokalniDjelokrug.povratniTipUgnjezdujuceFunkcije() ), na);
        }
    }

    public void provjeri(PrijevodnaJedinica pi) {
        if (pi.children.get(0) instanceof VanjskaDeklaracija) {
            // <prijevodna_jedinica> ::= <vanjska_deklaracija>
            VanjskaDeklaracija vanjskaDeklaracija = (VanjskaDeklaracija) pi.children.get(0);

            provjeri(vanjskaDeklaracija);
        } else if (pi.children.get(0) instanceof PrijevodnaJedinica) {
            // <prijevodna_jedinica> ::= <prijevodna_jedinica> <vanjska_deklaracija>
            PrijevodnaJedinica prijevodnaJedinica = (PrijevodnaJedinica) pi.children.get(0);
            VanjskaDeklaracija vanjskaDeklaracija = (VanjskaDeklaracija) pi.children.get(1);

            provjeri(prijevodnaJedinica);
            provjeri(vanjskaDeklaracija);
        }
    }

    public void provjeri(VanjskaDeklaracija vd) {
        if (vd.children.get(0) instanceof DefinicijaFunkcije) {
            DefinicijaFunkcije definicijaFunkcije = (DefinicijaFunkcije) vd.children.get(0);

            provjeri(definicijaFunkcije);
        } else {
            Deklaracija deklaracija = (Deklaracija) vd.children.get(0);

            provjeri(deklaracija);
        }
    }

    public void provjeri(DefinicijaFunkcije de) {
        if (de.children.get(3) instanceof Konstanta) {
            // <definicija_funkcije> ::= <ime_tipa> IDN L_ZAGRADA KR_VOID D_ZAGRADA <slozena_naredba>
            ImeTipa imeTipa = (ImeTipa) de.children.get(0);
            Konstanta identifikator = (Konstanta) de.children.get(1);
            SlozenaNaredba slozenaNaredba = (SlozenaNaredba) de.children.get(5);

            provjeri(imeTipa);
            assertOrError( ! Tip.isConstT(imeTipa.tip), de);
            assertOrError( ! postojiDefiniranaFunkcija(identifikator.vrijednost), de);
            Identifikator funkcija = globalniDjelokrug.lokalnaVarijabla(identifikator.vrijednost);
            FunkcijaTip tipFunkcije = new FunkcijaTip(new Tip[0], imeTipa.tip);
            if(funkcija != null) {
                assertOrError(funkcija.tip.equals(tipFunkcije), de);
            }
            definirajFunkciju(identifikator.vrijednost, tipFunkcije);
            lokalniDjelokrug = new Djelokrug(lokalniDjelokrug);
            lokalniDjelokrug.setFunkcija(tipFunkcije);
            provjeri(slozenaNaredba);
            lokalniDjelokrug = lokalniDjelokrug.ugnjezdujuciDjelokrug;

        } else if (de.children.get(3) instanceof ListaParametara) {
            // <definicija_funkcije> ::= <ime_tipa> IDN L_ZAGRADA <lista_parametara> D_ZAGRADA <slozena_naredba>
            ImeTipa imeTipa = (ImeTipa) de.children.get(0);
            Konstanta identifikator = (Konstanta) de.children.get(1);
            ListaParametara listaParametara = (ListaParametara) de.children.get(3);
            SlozenaNaredba slozenaNaredba = (SlozenaNaredba) de.children.get(5);

            provjeri(imeTipa);
            assertOrError( ! Tip.isConstT(imeTipa.tip), de);
            assertOrError( ! postojiDefiniranaFunkcija(identifikator.vrijednost), de);
            provjeri(listaParametara);
            Identifikator funkcija = globalniDjelokrug.lokalnaVarijabla(identifikator.vrijednost);
            FunkcijaTip tipFunkcije = new FunkcijaTip(listaParametara.tipovi, imeTipa.tip);
            if(funkcija != null) {
                assertOrError(funkcija.tip.equals(tipFunkcije), de);
            }
            definirajFunkciju(identifikator.vrijednost, tipFunkcije);
            lokalniDjelokrug = new Djelokrug(lokalniDjelokrug);
            lokalniDjelokrug.setFunkcija(tipFunkcije);
            for(int i = 0; i < listaParametara.tipovi.length; i++) {
                lokalniDjelokrug.zabiljeziIdentifikator(listaParametara.imena[i], listaParametara.tipovi[i]);
            }
            provjeri(slozenaNaredba);
            lokalniDjelokrug = lokalniDjelokrug.ugnjezdujuciDjelokrug;
        }
    }

    public void provjeri(ListaParametara lp) {
        if (lp.children.get(0) instanceof DeklaracijaParametra) {
            // <lista_parametara> ::= <deklaracija_parametra>
            DeklaracijaParametra deklaracijaParametra = (DeklaracijaParametra) lp.children.get(0);

            provjeri(deklaracijaParametra);

            Tip[] tipovi = { deklaracijaParametra.tip };
            lp.tipovi = tipovi;
            String[] imena = { deklaracijaParametra.ime };
            lp.imena = imena;
        } else if (lp.children.get(0) instanceof ListaParametara) {
            // <lista_parametara> ::= <lista_parametara> ZAREZ <deklaracija_parametra>
            ListaParametara listaParametara = (ListaParametara) lp.children.get(0);
            DeklaracijaParametra deklaracijaParametra = (DeklaracijaParametra) lp.children.get(2);

            provjeri(listaParametara);
            provjeri(deklaracijaParametra);
            assertOrError(! Arrays.stream(listaParametara.imena).anyMatch(deklaracijaParametra.ime::equals), lp);
            // boolean anyEqual = false;    // if anyMatch above does not work :)
            // for(String ime : listaParametara.imena) {
            //     if( ime.equals(deklaracijaParametra.ime) ){
            //         anyEqual = true;
            //         break;
            //     }
            // }
            // assertOrError(anyEqual, lp);

            Tip[] tipovi = new Tip[listaParametara.tipovi.length + 1];
            String[] imena = new String[listaParametara.imena.length + 1];
            for (int i = 0; i < listaParametara.tipovi.length; i++) {
                tipovi[i] = listaParametara.tipovi[i];
                imena[i] = listaParametara.imena[i];
            }
            tipovi[listaParametara.tipovi.length] = deklaracijaParametra.tip;
            imena[listaParametara.imena.length] = deklaracijaParametra.ime;

            lp.tipovi = tipovi;
            lp.imena = imena;
        }
    }

    public void provjeri(DeklaracijaParametra de) {
        if (de.children.size() == 2) {
            // <deklaracija_parametra> ::= <ime_tipa> IDN
            ImeTipa imeTipa = (ImeTipa) de.children.get(0);
            Konstanta identifikator = (Konstanta) de.children.get(1);

            provjeri(imeTipa);
            assertOrError(!imeTipa.tip.equals(new Tip(TipEnum.VOID)), de);

            de.tip = imeTipa.tip;
            de.ime = identifikator.vrijednost;
        } else if (de.children.size() == 4) {
            // <deklaracija_parametra> ::= <ime_tipa> IDN L_UGL_ZAGRADA D_UGL_ZAGRADA
            ImeTipa imeTipa = (ImeTipa) de.children.get(0);
            Konstanta identifikator = (Konstanta) de.children.get(1);

            provjeri(imeTipa);
            assertOrError(!imeTipa.tip.equals(new Tip(TipEnum.VOID)), de);

            de.tip = new KompozitniTip(TipEnum.NIZ, imeTipa.tip);
            de.ime = identifikator.vrijednost;
        }
    }

    public void provjeri(ListaDeklaracija ld) {
        if (ld.children.get(0) instanceof Deklaracija) {
            // <lista_deklaracija> ::= <deklaracija>
            Deklaracija deklaracija = (Deklaracija) ld.children.get(0);

            provjeri(deklaracija);
        } else if (ld.children.get(0) instanceof ListaDeklaracija) {
            // <lista_deklaracija> ::= <lista_deklaracija> <deklaracija>
            ListaDeklaracija listaDeklaracija = (ListaDeklaracija) ld.children.get(0);
            Deklaracija deklaracija = (Deklaracija) ld.children.get(1);

            provjeri(listaDeklaracija);
            provjeri(deklaracija);
        }
    }

    public void provjeri(Deklaracija de) {
        // <deklaracija> ::= <ime_tipa> <lista_init_deklaratora> TOCKAZAREZ
        ImeTipa imeTipa = (ImeTipa) de.children.get(0);
        ListaInitDeklaratora listaInitDeklaratora = (ListaInitDeklaratora) de.children.get(1);

        provjeri(imeTipa);
        listaInitDeklaratora.ntip = imeTipa.tip;
        provjeri(listaInitDeklaratora);

    }

    public void provjeri(ListaInitDeklaratora ld) {
        if (ld.children.get(0) instanceof InitDeklarator) {
            // <lista_init_deklaratora> ::= <init_deklarator>
            InitDeklarator initDeklarator = (InitDeklarator) ld.children.get(0);

            initDeklarator.ntip = ld.ntip;
            provjeri(initDeklarator);
        } else if (ld.children.get(0) instanceof ListaInitDeklaratora) {
            // <lista_init_deklaratora>1 ::= <lista_init_deklaratora>2 ZAREZ
            // <init_deklarator>
            ListaInitDeklaratora listaInitDeklaratora = (ListaInitDeklaratora) ld.children.get(0);
            InitDeklarator initDeklarator = (InitDeklarator) ld.children.get(2);

            listaInitDeklaratora.ntip = ld.ntip;
            provjeri(listaInitDeklaratora);
            initDeklarator.ntip = ld.ntip;
            provjeri(initDeklarator);
        }

    }

    public void provjeri(InitDeklarator de) {
        if (de.children.size() == 1) {
            // <init_deklarator> ::= <izravni_deklarator>
            IzravniDeklarator izravniDeklarator = (IzravniDeklarator) de.children.get(0);

            izravniDeklarator.ntip = de.ntip;
            provjeri(izravniDeklarator);
            assertOrError(!Tip.isConstT(izravniDeklarator.tip), de);
            assertOrError(!Tip.isNizConstT(izravniDeklarator.tip), de);
        } else if (de.children.size() == 3) {
            // <init_deklarator> ::= <izravni_deklarator> OP_PRIDRUZI <inicijalizator>
            IzravniDeklarator izravniDeklarator = (IzravniDeklarator) de.children.get(0);
            Inicijalizator inicijalizator = (Inicijalizator) de.children.get(2);

            izravniDeklarator.ntip = de.ntip;
            provjeri(izravniDeklarator);
            provjeri(inicijalizator);
            if (Tip.isT(izravniDeklarator.tip) || Tip.isConstT(izravniDeklarator.tip)) {
                assertOrError(Tip.seMozeImplicitnoPretvoritiUT(inicijalizator.tip), de);
            } else if (Tip.isNizT(izravniDeklarator.tip) || Tip.isNizConstT(izravniDeklarator.tip)) {
                assertOrError(inicijalizator.br_elem <= izravniDeklarator.br_elem, de);
                assertOrError(inicijalizator.tipovi != null, de);   // TODO nije po uputama, ali popravlja jedan test-case. Nije mi jasno zasto je tu potrebno
                for (Tip u : inicijalizator.tipovi) {
                    assertOrError(Tip.seMozeImplicitnoPretvoritiUT(u), de);
                }
            } else {
                ispisiError(de);
            }
        }
    }

    public void provjeri(IzravniDeklarator de) {
        if (de.children.size() == 1) {
            // <izravni_deklarator> ::= IDN
            Konstanta identifikator = (Konstanta) de.children.get(0);

            assertOrError( ! de.ntip.equals(new Tip(TipEnum.VOID)), de);
            assertOrError( ! lokalniDjelokrug.sadrziLokalnuVarijablu(identifikator.vrijednost), de);
            zabiljeziIdentifikator(identifikator.vrijednost, de.ntip);

            de.tip = de.ntip;
        } else if (de.children.get(2) instanceof Konstanta) {
            Konstanta konstanta = (Konstanta) de.children.get(2);
            if (konstanta.konstantaTip == KonstantaEnum.BROJ) {
                // <izravni_deklarator> ::= IDN L_UGL_ZAGRADA BROJ D_UGL_ZAGRADA
                Konstanta identifikator = (Konstanta) de.children.get(0);

                assertOrError( ! de.ntip.equals(new Tip(TipEnum.VOID)), de);
                assertOrError( ! lokalniDjelokrug.sadrziLokalnuVarijablu(identifikator.vrijednost), de);
                try {
                    Integer.parseInt(konstanta.vrijednost);
                }
                catch (Exception e) {
                    ispisiError(de);  // integer izvan range-a (32 bit)
                }
                Tip tip = new KompozitniTip(TipEnum.NIZ, de.ntip);
                zabiljeziIdentifikator(identifikator.vrijednost, tip);

                de.tip = tip;
                de.br_elem = Integer.parseInt(konstanta.vrijednost);
            } else if (konstanta.konstantaTip == KonstantaEnum.KR_VOID) {
                // <izravni_deklarator> ::= IDN L_ZAGRADA KR_VOID D_ZAGRADA
                Konstanta identifikator = (Konstanta) de.children.get(0);
                Tip tipFunkcije = new FunkcijaTip(new Tip[0], de.ntip);
                
                if (lokalniDjelokrug.sadrziLokalnuVarijablu(identifikator.vrijednost)) {
                    Tip tipDeklarirane = lokalniDjelokrug.varijabla(identifikator.vrijednost).tip;
                    assertOrError(tipDeklarirane.equals(tipFunkcije), de);
                } else {
                    zabiljeziIdentifikator(identifikator.vrijednost, tipFunkcije);
                }

                de.tip = tipFunkcije;
            }
        } else if (de.children.get(2) instanceof ListaParametara) {
            // <izravni_deklarator> ::= IDN L_ZAGRADA <lista_parametara> D_ZAGRADA
            Konstanta identifikator = (Konstanta) de.children.get(0);
            ListaParametara listaParametara = (ListaParametara) de.children.get(2);
            
            provjeri(listaParametara);
            Tip tipFunkcije = new FunkcijaTip(listaParametara.tipovi, de.ntip);
            if (lokalniDjelokrug.sadrziLokalnuVarijablu(identifikator.vrijednost)) {
                Tip tipDeklarirane = lokalniDjelokrug.varijabla(identifikator.vrijednost).tip;
                assertOrError(tipDeklarirane.equals(tipFunkcije), de);
            } else {
                zabiljeziIdentifikator(identifikator.vrijednost, tipFunkcije);
            }
            
            de.tip = tipFunkcije;
        }
    }

    public void provjeri(Inicijalizator ic) {
        if (ic.children.get(0) instanceof IzrazPridruzivanja) {
            // <inicijalizator> ::= <izraz_pridruzivanja>
            IzrazPridruzivanja izrazPridruzivanja = (IzrazPridruzivanja) ic.children.get(0);

            provjeri(izrazPridruzivanja);

            Konstanta nizZnakova = izrazPridruzivanja.generira(KonstantaEnum.NIZ_ZNAKOVA);
            if (nizZnakova != null) {
                Tip[] tipovi = new Tip[ic.br_elem];
                for (int i = 0; i < ic.br_elem; i++) {
                    tipovi[i] = new Tip(TipEnum.CHAR);
                }
                ic.br_elem = nizZnakova.vrijednost.length() + 1;
                ic.tipovi = tipovi;
            } else {
                ic.tip = izrazPridruzivanja.tip;
            }
        } else {
            // <inicijalizator> ::= L_VIT_ZAGRADA <lista_izraza_pridruzivanja> D_VIT_ZAGRADA
            ListaIzrazaPridruzivanja listaIzrazaPridruzivanja = (ListaIzrazaPridruzivanja) ic.children.get(1);

            provjeri(listaIzrazaPridruzivanja);

            ic.br_elem = listaIzrazaPridruzivanja.br_elem;
            ic.tipovi = listaIzrazaPridruzivanja.tipovi;
        }
    }

    public void provjeri(ListaIzrazaPridruzivanja lp) {
        if (lp.children.get(0) instanceof IzrazPridruzivanja) {
            // <lista_izraza_pridruzivanja> ::= <izraz_pridruzivanja>
            IzrazPridruzivanja izrazPridruzivanja = (IzrazPridruzivanja) lp.children.get(0);

            provjeri(izrazPridruzivanja);

            Tip[] tipovi = { izrazPridruzivanja.tip };
            lp.tipovi = tipovi;
            lp.br_elem = 1;
        } else {
            // <lista_izraza_pridruzivanja> ::= <lista_izraza_pridruzivanja> ZAREZ
            // <izraz_pridruzivanja>
            ListaIzrazaPridruzivanja listaIzrazaPridruzivanja = (ListaIzrazaPridruzivanja) lp.children.get(0);
            IzrazPridruzivanja izrazPridruzivanja = (IzrazPridruzivanja) lp.children.get(2);

            provjeri(listaIzrazaPridruzivanja);
            provjeri(izrazPridruzivanja);

            Tip[] tipovi = new Tip[listaIzrazaPridruzivanja.br_elem + 1];
            for (int i = 0; i < listaIzrazaPridruzivanja.tipovi.length; i++) {
                tipovi[i] = listaIzrazaPridruzivanja.tipovi[i];
            }
            tipovi[listaIzrazaPridruzivanja.tipovi.length] = izrazPridruzivanja.tip;

            lp.tipovi = tipovi;
            lp.br_elem = listaIzrazaPridruzivanja.br_elem + 1;
        }
    }

    public void zabiljeziIdentifikator(String ime, Tip tip) {
        lokalniDjelokrug.zabiljeziIdentifikator(ime, tip);
    }
}
