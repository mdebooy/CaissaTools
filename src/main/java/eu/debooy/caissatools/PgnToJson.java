/**
 * Copyright (c) 2017 Marco de Booij
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
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import static eu.debooy.doosutils.Batchjob.help;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.ParameterBundle;
import eu.debooy.doosutils.access.BestandConstants;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import java.io.IOException;
import java.io.Serializable;
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
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());

  private static final String   DEFSTUKKEN  =
      CaissaConstants.Stukcodes.valueOf("EN").getStukcodes();
  private static final Integer  EXTRA       = -2;
  private static final Integer  OUT         = -1;
  private static final Integer  PROMOTIE    = -3;

  private static  String  defaultEco;
  private static  boolean metFen        = false;
  private static  boolean metTrajecten  = false;
  private static  boolean metPgnviewer  = false;
  private static  String  naarStukken;
  private static  String  vanStukken;
  private static  boolean voorNico      = false;

  protected PgnToJson() {}

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

    @Override
    public int compare(String id1, String id2) {
      var string1 = STUKKEN.get(id1.substring(0, 1)) + id1.substring(2)
                     + id1.substring(1, 2);
      var string2 = STUKKEN.get(id2.substring(0, 1)) + id2.substring(2)
                     + id2.substring(1, 2);

      return string1.compareTo(string2);
    }
  }

  public static void execute(String[] args) {
    setParameterBundle(new ParameterBundle.Builder()
                           .setBaseName(CaissaTools.TOOL_PGNTOJSON)
                           .setValidator(new BestandDefaultParameters())
                           .build());

    Banner.printMarcoBanner(DoosUtils.nullToEmpty(paramBundle.getBanner()));

    if (!paramBundle.isValid()
        || !paramBundle.setArgs(args)) {
      help();
      printFouten();
      return;
    }

    var includeLege = paramBundle.getBoolean(CaissaTools.PAR_INCLUDELEGE);

    defaultEco    = paramBundle.getString(CaissaTools.PAR_DEFAULTECO);
    metFen        = paramBundle.getBoolean(CaissaTools.PAR_METFEN);
    metTrajecten  = paramBundle.getBoolean(CaissaTools.PAR_METTRAJECTEN);
    metPgnviewer  = paramBundle.getBoolean(CaissaTools.PAR_METPGNVIEWER);
    voorNico      = paramBundle.getBoolean(CaissaTools.PAR_VOORNICO);

    // Haal de stukcodes op
    naarStukken =
        CaissaConstants.Stukcodes
                       .valueOf(paramBundle.getString(CaissaTools.PAR_NAARTAAL)
                                           .toUpperCase())
                       .getStukcodes();
    vanStukken  =
        CaissaConstants.Stukcodes
                       .valueOf(paramBundle.getString(CaissaTools.PAR_VANTAAL)
                                           .toUpperCase())
                       .getStukcodes();

    var invoer    = paramBundle.getBestand(CaissaTools.PAR_BESTAND,
                                           BestandConstants.EXT_PGN);
    var mapper    = new ObjectMapper();
    List<Map<String, Object>>
                    lijst     = new ArrayList<>();
    var uitvoer   = paramBundle.getBestand(CaissaTools.PAR_JSON,
                                           BestandConstants.EXT_JSON);

    Collection<PGN> partijen  = null;
    var             partijnr  = 1;
    try (var output  =
          new TekstBestand.Builder()
                          .setBestand(uitvoer)
                          .setLezen(false).build()) {
      partijen =
          CaissaUtils.laadPgnBestand(invoer);

      for (var pgn: partijen) {
        if (Boolean.TRUE.equals(includeLege)
            || DoosUtils.isNotBlankOrNull(pgn.getZuivereZetten())) {
          lijst.add(verwerkPartij(pgn, partijnr));
          partijnr++;
        }
      }

      output.write(mapper.writeValueAsString(lijst));
    } catch (BestandException | IOException | PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }

    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString(CaissaTools.LBL_BESTAND),
                             invoer));
    if (null != partijen) {
      DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString(CaissaTools.LBL_PARTIJEN),
                             partijen.size()));
    }
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString(CaissaTools.LBL_UITVOER),
                             uitvoer));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString(CaissaTools.LBL_PARTIJEN),
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

  private static void setFenbord(int[] bord, Map<String, Integer> ids) {
    for (var i = 9; i > 1; i--) {
      for (var j = 1; j < 9; j++) {
        Integer positie = i*10+j;
        if (bord[positie] != 0) {
          String  id  = "" + CaissaUtils.getStuk(bord[positie])
                          + CaissaUtils.internToExtern(positie);
          ids.put(id, getCoordinaat(positie));
        }
      }
    }
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

  private static void verwerkBord(int[] bord, Map<String, Integer> ids,
                                 Map<String, String> verplaatst,
                                 Map<String, List<Integer>> trajecten) {
    for (var i = 9; i > 1; i--) {
      for (var j = 1; j < 9; j++) {
        var positie = i*10+j;
        if (bord[positie] != 0) {
          if (DoosUtils.isBlankOrNull(getKey(ids, getCoordinaat(positie)))) {
            var stuk  = String.valueOf(CaissaUtils.getStuk(bord[positie]));
            if (verplaatst.containsKey(stuk)) {
              if (ids.get(verplaatst.get(stuk)).equals(OUT)) {
                ids.put(verplaatst.get(stuk), getCoordinaat(positie));
              }
            } else {
              var plies     = trajecten.values().iterator().next().size();
              var promotie  = "";
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
              var           id      = stuk + promotie.substring(1);
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
  }

  private static void verwerkIds(int[] bord, Map<String, Integer> ids,
                                 Map<String, String> verplaatst) {
    ids.entrySet().forEach(id -> {
      if (id.getValue() >= 0
          && id.getValue() < 100) {
        var positie = getPositie(id.getValue()%100);
        var stuk  = CaissaUtils.zoekStuk(id.getKey().charAt(0));
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
    });
  }

  private static Map<String, Object> verwerkPartij(PGN pgn, int partijnr)
      throws PgnException {
    FEN fen;

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

    var bord  = fen.getBord();
    setFenbord(bord, ids);

    ids.entrySet().forEach(id -> {
      List<Integer> traject = new ArrayList<>();
      traject.add(id.getValue());
      trajecten.put(id.getKey(), traject);
    });

    partij.put("_gamekey", "" + partijnr);
    pgn.getTags().entrySet().forEach(tag -> partij.put(tag.getKey(),
                                                       tag.getValue()));
    partij.put("_moves", pgn.getZetten());
    var zuivereZetten = pgn.getZuivereZetten();
    if (metPgnviewer) {
      partij.put("_pgnviewer",
                 CaissaUtils.pgnZettenToChessTheatre(zuivereZetten));
    }

    if (DoosUtils.isNotBlankOrNull(zuivereZetten)) {
      verwerkZuivereZetten(partij, zuivereZetten, fen, ids, trajecten);
    }

    return partij;
  }

  private static void verwerkZuivereZetten(Map<String, Object> partij,
                                           String zuivereZetten, FEN fen,
                                           Map<String, Integer> ids,
                                           Map<String, List<Integer>> trajecten)
      throws PgnException {
    var                 zetten      =
        vertaal(zuivereZetten, vanStukken, naarStukken).split(" ");
    Map<String, Object> jsonZetten  = new LinkedHashMap<>();
    for (var i = 0; i < zetten.length; i++) {
      Map<String, String> jsonZet   = new LinkedHashMap<>();
      var                 pgnZet    = zetten[i].replaceAll("^[0-9]*\\.", "");
      jsonZet.put("notatie", pgnZet);
      if (metFen || metTrajecten) {
        var zet = CaissaUtils.vindZet(fen, vertaal(pgnZet, naarStukken,
                                      DEFSTUKKEN));
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

  private static void wijzigTrajecten(FEN fen, Map<String, Integer> ids,
                                      Map<String, List<Integer>> trajecten) {
    var                 bord        = fen.getBord();
    Map<String, String> verplaatst  = new HashMap<>();

    verwerkIds(bord, ids, verplaatst);
    verwerkBord(bord, ids, verplaatst, trajecten);

    trajecten.keySet().forEach(stuk -> trajecten.get(stuk).add(ids.get(stuk)));
  }
}
