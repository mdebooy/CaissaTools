/**
 * Copyright 2017 Marco de Booij
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
import eu.debooy.caissa.FEN;
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.Zet;
import eu.debooy.caissa.exceptions.FenException;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.TreeMap;

import org.codehaus.jackson.map.ObjectMapper;


/**
 * @author Marco de Booij
 */
public final class PgnToJson {
  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private static final Integer  EXTRA = Integer.valueOf(-1);
  private static final Integer  OUT   = Integer.valueOf(0);

  private PgnToJson() {}

  public static void execute(String[] args) throws PgnException {
    String        charsetIn   = Charset.defaultCharset().name();
    String        charsetUit  = Charset.defaultCharset().name();
    String        defStukken  = CaissaConstants.Stukcodes.valueOf("EN")
                                               .getStukcodes();
    List<String>  fouten      = new ArrayList<String>();
    String        naarTaal    = Locale.getDefault().getLanguage();
    String        naarStukken = "";
    TekstBestand  output      = null;
    String        vanTaal     = Locale.getDefault().getLanguage();
    String        vanStukken  = "";

    Banner.printBanner(resourceBundle.getString("banner.pgntojson"));

    Arguments arguments = new Arguments(args);
    arguments.setParameters(new String[] {CaissaTools.BESTAND,
                                          CaissaTools.CHARDSETIN,
                                          CaissaTools.CHARDSETUIT,
                                          CaissaTools.DEFAULTECO,
                                          CaissaTools.EXTRAS,
                                          CaissaTools.INCLUDELEGE,
                                          CaissaTools.INVOERDIR,
                                          CaissaTools.JSON,
                                          CaissaTools.NAARTAAL,
                                          CaissaTools.UITVOERDIR,
                                          CaissaTools.VANTAAL});
    arguments.setVerplicht(new String[] {CaissaTools.BESTAND});
    if (!arguments.isValid()) {
      help();
      return;
    }

    String    bestand       = arguments.getArgument(CaissaTools.BESTAND);
    if (bestand.contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              resourceBundle.getString(CaissaTools.ERR_BEVATDIRECTORY),
                                       CaissaTools.BESTAND));
    }
    if (bestand.endsWith(CaissaTools.EXTENSIE_PGN)) {
      bestand       = bestand.substring(0, bestand.length() - 4);
    }
    if (arguments.hasArgument(CaissaTools.CHARDSETIN)) {
      charsetIn     = arguments.getArgument(CaissaTools.CHARDSETIN);
    }
    if (arguments.hasArgument(CaissaTools.CHARDSETUIT)) {
      charsetUit    = arguments.getArgument(CaissaTools.CHARDSETUIT);
    }
    String    defaultEco    = "";
    if (arguments.hasArgument(CaissaTools.DEFAULTECO)) {
      defaultEco  = arguments.getArgument(CaissaTools.DEFAULTECO);
    }
    boolean   metFen        = false;
    boolean   metTrajecten  = false;
    if (arguments.hasArgument(CaissaTools.EXTRAS)) {
      List<String>  extras  =
          Arrays.asList(arguments.getArgument(CaissaTools.EXTRAS).toLowerCase()
                                 .split(","));
      if (extras.contains("fen")) {
        metFen  = true;
      }
      if (extras.contains("trj")) {
        metTrajecten  = true;
      }
    }
    boolean   includeLege   = false;
    if (arguments.hasArgument(CaissaTools.INCLUDELEGE)) {
      includeLege   =
          DoosConstants.WAAR
              .equalsIgnoreCase(arguments.getArgument(CaissaTools.INCLUDELEGE));
    }
    String    invoerdir     = ".";
    if (arguments.hasArgument(CaissaTools.INVOERDIR)) {
      invoerdir     = arguments.getArgument(CaissaTools.INVOERDIR);
    }
    if (invoerdir.endsWith(File.separator)) {
      invoerdir     = invoerdir.substring(0,
                                          invoerdir.length()
                                          - File.separator.length());
    }
    String    uitvoerdir    = invoerdir;
    if (arguments.hasArgument(CaissaTools.UITVOERDIR)) {
      uitvoerdir    = arguments.getArgument(CaissaTools.UITVOERDIR);
    }
    if (uitvoerdir.endsWith(File.separator)) {
      uitvoerdir    = uitvoerdir.substring(0,
                                           uitvoerdir.length()
                                           - File.separator.length());
    }
    String    jsonBestand   = "";
    if (arguments.hasArgument(CaissaTools.JSON)) {
      jsonBestand   = arguments.getArgument(CaissaTools.JSON);
    }
    if (DoosUtils.isBlankOrNull(jsonBestand)) {
      jsonBestand   = bestand;
    }
    if (!jsonBestand.endsWith(CaissaTools.EXTENSIE_JSON)) {
      jsonBestand   = jsonBestand + CaissaTools.EXTENSIE_JSON;
    }
    if (arguments.hasArgument(CaissaTools.NAARTAAL)) {
      naarTaal    = arguments.getArgument(CaissaTools.NAARTAAL).toLowerCase();
    }
    if (arguments.hasArgument(CaissaTools.VANTAAL)) {
      vanTaal     = arguments.getArgument(CaissaTools.VANTAAL).toLowerCase();
    }

    if (!fouten.isEmpty() ) {
      help();
      for (String fout : fouten) {
        DoosUtils.foutNaarScherm(fout);
      }
      return;
    }

    // Haal de stukcodes op
    naarStukken = CaissaConstants.Stukcodes.valueOf(naarTaal.toUpperCase())
                                 .getStukcodes();
    vanStukken  = CaissaConstants.Stukcodes.valueOf(vanTaal.toUpperCase())
                                 .getStukcodes();

    FEN             fen       = new FEN();
    ObjectMapper    mapper    = new ObjectMapper();
    List<Map<String, Object>>
                    lijst     = new ArrayList<Map<String, Object>>();
    Collection<PGN> partijen  =
        CaissaUtils.laadPgnBestand(invoerdir + File.separator + bestand
                                     + CaissaTools.EXTENSIE_PGN,
                                   charsetIn);
    int partijnr  = 1;
    try {
      for (PGN pgn: partijen) {
        if (includeLege
            || DoosUtils.isNotBlankOrNull(pgn.getZuivereZetten())) {
          Map<String, Integer>        ids       =
              new HashMap<String, Integer>();
          Map<String, Object>         partij    =
              new LinkedHashMap<String, Object>();
          Map<String, List<Integer>>  trajecten =
              new TreeMap<String, List<Integer>>();
          if (pgn.hasTag(CaissaConstants.PGNTAG_FEN)) {
            fen = new FEN(pgn.getTag(CaissaConstants.PGNTAG_FEN));
          } else {
            fen = new FEN();
          }

          if (!pgn.hasTag(CaissaConstants.PGNTAG_ECO)
              && DoosUtils.isNotBlankOrNull(defaultEco)) {
            pgn.addTag(CaissaConstants.PGNTAG_ECO, defaultEco);
          }

          int[] bord  = fen.getBord();
          for (int i = 9; i > 1; i--) {
            for (int j = 1; j < 9; j++) {
              if (bord[i*10+j] != 0) {
                Integer       positie = (i-1)*10+j;
                String        id      = "" + CaissaUtils.getStuk(bord[i*10+j])
                    + CaissaUtils.internToExtern(positie + 10);
                List<Integer> traject = new ArrayList<Integer>();
                ids.put(id, positie);
                traject.add(positie);
                trajecten.put(id, traject);
              }
            }
          }

          partij.put("_gamekey", "" + partijnr);
          for (Map.Entry<String, String> tag : pgn.getTags().entrySet()) {
            partij.put(tag.getKey(), tag.getValue());
          }
          String zuivereZetten  = pgn.getZuivereZetten();
          if (DoosUtils.isNotBlankOrNull(zuivereZetten)) {
            String[]  zetten        =
                vertaal(zuivereZetten,
                        vanStukken, naarStukken).split(" ");
            Map<String, Object> jsonZetten  =
                new LinkedHashMap<String, Object>();
            for (int i = 0; i < zetten.length; i++) {
              Map<String, String> jsonZet = new LinkedHashMap<String, String>();
              String              pgnZet  = zetten[i].replaceAll("^[0-9]*\\.",
                                                                 "");
              jsonZet.put("notatie", pgnZet);
              if (metFen || metTrajecten) {
                Zet zet = CaissaUtils.vindZet(fen, vertaal(pgnZet, naarStukken,
                                              defStukken));
                fen.doeZet(zet);
              }
              if (metFen) {
                jsonZet.put("fen", fen.getFen());
              }
              if (metTrajecten) {
                wijzigTrajecten(fen, ids, trajecten);
              }
              jsonZetten.put(Integer.toString(i), jsonZet);
            }
            if (metTrajecten) {
              partij.put("trajecten", trajecten);
            }
            partij.put("moves", jsonZetten);
          }
          lijst.add(partij);
          partijnr++;
        }
      }

      output  = new TekstBestand.Builder()
                                .setBestand(uitvoerdir + File.separator
                                            + jsonBestand)
                                .setCharset(charsetUit)
                                .setLezen(false).build();
      output.write(mapper.writeValueAsString(lijst));
    } catch (BestandException | FenException | IOException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (output != null) {
          output.close();
        }
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }

    DoosUtils.naarScherm(resourceBundle.getString("label.bestand") + " "
                         + invoerdir + File.separator + bestand
                         + CaissaTools.EXTENSIE_PGN);
    DoosUtils.naarScherm(resourceBundle.getString("label.partijen") + " "
                         + partijen.size());
    DoosUtils.naarScherm(resourceBundle.getString("label.uitvoer") + " "
                         + uitvoerdir + File.separator + jsonBestand);
    DoosUtils.naarScherm(resourceBundle.getString("label.partijen") + " "
                          + (partijnr - 1));
    DoosUtils.naarScherm(resourceBundle.getString("label.klaar"));
  }

  protected static String getKey(Map<String, Integer> ids, Integer veld) {
    for (Entry<String, Integer> id : ids.entrySet()) {
      if (id.getValue().equals(veld)) {
        return id.getKey();
      }
    }

    return null;
  }

  protected static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar PgnToJson ["
                         + resourceBundle.getString("label.optie")
                         + "] --bestand=<"
                         + resourceBundle.getString("label.pgnbestand") + ">");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm("  --bestand       ",
                         resourceBundle.getString("help.bestand"), 80);
    DoosUtils.naarScherm("  --charsetin     ",
        MessageFormat.format(resourceBundle.getString("help.charsetin"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --charsetuit    ",
        MessageFormat.format(resourceBundle.getString("help.charsetuit"),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm("  --defaulteco    ",
                         resourceBundle.getString("help.defaulteco"), 80);
    DoosUtils.naarScherm("  --pgnviewer     ",
                         resourceBundle.getString("help.extras"), 80);
    DoosUtils.naarScherm("  --includelege   ",
                         resourceBundle.getString("help.includelege"), 80);
    DoosUtils.naarScherm("  --invoerdir     ",
                         resourceBundle.getString("help.invoerdir"), 80);
    DoosUtils.naarScherm("  --json          ",
                         resourceBundle.getString("help.json"), 80);
    DoosUtils.naarScherm("  --naartaal      ",
        MessageFormat.format(resourceBundle.getString("help.naartaal"),
                             Locale.getDefault().getLanguage()), 80);
    DoosUtils.naarScherm("  --uitvoerdir    ",
                         resourceBundle.getString("help.uitvoerdir"), 80);
    DoosUtils.naarScherm("  --vantaal       ",
        MessageFormat.format(resourceBundle.getString("help.vantaal"),
                             Locale.getDefault().getLanguage()), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("help.paramverplicht"),
                             CaissaTools.BESTAND), 80);
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("help.talengelijk"),
                             CaissaTools.BESTAND), 80);
    DoosUtils.naarScherm();
  }

  private static String vertaal(String zetten,
                                String vanStukken, String naarStukken) {
    if (!vanStukken.equals(naarStukken)) {
      try {
        return CaissaUtils.vertaalStukken(zetten, vanStukken, naarStukken);
      } catch (PgnException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }

    return zetten;
  }

  private static void wijzigTrajecten(FEN fen, Map<String, Integer> ids,
                                      Map<String, List<Integer>> trajecten) {
    Map<String, String>  verplaatst  = new HashMap<String, String>(); 
    for (Entry<String, Integer> id : ids.entrySet()) {
      int[]   bord    = fen.getBord();
      Integer positie = id.getValue() + 10;
      int     stuk    = CaissaUtils.zoekStuk(id.getKey().charAt(0));
      if (bord[positie] != stuk) {
        if (bord[positie] == 0) {
          verplaatst.put(id.getKey().substring(0, 1), id.getKey());
        }
        ids.put(id.getKey(), OUT);
      }
    }

    int[] bord  = fen.getBord();
    for (int i = 9; i > 1; i--) {
      for (int j = 1; j < 9; j++) {
        Integer positie = i*10+j;
        if (bord[positie] != 0) {
          if (DoosUtils.isBlankOrNull(getKey(ids, positie-10))) {
            String  stuk  = String.valueOf(CaissaUtils.getStuk(bord[positie]));
            if (verplaatst.containsKey(stuk)) {
              if (ids.get(verplaatst.get(stuk)).equals(OUT)) {
                ids.put(verplaatst.get(stuk), positie-10);
              }
            } else {
              int           plies   = trajecten.values().iterator().next()
                                               .size(); 
              String        start   = stuk
                                        + CaissaUtils.internToExtern(positie);
              List<Integer> traject = new ArrayList<Integer>();
              while (plies > 0) {
                traject.add(EXTRA);
                plies--;
              }
              ids.put(start, positie-10);
              trajecten.put(start, traject);
            }
          }
        }
      }
    }

    for (String stuk : trajecten.keySet()) {
      trajecten.get(stuk).add(ids.get(stuk));
    }
  }
}
