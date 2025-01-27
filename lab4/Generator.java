package lab4;

import java.nio.channels.UnsupportedAddressTypeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import lab4.tip.*;
import lab4.znakovi.*;

public class Generator {
    private static int STACK_START = 0x40000;

    private Djelokrug lokalniDjelokrug;
    private Djelokrug globalniDjelokrug;

    private int labelaIndeks;
    private ArrayList<Identifikator> sviIdentifikatori;
    private Map<String, IdentifikatorFunkcije> sveFunkcije;

    private String generiraniKod;

    public Generator() {
        globalniDjelokrug = new Djelokrug();
        lokalniDjelokrug = globalniDjelokrug;

        labelaIndeks = 0;
        sviIdentifikatori = new ArrayList<>();
        sveFunkcije = new HashMap<String,IdentifikatorFunkcije>();

    }

    // public void analiziraj(PrijevodnaJedinica prijevodnaJedinica) {

        // TODO: remove all code connected strictly to the semantic analyzer

    // provjeri(prijevodnaJedinica);

    // assertOrError(postojiDefiniranaFunkcija("main"), "main");

    // }

    public String generirajProgram(PrijevodnaJedinica prijevodnaJedinica) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n\tMOVE %x, R7", STACK_START));

        sb.append(generiraj(prijevodnaJedinica));

        IdentifikatorFunkcije main = globalniDjelokrug.funkcija("main");
        assertOrError(main != null, "main");

        sb.append(String.format("\n\tCALL %s", main.labela));
        sb.append("\n\tHALT");

        sb.append(generirajFunkcije());
        sb.append(generirajLokacijeIdentifikatora());

