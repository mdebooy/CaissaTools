/**
 * Copyright (c) 2008 Marco de Booij
 *
 * Licensed under the EUPL, Version 1.0 or - as soon they will be approved by
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
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.Partij;
import eu.debooy.caissa.Spelerinfo;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.JsonBestand;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.html.Utilities;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/**
 * @author Marco de Booij
 */
public final class PgnToHtml extends Batchjob {
  public static final String  HTML_LEGENDA                = "table.legenda.";
  public static final String  HTML_TABLE_BEGIN            = "table.begin.";
  public static final String  HTML_TABLE_BODY             = "table.body.";
  public static final String  HTML_TABLE_BODY_BEGIN       = "table.body.begin.";
  public static final String  HTML_TABLE_BODY_EIND        = "table.body.eind.";
  public static final String  HTML_TABLE_BODY_FOOTER      =
      "table.body.footer.";
  public static final String  HTML_TABLE_COLGROUP         = "table.colgroup.";
  public static final String  HTML_TABLE_COLGROUP_BEGIN   =
      "table.colgroup.begin.";
  public static final String  HTML_TABLE_COLGROUP_DUBBEL  =
      "table.colgroup.dubbel.";
  public static final String  HTML_TABLE_COLGROUP_EIND    =
      "table.colgroup.eind.";
  public static final String  HTML_TABLE_COLGROUP_ENKEL   =
      "table.colgroup.enkel.";
  public static final String  HTML_TABLE_EIND             = "table.eind.";
  public static final String  HTML_TABLE_HEAD             = "table.head.";
  public static final String  HTML_TABLE_HEAD_BEGIN       = "table.head.begin.";
  public static final String  HTML_TABLE_HEAD_BEGIN_M     = "table.head.begin";
  public static final String  HTML_TABLE_HEAD_DUBBEL      = "table.head.dubbel";
  public static final String  HTML_TABLE_HEAD_DUBBEL2     =
      "table.head.dubbel2";
  public static final String  HTML_TABLE_HEAD_EIND        = "table.head.eind.";
  public static final String  HTML_TABLE_HEAD_ENKEL       = "table.head.enkel";
  public static final String  HTML_TABLE_HEAD_NAAM        = "table.head.naam";
  public static final String  HTML_TABLE_HEAD_NR          = "table.head.nr";
  public static final String  HTML_TABLE_HEAD_PARTIJEN    =
      "table.head.partijen";
  public static final String  HTML_TABLE_HEAD_PUNTEN      = "table.head.punten";
  public static final String  HTML_TABLE_HEAD_SB          = "table.head.sb";
  public static final String  HTML_TABLE_ROW_BEGIN        = "table.row.begin";
  public static final String  HTML_TABLE_ROW_EIND         = "table.row.eind";
  public static final String  HTML_TABLE_ROW_NAAM         = "table.row.naam";
  public static final String  HTML_TABLE_ROW_NR           = "table.row.nr";
  public static final String  HTML_TABLE_ROW_PARTIJEN     =
      "table.row.partijen";
  public static final String  HTML_TABLE_ROW_PUNTEN       = "table.row.punten";
  public static final String  HTML_TABLE_ROW_SB           = "table.row.sb";
  public static final String  HTML_TABLE_ROW_WIT          = "table.row.wit";
  public static final String  HTML_TABLE_ROW_ZELF         = "table.row.zelf";
  public static final String  HTML_TABLE_ROW_ZWART        = "table.row.zwart";

  public static final String  TAG_NAAM      = "tag.naam";
  public static final String  TAG_NUMMER    = "tag.nummer";
  public static final String  TAG_PARTIJEN  = "tag.partijen";
  public static final String  TAG_PUNTEN    = "tag.punten";
  public static final String  TAG_SB        = "tag.sb";
  public static final String  TAG_WIT       = "tag.wit";
  public static final String  TAG_ZWART     = "tag.zwart";

  public static final String  PROP_INDENT = "indent";

  private static  final ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private static  TekstBestand  output;
  private static  String        prefix  = "";
  private static  Properties    skelet;

  private PgnToHtml() {}

