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
import eu.debooy.caissa.Spelerinfo;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;


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

  public static void  main(String[] args) {
    String[]  params  = new String[] {"--bestand=partijen.pgn",
                                      "--enkel=N",
                                      "--invoerdir=/homes/booymar/Schaken/Clubs en Organisaties/De Brug/Seizoen 2017-2018",
                                      "--uitvoerdir=/homes/booymar/Downloads"};
    execute(params);
  }

  public static void execute(String[] args) {
    Set<String>   spelers     = new HashSet<>();

    Banner.printMarcoBanner(resourceBundle.getString("banner.pgntohtml"));

    if (!setParameters(args)) {
      return;
    }

    // enkel: 0 = Tweekamp, 1 = Enkelrondig, 2 = Dubbelrondig
    // 1 is default waarde.
    int enkel;
    switch (parameters.get(CaissaTools.PAR_ENKEL)) {
    case DoosConstants.WAAR:
      enkel = CaissaConstants.TOERNOOI_ENKEL;
      break;
    case DoosConstants.ONWAAR:
      enkel = CaissaConstants.TOERNOOI_DUBBEL;
      break;
    default:
      enkel = CaissaConstants.TOERNOOI_MATCH;
      break;
    }
    String[]  halve         =
      DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_HALVE)).split(";");
    String    invoer        = parameters.get(PAR_INVOERDIR)
                              + parameters.get(CaissaTools.PAR_BESTAND)
                              + EXT_PGN;
    boolean   matrixOpStand =
        DoosConstants.WAAR.equalsIgnoreCase(
            parameters.get(CaissaTools.PAR_MATRIXOPSTAND));
    String    uitvoerdir    = parameters.get(PAR_UITVOERDIR);

    Arrays.sort(halve, String.CASE_INSENSITIVE_ORDER);

    Collection<PGN> partijen;
    try {
      partijen = CaissaUtils.laadPgnBestand(invoer,
                                            parameters.get(PAR_CHARSETIN));
    } catch (PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    partijen.forEach(partij -> {
      // Verwerk de spelers
      String  wit   = partij.getTag(CaissaConstants.PGNTAG_WHITE);
      String  zwart = partij.getTag(CaissaConstants.PGNTAG_BLACK);
      if (!"bye".equalsIgnoreCase(wit)
              && DoosUtils.isNotBlankOrNull(wit)) {
        spelers.add(wit);
      }
      if (!"bye".equalsIgnoreCase(zwart)
              && DoosUtils.isNotBlankOrNull(zwart)) {
        spelers.add(zwart);
      }
    });

    // Maak de Matrix.
    int           noSpelers = spelers.size();
    int           kolommen  = noSpelers * enkel;
    Spelerinfo[]  punten    = new Spelerinfo[noSpelers];
    String[]      namen     = new String[noSpelers];
    double[][]    matrix    = new double[noSpelers][noSpelers * enkel];
    Iterator<String>
                speler    = spelers.iterator();
    for (int i = 0; i < noSpelers; i++) {
      namen[i]  = speler.next();
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

    // Maak de index.html file
    maakIndex(punten, noSpelers);

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

  public static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar PgnToHtml ["
                         + getMelding(LBL_OPTIE)
                         + "] --bestand=<"
                         + resourceBundle.getString("label.pgnbestand") + ">");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_BESTAND, 14),
                         resourceBundle.getString("help.bestand"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETIN, 14),
        MessageFormat.format(getMelding(HLP_CHARSETIN),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETUIT, 14),
        MessageFormat.format(getMelding(HLP_CHARSETUIT),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_ENKEL, 14),
                         resourceBundle.getString("help.enkel"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_HALVE, 14),
                         resourceBundle.getString("help.halve"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_INVOERDIR, 14),
                         getMelding(HLP_INVOERDIR), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_MATRIXOPSTAND, 14),
                         resourceBundle.getString("help.matrixopstand"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_UITVOERDIR, 14),
                         getMelding(HLP_UITVOERDIR), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(getMelding(HLP_PARAMVERPLICHT),
                             CaissaTools.PAR_BESTAND), 80);
    DoosUtils.naarScherm();
  }

  private static void maakIndex(Spelerinfo[] punten, int noSpelers) {
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
      } catch (BestandException ex) {
        DoosUtils.foutNaarScherm(ex.getLocalizedMessage());
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
                                          CaissaTools.PAR_ENKEL,
                                          CaissaTools.PAR_HALVE,
                                          PAR_INVOERDIR,
                                          CaissaTools.PAR_MATRIXOPSTAND,
                                          PAR_UITVOERDIR});
    arguments.setVerplicht(new String[] {CaissaTools.PAR_BESTAND});
    if (!arguments.isValid()) {
      fouten.add(getMelding(ERR_INVALIDPARAMS));
    }

    parameters.clear();
    setBestandParameter(arguments, CaissaTools.PAR_BESTAND, EXT_PGN);
    setParameter(arguments, PAR_CHARSETIN, Charset.defaultCharset().name());
    setParameter(arguments, PAR_CHARSETUIT, Charset.defaultCharset().name());
    setParameter(arguments, CaissaTools.PAR_ENKEL, DoosConstants.WAAR);
    setParameter(arguments, CaissaTools.PAR_HALVE);
    setDirParameter(arguments, PAR_INVOERDIR);
    setParameter(arguments, CaissaTools.PAR_MATRIXOPSTAND,
                 DoosConstants.ONWAAR);
    setDirParameter(arguments, PAR_UITVOERDIR, getParameter(PAR_INVOERDIR));

    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_BESTAND))
                 .contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              getMelding(ERR_BEVATDIRECTORY), CaissaTools.PAR_BESTAND));
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
}