        generiraniKod = sb.toString();
        return generiraniKod;
    }

    private String generirajLokacijeIdentifikatora() {
        StringBuilder sb = new StringBuilder();
        // TODO podrska za identifikatore tipa niz i sl. Tip je vec zapisan u idn
        for (Identifikator idn : sviIdentifikatori) {
            if (idn.tip.equals(new Tip(TipEnum.INT))) {
                if (idn.initalValue != null) {
                    sb.append(String.format("\n%s\tDW %%D %s", idn.labela, idn.initalValue));
                } else {
                    sb.append(String.format("\n%s\tDW %%D %s", idn.labela, "0"));
                }
            }
        }
        return sb.toString();
    }

    private String generirajFunkcije() {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, IdentifikatorFunkcije> idfEntry : sveFunkcije.entrySet()) {
            IdentifikatorFunkcije idf = idfEntry.getValue();
            if (idf.labela == null) {
                System.err.println(String.format(
                        "identifikator %s nema labelu a gneerirram mu funkciju?? kak su ju ikad druge funkcije pozivale/? huh",
                        idf.ime));
                throw new Error();
            }
            if (!idf.definirana) {
                System.err.println(String.format("funkcija %s nije definirana?? h/? huh", idf.ime));
                throw new Error();
            }
            sb.append(String.format("\n%s\t", idf.labela));
            sb.append(idf.kodTjelaFunkcije);
        }
        return sb.toString();
    }

    private String novoImeLabele() {
        return String.format("L%06d", labelaIndeks++);
    }

    // TODO delete
    // private IdentifikatorFunkcije referncaNaFunkciju(String ime) {
    // if(sveFunkcije.containsKey(ime)){
    // return sveFunkcije.get(ime);
    // }
    // // else
    // }

    // TODO pobrinut se da se nigdje ne zove lokalniDjelokrug.zabiljeziIdentifikator
    // direktno
    private Identifikator zabiljeziIdentifikator(String ime, Tip tip) {
        Identifikator idn = lokalniDjelokrug.zabiljeziIdentifikator(ime, tip);
        idn.labela = novoImeLabele();
        sviIdentifikatori.add(idn);
        return idn;
    }

    private IdentifikatorFunkcije deklarirajFunkciju(String ime, FunkcijaTip tip) {
        if (sveFunkcije.containsKey(ime)) {
            IdentifikatorFunkcije idf = sveFunkcije.get(ime);
            lokalniDjelokrug.funkcije.put(ime, idf);
            return idf;
        }
        // else
        IdentifikatorFunkcije f = (IdentifikatorFunkcije) lokalniDjelokrug.zabiljeziIdentifikator(ime, tip);
        f.labela = novoImeLabele();
        sveFunkcije.put(ime, f);
        return f;
    }

    private IdentifikatorFunkcije definirajFunkciju(String ime, FunkcijaTip tip) {
        IdentifikatorFunkcije f = deklarirajFunkciju(ime, tip);
        /// definirajFunkciju ce se jedino ikad zvati iz globalnog djelokruga
        /// tako da ce globalniDjelokrug.funkcija(ime) zasigurno postojati
        /// iako deklarirajFunkciju radi nad lokalnim djelokrugom
        globalniDjelokrug.funkcija(ime).definirana = true;
        f.definirana = true;
        return f;
    }

    public boolean postojiDefiniranaFunkcija(String ime) {
        IdentifikatorFunkcije funkcija = globalniDjelokrug.funkcija(ime);
        if (funkcija == null) {
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
        }
    }

    private void assertOrError(boolean condition, String mistake) {
        if (!condition) {
            ispisiError(mistake);
        }
    }

    private void ispisiError(String mistake) {
        System.out.println(mistake);
        System.exit(0);
    }

    private void ispisiError(Node mistake) {

        System.out.printf(mistake.toString() + " ::=");

        for (Node child : mistake.children) {
            System.out.printf(" " + child.toString());
        }

        System.out.printf("\n");

        // izadji iz programa
        System.exit(0);
    }

    private String generiraj(PrimarniIzraz iz) {
        Konstanta c = (Konstanta) iz.children.get(0);

        if (c.konstantaTip == KonstantaEnum.IDN) {
            // <primarni_izraz> ::= IDN
            Konstanta idn = (Konstanta) iz.children.get(0);
            assertOrError(lokalniDjelokrug.sadrziDeklaraciju(idn.vrijednost), iz);
            Identifikator identifikator = lokalniDjelokrug.deklaracija(idn.vrijednost);
            iz.tip = identifikator.tip;
            iz.l_izraz = identifikator.l_izraz;
            iz.labela = identifikator.labela;

            return String.format("\n\tLOAD R0, (%s)\n\tPUSH R0", identifikator.labela);
        } else if (c.konstantaTip == KonstantaEnum.BROJ) {
            // <primarni_izraz> ::= BROJ
            try {
                Integer.parseInt(c.vrijednost);
            } catch (Exception e) {
                ispisiError(iz); // integer izvan range-a (32 bit)
            }

            iz.tip = new Tip(TipEnum.INT);
            iz.l_izraz = false;

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("\n\tMOVE %%D %s, R1", Integer.parseInt(c.vrijednost)));
            sb.append("\n\tPUSH R1");
            return sb.toString();

        } else if (c.konstantaTip == KonstantaEnum.ZNAK) {
            // <primarni_izraz> ::= ZNAK
            // TODO: provjerit metodu
            throw new UnsupportedOperationException(); // TODO support
            /* 
            String chr = c.vrijednost.substring(1, c.vrijednost.length() - 1);
            if (chr.equals("\\t") || chr.equals("\\n") || chr.equals("\\0")
                    || chr.equals("\\'") || chr.equals("\\\"") || chr.equals("\\\\")
                    || chr.length() == 1 && ((int) chr.charAt(0)) >= 0 && ((int) chr.charAt(0)) < 128) {
                iz.tip = new Tip(TipEnum.CHAR);
                iz.l_izraz = false;
            } else {
                ispisiError(iz); // invalid char
            }//* */

        } else if (c.konstantaTip == KonstantaEnum.NIZ_ZNAKOVA) {
            // <primarni_izraz> ::= NIZ_ZNAKOVA
            throw new UnsupportedOperationException(); // TODO support
            /*
            String str = c.vrijednost.substring(1, c.vrijednost.length() - 1);
            for (int i = 0; i < str.length(); i++) {
                if (str.charAt(i) == '\\') {
                    i++;
                    try {
                        char a = str.charAt(i);
                        if (a != 't' && a != 'n' && a != '0' && a != '\'' && a != '"' && a != '\\') {
                            ispisiError(iz);
                        }
                    } catch (IndexOutOfBoundsException e) {
                        ispisiError(iz);
                    }
                }
                if (!(((int) str.charAt(i)) >= 0 && ((int) str.charAt(i)) < 128)) {
                    ispisiError(iz);
                }
            }
            iz.tip = new KompozitniTip(TipEnum.NIZ, new KompozitniTip(TipEnum.CONST, new Tip(TipEnum.CHAR)));
            iz.l_izraz = false;//* */
        } else {// if (c.konstantaTip == KonstantaEnum.L_ZAGRADA) {
            // <<primarni_izraz> ::= L_ZAGRADA <izraz> D_ZAGRADA
            Izraz izraz = (Izraz) iz.children.get(1);

            String s = generiraj(izraz);

            iz.tip = izraz.tip;
            iz.l_izraz = izraz.l_izraz;
            iz.labela = izraz.labela;
            return s;
        }

    }

    public String generiraj(PostfiksIzraz iz) {
        if (iz.children.size() == 1 && iz.children.get(0) instanceof PrimarniIzraz) {
            // <postfiks_izraz> ::= <primarni_izraz>
            PrimarniIzraz izraz = (PrimarniIzraz) iz.children.get(0);
            String s = generiraj(izraz);

            iz.tip = izraz.tip;
            iz.l_izraz = izraz.l_izraz;
            iz.labela = izraz.labela;
            return s;
        } else {
            PostfiksIzraz postfiksIzraz = (PostfiksIzraz) iz.children.get(0);
            Konstanta k1 = (Konstanta) iz.children.get(1);
            if (k1.konstantaTip == KonstantaEnum.L_UGL_ZAGRADA) {
                // <postfiks_izraz> ::= <postfiks_izraz> L_UGL_ZAGRADA <izraz> D_UGL_ZAGRADA
                throw new UnsupportedOperationException(); // TODO support
                /*
                Izraz izraz = (Izraz) iz.children.get(2);

                generiraj(postfiksIzraz);
                assertOrError(Tip.isNizX(postfiksIzraz.tip), iz);
                generiraj(izraz);
                assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(izraz.tip, new Tip(TipEnum.INT)), iz);

                Tip X = ((KompozitniTip) postfiksIzraz.tip).subTip;
                iz.tip = X;
                iz.l_izraz = !Tip.isConstT(X);
                iz.labela = postfiksIzraz.labela//* */
            } else if (k1.konstantaTip == KonstantaEnum.L_ZAGRADA && iz.children.size() == 3) {
                // <postfiks_izraz> ::= <postfiks_izraz> L_ZAGRADA D_ZAGRADA

                // TODO: ovaj generiraj je ionak redundantan, makni ga sa svim ostalim vezanim iskljucivo uz prosli labos
                generiraj(postfiksIzraz);
                assertOrError(postfiksIzraz.tip instanceof FunkcijaTip, iz);
                FunkcijaTip funkcijaTip = (FunkcijaTip) postfiksIzraz.tip;
                assertOrError(funkcijaTip.isVoidFunction(), iz);

                iz.tip = funkcijaTip.rval;
                iz.l_izraz = false;

                return String.format("\n\tCALL %s", postfiksIzraz.labela) + "\n\tPUSH R6";

            } else if (k1.konstantaTip == KonstantaEnum.L_ZAGRADA && iz.children.size() == 4) {
                // <postfiks_izraz> ::= <postfiks_izraz> L_ZAGRADA <lista_argumenata> D_ZAGRADA
                throw new UnsupportedOperationException(); // TODO support
/*
                ListaArgumenata listaArgumenata = (ListaArgumenata) iz.children.get(2);
                generiraj(postfiksIzraz);
                provjeri(listaArgumenata);
                assertOrError(postfiksIzraz.tip instanceof FunkcijaTip, iz);
                FunkcijaTip funkcijaTip = (FunkcijaTip) postfiksIzraz.tip;
                assertOrError(listaArgumenata.tipovi.length == funkcijaTip.args.length, iz);
                for (int i = 0; i < listaArgumenata.tipovi.length; i++) {
                    assertOrError(listaArgumenata.tipovi[i].equals(funkcijaTip.args[i]), iz);
                }

                iz.tip = funkcijaTip.rval;
                iz.l_izraz = false;//* */

            } else {
                // <postfiks_izraz> ::= <postfiks_izraz> (OP_INC | OP_DEC)
                String s = generiraj(postfiksIzraz);
                assertOrError(postfiksIzraz.l_izraz == true, iz);
                assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(postfiksIzraz.tip, new Tip(TipEnum.INT)), iz);

                iz.tip = new Tip(TipEnum.INT);
                iz.l_izraz = false;

                StringBuilder sb = new StringBuilder();
                sb.append(s);
                sb.append("\n\tPOP R0");
                sb.append("\n\tPUSH R0");
                if (k1.konstantaTip == KonstantaEnum.OP_INC) {
                    sb.append("\n\tADD R0, 1, R0");
                } else {
                    sb.append("\n\tSUB R0, 1, R0");
                }
                sb.append(String.format("\n\tSTORE R0, %s", iz.labela));

                return sb.toString();
            }
        }
    }

    public void provjeri(ListaArgumenata la) {
        if (la.children.get(0) instanceof IzrazPridruzivanja) {
            // <lista_argumenata> ::= <izraz_pridruzivanja>
            IzrazPridruzivanja izrazPridruzivanja = (IzrazPridruzivanja) la.children.get(0);
            // TODO provjeri(izrazPridruzivanja);

            Tip[] tipovi = { izrazPridruzivanja.tip };
            la.tipovi = tipovi;
        } else if (la.children.get(0) instanceof ListaArgumenata) {
            // <lista_argumenata> ::= <lista_argumenata> ZAREZ <izraz_pridruzivanja>
            ListaArgumenata listaArgumenata = (ListaArgumenata) la.children.get(0);
            IzrazPridruzivanja izrazPridruzivanja = (IzrazPridruzivanja) la.children.get(2);

            provjeri(listaArgumenata);
            // TODO provjeri(izrazPridruzivanja);

            Tip[] tipovi = new Tip[listaArgumenata.tipovi.length + 1];
            for (int i = 0; i < listaArgumenata.tipovi.length; i++) {
                tipovi[i] = listaArgumenata.tipovi[i];
            }
            tipovi[listaArgumenata.tipovi.length] = izrazPridruzivanja.tip;

            la.tipovi = tipovi;
        }
    }

    public String generiraj(UnarniIzraz ui) {
        if (ui.children.get(0) instanceof PostfiksIzraz) {
            // <unarni_izraz> ::= <postfiks_izraz>
            PostfiksIzraz postfiksIzraz = (PostfiksIzraz) ui.children.get(0);

            String s = generiraj(postfiksIzraz);

            ui.tip = postfiksIzraz.tip;
            ui.l_izraz = postfiksIzraz.l_izraz;
            ui.labela = postfiksIzraz.labela;
            return s;
        } else if (ui.children.get(0) instanceof Konstanta) {
            // <unarni_izraz> ::= (OP_INC | OP_DEC) <unarni_izraz>
            // zasigurno je OP_INC ili OP_DEC prema gramatickim pravilima pa je provjera
            // suvisna
            Konstanta op = (Konstanta) ui.children.get(0);
            UnarniIzraz unarniIzraz = (UnarniIzraz) ui.children.get(1);

            String s = generiraj(unarniIzraz);
            assertOrError(unarniIzraz.l_izraz == true, ui);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(unarniIzraz.tip, new Tip(TipEnum.INT)), ui);

            ui.tip = new Tip(TipEnum.INT);
            ui.l_izraz = false;

            StringBuilder sb = new StringBuilder();
            sb.append(s);
            sb.append("\n\tPOP R0");
            if (op.konstantaTip == KonstantaEnum.OP_INC) {
                sb.append("\n\tADD R0, 1, R0");
            } else {
                sb.append("\n\tSUB R0, 1, R0");
            }
            sb.append(String.format("\n\tSTORE R0, (%s)", unarniIzraz.labela));
            sb.append("\n\tPUSH R0");

            return sb.toString();
        } else {// if (ui.children.get(0) instanceof UnarniOperator) {
            // <unarni_izraz> ::= <unarni_operator> <cast_izraz>
            // UnarniOperator unarniOperator = (UnarniOperator) ui.children.get(0);
            throw new UnsupportedOperationException(); // TODOs
            // UnarniOperator unarniOperator = (UnarniOperator) ui.children.get(0);
            // CastIzraz castIzraz = (CastIzraz) ui.children.get(1);

            // String s1 = generiraj(castIzraz);
            // assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(castIzraz.tip, new
            // Tip(TipEnum.INT)), ui);

            // // TODO String s2 = generiraj(unarniOperator);

            // ui.tip = new Tip(TipEnum.INT);
            // ui.l_izraz = false;

            // // return s1 + s2;
        }
    }

    public String generiraj(CastIzraz iz) {
        if (iz.children.get(0) instanceof UnarniIzraz) {
            // <cast_izraz> ::= <unarni_izraz>
            UnarniIzraz unarniIzraz = (UnarniIzraz) iz.children.get(0);

            String s = generiraj(unarniIzraz);

            iz.tip = unarniIzraz.tip;
            iz.l_izraz = unarniIzraz.l_izraz;
            iz.labela = unarniIzraz.labela;

            return s;
        } else {// if (iz.children.get(0) instanceof Konstanta) {
            // zasigurno L_ZAGRADA
            // <cast_izraz> ::= L_ZAGRADA <ime_tipa> D_ZAGRADA <cast_izraz>
            ImeTipa imeTipa = (ImeTipa) iz.children.get(1);
            CastIzraz castIzraz = (CastIzraz) iz.children.get(3);

            provjeri(imeTipa);
            String s = generiraj(castIzraz);
            assertOrError(Tip.seMozePretvoritiIzU(castIzraz.tip, imeTipa.tip), iz);

            iz.tip = imeTipa.tip;
            iz.l_izraz = false;

            return s;
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

    public String generiraj(MultiplikativniIzraz iz) {
        if (iz.children.get(0) instanceof CastIzraz) {
            // <multiplikativni_izraz> ::= <cast_izraz>
            CastIzraz castIzraz = (CastIzraz) iz.children.get(0);

            String s = generiraj(castIzraz);

            iz.tip = castIzraz.tip;
            iz.l_izraz = castIzraz.l_izraz;
            iz.labela = castIzraz.labela;

            return s;
        } else {
            // <multiplikativni_izraz> ::= <multiplikativni_izraz> (OP_PUTA | OP_DIJELI |
            // OP_MOD) <cast_izraz>
            MultiplikativniIzraz multiplikativniIzraz = (MultiplikativniIzraz) iz.children.get(0);
            CastIzraz castIzraz = (CastIzraz) iz.children.get(2);

            String s1 = generiraj(multiplikativniIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(multiplikativniIzraz.tip, new Tip(TipEnum.INT)), iz);
            String s2 = generiraj(castIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(castIzraz.tip, new Tip(TipEnum.INT)), iz);

            iz.tip = new Tip(TipEnum.INT);
            iz.l_izraz = false;

            throw new UnsupportedOperationException();
            // TODO: implement mnozenje i djeljennje i sl.
            // return s1+s2; // :)
        }
    }

    public String generiraj(AditivniIzraz iz) {
        if (iz.children.get(0) instanceof MultiplikativniIzraz) {
            // <aditivni_izraz> ::= <multiplikativni_izraz>
            MultiplikativniIzraz multiplikativniIzraz = (MultiplikativniIzraz) iz.children.get(0);

            String s = generiraj(multiplikativniIzraz);

            iz.tip = multiplikativniIzraz.tip;
            iz.l_izraz = multiplikativniIzraz.l_izraz;
            iz.labela = multiplikativniIzraz.labela;

            return s;
        } else {
            // <aditivni_izraz> ::= <aditivni_izraz> (PLUS | MINUS) <multiplikativni_izraz>
            AditivniIzraz aditivniIzraz = (AditivniIzraz) iz.children.get(0);
            Konstanta op = (Konstanta) iz.children.get(1);
            MultiplikativniIzraz multiplikativniIzraz = (MultiplikativniIzraz) iz.children.get(2);

            String s1 = generiraj(aditivniIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(aditivniIzraz.tip, new Tip(TipEnum.INT)), iz);
            String s2 = generiraj(multiplikativniIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(multiplikativniIzraz.tip, new Tip(TipEnum.INT)), iz);

            iz.tip = new Tip(TipEnum.INT);
            iz.l_izraz = false;

            StringBuilder sb = new StringBuilder();
            sb.append(s1);
            sb.append(s2);
            sb.append("\n\tPOP R0");
            sb.append("\n\tPOP R1");
            if (op.konstantaTip == KonstantaEnum.PLUS) {
                sb.append("\n\tADD R0, R1, R0");
            } else {
                sb.append("\n\tSUB R0, R1, R0");
            }
            sb.append("\n\tPUSH R0");

            return sb.toString();
        }
    }

    public String generiraj(OdnosniIzraz iz) {
        if (iz.children.get(0) instanceof AditivniIzraz) {
            // <odnosni_izraz> ::= <aditivni_izraz>
            AditivniIzraz aditivniIzraz = (AditivniIzraz) iz.children.get(0);

            String s = generiraj(aditivniIzraz);

            iz.tip = aditivniIzraz.tip;
            iz.l_izraz = aditivniIzraz.l_izraz;
            iz.labela = aditivniIzraz.labela;

            return s;
        } else {
            // <odnosni_izraz> ::= <odnosni_izraz> (OP_LT | OP_GT | OP_LTE | OP_GTE)
            // <aditivni_izraz>
            OdnosniIzraz odnosniIzraz = (OdnosniIzraz) iz.children.get(0);
            Konstanta kljucnaRijec = (Konstanta) iz.children.get(1);
            AditivniIzraz aditivniIzraz = (AditivniIzraz) iz.children.get(2);

            String s1 = generiraj(odnosniIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(odnosniIzraz.tip, new Tip(TipEnum.INT)), iz);
            String s2 = generiraj(aditivniIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(aditivniIzraz.tip, new Tip(TipEnum.INT)), iz);

            iz.tip = new Tip(TipEnum.INT);
            iz.l_izraz = false;

            StringBuilder sb = new StringBuilder();
            sb.append(s1);
            sb.append(s2);
            sb.append("\n\tPOP R1");
            sb.append("\n\tPOP R0");
            sb.append("\n\tCMP R0, R1");
            sb.append("\n\tMOVE 1, R2");
            String l1 = novoImeLabele();
            switch (kljucnaRijec.konstantaTip) {
                case KonstantaEnum.OP_LT:
                    sb.append(String.format("\n\tJ_SLT %s", l1));
                    break;
                case KonstantaEnum.OP_GT:
                    sb.append(String.format("\n\tJ_SGT %s", l1));
                    break;
                case KonstantaEnum.OP_LTE:
                    sb.append(String.format("\n\tJ_SLE %s", l1));
                    break;
                case KonstantaEnum.OP_GTE:
                    sb.append(String.format("\n\tJ_SGE %s", l1));
                    break;

                default:
                    break;
            }
            sb.append("\n\tMOVE 0, R2");
            sb.append(String.format("\n%s\tPUSH R2", l1));

            return sb.toString();
        }
    }

    public String generiraj(JednakosniIzraz iz) {
        if (iz.children.get(0) instanceof OdnosniIzraz) {
            // <jednakosni_izraz> ::= <odnosni_izraz>
            OdnosniIzraz odnosniIzraz = (OdnosniIzraz) iz.children.get(0);

            String s = generiraj(odnosniIzraz);

            iz.tip = odnosniIzraz.tip;
            iz.l_izraz = odnosniIzraz.l_izraz;
            iz.labela = odnosniIzraz.labela;

            return s;
        } else {
            // <jednakosni_izraz> ::= <jednakosni_izraz> (OP_EQ | OP_NEQ) <odnosni_izraz>
            JednakosniIzraz jednakosniIzraz = (JednakosniIzraz) iz.children.get(0);
            Konstanta kljucnaRijec = (Konstanta) iz.children.get(1);
            OdnosniIzraz odnosniIzraz = (OdnosniIzraz) iz.children.get(2);

            String s1 = generiraj(jednakosniIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(jednakosniIzraz.tip, new Tip(TipEnum.INT)), iz);
            String s2 = generiraj(odnosniIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(odnosniIzraz.tip, new Tip(TipEnum.INT)), iz);

            iz.tip = new Tip(TipEnum.INT);
            iz.l_izraz = false;

            StringBuilder sb = new StringBuilder();
            sb.append(s1);
            sb.append(s2);
            sb.append("\n\tPOP R1");
            sb.append("\n\tPOP R0");
            sb.append("\n\tCMP R0, R1");
            sb.append("\n\tMOVE 1, R2");
            String l1 = novoImeLabele();
            if(kljucnaRijec.konstantaTip == KonstantaEnum.OP_EQ) {
                sb.append(String.format("\n\tJ_EQ %s", l1));
            }
            else {
                sb.append(String.format("\n\tJ_NE %s", l1));
            }
            sb.append("\n\tMOVE 0, R2");
            sb.append(String.format("\n%s\tPUSH R2", l1));

            return sb.toString();
        }
    }

    public String generiraj(BinIIzraz iz) {
        if (iz.children.get(0) instanceof JednakosniIzraz) {
            // <bin_i_izraz> ::= <jednakosni_izraz>
            JednakosniIzraz jednakosniIzraz = (JednakosniIzraz) iz.children.get(0);

            String s = generiraj(jednakosniIzraz);

            iz.tip = jednakosniIzraz.tip;
            iz.l_izraz = jednakosniIzraz.l_izraz;
            iz.labela = jednakosniIzraz.labela;

            return s;
        } else {
            // <bin_i_izraz> ::= <bin_i_izraz> OP_BIN_I <jednakosni_izraz>
            BinIIzraz binIIzraz = (BinIIzraz) iz.children.get(0);
            JednakosniIzraz jednakosniIzraz = (JednakosniIzraz) iz.children.get(2);

            String s1 = generiraj(binIIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(binIIzraz.tip, new Tip(TipEnum.INT)), iz);
            String s2 = generiraj(jednakosniIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(jednakosniIzraz.tip, new Tip(TipEnum.INT)), iz);

            iz.tip = new Tip(TipEnum.INT);
            iz.l_izraz = false;

            throw new UnsupportedOperationException();
        }
    }

    public String generiraj(BinXiliIzraz iz) {
        if (iz.children.get(0) instanceof BinIIzraz) {
            // <bin_xili_izraz> ::= <bin_i_izraz>
            BinIIzraz binIIzraz = (BinIIzraz) iz.children.get(0);

            String s = generiraj(binIIzraz);

            iz.tip = binIIzraz.tip;
            iz.l_izraz = binIIzraz.l_izraz;
            iz.labela = binIIzraz.labela;

            return s;
        } else {
            // <bin_xili_izraz> ::= <bin_xili_izraz> OP_BIN_XILI <bin_i_izraz>
            BinXiliIzraz binXiliIzraz = (BinXiliIzraz) iz.children.get(0);
            BinIIzraz binIIzraz = (BinIIzraz) iz.children.get(2);

            String s1 = generiraj(binXiliIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(binXiliIzraz.tip, new Tip(TipEnum.INT)), iz);
            String s2 = generiraj(binIIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(binIIzraz.tip, new Tip(TipEnum.INT)), iz);

            iz.tip = new Tip(TipEnum.INT);
            iz.l_izraz = false;

            throw new UnsupportedOperationException();
        }
    }

    public String generiraj(BinIliIzraz iz) {
        if (iz.children.get(0) instanceof BinXiliIzraz) {
            // <bin_ili_izraz> ::= <bin_xili_izraz>
            BinXiliIzraz binXiliIzraz = (BinXiliIzraz) iz.children.get(0);

            String s = generiraj(binXiliIzraz);

            iz.tip = binXiliIzraz.tip;
            iz.l_izraz = binXiliIzraz.l_izraz;
            iz.labela = binXiliIzraz.labela;

            return s;
        } else {
            // <bin_ili_izraz> ::= <bin_ili_izraz> OP_BIN_ILI <bin_xili_izraz>
            BinIliIzraz binIliIzraz = (BinIliIzraz) iz.children.get(0);
            BinXiliIzraz binXiliIzraz = (BinXiliIzraz) iz.children.get(2);

            String s1 = generiraj(binIliIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(binIliIzraz.tip, new Tip(TipEnum.INT)), iz);
            String s2 = generiraj(binXiliIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(binXiliIzraz.tip, new Tip(TipEnum.INT)), iz);

            iz.tip = new Tip(TipEnum.INT);
            iz.l_izraz = false;

            throw new UnsupportedOperationException();
        }
    }

    public String generiraj(LogIIzraz iz) {
        if (iz.children.get(0) instanceof BinIliIzraz) {
            // <log_i_izraz> ::= <bin_ili_izraz>
            BinIliIzraz binIliIzraz = (BinIliIzraz) iz.children.get(0);

            String s = generiraj(binIliIzraz);

            iz.tip = binIliIzraz.tip;
            iz.l_izraz = binIliIzraz.l_izraz;
            iz.labela = binIliIzraz.labela;

            return s;
        } else {
            // <log_i_izraz> ::= <log_i_izraz> OP_I <bin_ili_izraz>
            LogIIzraz logIIzraz = (LogIIzraz) iz.children.get(0);
            BinIliIzraz binIliIzraz = (BinIliIzraz) iz.children.get(2);

            String s1 = generiraj(logIIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(logIIzraz.tip, new Tip(TipEnum.INT)), iz);
            String s2 = generiraj(binIliIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(binIliIzraz.tip, new Tip(TipEnum.INT)), iz);

            iz.tip = new Tip(TipEnum.INT);
            iz.l_izraz = false;

            StringBuilder sb = new StringBuilder();
            sb.append(s1);
            sb.append("\n\tPOP R0");
            sb.append("\n\tCMP R0, 0");
            sb.append("\n\tMOVE 0, R2");
            String l0 = novoImeLabele();
            sb.append(String.format("\n\tJ_EQ %s", l0));

            sb.append(s2);
            sb.append("\n\tPOP R0");
            sb.append("\n\tCMP R0, 0");
            sb.append("\n\tMOVE 0, R2");
            sb.append(String.format("\n\tJ_EQ %s", l0));

            sb.append("\n\tMOVE 1, R2");
            sb.append(String.format("\n%s\tPUSH R2", l0));

            return sb.toString();
        }
    }

    public String generiraj(LogIliIzraz iz) {
        if (iz.children.get(0) instanceof LogIIzraz) {
            // <log_ili_izraz> ::= <log_i_izraz>
            LogIIzraz logIIzraz = (LogIIzraz) iz.children.get(0);

            String s = generiraj(logIIzraz);

            iz.tip = logIIzraz.tip;
            iz.l_izraz = logIIzraz.l_izraz;
            iz.labela = logIIzraz.labela;

            return s;
        } else {
            // <log_ili_izraz> ::= <log_ili_izraz> OP_ILI <log_i_izraz>
            LogIliIzraz logIliIzraz = (LogIliIzraz) iz.children.get(0);
            LogIIzraz logIIzraz = (LogIIzraz) iz.children.get(2);

            String s1 = generiraj(logIliIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(logIliIzraz.tip, new Tip(TipEnum.INT)), iz);
            String s2 = generiraj(logIIzraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(logIIzraz.tip, new Tip(TipEnum.INT)), iz);

            iz.tip = new Tip(TipEnum.INT);
            iz.l_izraz = false;

            StringBuilder sb = new StringBuilder();
            sb.append(s1);
            sb.append("\n\tPOP R0");
            sb.append("\n\tCMP R0, 0");
            sb.append("\n\tMOVE 1, R2");
            String l0 = novoImeLabele();
            sb.append(String.format("\n\tJ_NE %s", l0));

            sb.append(s2);
            sb.append("\n\tPOP R0");
            sb.append("\n\tCMP R0, 0");
            sb.append("\n\tMOVE 1, R2");
            sb.append(String.format("\n\tJ_NE %s", l0));

            sb.append("\n\tMOVE 0, R2");
            sb.append(String.format("\n%s\tPUSH R2", l0));

            return sb.toString();
        }
    }

    public String generiraj(IzrazPridruzivanja iz) {
        if (iz.children.get(0) instanceof LogIliIzraz) {
            // <izraz_pridruzivanja> ::= <log_ili_izraz>
            LogIliIzraz logIliIzraz = (LogIliIzraz) iz.children.get(0);

            String s = generiraj(logIliIzraz);

            iz.tip = logIliIzraz.tip;
            iz.l_izraz = logIliIzraz.l_izraz;
            iz.labela = logIliIzraz.labela;

            return s;
        } else {
            // <izraz_pridruzivanja> ::= <postfiks_izraz> OP_PRIDRUZI <izraz_pridruzivanja>
            PostfiksIzraz postfiksIzraz = (PostfiksIzraz) iz.children.get(0);
            IzrazPridruzivanja izrazPridruzivanja = (IzrazPridruzivanja) iz.children.get(2);

            String s1 = generiraj(postfiksIzraz);
            assertOrError(postfiksIzraz.l_izraz == true, iz);
            String s2 = generiraj(izrazPridruzivanja);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(izrazPridruzivanja.tip, postfiksIzraz.tip), iz);

            iz.tip = postfiksIzraz.tip;
            iz.l_izraz = false;

            StringBuilder sb = new StringBuilder();
            sb.append(s1);
            sb.append(s2);
            sb.append("\n\tPOP R0"); // will contain value of izraz_pridruzivanja
            sb.append("\n\tPOP R1");
            sb.append(String.format("\n\tSTORE R0, (%s)", postfiksIzraz.labela));
            sb.append("\n\tPUSH R0");

            return sb.toString();
        }
    }

    public String generiraj(Izraz iz) {
        if (iz.children.get(0) instanceof IzrazPridruzivanja) {
            // <izraz> ::= <izraz_pridruzivanja>
            IzrazPridruzivanja izrazPridruzivanja = (IzrazPridruzivanja) iz.children.get(0);

            String s = generiraj(izrazPridruzivanja);

            iz.tip = izrazPridruzivanja.tip;
            iz.l_izraz = izrazPridruzivanja.l_izraz;
            iz.labela = izrazPridruzivanja.labela;

            return s;
        } else {
            // <izraz> ::= <izraz> ZAREZ <izraz_pridruzivanja>
            Izraz izraz = (Izraz) iz.children.get(0);
            IzrazPridruzivanja izrazPridruzivanja = (IzrazPridruzivanja) iz.children.get(2);

            String s1 = generiraj(izraz);
            String s2 = generiraj(izrazPridruzivanja);

            iz.tip = izraz.tip;
            iz.l_izraz = false;

            StringBuilder sb = new StringBuilder();
            sb.append(s1);
            sb.append(s2);
            sb.append("\n\tPOP R0");
            sb.append("\n\tPOP R1");
            sb.append("\n\tPUSH R0");

            return sb.toString();
        }
    }

    public String generiraj(SlozenaNaredba na) {

        if (na.children.get(1) instanceof ListaNaredbi) {
            // <slozena_naredba> ::= L_VIT_ZAGRADA <lista_naredbi> D_VIT_ZAGRADA
            ListaNaredbi listaNaredbi = (ListaNaredbi) na.children.get(1);

            return generiraj(listaNaredbi);
        } else {
            // <slozena_naredba> ::= L_VIT_ZAGRADA <lista_deklaracija> <lista_naredbi>
            // D_VIT_ZAGRADA
            ListaDeklaracija listaDeklaracija = (ListaDeklaracija) na.children.get(1);
            ListaNaredbi listaNaredbi = (ListaNaredbi) na.children.get(2);

            String s1 = generiraj(listaDeklaracija);
            String s2 = generiraj(listaNaredbi);
            return s1 + s2;
        }
    }

    public String generiraj(ListaNaredbi na) {
        if (na.children.get(0) instanceof Naredba) {
            // <lista_naredbi> ::= <naredba>
            Naredba naredba = (Naredba) na.children.get(0);

            return generiraj(naredba);
        } else if (na.children.get(0) instanceof ListaNaredbi) {
            // <lista_naredbi> ::= <lista_naredbi> <naredba>
            ListaNaredbi listaNaredbi = (ListaNaredbi) na.children.get(0);
            Naredba naredba = (Naredba) na.children.get(1);

            String s1 = generiraj(listaNaredbi);
            String s2 = generiraj(naredba);
            return s1 + s2;
        } else
            throw new BadCodeError();
    }

    public String generiraj(Naredba na) {
        if (na.children.get(0) instanceof SlozenaNaredba) {
            SlozenaNaredba naredba = (SlozenaNaredba) na.children.get(0);
            // slozena naredba u novom djelokrugu
            lokalniDjelokrug = new Djelokrug(lokalniDjelokrug);
            if (na.parent instanceof NaredbaPetlje) {
                lokalniDjelokrug.tipDjelokruga = TipDjelokruga.PETLJA;
            }
            String s1 = generiraj(naredba);
            lokalniDjelokrug = lokalniDjelokrug.ugnjezdujuciDjelokrug;
            return s1;
        } else if (na.children.get(0) instanceof IzrazNaredba) {
            IzrazNaredba naredba = (IzrazNaredba) na.children.get(0);
            // TODO dal tu ide pop?
            return generiraj(naredba);
        } else if (na.children.get(0) instanceof NaredbaGrananja) {
            NaredbaGrananja naredba = (NaredbaGrananja) na.children.get(0);
            return generiraj(naredba);
        } else if (na.children.get(0) instanceof NaredbaPetlje) {
            NaredbaPetlje naredba = (NaredbaPetlje) na.children.get(0);
            // TODO return generiraj(naredba);
            throw new UnsupportedOperationException();
        } else {
            NaredbaSkoka naredba = (NaredbaSkoka) na.children.get(0);
            return generiraj(naredba);
        }
    }

    public String generiraj(IzrazNaredba na) {
        if (na.children.get(0) instanceof Konstanta) {
            // <izraz_naredba> ::= TOCKAZAREZ
            // TODO razmisli dal tu treba stavljat nes na stack; kaj for(;;) ocekuje?
            na.tip = new Tip(TipEnum.INT);
            return "";
        } else {
            // <izraz_naredba> ::= <izraz> TOCKAZAREZ
            Izraz izraz = (Izraz) na.children.get(0);

            String s = generiraj(izraz);

            na.tip = izraz.tip;

            return s;
        }
    }

    public String generiraj(NaredbaGrananja na) {
        if (na.children.size() == 5) {
            // <naredba_grananja> ::= KR_IF L_ZAGRADA <izraz> D_ZAGRADA <naredba>
            Izraz izraz = (Izraz) na.children.get(2);
            Naredba naredba = (Naredba) na.children.get(4);

            String si = generiraj(izraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(izraz.tip, new Tip(TipEnum.INT)), na);
            String sn = generiraj(naredba);

            StringBuilder sb = new StringBuilder();

            sb.append(si);
            sb.append("\n\tPOP R0");
            sb.append("\n\tCMP R0, 0");
            
            String l1 = novoImeLabele();
            sb.append(String.format("\n\tJP_EQ %s", l1));
            sb.append(sn);
            sb.append(String.format("\n%s", l1));

            return sb.toString();

        } else {
            // <naredba_grananja> ::= KR_IF L_ZAGRADA <izraz> D_ZAGRADA <naredba>1 KR_ELSE
            // <naredba>2
            Izraz izraz = (Izraz) na.children.get(2);
            Naredba naredba1 = (Naredba) na.children.get(4);
            Naredba naredba2 = (Naredba) na.children.get(6);

            String sIzraz = generiraj(izraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(izraz.tip, new Tip(TipEnum.INT)), na);
            String sTrue = generiraj(naredba1);
            String sFalse = generiraj(naredba2);
            
            StringBuilder sb = new StringBuilder();

            sb.append(sIzraz);
            sb.append("\n\tPOP R0");
            sb.append("\n\tCMP R0, 0");
            
            String lElse = novoImeLabele();
            String lEnd = novoImeLabele();
            sb.append(String.format("\n\tJP_EQ %s", lElse));
            sb.append(sTrue);
            sb.append(String.format("\n\tJP %s", lEnd));

            sb.append(String.format("\n%s", lElse));
            sb.append(sFalse);
            sb.append(String.format("\n%s", lEnd));

            return sb.toString();
        }
    }

    public void provjeri(NaredbaPetlje na) {
        throw new UnsupportedOperationException();
        /*Konstanta kljucnaRijec = (Konstanta) na.children.get(0);
        if (kljucnaRijec.konstantaTip == KonstantaEnum.KR_WHILE) {
            // <naredba_petlje> ::= KR_WHILE L_ZAGRADA <izraz> D_ZAGRADA <naredba>
            Izraz izraz = (Izraz) na.children.get(2);
            Naredba naredba = (Naredba) na.children.get(4);

            generiraj(izraz);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(izraz.tip, new Tip(TipEnum.INT)), na);
            generiraj(naredba);
        } else if (kljucnaRijec.konstantaTip == KonstantaEnum.KR_FOR && na.children.size() == 6) {
            // <naredba_petlje> ::= KR_FOR L_ZAGRADA <izraz_naredba>1 <izraz_naredba>2
            // D_ZAGRADA <naredba>
            IzrazNaredba izrazNaredba1 = (IzrazNaredba) na.children.get(2);
            IzrazNaredba izrazNaredba2 = (IzrazNaredba) na.children.get(3);
            Naredba naredba = (Naredba) na.children.get(5);

            provjeri(izrazNaredba1);
            provjeri(izrazNaredba2);
            assertOrError(Tip.seMozeImplicitnoPretvoritiIzU(izrazNaredba2.tip, new Tip(TipEnum.INT)), na);
            generiraj(naredba);
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
            generiraj(izraz);
            generiraj(naredba);
        }*/
    }

    public String generiraj(NaredbaSkoka na) {
        KonstantaEnum kljucnaRijec = ((Konstanta) na.children.get(0)).konstantaTip;

        if (kljucnaRijec == KonstantaEnum.KR_CONTINUE || kljucnaRijec == KonstantaEnum.KR_BREAK) {
            // <naredba_skoka> ::= (KR_CONTINUE | KR_BREAK) TOCKAZAREZ

            assertOrError(lokalniDjelokrug.jeUnutarPetlje(), na);
            throw new UnsupportedOperationException();

        } else if (kljucnaRijec == KonstantaEnum.KR_RETURN && na.children.size() == 2) {
            // <naredba_skoka> ::= KR_RETURN TOCKAZAREZ

            assertOrError(lokalniDjelokrug.jeUnutarFunkcijePovratneVrijednosti(new Tip(TipEnum.VOID)), na);

            return "\nRET";

        } else {
            // <naredba_skoka> ::= KR_RETURN <izraz> TOCKAZAREZ
            Izraz izraz = (Izraz) na.children.get(1);

            String s = generiraj(izraz);
            assertOrError(
                    Tip.seMozeImplicitnoPretvoritiIzU(izraz.tip, lokalniDjelokrug.povratniTipUgnjezdujuceFunkcije()),
                    na);

            StringBuilder sb = new StringBuilder();
            sb.append(s);
            sb.append("\n\tPOP R6");
            sb.append("\n\tRET");
            return sb.toString();
        }
    }

    public String generiraj(PrijevodnaJedinica pi) {
        if (pi.children.get(0) instanceof VanjskaDeklaracija) {
            // <prijevodna_jedinica> ::= <vanjska_deklaracija>
            VanjskaDeklaracija vanjskaDeklaracija = (VanjskaDeklaracija) pi.children.get(0);

            return generiraj(vanjskaDeklaracija);
        } else {
            // <prijevodna_jedinica> ::= <prijevodna_jedinica> <vanjska_deklaracija>
            PrijevodnaJedinica prijevodnaJedinica = (PrijevodnaJedinica) pi.children.get(0);
            VanjskaDeklaracija vanjskaDeklaracija = (VanjskaDeklaracija) pi.children.get(1);

            String s1 = generiraj(prijevodnaJedinica);
            String s2 = generiraj(vanjskaDeklaracija);

            return s1 + s2;
        }
    }

    public String generiraj(VanjskaDeklaracija vd) {
        if (vd.children.get(0) instanceof DefinicijaFunkcije) {
            DefinicijaFunkcije definicijaFunkcije = (DefinicijaFunkcije) vd.children.get(0);

            return generiraj(definicijaFunkcije);
        } else {
            Deklaracija deklaracija = (Deklaracija) vd.children.get(0);
            return generiraj(deklaracija);
        }
    }

    public String generiraj(DefinicijaFunkcije de) {
        if (de.children.get(3) instanceof Konstanta) {
            // <definicija_funkcije> ::= <ime_tipa> IDN L_ZAGRADA KR_VOID D_ZAGRADA
            // <slozena_naredba>
            ImeTipa imeTipa = (ImeTipa) de.children.get(0);
            Konstanta identifikator = (Konstanta) de.children.get(1);
            SlozenaNaredba slozenaNaredba = (SlozenaNaredba) de.children.get(5);

            provjeri(imeTipa);
            assertOrError(!Tip.isConstT(imeTipa.tip), de);
            assertOrError(!postojiDefiniranaFunkcija(identifikator.vrijednost), de);
            Identifikator funkcija = globalniDjelokrug.lokalnaVarijabla(identifikator.vrijednost);
            FunkcijaTip tipFunkcije = new FunkcijaTip(new Tip[0], imeTipa.tip);
            if (funkcija != null) {
                assertOrError(funkcija.tip.equals(tipFunkcije), de);
            }
            IdentifikatorFunkcije f = definirajFunkciju(identifikator.vrijednost, tipFunkcije);
            lokalniDjelokrug = new Djelokrug(lokalniDjelokrug);
            lokalniDjelokrug.setFunkcija(tipFunkcije);
            f.kodTjelaFunkcije = generiraj(slozenaNaredba) + "\n\tRET"; // jer void funkcije ne moraju imat return
            lokalniDjelokrug = lokalniDjelokrug.ugnjezdujuciDjelokrug;
            return "";

        } else if (de.children.get(3) instanceof ListaParametara) {
            // <definicija_funkcije> ::= <ime_tipa> IDN L_ZAGRADA <lista_parametara>
            // D_ZAGRADA <slozena_naredba>
            ImeTipa imeTipa = (ImeTipa) de.children.get(0);
            Konstanta identifikator = (Konstanta) de.children.get(1);
            ListaParametara listaParametara = (ListaParametara) de.children.get(3);
            SlozenaNaredba slozenaNaredba = (SlozenaNaredba) de.children.get(5);

            provjeri(imeTipa);
            assertOrError(!Tip.isConstT(imeTipa.tip), de);
            assertOrError(!postojiDefiniranaFunkcija(identifikator.vrijednost), de);
            generiraj(listaParametara);
            Identifikator funkcija = globalniDjelokrug.lokalnaVarijabla(identifikator.vrijednost);
            FunkcijaTip tipFunkcije = new FunkcijaTip(listaParametara.tipovi, imeTipa.tip);
            if (funkcija != null) {
                assertOrError(funkcija.tip.equals(tipFunkcije), de);
            }
            IdentifikatorFunkcije f = definirajFunkciju(identifikator.vrijednost, tipFunkcije);
            lokalniDjelokrug = new Djelokrug(lokalniDjelokrug);
            lokalniDjelokrug.setFunkcija(tipFunkcije);
            for (int i = 0; i < listaParametara.tipovi.length; i++) {
                // TODO TODO bitno, pobrinut se da oi dobiju labele, mozda pozivom funkcije ove
                // klase
                // also maknut argumente sa stacka il nes kad se poziva i sl. GL
                lokalniDjelokrug.zabiljeziIdentifikator(listaParametara.imena[i], listaParametara.tipovi[i]);
            }
            f.kodTjelaFunkcije = generiraj(slozenaNaredba) + "\n\tRET"; // jer void funkcije ne moraju imat return;
            lokalniDjelokrug = lokalniDjelokrug.ugnjezdujuciDjelokrug;
            return "";
        }
        throw new BadCodeError();
    }

    public String generiraj(ListaParametara lp) {
        throw new UnsupportedOperationException();
        /*
         * if (lp.children.get(0) instanceof DeklaracijaParametra) {
         * // <lista_parametara> ::= <deklaracija_parametra>
         * DeklaracijaParametra deklaracijaParametra = (DeklaracijaParametra)
         * lp.children.get(0);
         * 
         * // TODO generiraj(deklaracijaParametra); // TODO provjeri, al vjv ne vraca
         * nis
         * 
         * Tip[] tipovi = { deklaracijaParametra.tip };
         * lp.tipovi = tipovi;
         * String[] imena = { deklaracijaParametra.ime };
         * lp.imena = imena;
         * return "";
         * } else if (lp.children.get(0) instanceof ListaParametara) {
         * // <lista_parametara> ::= <lista_parametara> ZAREZ <deklaracija_parametra>
         * ListaParametara listaParametara = (ListaParametara) lp.children.get(0);
         * DeklaracijaParametra deklaracijaParametra = (DeklaracijaParametra)
         * lp.children.get(2);
         * 
         * generiraj(listaParametara);
         * provjeri(deklaracijaParametra);
         * assertOrError(!Arrays.stream(listaParametara.imena).anyMatch(
         * deklaracijaParametra.ime::equals), lp);
         * // boolean anyEqual = false; // if anyMatch above does not work :)
         * // for(String ime : listaParametara.imena) {
         * // if( ime.equals(deklaracijaParametra.ime) ){
         * // anyEqual = true;
         * // break;
         * // }
         * // }
         * // assertOrError(anyEqual, lp);
         * 
         * Tip[] tipovi = new Tip[listaParametara.tipovi.length + 1];
         * String[] imena = new String[listaParametara.imena.length + 1];
         * for (int i = 0; i < listaParametara.tipovi.length; i++) {
         * tipovi[i] = listaParametara.tipovi[i];
         * imena[i] = listaParametara.imena[i];
         * }
         * tipovi[listaParametara.tipovi.length] = deklaracijaParametra.tip;
         * imena[listaParametara.imena.length] = deklaracijaParametra.ime;
         * 
         * lp.tipovi = tipovi;
         * lp.imena = imena;
         * }
         * //
         */
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

    public String generiraj(ListaDeklaracija ld) {
        if (ld.children.get(0) instanceof Deklaracija) {
            // <lista_deklaracija> ::= <deklaracija>
            Deklaracija deklaracija = (Deklaracija) ld.children.get(0);

            return generiraj(deklaracija);
        } else {
            // <lista_deklaracija> ::= <lista_deklaracija> <deklaracija>
            ListaDeklaracija listaDeklaracija = (ListaDeklaracija) ld.children.get(0);
            Deklaracija deklaracija = (Deklaracija) ld.children.get(1);

            String s1 = generiraj(listaDeklaracija);
            String s2 = generiraj(deklaracija);
            return s1+s2;
        }
    }

    public String generiraj(Deklaracija de) {
        // <deklaracija> ::= <ime_tipa> <lista_init_deklaratora> TOCKAZAREZ
        ImeTipa imeTipa = (ImeTipa) de.children.get(0);
        ListaInitDeklaratora listaInitDeklaratora = (ListaInitDeklaratora) de.children.get(1);

        // TODO: handleat liste. mozda nije tu potrebno neg negdje drugdje, al generalno
        provjeri(imeTipa);
        listaInitDeklaratora.ntip = imeTipa.tip;
        return generiraj(listaInitDeklaratora);

    }

    public String generiraj(ListaInitDeklaratora ld) {
        if (ld.children.get(0) instanceof InitDeklarator) {
            // <lista_init_deklaratora> ::= <init_deklarator>
            InitDeklarator initDeklarator = (InitDeklarator) ld.children.get(0);

            initDeklarator.ntip = ld.ntip;
            return generiraj(initDeklarator);
        } else {
            // <lista_init_deklaratora>1 ::= <lista_init_deklaratora>2 ZAREZ
            // <init_deklarator>
            ListaInitDeklaratora listaInitDeklaratora = (ListaInitDeklaratora) ld.children.get(0);
            InitDeklarator initDeklarator = (InitDeklarator) ld.children.get(2);

            listaInitDeklaratora.ntip = ld.ntip;
            String s1 = generiraj(listaInitDeklaratora);
            initDeklarator.ntip = ld.ntip;
            String s2 = generiraj(initDeklarator);

            return s1 + s2;
        }

    }

    public String generiraj(InitDeklarator de) {
        if (de.children.size() == 1) {
            // <init_deklarator> ::= <izravni_deklarator>
            IzravniDeklarator izravniDeklarator = (IzravniDeklarator) de.children.get(0);

            izravniDeklarator.ntip = de.ntip;
            provjeri(izravniDeklarator);
            assertOrError(!Tip.isConstT(izravniDeklarator.tip), de);
            assertOrError(!Tip.isNizConstT(izravniDeklarator.tip), de);

            return "";
        } else {
            // <init_deklarator> ::= <izravni_deklarator> OP_PRIDRUZI <inicijalizator>
            IzravniDeklarator izravniDeklarator = (IzravniDeklarator) de.children.get(0);
            Inicijalizator inicijalizator = (Inicijalizator) de.children.get(2);

            izravniDeklarator.ntip = de.ntip;
            provjeri(izravniDeklarator);
            String s2 = generiraj(inicijalizator);

            StringBuilder sb = new StringBuilder();
            sb.append(s2);
            sb.append("\n\tPOP R0");
            sb.append(String.format("\n\tSTORE R0, (%s)", izravniDeklarator.labela));

            // TODO handleat nizove
            if (Tip.isT(izravniDeklarator.tip) || Tip.isConstT(izravniDeklarator.tip)) {
                assertOrError(Tip.seMozeImplicitnoPretvoritiUT(inicijalizator.tip), de);

            } else if (Tip.isNizT(izravniDeklarator.tip) || Tip.isNizConstT(izravniDeklarator.tip)) {
                assertOrError(inicijalizator.br_elem <= izravniDeklarator.br_elem, de);
                assertOrError(inicijalizator.tipovi != null, de); // TODO nije po uputama, ali popravlja jedan
                                                                  // test-case. Nije mi jasno zasto je tu potrebno
                for (Tip u : inicijalizator.tipovi) {
                    assertOrError(Tip.seMozeImplicitnoPretvoritiUT(u), de);
                }
            } else {
                ispisiError(de);
            }
            return sb.toString();
        }
    }

    public void provjeri(IzravniDeklarator de) {
        if (de.children.size() == 1) {
            // <izravni_deklarator> ::= IDN
            Konstanta identifikator = (Konstanta) de.children.get(0);

            assertOrError(!de.ntip.equals(new Tip(TipEnum.VOID)), de);
            assertOrError(!lokalniDjelokrug.sadrziLokalnuVarijablu(identifikator.vrijednost), de);
            Identifikator idn = zabiljeziIdentifikator(identifikator.vrijednost, de.ntip);

            de.tip = de.ntip;
            de.labela = idn.labela;
        } else if (de.children.get(2) instanceof Konstanta) {
            Konstanta konstanta = (Konstanta) de.children.get(2);
            if (konstanta.konstantaTip == KonstantaEnum.BROJ) {
                throw new UnsupportedOperationException();
                /*
                // <izravni_deklarator> ::= IDN L_UGL_ZAGRADA BROJ D_UGL_ZAGRADA
                Konstanta identifikator = (Konstanta) de.children.get(0);

                assertOrError(!de.ntip.equals(new Tip(TipEnum.VOID)), de);
                assertOrError(!lokalniDjelokrug.sadrziLokalnuVarijablu(identifikator.vrijednost), de);
                try {
                    Integer.parseInt(konstanta.vrijednost);
                } catch (Exception e) {
                    ispisiError(de); // integer izvan range-a (32 bit)
                }
                Tip tip = new KompozitniTip(TipEnum.NIZ, de.ntip);
                Identifikator idn = zabiljeziIdentifikator(identifikator.vrijednost, tip);

                de.tip = tip;
                de.br_elem = Integer.parseInt(konstanta.vrijednost);
                de.labela = idn.labela;/* */
            } else {
                // <izravni_deklarator> ::= IDN L_ZAGRADA KR_VOID D_ZAGRADA
                Konstanta identifikator = (Konstanta) de.children.get(0);
                FunkcijaTip tipFunkcije = new FunkcijaTip(new Tip[0], de.ntip);

                IdentifikatorFunkcije idf = lokalniDjelokrug.funkcija(identifikator.vrijednost);
                if (null != idf) {
                    Tip tipDeklarirane = lokalniDjelokrug.funkcija(identifikator.vrijednost).tip;
                    assertOrError(tipDeklarirane.equals(tipFunkcije), de);
                } else {
                    idf = (IdentifikatorFunkcije) deklarirajFunkciju(identifikator.vrijednost, tipFunkcije);
                }

                de.tip = tipFunkcije;
                de.labela = idf.labela;
            }
        } else {
            // <izravni_deklarator> ::= IDN L_ZAGRADA <lista_parametara> D_ZAGRADA
            throw new UnsupportedOperationException();/*
            Konstanta identifikator = (Konstanta) de.children.get(0);
            ListaParametara listaParametara = (ListaParametara) de.children.get(2);

            generiraj(listaParametara);

            Tip tipFunkcije = new FunkcijaTip(listaParametara.tipovi, de.ntip);

            IdentifikatorFunkcije idf = lokalniDjelokrug.funkcija(identifikator.vrijednost);
            if (null != idf) {
                Tip tipDeklarirane = idf.tip;
                assertOrError(tipDeklarirane.equals(tipFunkcije), de);
            } else {
                idf = (IdentifikatorFunkcije) zabiljeziIdentifikator(identifikator.vrijednost, tipFunkcije);
            }

            de.tip = tipFunkcije;
            de.labela = idf.labela;
            //* */
        }
    }

    public String generiraj(Inicijalizator ic) {
        if (ic.children.get(0) instanceof IzrazPridruzivanja) {
            // <inicijalizator> ::= <izraz_pridruzivanja>
            IzrazPridruzivanja izrazPridruzivanja = (IzrazPridruzivanja) ic.children.get(0);

            String s1 = generiraj(izrazPridruzivanja);

            Konstanta nizZnakova = izrazPridruzivanja.generira(KonstantaEnum.NIZ_ZNAKOVA);
            if (nizZnakova != null) {
                // TODO support za iniijalizaciju niza charova nizom charova
                throw new UnsupportedOperationException();/*
                Tip[] tipovi = new Tip[ic.br_elem];
                for (int i = 0; i < ic.br_elem; i++) {
                    tipovi[i] = new Tip(TipEnum.CHAR);
                }
                ic.br_elem = nizZnakova.vrijednost.length() + 1;
                ic.tipovi = tipovi;
                //* */
            } else {
                ic.tip = izrazPridruzivanja.tip;
                return s1;
            }
        } else {
            // <inicijalizator> ::= L_VIT_ZAGRADA <lista_izraza_pridruzivanja> D_VIT_ZAGRADA
            // ovo je za inicijalizacije listi
            throw new UnsupportedOperationException();/*
            ListaIzrazaPridruzivanja listaIzrazaPridruzivanja = (ListaIzrazaPridruzivanja) ic.children.get(1);

            provjeri(listaIzrazaPridruzivanja);

            ic.br_elem = listaIzrazaPridruzivanja.br_elem;
            ic.tipovi = listaIzrazaPridruzivanja.tipovi;
            //* */
        }
    }

    public void provjeri(ListaIzrazaPridruzivanja lp) {
        if (lp.children.get(0) instanceof IzrazPridruzivanja) {
            // <lista_izraza_pridruzivanja> ::= <izraz_pridruzivanja>
            IzrazPridruzivanja izrazPridruzivanja = (IzrazPridruzivanja) lp.children.get(0);

            // TODO provjeri(izrazPridruzivanja);

            Tip[] tipovi = { izrazPridruzivanja.tip };
            lp.tipovi = tipovi;
            lp.br_elem = 1;
        } else {
            // <lista_izraza_pridruzivanja> ::= <lista_izraza_pridruzivanja> ZAREZ
            // <izraz_pridruzivanja>
            ListaIzrazaPridruzivanja listaIzrazaPridruzivanja = (ListaIzrazaPridruzivanja) lp.children.get(0);
            IzrazPridruzivanja izrazPridruzivanja = (IzrazPridruzivanja) lp.children.get(2);

            provjeri(listaIzrazaPridruzivanja);
            // TODO provjeri(izrazPridruzivanja);

            Tip[] tipovi = new Tip[listaIzrazaPridruzivanja.br_elem + 1];
            for (int i = 0; i < listaIzrazaPridruzivanja.tipovi.length; i++) {
                tipovi[i] = listaIzrazaPridruzivanja.tipovi[i];
            }
            tipovi[listaIzrazaPridruzivanja.tipovi.length] = izrazPridruzivanja.tip;

            lp.tipovi = tipovi;
            lp.br_elem = listaIzrazaPridruzivanja.br_elem + 1;
        }
    }
}
