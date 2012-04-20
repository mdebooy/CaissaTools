/**
 * Copyright 2012 Marco de Booij
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the Licence. You may
 * obtain a copy of the Licence at:
 *
 * http://www.osor.eu/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package eu.debooy.caissatools;

import eu.debooy.caissa.CaissaUtils;
import eu.debooy.caissa.ELO;
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.Spelerinfo;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.access.Bestand;
import eu.debooy.doosutils.access.CvsBestand;
import eu.debooy.doosutils.exception.BestandException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * @author Marco de Booij
 */
public class ELOBerekenaar {
  private ELOBerekenaar() {}

  public static void execute(String[] args) throws PgnException {
    BufferedWriter    output      = null;
    int               startElo    = 1600;
    Map<String, Integer>
                      spelers     = new TreeMap<String, Integer>();
    List<PGN>         partijen    = new ArrayList<PGN>();
    List<Spelerinfo>  spelerinfos = new ArrayList<Spelerinfo>();
    String            charsetIn   = Charset.defaultCharset().name();
    String            charsetUit  = Charset.defaultCharset().name();
    String            startDatum  = "0000.00.00";
    String[]          uitslagen   = new String[] {"0-1", "1/2-1/2", "1-0"};

    Banner.printBanner("ELO Berekenaar");

    Arguments arguments = new Arguments(args);
    arguments.setParameters(new String[] {"charsetin", "charsetuit",
                                          "geschiedenisBestand",
                                          "spelerBestand", "startDatum",
                                          "startELO", "toernooiBestand",
                                          "uitvoerdir"});
    arguments.setVerplicht(new String[] {"spelerBestand", "toernooiBestand"});
    if (!arguments.isValid()) {
      help();
      return;
    }

    if (arguments.hasArgument("charsetin")) {
      charsetIn   = arguments.getArgument("charsetin");
    }
    if (arguments.hasArgument("charsetuit")) {
      charsetUit  = arguments.getArgument("charsetuit");
    }
    String  spelerBestand   = arguments.getArgument("spelerBestand");
    if (!spelerBestand.endsWith(".csv")) {
      spelerBestand = spelerBestand + ".csv";
    }
    if (arguments.hasArgument("startDatum")) {
      startDatum  = arguments.getArgument("startDatum");
    }
    String  toernooiBestand = arguments.getArgument("toernooiBestand");
    if (!toernooiBestand.endsWith(".pgn")) {
      toernooiBestand = toernooiBestand + ".pgn";
    }
    String    uitvoerdir  = ".";
    if (arguments.hasArgument("uitvoerdir")) {
      uitvoerdir  = arguments.getArgument("uitvoerdir");
    }
    if (uitvoerdir.endsWith(File.separator)) {
      uitvoerdir  = uitvoerdir.substring(0,
                                         uitvoerdir.length()
                                         - File.separator.length());
    }

    BufferedWriter  geschiedenis  = null;
    if (arguments.hasArgument("geschiedenisBestand")) {
      String  geschiedenisBestand =
          arguments.getArgument("geschiedenisBestand");
      if (!geschiedenisBestand.endsWith(".csv")) {
        geschiedenisBestand = geschiedenisBestand + ".csv";
      }
      try {
        geschiedenis  = Bestand.openUitvoerBestand(uitvoerdir + File.separator
                                                   + geschiedenisBestand,
                                                   charsetUit, true);
      } catch (BestandException e) {
        System.out.println(e.getLocalizedMessage());
      }
    }

    CvsBestand  cvs = null;
    try {
      cvs = new CvsBestand(uitvoerdir + File.separator + spelerBestand);
      while (cvs.hasNext()) {
        String[]  veld  = cvs.next();
        int spelerId  = spelers.size();
        spelers.put(veld[0], spelerId);
        Spelerinfo  spelerinfo  = new Spelerinfo();
        spelerinfo.setElo(new Integer(veld[1]));
        spelerinfo.setNaam(veld[0]);
        spelerinfo.setPartijen(new Integer(veld[2]));
        spelerinfo.setSpelerId(spelerId);
        spelerinfos.add(spelerId, spelerinfo);
      }
    } catch (BestandException e) {
      System.out.println("Nieuw bestand " + spelerBestand + " aanmaken.");
    } finally {
      try {
        if (cvs != null) {
          cvs.close();
        }
      } catch (BestandException e) {
        System.out.println(e.getLocalizedMessage());
      }
    }
    try {
      partijen  = CaissaUtils.laadPgnBestand(toernooiBestand, charsetIn);
      Collections.sort(partijen);
      for (PGN  partij : partijen) {
        String  datum     = partij.getTag("Date");
        if (startDatum.compareTo(datum) <= 0) {
          String  wit       = partij.getTag("White");
          String  zwart     = partij.getTag("Black");
          String  resultaat = partij.getTag("Result");
          int uitslag = 0;
          for (String s: uitslagen) {
            if (s.equals(resultaat)) {
              break;
            }
            uitslag++;
          }
          if (uitslag < 3) {
            if (!spelers.containsKey(wit)) {
              int spelerId  = spelers.size();
              spelers.put(wit, spelerId);
              Spelerinfo  spelerinfo  = new Spelerinfo();
              spelerinfo.setElo(startElo);
              spelerinfo.setNaam(wit);
              spelerinfo.setPartijen(new Integer(0));
              spelerinfo.setSpelerId(spelerId);
              spelerinfos.add(spelerId, spelerinfo);
            }
            if (!spelers.containsKey(zwart)) {
              int spelerId  = spelers.size();
              spelers.put(zwart, spelerId);
              Spelerinfo  spelerinfo  = new Spelerinfo();
              spelerinfo.setElo(startElo);
              spelerinfo.setNaam(zwart);
              spelerinfo.setPartijen(new Integer(0));
              spelerinfo.setSpelerId(spelerId);
              spelerinfos.add(spelerId, spelerinfo);
            }
            int     witId     = spelers.get(wit);
            int     zwartId   = spelers.get(zwart);
            Integer witElo    = spelerinfos.get(witId).getElo();
            Integer zwartElo  = spelerinfos.get(zwartId).getElo();
            spelerinfos.get(witId)
                       .setElo(ELO.berekenELO(witElo, uitslag, zwartElo,
                                              spelerinfos.get(witId)
                                                         .getPartijen()));
            spelerinfos.get(witId).addPartij();
            spelerinfos.get(zwartId)
                       .setElo(ELO.berekenELO(zwartElo, 2 - uitslag, witElo,
                                              spelerinfos.get(zwartId)
                                                         .getPartijen()));
            spelerinfos.get(zwartId).addPartij();
            if (null != geschiedenis) {
              geschiedenis.write("\"" + wit + "\"," + datum + ","
                                 + spelerinfos.get(witId).getElo() + ","
                                 + spelerinfos.get(witId).getPartijen());
              geschiedenis.newLine();
              geschiedenis.write("\"" + zwart + "\"," + datum + ","
                                 + spelerinfos.get(zwartId).getElo() + ","
                                 + spelerinfos.get(zwartId).getPartijen());
              geschiedenis.newLine();
            }

          }
        }
      }

      if (null != geschiedenis) {
        geschiedenis.close();
      }

      output  = Bestand.openUitvoerBestand(uitvoerdir + File.separator
                                           + spelerBestand, charsetUit);
      String  lijn  = "\"speler\",\"elo\",\"partijen\"";
      output.write(lijn);
      output.newLine();
      for (Integer spelerId  : spelers.values()) {
        lijn  = "\"" + spelerinfos.get(spelerId).getNaam() + "\","
                + spelerinfos.get(spelerId).getElo() + ","
                + spelerinfos.get(spelerId).getPartijen();
        output.write(lijn);
        output.newLine();
      }
    } catch (IOException e) {
      System.out.println(e.getLocalizedMessage());
    } catch (BestandException e) {
      System.out.println(e.getLocalizedMessage());
    } finally {
      try {
        if (output != null) {
          output.close();
        }
      } catch (IOException ex) {
        System.out.println(ex.getLocalizedMessage());
      }
    }
    System.out.println("Bestand : " + spelerBestand);
    System.out.println("Partijen: " + partijen.size());
    System.out.println("Klaar.");
  }


