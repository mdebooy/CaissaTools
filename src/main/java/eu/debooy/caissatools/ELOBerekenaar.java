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
import eu.debooy.doosutils.access.CsvBestand;
import eu.debooy.doosutils.exception.BestandException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * @author Marco de Booij
 */
public final class ELOBerekenaar {
  private static final  int START_ELO = 1600;

  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());
  private static  Integer         kFactor;
  private static  Integer         maxVerschil = ELO.MAX_VERSCHIL;

  private ELOBerekenaar() {}

  public static void execute(String[] args) throws PgnException {
    String            charsetIn   = Charset.defaultCharset().name();
    String            charsetUit  = Charset.defaultCharset().name();
    String            eindDatum   = "9999.99.99";
    List<Spelerinfo>  spelerinfos = new ArrayList<Spelerinfo>();
    Map<String, Integer>
                      spelers     = new TreeMap<String, Integer>();
    String            startDatum  = "0000.00.00";
    int               startElo    = START_ELO;
    List<String>      fouten      = new ArrayList<String>();

    Banner.printBanner(resourceBundle.getString("banner.eloberekenaar"));

    Arguments arguments = new Arguments(args);
    arguments.setParameters(new String[] {"charsetin", "charsetuit", 
                                          "eindDatum", "geschiedenisBestand",
                                          "invoerdir", "maxVerschil",
                                          "spelerBestand", "startDatum",
                                          "startELO", "toernooiBestand",
                                          "uitvoerdir", "vasteKfactor"});
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
    if (spelerBestand.contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              resourceBundle.getString("error.bevatdirectory"),
                                       "spelerBestand"));
    }
    if (!spelerBestand.endsWith(".csv")) {
      spelerBestand = spelerBestand + ".csv";
    }
    if (spelerBestand.contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              resourceBundle.getString("error.bevatdirectory"),
                                       "spelerBestand"));
    }
    if (arguments.hasArgument("eindDatum")) {
      eindDatum   = arguments.getArgument("eindDatum");
    }
    if (arguments.hasArgument("startDatum")) {
      startDatum  = arguments.getArgument("startDatum");
    }
    if (eindDatum.compareTo(startDatum) < 0) {
      fouten.add(
          MessageFormat.format(
              resourceBundle.getString("error.eind.voor.start"),
                                       startDatum, eindDatum));
    }
    if (arguments.hasArgument("startELO")) {
      startElo    = Integer.parseInt(arguments.getArgument("startELO"));
    }
    String  toernooiBestand = arguments.getArgument("toernooiBestand");
    if (toernooiBestand.contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              resourceBundle.getString("error.bevatdirectory"),
                                       "toernooiBestand"));
    }
    if (!toernooiBestand.endsWith(".pgn")) {
      toernooiBestand = toernooiBestand + ".pgn";
    }
    String    invoerdir   = ".";
    if (arguments.hasArgument("invoerdir")) {
      invoerdir   = arguments.getArgument("invoerdir");
    }
    if (invoerdir.endsWith(File.separator)) {
      invoerdir   = invoerdir.substring(0,
                                        invoerdir.length()
                                        - File.separator.length());
    }
    String    uitvoerdir  = invoerdir;
    if (arguments.hasArgument("uitvoerdir")) {
      uitvoerdir  = arguments.getArgument("uitvoerdir");
    }
    if (uitvoerdir.endsWith(File.separator)) {
      uitvoerdir  = uitvoerdir.substring(0,
                                         uitvoerdir.length()
                                         - File.separator.length());
    }
    if (arguments.hasArgument("vasteKfactor")) {
      kFactor = Integer.parseInt(arguments.getArgument("vasteKfactor"));
    }
    if (arguments.hasArgument("maxVerschil")) {
      if (null == kFactor) {
        fouten.add(resourceBundle.getString("error.maxverschil"));
      }
      maxVerschil = Integer.parseInt(arguments.getArgument("maxVerschil"));
    }

    String  geschiedenisBestand = null;
    if (arguments.hasArgument("geschiedenisBestand")) {
      geschiedenisBestand = arguments.getArgument("geschiedenisBestand");
      if (geschiedenisBestand.contains(File.separator)) {
        fouten.add(
            MessageFormat.format(
                resourceBundle.getString("error.bevatdirectory"),
                                         "geschiedenisBestand"));
      }
      if (!geschiedenisBestand.endsWith(".csv")) {
        geschiedenisBestand = geschiedenisBestand + ".csv";
      }
    }

    if (!fouten.isEmpty() ) {
      help();
      for (String fout : fouten) {
        DoosUtils.foutNaarScherm(fout);
      }
      return;
    }

    if (DoosUtils.isBlankOrNull(geschiedenisBestand)) {
      geschiedenisBestand =
          spelerBestand.substring(0, spelerBestand.length() - 4) + "H.csv";
    }

    startDatum  = leesSpelers(spelers, spelerinfos, startDatum,
                              uitvoerdir + File.separator + spelerBestand,
                              charsetUit);
    String  info  = verwerkToernooi(spelers, spelerinfos, startDatum, eindDatum,
                                    startElo,
                                    invoerdir + File.separator
                                    + toernooiBestand,
                                    uitvoerdir + File.separator
                                    + geschiedenisBestand,
                                    charsetIn, charsetUit);
    if (info.contains(":") && Integer.valueOf(info.split(":")[1]) > 0) {
      schrijfSpelers(spelers, spelerinfos,
                     uitvoerdir + File.separator + spelerBestand, charsetUit);
    }

    DoosUtils.naarScherm(resourceBundle.getString("label.bestand") + " "
                         + uitvoerdir + File.separator + spelerBestand);
    DoosUtils.naarScherm(resourceBundle.getString("label.startdatum") + " "
                         + startDatum);
    if (!"9999.99.99".equals(eindDatum)) {
      DoosUtils.naarScherm(resourceBundle.getString("label.einddatum") + " "
                           + eindDatum);
    }
    DoosUtils.naarScherm(resourceBundle.getString("label.partijen") + " "
                         + info.split(":")[0]);
    if (info.contains(":")) {
      DoosUtils.naarScherm(resourceBundle.getString("label.verwerkt") + " "
                           + info.split(":")[1]);
    }
    if (null != kFactor) {
      DoosUtils.naarScherm(resourceBundle.getString("label.kFactor") + " "
                           + kFactor);
    }
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
    DoosUtils.naarScherm("  --eindDatum           ",
                         resourceBundle.getString("help.einddatum"), 80);
    DoosUtils.naarScherm("  --geschiedenisBestand ",
                         resourceBundle.getString("help.geschiedenisbestand"),
                         80);
    DoosUtils.naarScherm("  --invoerdir           ",
                         resourceBundle.getString("help.invoerdir"), 80);
    DoosUtils.naarScherm("  --maxVerschil         ",
        MessageFormat.format(resourceBundle.getString("help.maxverschil"),
                             ELO.MAX_VERSCHIL), 80);
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
    DoosUtils.naarScherm("  --vasteKfactor        ",
                         resourceBundle.getString("help.vastekfactor"), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("help.paramsverplicht"),
                             "spelerBestand", "toernooiBestand"), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(resourceBundle.getString("help.eloberekenaar.extra"));
    DoosUtils.naarScherm();
  }

  private static String leesSpelers(Map<String, Integer> spelers,
                                    List<Spelerinfo> spelerinfos,
                                    String startDatum,
                                    String spelerBestand, String charsetUit) {
    String      laatsteDatum  = startDatum;
    CsvBestand  invoer        = null;
    try {
      Calendar  calendar  = Calendar.getInstance();
      // Is eigenlijk een uitvoer.
      invoer  = new CsvBestand(spelerBestand, charsetUit);
      while (invoer.hasNext()) {
        String[]    veld        = invoer.next();
        int         spelerId    = spelers.size();
        spelers.put(veld[0], spelerId);
        Spelerinfo  spelerinfo  = new Spelerinfo();
        spelerinfo.setElo(Integer.valueOf(veld[1]));
        spelerinfo.setNaam(veld[0]);
        spelerinfo.setPartijen(Integer.valueOf(veld[2]));
        spelerinfo.setEerstePartij(Datum.toDate(veld[3],
                                   CaissaConstants.PGN_DATUM_FORMAAT));
        spelerinfo.setLaatstePartij(Datum.toDate(veld[4],
                                    CaissaConstants.PGN_DATUM_FORMAAT));
        if (spelerinfo.getPartijen() >= ELO.MIN_PARTIJEN) {
          spelerinfo.setOfficieel(Datum.toDate(veld[5],
                                  CaissaConstants.PGN_DATUM_FORMAAT));
          spelerinfo.setMinElo(Integer.valueOf(veld[6]));
          spelerinfo.setMinDatum(Datum.toDate(veld[7],
                                 CaissaConstants.PGN_DATUM_FORMAAT));
          spelerinfo.setMaxElo(Integer.valueOf(veld[8]));
          spelerinfo.setMaxDatum(Datum.toDate(veld[9],
                                 CaissaConstants.PGN_DATUM_FORMAAT));
        }
        spelerinfo.setSpelerId(spelerId);
        spelerinfos.add(spelerId, spelerinfo);
        if (veld[4].compareTo(laatsteDatum) >= 0) {
          calendar.setTime(spelerinfo.getLaatstePartij());
          calendar.add(Calendar.DATE, 1);
          laatsteDatum  = Datum.fromDate(calendar.getTime(),
                                         CaissaConstants.PGN_DATUM_FORMAAT);
        }
      }
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(
          MessageFormat.format(
              resourceBundle.getString("message.nieuwbestand"), spelerBestand));
    } catch (ParseException e) {
      DoosUtils.foutNaarScherm(
          MessageFormat.format(
              resourceBundle.getString("error.foutedatumin"),
              spelerBestand) + " [" + e.getLocalizedMessage() + "]");
    } finally {
      try {
        if (invoer != null) {
          invoer.close();
        }
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }

    return laatsteDatum;
  }

  private static void pasSpelerAan(int id, List<Spelerinfo> spelerinfos,
                                   Date eloDatum, Integer andereElo,
                                   int uitslag) {
    spelerinfos.get(id).setLaatstePartij(eloDatum);
    int     aantal  = spelerinfos.get(id).getPartijen();
    Integer elo;
    if (null == kFactor) {
      elo     = ELO.berekenELO(spelerinfos.get(id).getElo(), uitslag, andereElo,
                               aantal);
    } else {
      elo     = ELO.berekenELO(spelerinfos.get(id).getElo(), uitslag, andereElo,
                               kFactor, maxVerschil);
    }
    spelerinfos.get(id).setElo(elo);
    spelerinfos.get(id).addPartij();
    aantal++;
    if (aantal == ELO.MIN_PARTIJEN) {
      spelerinfos.get(id).setMinElo(elo);
      spelerinfos.get(id).setMaxElo(elo);
      spelerinfos.get(id).setMinDatum(eloDatum);
      spelerinfos.get(id).setMaxDatum(eloDatum);
      spelerinfos.get(id).setOfficieel(eloDatum);
    }
    if (aantal > ELO.MIN_PARTIJEN) {
      if (spelerinfos.get(id).getMinElo() >= elo) {
        spelerinfos.get(id).setMinElo(elo);
        spelerinfos.get(id).setMinDatum(eloDatum);
      }
      if (spelerinfos.get(id).getMaxElo() <= elo) {
        spelerinfos.get(id).setMaxElo(elo);
        spelerinfos.get(id).setMaxDatum(eloDatum);
      }
    }
  }

  private static void schrijfSpelers(Map<String, Integer> spelers,
                                     List<Spelerinfo> spelerinfos,
                                     String spelerBestand, String charsetUit) {
    BufferedWriter  uitvoer = null;
    try {
      uitvoer = Bestand.openUitvoerBestand(spelerBestand, charsetUit);
      StringBuilder lijn  = new StringBuilder();
      lijn.append("\"speler\",\"elo\",\"partijen\",")
          .append("\"eerstePartij\",\"laatstePartij\",\"eersteEloDatum\",")
          .append("\"minElo\",\"minEloDatum\",\"maxElo\",\"maxEloDatum\"");
      Bestand.schrijfRegel(uitvoer, lijn.toString());
      for (Integer spelerId  : spelers.values()) {
        try {
          lijn  = new StringBuilder();
          lijn.append("\"")
              .append(spelerinfos.get(spelerId).getNaam()).append("\",")
              .append(spelerinfos.get(spelerId).getElo()).append(",")
              .append(spelerinfos.get(spelerId).getPartijen()).append(",")
              .append(Datum.fromDate(spelerinfos.get(spelerId)
                                                .getEerstePartij(), 
                                     CaissaConstants.PGN_DATUM_FORMAAT))
              .append(",")
              .append(Datum.fromDate(spelerinfos.get(spelerId)
                                                .getLaatstePartij(), 
                                     CaissaConstants.PGN_DATUM_FORMAAT))
              .append(",");
          if (spelerinfos.get(spelerId).getPartijen() < ELO.MIN_PARTIJEN) {
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
        Bestand.schrijfRegel(uitvoer, lijn.toString());
      }
    } catch (IOException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (null != uitvoer) {
          uitvoer.close();
        }
      } catch (IOException ex) {
        DoosUtils.foutNaarScherm(ex.getLocalizedMessage());
      }
    }
  }

  private static String verwerkToernooi(Map<String, Integer> spelers,
                                        List<Spelerinfo> spelerinfos,
                                        String startDatum, String eindDatum,
                                        int startElo, String toernooiBestand,
                                        String geschiedenisBestand,
                                        String charsetIn,
                                        String charsetUit) {
    Date            eloDatum      = null;
    BufferedWriter  geschiedenis  = null;
    StringBuilder   info          = new StringBuilder();
    String[]        uitslagen     = new String[] {"0-1", "1/2-1/2", "1-0"};
    int             verwerkt      = 0;

    try {
      geschiedenis  = Bestand.openUitvoerBestand(geschiedenisBestand,
                                                 charsetUit, true);
      Collection<PGN>
          partijen  = new TreeSet<PGN>(new PGN.defaultComparator());
      partijen.addAll(CaissaUtils.laadPgnBestand(toernooiBestand, charsetIn));

      for (PGN  partij : partijen) {
        if (!partij.isBye()
            && partij.isRated()) {
          String  datum       = partij.getTag("Date");
          if (startDatum.compareTo(datum) <= 0
              && eindDatum.compareTo(datum) >= 0) {
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
            if (uitslag < 3) {
              if (!spelers.containsKey(wit)) {
                voegSpelerToe(wit, spelers, spelerinfos, eloDatum, startElo);
              }
              if (!spelers.containsKey(zwart)) {
                voegSpelerToe(zwart, spelers, spelerinfos, eloDatum, startElo);
              }
              int     witId     = spelers.get(wit);
              int     zwartId   = spelers.get(zwart);
              Integer witElo    = spelerinfos.get(witId).getElo();
              Integer zwartElo  = spelerinfos.get(zwartId).getElo();
              pasSpelerAan(witId,   spelerinfos, eloDatum, zwartElo, uitslag);
              pasSpelerAan(zwartId, spelerinfos, eloDatum, witElo, 2 - uitslag);
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
      }
      info.append(partijen.size()).append(":");
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } catch (PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } catch (IOException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (geschiedenis != null) {
          geschiedenis.close();
        }
      } catch (IOException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }

    return info.append(verwerkt).toString();
  }

  private static void voegSpelerToe(String speler, Map<String, Integer> spelers,
                                    List<Spelerinfo> spelerinfos,
                                    Date eloDatum, int startElo) {
    int spelerId  = spelers.size();
    spelers.put(speler, spelerId);
    Spelerinfo  spelerinfo  = new Spelerinfo();
    spelerinfo.setEerstePartij(eloDatum);
    spelerinfo.setElo(startElo);
    spelerinfo.setNaam(speler);
    spelerinfo.setPartijen(Integer.valueOf(0));
    spelerinfo.setSpelerId(spelerId);
    spelerinfos.add(spelerId, spelerinfo);
  }
}
