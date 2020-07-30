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
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.TreeMap;
import org.codehaus.jackson.map.ObjectMapper;


/**
 * @author Marco de Booij
 */
public final class PgnToJson extends Batchjob {
  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private static final Integer  EXTRA     = -2;
  private static final Integer  OUT       = -1;
  private static final Integer  PROMOTIE  = -3;

  private static boolean  voorNico  = false;

  private PgnToJson() {}

  public static class IdComparator
  implements Comparator<String>, Serializable {
    private static final  long  serialVersionUID  = 1L;

    private static final  Map<String, Integer> STUKKEN =
        new HashMap<String, Integer>() {
          private static final long serialVersionUID = 1L;
          {
            put("q", 11);
            put("r", 12);
            put("b", 13);
            put("n", 14);
            put("k", 15);
            put("p", 16);
            put("Q", 21);
            put("R", 22);
            put("B", 23);
            put("N", 24);
            put("K", 25);
            put("P", 26);
          }
        };

    public int compare(String id1, String id2) {
      String string1 = STUKKEN.get(id1.substring(0, 1)) + id1.substring(2)
                       + id1.substring(1, 2);
      String string2 = STUKKEN.get(id2.substring(0, 1)) + id2.substring(2)
                       + id2.substring(1, 2);

      return string1.compareTo(string2);
    }
  }