  /**
   * Geeft de 'help' pagina.
   */
  protected static void help() {
    System.out.println("java -jar CaissaTools.jar ELOBerekenaar [OPTIE...] \\");
    System.out.println("  --spelerBestand=<CSV bestand> \\");
    System.out.println("  --toernooiBestand=<PGN bestand>");
    System.out.println();
    System.out.println("  --charsetin           De characterset van <bestand> als deze niet "+ Charset.defaultCharset().name() + " is.");
    System.out.println("  --charsetuit          De characterset van de uitvoer als deze niet "+ Charset.defaultCharset().name() + " moet zijn.");
    System.out.println("  --geschiedenisBestand Het bestand (met .csv extensie) met de evolutie van de ratings.");
    System.out.println("  --spelerBestand       Een CSV bestand (met .csv extensie) met de huidige rating van de spelers.");
    System.out.println("                        Dit bestand wordt bijgewerkt of aangemaakt.");
    System.out.println("  --startDatum          De datum vanaf wanneer de partijen meetellen (yyyy.mm.dd)");
    System.out.println("  --startELO            De start ELO punten voor 'unrated' spelers. De default waarde is 1600.");
    System.out.println("  --toernooiBestand     Het bestand met de partijen in PGN formaat.");
    System.out.println("  --uitvoerdir          De directory voor het geschiedenisBestqnd en spelerBestand.");
    System.out.println();
    System.out.println("spelerBestand en toernooiBestand zijn verplicht.");
    System.out.println();
  }
}
