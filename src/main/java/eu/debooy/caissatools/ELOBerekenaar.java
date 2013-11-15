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

import eu.debooy.caissa.CaissaConstants;
import eu.debooy.caissa.CaissaUtils;
import eu.debooy.caissa.ELO;
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.Spelerinfo;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.Bestand;
import eu.debooy.doosutils.access.CvsBestand;
import eu.debooy.doosutils.exception.BestandException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;


/**
 * @author Marco de Booij
 */
public final class ELOBerekenaar {
  private static final  int START_ELO       = 1600;

  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private ELOBerekenaar() {}

  public static void execute(String[] args) throws PgnException {
    BufferedWriter    output      = null;
    Date              eloDatum    = null;
    int               aantal      = 0;
    int               startElo    = START_ELO;
    int               verwerkt    = 0;
    Map<String, Integer>
                      spelers     = new TreeMap<String, Integer>();
    List<PGN>         partijen    = new ArrayList<PGN>();
    List<Spelerinfo>  spelerinfos = new ArrayList<Spelerinfo>();
    String            charsetIn   = Charset.defaultCharset().name();
    String            charsetUit  = Charset.defaultCharset().name();
    String            startDatum  = "0000.00.00";
    String[]          uitslagen   = new String[] {"0-1", "1/2-1/2", "1-0"};

    Banner.printBanner(resourceBundle.getString("banner.eloberekenaar"));

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
    if (spelerBestand.contains(File.separator)) {
      help();
      return;
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
      if (geschiedenisBestand.contains(File.separator)) {
        help();
        return;
      }
      if (!geschiedenisBestand.endsWith(".csv")) {
        geschiedenisBestand = geschiedenisBestand + ".csv";
      }
      try {
        geschiedenis  = Bestand.openUitvoerBestand(uitvoerdir + File.separator
                                                   + geschiedenisBestand,
                                                   charsetUit, true);
      } catch (BestandException e) {
        DoosUtils.naarScherm(e.getLocalizedMessage());
      }
    }

    CvsBestand  cvs = null;
    try {
      // Is eigenlijk een uitvoer.
      cvs = new CvsBestand(uitvoerdir + File.separator + spelerBestand,
                           charsetUit);
      while (cvs.hasNext()) {
        String[]  veld  = cvs.next();
        int spelerId  = spelers.size();
        spelers.put(veld[0], spelerId);
        Spelerinfo  spelerinfo  = new Spelerinfo();
        spelerinfo.setElo(Integer.valueOf(veld[1]));
        spelerinfo.setNaam(veld[0]);
        spelerinfo.setPartijen(Integer.valueOf(veld[2]));
        if (spelerinfo.getPartijen() > ELO.MIN_PARTIJEN) {
          spelerinfo.setOfficieel(Datum.toDate(veld[3],
                                  CaissaConstants.PGN_DATUM_FORMAAT));
          spelerinfo.setMinElo(Integer.valueOf(veld[4]));
          spelerinfo.setMinDatum(Datum.toDate(veld[5],
                                 CaissaConstants.PGN_DATUM_FORMAAT));
          spelerinfo.setMaxElo(Integer.valueOf(veld[6]));
          spelerinfo.setMaxDatum(Datum.toDate(veld[7],
                                 CaissaConstants.PGN_DATUM_FORMAAT));
        }
        spelerinfo.setSpelerId(spelerId);
        spelerinfos.add(spelerId, spelerinfo);
      }
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(
          MessageFormat.format(
              resourceBundle.getString("error.maaknieuwbestand"),
              spelerBestand));
    } catch (ParseException e) {
      DoosUtils.foutNaarScherm(
          MessageFormat.format(
              resourceBundle.getString("error.foutedatumin"),
              spelerBestand) + " [" + e.getLocalizedMessage() + "].");
    } finally {
      try {
        if (cvs != null) {
          cvs.close();
        }
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }
    try {
      partijen  = CaissaUtils.laadPgnBestand(toernooiBestand, charsetIn);
      Collections.sort(partijen);
      for (PGN  partij : partijen) {
        if (!partij.isBye()
            && partij.isRated()) {
          String  datum     = partij.getTag("Date");
          if (startDatum.compareTo(datum) <= 0) {
            verwerkt++;
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
                spelerinfo.setPartijen(Integer.valueOf(0));
                spelerinfo.setSpelerId(spelerId);
                spelerinfos.add(spelerId, spelerinfo);
              }
              if (!spelers.containsKey(zwart)) {
                int spelerId  = spelers.size();
                spelers.put(zwart, spelerId);
                Spelerinfo  spelerinfo  = new Spelerinfo();
                spelerinfo.setElo(startElo);
                spelerinfo.setNaam(zwart);
                spelerinfo.setPartijen(Integer.valueOf(0));
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
              // Min and Max ELO
              try {
                eloDatum  =
                    Datum.toDate(datum, CaissaConstants.PGN_DATUM_FORMAAT);
              } catch (ParseException e) {
                DoosUtils.foutNaarScherm(
                    MessageFormat.format(
                        resourceBundle.getString("error.foutedatum"),
                        datum) + " [" + e.getLocalizedMessage() + "].");
                eloDatum  = null;
              }
              // Wit
              aantal    = spelerinfos.get(witId).getPartijen();
              witElo    = spelerinfos.get(witId).getElo();
              if (aantal == ELO.MIN_PARTIJEN + 1) {
                spelerinfos.get(witId).setMinElo(witElo);
                spelerinfos.get(witId).setMaxElo(witElo);
                spelerinfos.get(witId).setMinDatum(eloDatum);
                spelerinfos.get(witId).setMaxDatum(eloDatum);
                spelerinfos.get(witId).setOfficieel(eloDatum);
              }
              if (aantal > ELO.MIN_PARTIJEN + 1) {
                if (spelerinfos.get(witId).getMinElo() > witElo) {
                  spelerinfos.get(witId).setMinElo(witElo);
                  spelerinfos.get(witId).setMinDatum(eloDatum);
                }
                if (spelerinfos.get(witId).getMaxElo() < witElo) {
                  spelerinfos.get(witId).setMaxElo(witElo);
                  spelerinfos.get(witId).setMaxDatum(eloDatum);
                }
              }
              // Zwart
              aantal    = spelerinfos.get(zwartId).getPartijen();
              zwartElo  = spelerinfos.get(zwartId).getElo();
              if (aantal == ELO.MIN_PARTIJEN + 1) {
                spelerinfos.get(zwartId).setMinElo(zwartElo);
                spelerinfos.get(zwartId).setMaxElo(zwartElo);
                spelerinfos.get(zwartId).setMinDatum(eloDatum);
                spelerinfos.get(zwartId).setMaxDatum(eloDatum);
                spelerinfos.get(zwartId).setOfficieel(eloDatum);
              }
              if (aantal > ELO.MIN_PARTIJEN + 1) {
                if (spelerinfos.get(zwartId).getMinElo() > zwartElo) {
                  spelerinfos.get(zwartId).setMinElo(zwartElo);
                  spelerinfos.get(zwartId).setMinDatum(eloDatum);
                }
                if (spelerinfos.get(zwartId).getMaxElo() < zwartElo) {
                  spelerinfos.get(zwartId).setMaxElo(zwartElo);
                  spelerinfos.get(zwartId).setMaxDatum(eloDatum);
                }
              }
  
            }
          }
        }
      }

      if (null != geschiedenis) {
        geschiedenis.close();
      }

      output  = Bestand.openUitvoerBestand(uitvoerdir + File.separator
                                           + spelerBestand, charsetUit);
      StringBuffer  lijn  = new StringBuffer();
      lijn.append("\"speler\",\"elo\",\"partijen\",\"eersteEloDatum\",")
          .append("\"minElo\",\"minEloDatum\",\"maxElo\",\"maxEloDatum\"");
      output.write(lijn.toString());
      output.newLine();
      for (Integer spelerId  : spelers.values()) {
        try {
          aantal  = spelerinfos.get(spelerId).getPartijen();
          lijn    = new StringBuffer();
          lijn.append("\"")
              .append(spelerinfos.get(spelerId).getNaam()).append("\",")
              .append(spelerinfos.get(spelerId).getElo()).append(",")
              .append(aantal).append(",");
          if (spelerinfos.get(spelerId).getPartijen() <= ELO.MIN_PARTIJEN) {
            lijn.append(",,,,");
          } else {
            lijn.append(Datum.fromDate(spelerinfos.get(spelerId).getOfficieel(),
                                       CaissaConstants.PGN_DATUM_FORMAAT))
                .append(",")
                .append(spelerinfos.get(spelerId).getMinElo()).append(",")
                .append(Datum.fromDate(spelerinfos.get(spelerId).getMinDatum(),
                                       CaissaConstants.PGN_DATUM_FORMAAT))
                .append(",")
                .append(spelerinfos.get(spelerId).getMaxElo()).append(",")
                .append(Datum.fromDate(spelerinfos.get(spelerId).getMaxDatum(),
                                       CaissaConstants.PGN_DATUM_FORMAAT));
          }
        } catch (ParseException e) {
          DoosUtils.foutNaarScherm(resourceBundle.getString("error.foutedatum")
                               + e.getLocalizedMessage() + "].");
        }
        output.write(lijn.toString());
        output.newLine();
      }
    } catch (IOException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }
    try {
      if (null != output) {
        output.close();
      }
    } catch (IOException ex) {
      DoosUtils.foutNaarScherm(ex.getLocalizedMessage());
    }

    DoosUtils.naarScherm(resourceBundle.getString("label.bestand") + " "
                         + uitvoerdir + File.separator + spelerBestand);
    DoosUtils.naarScherm(resourceBundle.getString("label.partijen") + " "
                         + partijen.size());
    DoosUtils.naarScherm(resourceBundle.getString("label.verwerkt") + " "
                         + verwerkt);
    DoosUtils.naarScherm(resourceBundle.getString("label.klaar"));
  }