  public static void execute(String[] args) {
    List<Spelerinfo>  spelers = new ArrayList<>();

    Banner.printMarcoBanner(resourceBundle.getString("banner.pgntohtml"));

    if (!setParameters(args)) {
      return;
    }

    String    invoer        = parameters.get(PAR_INVOERDIR)
                              + parameters.get(CaissaTools.PAR_BESTAND)
                              + EXT_PGN;
    boolean   matrixOpStand =
        DoosConstants.WAAR.equalsIgnoreCase(
            parameters.get(CaissaTools.PAR_MATRIXOPSTAND));
    String    uitvoerdir    = parameters.get(PAR_UITVOERDIR);

    Collection<PGN> partijen;
    try {
      partijen = CaissaUtils.laadPgnBestand(invoer,
                                            parameters.get(PAR_CHARSETIN));
    } catch (PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    JsonBestand competitie;
    try {
      competitie  =
          new JsonBestand.Builder()
                         .setBestand(parameters.get(PAR_INVOERDIR)
                                     + parameters.get(CaissaTools.PAR_SCHEMA)
                                     + EXT_JSON)
                         .setCharset(parameters.get(PAR_CHARSETIN))
                         .build();
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    JSONArray   jsonArray = competitie.getArray("spelers");
    int         spelerId  = 1;
    for (Object naam : jsonArray.toArray()) {
      Spelerinfo  speler  = new Spelerinfo();
      speler.setSpelerId(spelerId);
      speler.setNaam(((JSONObject) naam).get("naam").toString());
      spelers.add(speler);
      spelerId++;
    }

    String[]  halve;
    if (competitie.containsKey("halvespelers")) {
      jsonArray = competitie.getArray("halvespelers");
      halve     = new String[jsonArray.size()];
      for (int i = 0; i < jsonArray.size(); i++) {
        halve[i]  = jsonArray.get(i).toString();
      }
      Arrays.sort(halve, String.CASE_INSENSITIVE_ORDER);
    } else {
      halve = new String[0];
    }

    // enkel: 0 = Tweekamp, 1 = Enkelrondig, 2 = Dubbelrondig
    int enkel;
    if (competitie.containsKey("enkelrondig")) {
      if ((boolean) competitie.get("enkelrondig")) {
        enkel = CaissaConstants.TOERNOOI_ENKEL;
      } else {
        enkel = CaissaConstants.TOERNOOI_DUBBEL;
      }
    } else {
      enkel = CaissaConstants.TOERNOOI_MATCH;
    }

    // Maak de Matrix.
    int           noSpelers = spelers.size();
    int           kolommen  = noSpelers * enkel;
    Spelerinfo[]  punten    = new Spelerinfo[noSpelers];
    String[]      namen     = new String[noSpelers];
    double[][]    matrix    = new double[noSpelers][noSpelers * enkel];
    Iterator<Spelerinfo>
                speler    = spelers.iterator();
    for (int i = 0; i < noSpelers; i++) {
      namen[i]  = speler.next().getNaam();
      for (int j = 0; j < kolommen; j++) {
        matrix[i][j] = -1.0;
      }
    }
    Arrays.sort(namen, String.CASE_INSENSITIVE_ORDER);
    for (int i = 0; i < noSpelers; i++) {
      punten[i] = new Spelerinfo();
      punten[i].setNaam(namen[i]);
    }

    // Bepaal de score en SB score.
    CaissaUtils.vulToernooiMatrix(partijen, punten, halve, matrix, enkel,
                                  matrixOpStand, CaissaConstants.TIEBREAK_SB);

    // Maak het matrix.html bestand.
    maakMatrix(punten, matrix, enkel, noSpelers, kolommen);

    // Maak het index.html bestand.
    maakIndex(punten, noSpelers);

    // Maak het uitslagen.html bestand.
    if (enkel != CaissaConstants.TOERNOOI_MATCH) {
      boolean     enkelrondig = (enkel == CaissaConstants.TOERNOOI_ENKEL);
      Set<Partij> schema      =
          CaissaUtils.genereerSpeelschema(spelers, enkelrondig, partijen);
      if (!schema.isEmpty()) {
        String[]    data      = vulKalender(spelers.size(), enkel,
                                            competitie.getArray("kalender"));
        maakUitslagen(schema, data);
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
                             uitvoerdir));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  private static void genereerLegenda() throws BestandException {
    String  forfait   = resourceBundle.getString("message.forfait");
    String  notRanked = resourceBundle.getString("message.notranked");
    genereerTabelheading();
    schrijfUitvoer(HTML_TABLE_BODY_BEGIN);
    schrijfUitvoer(HTML_LEGENDA, notRanked, forfait);
    schrijfUitvoer(HTML_TABLE_BODY_EIND);
    schrijfUitvoer(HTML_TABLE_EIND);
  }

  private static void genereerRondefooting() throws BestandException {
    schrijfUitvoer(HTML_TABLE_BODY_FOOTER);
    schrijfUitvoer(HTML_TABLE_BODY_EIND);
    schrijfUitvoer(HTML_TABLE_EIND);
  }

  private static void genereerRondeheading(int ronde, String datum)
      throws BestandException {
    genereerTabelheading();
    schrijfUitvoer(HTML_TABLE_HEAD, resourceBundle.getString("label.ronde"),
                   ronde, datum);
    schrijfUitvoer(HTML_TABLE_BODY_BEGIN);
  }

  private static void genereerTabelheading() throws BestandException {
    schrijfUitvoer(HTML_TABLE_BEGIN);
    schrijfUitvoer(HTML_TABLE_COLGROUP);
  }

  private static void genereerUitslagtabel(Set<Partij> schema, String[] data)
      throws BestandException {
    Iterator<Partij>  iter    = schema.iterator();
    Partij            partij  = iter.next();
    int               vorige  = Integer.parseInt(partij.getRonde()
                                                       .getRound()
                                                       .split("\\.")[0]);
    genereerRondeheading(vorige, DoosUtils.nullToEmpty(data[vorige]));

    do {
      int ronde = Integer.parseInt(partij.getRonde().getRound()
                                         .split("\\.")[0]);
      if (ronde != vorige) {
        genereerRondefooting();
        genereerRondeheading(ronde, DoosUtils.nullToEmpty(data[ronde]));
        vorige  = ronde;
      }

      String  klasse  =
          (partij.isRanked() && !partij.isBye()) ? "" : " class=\"btncmp\"";
      String  uitslag = partij.getUitslag().replaceAll("1/2", "&frac12;")
                              .replace('-', (partij.isForfait() ? 'F' : '-'))
                              .replace('*', '-');
      String  wit     = partij.getWitspeler().getVolledigenaam();
      String  zwart   = partij.getZwartspeler().getVolledigenaam();
      schrijfUitvoer(HTML_TABLE_BODY, klasse, wit, zwart, uitslag);
      if (iter.hasNext()) {
        partij  = iter.next();
      } else {
        partij  = null;
      }
    } while (null != partij);

    genereerRondefooting();
    genereerLegenda();
  }

  public static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar PgnToHtml ["
                         + getMelding(LBL_OPTIE)
                         + "] --bestand=<"
                         + resourceBundle.getString("label.pgnbestand") + ">"
                         + " --schema=<"
                         + resourceBundle.getString("label.competitieschema")
                         + ">");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_BESTAND, 14),
                         resourceBundle.getString("help.bestand"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETIN, 14),
        MessageFormat.format(getMelding(HLP_CHARSETIN),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETUIT, 14),
        MessageFormat.format(getMelding(HLP_CHARSETUIT),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_INVOERDIR, 14),
                         getMelding(HLP_INVOERDIR), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_MATRIXOPSTAND, 14),
                         resourceBundle.getString("help.matrixopstand"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_SCHEMA, 14),
                         resourceBundle.getString("help.competitieschema"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_UITVOERDIR, 14),
                         getMelding(HLP_UITVOERDIR), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(getMelding(HLP_PARAMSVERPLICHT),
                             CaissaTools.PAR_BESTAND, CaissaTools.PAR_SCHEMA),
                             80);
    DoosUtils.naarScherm();
  }

  private static void maakIndex(Spelerinfo[] punten, int noSpelers) {
    skelet  = new Properties();
    Arrays.sort(punten);
    try {
      output    = new TekstBestand.Builder()
                                  .setBestand(parameters.get(PAR_UITVOERDIR)
                                              + "index.html")
                                  .setCharset(parameters.get(PAR_CHARSETUIT))
                                  .setLezen(false).build();
      skelet.load(PgnToHtml.class.getClassLoader()
                           .getResourceAsStream("index.properties"));

      if (skelet.containsKey(PROP_INDENT)) {
        prefix  = DoosUtils.stringMetLengte("",
            Integer.valueOf(skelet.getProperty(PROP_INDENT)));
      } else {
        prefix  = "";
      }

      // Start de tabel
      schrijfUitvoer(HTML_TABLE_BEGIN);
      // De colgroup
      schrijfUitvoer(HTML_TABLE_COLGROUP);
      // De thead
      schrijfUitvoer(HTML_TABLE_HEAD_BEGIN);
      output.write(prefix
          + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_NR),
                                 resourceBundle.getString(TAG_NUMMER)));
      output.write(prefix
          + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_NAAM),
                                 resourceBundle.getString(TAG_NAAM)));
      output.write(prefix
          + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_PUNTEN),
                                 resourceBundle.getString(TAG_PUNTEN)));
      output.write(prefix
          + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_PARTIJEN),
                                 resourceBundle.getString(TAG_PARTIJEN)));
      output.write(prefix
          + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_SB),
                                 resourceBundle.getString(TAG_SB)));
      schrijfUitvoer(HTML_TABLE_HEAD_EIND);
      // De tbody
      schrijfUitvoer(HTML_TABLE_BODY_BEGIN);
      for (int i = 0; i < noSpelers; i++) {
        maakIndexBody(punten[i], i + 1);
      }
      // Alles netjes afsluiten
      schrijfUitvoer(HTML_TABLE_BODY_EIND);
      schrijfUitvoer(HTML_TABLE_EIND);
    } catch (BestandException | IOException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (output != null) {
          output.close();
        }
      } catch (BestandException ex) {
        DoosUtils.foutNaarScherm(ex.getLocalizedMessage());
      }
    }
  }

  private static void maakIndexBody(Spelerinfo speler, int plaats)
      throws BestandException {
    output.write(prefix + skelet.getProperty(HTML_TABLE_ROW_BEGIN));
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_NR), plaats));
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_NAAM),
                               swapNaam(speler.getNaam())));
    int     pntn  = speler.getPunten().intValue();
    String  decim = Utilities.kwart(speler.getPunten());
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_PUNTEN),
            ((pntn == 0 && "".equals(decim)) || pntn >= 1 ? pntn : decim),
            (pntn == 0 && "".equals(decim)) || pntn >= 1 ? decim : ""));
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_PARTIJEN),
                               speler.getPartijen()));
    int     wpntn   = speler.getTieBreakScore().intValue();
    String  wdecim  = Utilities.kwart(speler.getTieBreakScore());
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_SB),
            ((wpntn == 0 && "".equals(wdecim)) || wpntn >= 1 ? wpntn : wdecim),
            (wpntn == 0 && "".equals(wdecim)) || wpntn >= 1 ? wdecim : ""));
    output.write(prefix + skelet.getProperty(HTML_TABLE_ROW_EIND));
  }

  private static void maakMatrix(Spelerinfo[]  punten, double[][] matrix,
                                 int enkel, int noSpelers, int kolommen) {
    skelet  = new Properties();
    try {
      output  = new TekstBestand.Builder()
                                .setBestand(parameters.get(PAR_UITVOERDIR)
                                            + "matrix.html")
                                .setCharset(parameters.get(PAR_CHARSETUIT))
                                .setLezen(false).build();
      skelet.load(PgnToHtml.class.getClassLoader()
                           .getResourceAsStream("matrix.properties"));

      if (skelet.containsKey(PROP_INDENT)) {
        prefix  = DoosUtils.stringMetLengte("",
            Integer.valueOf(skelet.getProperty(PROP_INDENT)));
      } else {
        prefix  = "";
      }

      // Start de tabel
      schrijfUitvoer(HTML_TABLE_BEGIN);
      // De colgroup
      schrijfUitvoer(HTML_TABLE_COLGROUP_BEGIN);
      for (int i = 0; i < noSpelers; i++) {
        if (enkel == 1) {
          schrijfUitvoer(HTML_TABLE_COLGROUP_ENKEL);
        } else {
          schrijfUitvoer(HTML_TABLE_COLGROUP_DUBBEL);
        }
      }
      schrijfUitvoer(HTML_TABLE_COLGROUP_EIND);
      // De thead
      schrijfUitvoer(HTML_TABLE_HEAD_BEGIN);
      output.write(prefix + skelet.getProperty(HTML_TABLE_HEAD_BEGIN_M));
      for (int i = 0; i < noSpelers; i++) {
        if (enkel == 1) {
          output.write(prefix
              + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_ENKEL),
                                     (i + 1)));
        } else {
          output.write(prefix
              + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_DUBBEL),
                                     (i + 1)));
        }
      }
      output.write(prefix
          + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_PUNTEN),
                                 resourceBundle.getString(TAG_PUNTEN)));
      output.write(prefix
          + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_PARTIJEN),
                                 resourceBundle.getString(TAG_PARTIJEN)));
      output.write(prefix
          + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_SB),
                                 resourceBundle.getString(TAG_SB)));
      if (enkel == 2) {
        output.write(prefix + skelet.getProperty(HTML_TABLE_HEAD_EIND + 1));
        output.write(prefix + skelet.getProperty(HTML_TABLE_HEAD_BEGIN + 2));
        output.write(prefix + skelet.getProperty(HTML_TABLE_HEAD_BEGIN_M));
        for (int i = 0; i < noSpelers; i++) {
          output.write(prefix + MessageFormat.format(
                                    skelet.getProperty(HTML_TABLE_HEAD_DUBBEL2),
                                    resourceBundle.getString(TAG_WIT),
                                    resourceBundle.getString(TAG_ZWART)));
        }
        output.write(prefix
            + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_PUNTEN),
                                   ""));
        output.write(prefix
            + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_PARTIJEN),
                                   ""));
        output.write(prefix
            + MessageFormat.format(skelet.getProperty(HTML_TABLE_HEAD_SB), ""));
      }
      schrijfUitvoer(HTML_TABLE_HEAD_EIND);
      // De tbody
      schrijfUitvoer(HTML_TABLE_BODY_BEGIN);
      for (int i = 0; i < noSpelers; i++) {
        maakMatrixBody(punten[i], i, enkel, kolommen, matrix);
      }
      // Alles netjes afsluiten
      schrijfUitvoer(HTML_TABLE_BODY_EIND);
      schrijfUitvoer(HTML_TABLE_EIND);
    } catch (BestandException | IOException e) {
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
  }

  private static void maakMatrixBody(Spelerinfo speler, int i, int enkel,
                                     int kolommen, double[][] matrix)
      throws BestandException {
    output.write(prefix + skelet.getProperty(HTML_TABLE_ROW_BEGIN));
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_NR),
                               (i + 1)));
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_NAAM),
                               swapNaam(speler.getNaam())));
    StringBuilder lijn  = new StringBuilder();
    for (int j = 0; j < kolommen; j++) {
      if ((j / enkel) * enkel == j) {
        lijn.append(prefix).append("      ");
      }
      if (i == j / enkel) {
        lijn.append(skelet.getProperty(HTML_TABLE_ROW_ZELF));
      } else {
        // -1 is een niet gespeelde partij.
        if ((j / enkel) * enkel == j) {
          lijn.append(
              MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_WIT),
                  (matrix[i][j] < 0.0 ? ""
                      : (matrix[i][j] == 0.5 ? Utilities.kwart(matrix[i][j])
                          : "" + ((Double) matrix[i][j]).intValue()
                              + Utilities.kwart(matrix[i][j])))));
        } else {
          lijn.append(
              MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_ZWART),
                  (matrix[i][j] < 0.0 ? ""
                      : (matrix[i][j] == 0.5 ? Utilities.kwart(matrix[i][j])
                          : "" + ((Double) matrix[i][j]).intValue()
                              + Utilities.kwart(matrix[i][j])))));
        }
      }
      if ((j / enkel) * enkel != j) {
        output.write(lijn.toString());
        lijn  = new StringBuilder();
      }
    }
    int     pntn  = speler.getPunten().intValue();
    String  decim = Utilities.kwart(speler.getPunten());
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_PUNTEN),
            ((pntn == 0 && "".equals(decim)) || pntn >= 1 ? pntn : decim),
            (pntn == 0 && "".equals(decim)) || pntn >= 1 ? decim : ""));
    output.write(prefix + MessageFormat.format(
        skelet.getProperty(HTML_TABLE_ROW_PARTIJEN), speler.getPartijen()));
    int     wpntn   = speler.getTieBreakScore().intValue();
    String  wdecim  = Utilities.kwart(speler.getTieBreakScore());
    output.write(prefix
        + MessageFormat.format(skelet.getProperty(HTML_TABLE_ROW_SB),
            ((wpntn == 0 && "".equals(wdecim)) || wpntn >= 1 ? wpntn : wdecim),
            (wpntn == 0 && "".equals(wdecim)) || wpntn >= 1 ? wdecim : ""));
    output.write(prefix + skelet.getProperty(HTML_TABLE_ROW_EIND));
  }

  private static void maakUitslagen(Set<Partij> schema, String[] data) {
    skelet  = new Properties();
    try {
      output  = new TekstBestand.Builder()
                                .setBestand(parameters.get(PAR_UITVOERDIR)
                                            + "uitslagen.html")
                                .setCharset(parameters.get(PAR_CHARSETUIT))
                                .setLezen(false).build();
      skelet.load(PgnToHtml.class.getClassLoader()
                           .getResourceAsStream("uitslagen.properties"));

      if (skelet.containsKey(PROP_INDENT)) {
        prefix  = DoosUtils.stringMetLengte("",
            Integer.valueOf(skelet.getProperty(PROP_INDENT)));
      } else {
        prefix  = "";
      }

      genereerUitslagtabel(schema, data);
    } catch (BestandException | IOException e) {
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
  }

  private static void schrijfUitvoer(String parameter)
      throws BestandException {
    int k = 1;
    while (skelet.containsKey(parameter + k)) {
      output.write(prefix + skelet.getProperty(parameter + k));
      k++;
    }
  }

  private static void schrijfUitvoer(String parameter, Object... params)
      throws BestandException {
    int k = 1;
    while (skelet.containsKey(parameter + k)) {
      output.write(
          MessageFormat.format(prefix + skelet.getProperty(parameter + k),
          params));
      k++;
    }
  }

  private static boolean setParameters(String[] args) {
    Arguments     arguments = new Arguments(args);
    List<String>  fouten    = new ArrayList<>();

    arguments.setParameters(new String[] {CaissaTools.PAR_BESTAND,
                                          PAR_CHARSETIN,
                                          PAR_CHARSETUIT,
                                          PAR_INVOERDIR,
                                          CaissaTools.PAR_MATRIXOPSTAND,
                                          CaissaTools.PAR_SCHEMA,
                                          PAR_UITVOERDIR});
    arguments.setVerplicht(new String[] {CaissaTools.PAR_BESTAND,
                                         CaissaTools.PAR_SCHEMA});
    if (!arguments.isValid()) {
      fouten.add(getMelding(ERR_INVALIDPARAMS));
    }

    parameters.clear();
    setBestandParameter(arguments, CaissaTools.PAR_BESTAND, EXT_PGN);
    setParameter(arguments, PAR_CHARSETIN, Charset.defaultCharset().name());
    setParameter(arguments, PAR_CHARSETUIT, Charset.defaultCharset().name());
    setDirParameter(arguments, PAR_INVOERDIR);
    setParameter(arguments, CaissaTools.PAR_MATRIXOPSTAND,
                 DoosConstants.ONWAAR);
    setBestandParameter(arguments, CaissaTools.PAR_SCHEMA, EXT_JSON);
    setDirParameter(arguments, PAR_UITVOERDIR, getParameter(PAR_INVOERDIR));

    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_BESTAND))
                 .contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              getMelding(ERR_BEVATDIRECTORY), CaissaTools.PAR_BESTAND));
    }
    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_SCHEMA))
                 .contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              getMelding(ERR_BEVATDIRECTORY), CaissaTools.PAR_SCHEMA));
    }

    if (fouten.isEmpty()) {
      return true;
    }

    help();
    printFouten(fouten);

    return false;
  }

  private static String swapNaam(String naam) {
    String[]  deel  = naam.split(",");
    if (deel.length == 1) {
      return naam;
    }

    return deel[1].trim() + " " + deel[0].trim();
  }

  private static String[] vulKalender(int aantalSpelers, int enkel,
                                      JSONArray kalender) {
    String[]  data  = new String[((aantalSpelers-1)*enkel)+1];
    for (int i = 0; i < kalender.size(); i++) {
      JSONObject  item  = (JSONObject) kalender.get(i);
      if (item.containsKey("ronde")
          && item.containsKey("datum")) {
        int ronde = Integer.parseInt(item.get("ronde").toString());
        data[ronde] = item.get("datum").toString();
      }
    }

    return data;
  }
}