  public static void execute(String[] args) {
    String        defStukken  = CaissaConstants.Stukcodes.valueOf("EN")
                                               .getStukcodes();
    TekstBestand  output      = null;

    Banner.printMarcoBanner(resourceBundle.getString("banner.pgntojson"));

    if (!setParameters(args)) {
      return;
    }

    String    defaultEco    = parameters.get(CaissaTools.PAR_DEFAULTECO);
    boolean   includeLege   =
        parameters.get(CaissaTools.PAR_INCLUDELEGE).equals(DoosConstants.WAAR);
    boolean   metFen        =
        parameters.get(CaissaTools.PAR_METFEN).equals(DoosConstants.WAAR);
    boolean   metTrajecten  =
        parameters.get(CaissaTools.PAR_METTRAJECTEN).equals(DoosConstants.WAAR);
    voorNico                =
        parameters.get(CaissaTools.PAR_VOORNICO).equals(DoosConstants.WAAR);

    // Haal de stukcodes op
    String  naarStukken =
        CaissaConstants.Stukcodes
                       .valueOf(parameters.get(CaissaTools.PAR_NAARTAAL)
                                          .toUpperCase())
                       .getStukcodes();
    String  vanStukken  =
        CaissaConstants.Stukcodes
                       .valueOf(parameters.get(CaissaTools.PAR_VANTAAL)
                                          .toUpperCase())
                       .getStukcodes();

    FEN             fen;
    String          invoer    = parameters.get(PAR_INVOERDIR)
                                + parameters.get(CaissaTools.PAR_BESTAND)
                                + EXT_PGN;
    ObjectMapper    mapper    = new ObjectMapper();
    List<Map<String, Object>>
                    lijst     = new ArrayList<>();
    String          uitvoer   = parameters.get(PAR_INVOERDIR)
                                + parameters.get(CaissaTools.PAR_JSON)
                                + EXT_JSON;

    Collection<PGN> partijen;
    try {
      partijen = CaissaUtils.laadPgnBestand(invoer,
                                            parameters.get(PAR_CHARSETIN));
    } catch (PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    int partijnr  = 1;
    try {
      for (PGN pgn: partijen) {
        if (includeLege
            || DoosUtils.isNotBlankOrNull(pgn.getZuivereZetten())) {
          Map<String, Integer>        ids       =
              new TreeMap<>(new IdComparator());
          Map<String, Object>         partij    =
              new LinkedHashMap<>();
          Map<String, List<Integer>>  trajecten =
              new LinkedHashMap<>();
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
              Integer positie = i*10+j;
              if (bord[positie] != 0) {
                String  id  = "" + CaissaUtils.getStuk(bord[positie])
                                + CaissaUtils.internToExtern(positie);
                ids.put(id, getCoordinaat(positie));
              }
            }
          }

          for (Entry<String, Integer> id : ids.entrySet()) {
            List<Integer> traject = new ArrayList<>();
            traject.add(id.getValue());
            trajecten.put(id.getKey(), traject);
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
            Map<String, Object> jsonZetten  = new LinkedHashMap<>();
            for (int i = 0; i < zetten.length; i++) {
              Map<String, String> jsonZet = new LinkedHashMap<>();
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
                                .setBestand(uitvoer)
                                .setCharset(parameters.get(PAR_CHARSETUIT))
                                .setLezen(false).build();
      output.write(mapper.writeValueAsString(lijst));
    } catch (BestandException | FenException | IOException | PgnException e) {
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

    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.bestand"),
                             invoer));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.partijen"),
                             partijen.size()));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.uitvoer"),
                             uitvoer));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.partijen"),
                             (partijnr - 1)));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  protected static Integer getCoordinaat(Integer positie) {
    return (((positie%10) - 1) * 10) + (9 - (positie/10));
  }

  protected static String getKey(Map<String, Integer> ids, Integer veld) {
    for (Entry<String, Integer> id : ids.entrySet()) {
      if (id.getValue().equals(veld)) {
        return id.getKey();
      }
    }

    return null;
  }

  protected static Integer getPositie(Integer coordinaat) {
    return ((9 - (coordinaat%10)) * 10) + (coordinaat/10) + 1;
  }

  public static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar PgnToJson ["
                         + getMelding(LBL_OPTIE)
                         + "] --bestand=<"
                         + resourceBundle.getString("label.pgnbestand") + ">");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_BESTAND, 13),
                         resourceBundle.getString("help.bestand"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETIN, 13),
        MessageFormat.format(getMelding(HLP_CHARSETIN),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETUIT, 13),
        MessageFormat.format(getMelding(HLP_CHARSETUIT),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_DEFAULTECO, 13),
                         resourceBundle.getString("help.defaulteco"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_INCLUDELEGE, 13),
                         resourceBundle.getString("help.includelege"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_INVOERDIR, 13),
                         getMelding(HLP_INVOERDIR), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_JSON, 13),
                         resourceBundle.getString("help.json"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_METFEN, 13),
                         resourceBundle.getString("help.metfen"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_METTRAJECTEN, 13),
                         resourceBundle.getString("help.mettrajecten"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_NAARTAAL, 13),
        MessageFormat.format(resourceBundle.getString("help.naartaal"),
                             Locale.getDefault().getLanguage()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_UITVOERDIR, 13),
                         getMelding(HLP_UITVOERDIR), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_VANTAAL, 13),
        MessageFormat.format(resourceBundle.getString("help.vantaal"),
                             Locale.getDefault().getLanguage()), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(getMelding(HLP_PARAMVERPLICHT),
                             CaissaTools.PAR_BESTAND), 80);
    DoosUtils.naarScherm(resourceBundle.getString("help.talengelijk"), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  private static boolean setParameters(String[] args) {
    Arguments     arguments = new Arguments(args);
    List<String>  fouten    = new ArrayList<>();

    arguments.setParameters(new String[] {CaissaTools.PAR_BESTAND,
                                          PAR_CHARSETIN,
                                          PAR_CHARSETUIT,
                                          CaissaTools.PAR_DEFAULTECO,
                                          CaissaTools.PAR_INCLUDELEGE,
                                          PAR_INVOERDIR,
                                          CaissaTools.PAR_JSON,
                                          CaissaTools.PAR_METFEN,
                                          CaissaTools.PAR_METTRAJECTEN,
                                          CaissaTools.PAR_NAARTAAL,
                                          PAR_UITVOERDIR,
                                          CaissaTools.PAR_VANTAAL,
                                          CaissaTools.PAR_VOORNICO});
    arguments.setVerplicht(new String[] {CaissaTools.PAR_BESTAND});
    if (!arguments.isValid()) {
      fouten.add(getMelding(ERR_INVALIDPARAMS));
    }

    parameters.clear();
    setBestandParameter(arguments, CaissaTools.PAR_BESTAND, EXT_PGN);
    setParameter(arguments, PAR_CHARSETIN, Charset.defaultCharset().name());
    setParameter(arguments, PAR_CHARSETUIT, Charset.defaultCharset().name());
    setParameter(arguments, CaissaTools.PAR_DEFAULTECO, "");
    setParameter(arguments, CaissaTools.PAR_INCLUDELEGE, DoosConstants.ONWAAR);
    setDirParameter(arguments, PAR_INVOERDIR);
    if (arguments.hasArgument(CaissaTools.PAR_JSON)) {
      setBestandParameter(arguments, CaissaTools.PAR_JSON, EXT_JSON);
    } else {
      setParameter(CaissaTools.PAR_JSON,
                   parameters.get(CaissaTools.PAR_BESTAND));
    }
    setParameter(CaissaTools.PAR_METFEN,
                 arguments.hasArgument(CaissaTools.PAR_METFEN)
                     ? DoosConstants.WAAR : DoosConstants.ONWAAR);
    setParameter(CaissaTools.PAR_METTRAJECTEN,
                 arguments.hasArgument(CaissaTools.PAR_METTRAJECTEN)
                     ? DoosConstants.WAAR : DoosConstants.ONWAAR);
    setParameter(arguments, CaissaTools.PAR_NAARTAAL,
                 Locale.getDefault().getLanguage());
    setDirParameter(arguments, PAR_UITVOERDIR, getParameter(PAR_INVOERDIR));
    setParameter(arguments, CaissaTools.PAR_VANTAAL,
                 Locale.getDefault().getLanguage());
    setParameter(CaissaTools.PAR_VOORNICO,
                 arguments.hasArgument(CaissaTools.PAR_VOORNICO)
                     ? DoosConstants.WAAR : DoosConstants.ONWAAR);

    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_BESTAND))
                 .contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              getMelding(ERR_BEVATDIRECTORY), CaissaTools.PAR_BESTAND));
    }
    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_JSON))
                 .contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              getMelding(ERR_BEVATDIRECTORY), CaissaTools.PAR_JSON));
    }

    if (fouten.isEmpty()) {
      return true;
    }

    help();
    printFouten(fouten);

    return false;
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
    int[]               bord        = fen.getBord();
    Map<String, String> verplaatst  = new HashMap<>();
    for (Entry<String, Integer> id : ids.entrySet()) {
      if (id.getValue() >= 0
              && id.getValue() < 100) {
        Integer positie = getPositie(id.getValue()%100);
        int stuk  = CaissaUtils.zoekStuk(id.getKey().charAt(0));
        if (bord[positie] != stuk) {
          if (bord[positie] == 0) {
            verplaatst.put(id.getKey().substring(0, 1), id.getKey());
          }
          ids.put(id.getKey(), OUT);
        }
      } else {
        if (Objects.equals(id.getValue(), PROMOTIE)
            || (id.getValue() >= 100 && id.getValue() < 200)) {
          ids.put(id.getKey(), OUT);
        }
        if (id.getValue() >= 200) {
          ids.put(id.getKey(), id.getValue() - 200);
        }
      }
    }

    for (int i = 9; i > 1; i--) {
      for (int j = 1; j < 9; j++) {
        Integer positie = i*10+j;
        if (bord[positie] != 0) {
          if (DoosUtils.isBlankOrNull(getKey(ids, getCoordinaat(positie)))) {
            String  stuk  = String.valueOf(CaissaUtils.getStuk(bord[positie]));
            if (verplaatst.containsKey(stuk)) {
              if (ids.get(verplaatst.get(stuk)).equals(OUT)) {
                ids.put(verplaatst.get(stuk), getCoordinaat(positie));
              }
            } else {
              int           plies     = trajecten.values().iterator().next()
                                                 .size();
              String        promotie  = "";
              if (verplaatst.containsKey("P")) {
                promotie  = verplaatst.get("P");
              }
              if (verplaatst.containsKey("p")) {
                promotie  = verplaatst.get("P");
              }
              if (voorNico) {
                ids.put(promotie, 100 + getCoordinaat(positie));
              } else {
                ids.put(promotie, PROMOTIE);
              }
              String        id      = stuk + promotie.substring(1);
              List<Integer> traject = new ArrayList<>();
              while (plies > 0) {
                traject.add(EXTRA);
                plies--;
              }
              if (voorNico) {
                ids.put(id, 200 + getCoordinaat(positie));
              } else {
                ids.put(id, getCoordinaat(positie));
              }
              trajecten.put(id, traject);
            }
          }
        }
      }
    }

    trajecten.keySet().forEach(stuk -> {
      trajecten.get(stuk).add(ids.get(stuk));
    });
  }
}