  /**
   * Geeft de 'help' pagina.
   */
  protected static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar ELOBerekenaar ["
                         + resourceBundle.getString("label.optie")
                         + "] \\");
    DoosUtils.naarScherm("    --spelerBestand=<"
                         + resourceBundle.getString("label.csvbestand")
                         + "> --toernooiBestand=<"
                         + resourceBundle.getString("label.pgnbestand") + ">");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm("  --charsetin           ",
        MessageFormat.format(resourceBundle.getString("help.charsetin"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --charsetuit          ",
        MessageFormat.format(resourceBundle.getString("help.charsetuit"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --geschiedenisBestand ",
                         resourceBundle.getString("help.geschiedenisbestand"),
                         80);
    DoosUtils.naarScherm("  --spelerBestand       ",
                         resourceBundle.getString("help.spelerbestand"), 80);
    DoosUtils.naarScherm("  --startDatum          ",
                         resourceBundle.getString("help.startdatum"), 80);
    DoosUtils.naarScherm("  --startELO            ",
        MessageFormat.format(resourceBundle.getString("help.startelo"),
                             START_ELO), 80);
    DoosUtils.naarScherm("  --toernooiBestand     ",
                         resourceBundle.getString("help.toernooibestand"), 80);
    DoosUtils.naarScherm("  --uitvoerdir          ",
                         resourceBundle.getString("help.uitvoerdir"), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("help.paramsverplicht"),
                             "spelerBestand", "toernooiBestand"), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(resourceBundle.getString("help.eloberekenaar.extra"));
    DoosUtils.naarScherm();
  }
}
