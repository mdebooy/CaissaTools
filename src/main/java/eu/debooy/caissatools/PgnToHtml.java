/**
 * Copyright 2008 Marco de Booij
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
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.Bestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.html.Utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
public final class PgnToHtml {
  public static final String  HTML_TABLE_COLGROUP       = "table.colgroup.";
  public static final String  HTML_TABLE_COLGROUP_BEGIN =
      "table.colgroup.begin.";
  public static final String  HTML_TABLE_COLGROUP_EIND  =
      "table.colgroup.eind.";
  public static final String  HTML_TABLE_HEAD_BEGIN     = "table.head.begin.";
  public static final String  HTML_TABLE_HEAD_EIND      = "table.head.eind.";
  public static final String  HTML_TABLE_HEAD_PUNTEN    = "table.head.punten";
  public static final String  HTML_TABLE_HEAD_PARTIJEN  = "table.head.partijen";
  public static final String  HTML_TABLE_HEAD_SB        = "table.head.sb";

  public static final String  PROP_INDENT               = "indent";

  private static  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private PgnToHtml() {}

  public static void execute(String[] args) throws PgnException {
    String        charsetIn   = Charset.defaultCharset().name();
    String        charsetUit  = Charset.defaultCharset().name();
    List<String>  fouten      = new ArrayList<String>();
    Set<String>   spelers     = new HashSet<String>();

    Banner.printBanner(resourceBundle.getString("banner.pgntohtml"));

    Arguments arguments = new Arguments(args);
    arguments.setParameters(new String[] {CaissaTools.BESTAND,
                                          CaissaTools.CHARDSETIN,
                                          CaissaTools.CHARDSETUIT,
                                          CaissaTools.ENKEL,
                                          CaissaTools.HALVE,
                                          CaissaTools.INVOERDIR,
                                          CaissaTools.MATRIXOPSTAND,
                                          CaissaTools.UITVOERDIR});
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
    if (arguments.hasArgument(CaissaTools.CHARDSETIN)) {
      charsetIn   = arguments.getArgument(CaissaTools.CHARDSETIN);
    }
    if (arguments.hasArgument(CaissaTools.CHARDSETUIT)) {
      charsetUit  = arguments.getArgument(CaissaTools.CHARDSETUIT);
    }
    // enkel: 0 = Tweekamp, 1 = Enkelrondig, 2 = Dubbelrondig
    // 1 is default waarde.
    int       enkel         = 1;
    if (arguments.hasArgument(CaissaTools.ENKEL)) {
      switch (arguments.getArgument(CaissaTools.ENKEL)) {
      case DoosConstants.WAAR:
        enkel = 1;
        break;
      case DoosConstants.ONWAAR:
        enkel = 2;
        break;
      default:
        enkel = 0;
        break;
      }
    }
    String[]  halve     =
      DoosUtils.nullToEmpty(arguments.getArgument(CaissaTools.HALVE))
               .split(";");
    boolean   matrixOpStand = false;
    if (arguments.hasArgument(CaissaTools.MATRIXOPSTAND)) {
      matrixOpStand =
          DoosConstants.WAAR.equalsIgnoreCase(
              arguments.getArgument(CaissaTools.MATRIXOPSTAND));
    }
    String    invoerdir   = ".";
    if (arguments.hasArgument(CaissaTools.INVOERDIR)) {
      invoerdir   = arguments.getArgument(CaissaTools.INVOERDIR);
    }
    if (invoerdir.endsWith(File.separator)) {
      invoerdir   = invoerdir.substring(0,
                                        invoerdir.length()
                                        - File.separator.length());
    }
    String    uitvoerdir  = invoerdir;
    if (arguments.hasArgument(CaissaTools.UITVOERDIR)) {
      uitvoerdir  = arguments.getArgument(CaissaTools.UITVOERDIR);
    }
    if (uitvoerdir.endsWith(File.separator)) {
      uitvoerdir  = uitvoerdir.substring(0,
                                         uitvoerdir.length()
                                         - File.separator.length());
    }

    Arrays.sort(halve, String.CASE_INSENSITIVE_ORDER);

    if (uitvoerdir.endsWith(File.separator)) {
      uitvoerdir  = uitvoerdir.substring(0, uitvoerdir.length()
                                            - File.separator.length());
    }

    if (!fouten.isEmpty() ) {
      help();
      for (String fout : fouten) {
        DoosUtils.foutNaarScherm(fout);
      }
      return;
    }

    Collection<PGN> partijen  =
        CaissaUtils.laadPgnBestand(invoerdir + File.separator + bestand
                                     + CaissaTools.EXTENSIE_PGN, charsetIn);

    for (PGN partij: partijen) {
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
    }

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
    maakMatrix(punten, uitvoerdir + File.separator + "matrix.html",
               charsetUit, matrix, enkel, noSpelers, kolommen);

    // Maak de index.html file
    maakIndex(punten, uitvoerdir + File.separator + "index.html", charsetUit,
              matrix, enkel, noSpelers);

    DoosUtils.naarScherm(resourceBundle.getString("label.bestand") + " "
                         + uitvoerdir + File.separator + bestand
                         + CaissaTools.EXTENSIE_PGN);
    DoosUtils.naarScherm(resourceBundle.getString("label.partijen") + " "
                         + partijen.size());
    DoosUtils.naarScherm(resourceBundle.getString("label.uitvoer") + " "
                         + uitvoerdir);
    DoosUtils.naarScherm(resourceBundle.getString("label.klaar"));
  }

  /**
   * Geeft de 'help' pagina.
   */
  protected static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar PgnToHtml ["
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
    DoosUtils.naarScherm("  --enkel         ",
                         resourceBundle.getString("help.enkel"), 80);
    DoosUtils.naarScherm("  --halve         ",
                         resourceBundle.getString("help.halve"), 80);
    DoosUtils.naarScherm("  --invoerdir     ",
                         resourceBundle.getString("help.invoerdir"), 80);
    DoosUtils.naarScherm("  --matrixopstand ",
                         resourceBundle.getString("help.matrixopstand"), 80);
    DoosUtils.naarScherm("  --uitvoerdir    ",
                         resourceBundle.getString("help.uitvoerdir"), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("help.paramverplicht"),
                             "bestand"), 80);
    DoosUtils.naarScherm();
  }

  public static void maakIndex(Spelerinfo[] punten, String bestand,
                               String charsetUit, double[][] matrix,
                               int enkel, int noSpelers) {
    Arrays.sort(punten);
    InputStream     instream  = null;
    BufferedWriter  output    = null;
    Properties      props     = new Properties();
    StringBuilder   prefix    = new StringBuilder();
    try {
      output    = Bestand.openUitvoerBestand(bestand, charsetUit);
      instream  = PgnToHtml.class.getClassLoader()
                           .getResourceAsStream("index.properties");
      props.load(instream);
  
      if (props.containsKey(PROP_INDENT)) {
        int indent = Integer.valueOf(props.getProperty(PROP_INDENT));
        while (prefix.length() < indent) {
          prefix.append(" ");
        }
      }
  
      // Start de tabel
      Bestand.schrijfRegel(output, prefix + props.getProperty("table.begin"));
      // De colgroup
      int k = 1;
      if (props.containsKey(HTML_TABLE_COLGROUP + k)) {
        while (props.containsKey(HTML_TABLE_COLGROUP + k)) {
          Bestand.schrijfRegel(output,
              prefix + props.getProperty(HTML_TABLE_COLGROUP + k));
          k++;
        }
      }
      // De thead
      k = 1;
      if (props.containsKey(HTML_TABLE_HEAD_BEGIN + k)) {
        while (props.containsKey(HTML_TABLE_HEAD_BEGIN + k)) {
          Bestand.schrijfRegel(output,
              prefix + props.getProperty(HTML_TABLE_HEAD_BEGIN + k));
          k++;
        }
      }
      Bestand.schrijfRegel(output,
          prefix
            + MessageFormat.format(props.getProperty("table.head.nr"),
                                   resourceBundle.getString("tag.nummer")));
      Bestand.schrijfRegel(output,
          prefix + MessageFormat.format(props.getProperty("table.head.naam"),
                                        resourceBundle.getString("tag.naam")));
      Bestand.schrijfRegel(output,
          prefix
            + MessageFormat.format(props.getProperty(HTML_TABLE_HEAD_PUNTEN),
                                   resourceBundle.getString("tag.punten")));
      Bestand.schrijfRegel(output,
          prefix
            + MessageFormat.format(props.getProperty(HTML_TABLE_HEAD_PARTIJEN),
                                   resourceBundle.getString("tag.partijen")));
      Bestand.schrijfRegel(output,
          prefix + MessageFormat.format(props.getProperty(HTML_TABLE_HEAD_SB),
                                        resourceBundle.getString("tag.sb")));
      k = 1;
      if (props.containsKey(HTML_TABLE_HEAD_EIND + k)) {
        while (props.containsKey(HTML_TABLE_HEAD_EIND + k)) {
          Bestand.schrijfRegel(output,
              prefix + props.getProperty(HTML_TABLE_HEAD_EIND + k));
          k++;
        }
      }
      // De tbody
      Bestand.schrijfRegel(output,
                           prefix + props.getProperty("table.body.begin"));
      for (int i = 0; i < noSpelers; i++) {
        maakIndexBody(output, prefix.toString(), props, punten[i], i + 1);
      }
      // Alles netjes afsluiten
      Bestand.schrijfRegel(output,
                           prefix + props.getProperty("table.body.eind"));
      Bestand.schrijfRegel(output, prefix + props.getProperty("table.eind"));
    } catch (IOException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (output != null) {
          output.close();
        }
      } catch (IOException ex) {
        DoosUtils.foutNaarScherm(ex.getLocalizedMessage());
      }
    }
  }

  public static void maakIndexBody(BufferedWriter output, String prefix,
                                   Properties props, Spelerinfo speler,
                                   int plaats) throws IOException {
    Bestand.schrijfRegel(output,
        prefix + props.getProperty("table.body.start"));
    Bestand.schrijfRegel(output,
        prefix + MessageFormat.format(props.getProperty("table.body.nr"),
                                      (plaats)));
    Bestand.schrijfRegel(output,
        prefix + MessageFormat.format(props.getProperty("table.body.naam"),
                                      swapNaam(speler.getNaam())));
    int     pntn  = speler.getPunten().intValue();
    String  decim = Utilities.kwart(speler.getPunten());
    Bestand.schrijfRegel(output,
        prefix + MessageFormat.format(props.getProperty("table.body.punten"),
            ((pntn == 0 && "".equals(decim)) || pntn >= 1 ? pntn : decim),
            (pntn == 0 && "".equals(decim)) || pntn >= 1 ? decim : ""));
    Bestand.schrijfRegel(output,
        prefix + MessageFormat.format(props.getProperty("table.body.partijen"),
                                      speler.getPartijen()));
    int     wpntn   = speler.getTieBreakScore().intValue();
    String  wdecim  = Utilities.kwart(speler.getTieBreakScore());
    Bestand.schrijfRegel(output,
        prefix + MessageFormat.format(props.getProperty("table.body.sb"),
            ((wpntn == 0 && "".equals(wdecim)) || wpntn >= 1 ? wpntn : wdecim),
            (wpntn == 0 && "".equals(wdecim)) || wpntn >= 1 ? wdecim : ""));
    Bestand.schrijfRegel(output, prefix + props.getProperty("table.body.stop"));
  }

  public static void maakMatrix(Spelerinfo[]  punten, String bestand,
                                String charsetUit, double[][] matrix,
                                int enkel, int noSpelers, int kolommen) {
    InputStream     instream  = null;
    BufferedWriter  output    = null;
    Properties      props     = new Properties();
    StringBuilder   prefix    = new StringBuilder();
    try {
      output  = Bestand.openUitvoerBestand(bestand, charsetUit);
      instream = PgnToHtml.class.getClassLoader()
          .getResourceAsStream("matrix.properties");
      props.load(instream);
  
      if (props.containsKey(PROP_INDENT)) {
        int indent = Integer.valueOf(props.getProperty(PROP_INDENT));
        while (prefix.length() < indent) {
          prefix.append(" ");
        }
      }
  
      // Start de tabel
      Bestand.schrijfRegel(output, prefix + props.getProperty("table.begin"));
      // De colgroup
      int k = 1;
      if (props.containsKey(HTML_TABLE_COLGROUP_BEGIN + k)) {
        while (props.containsKey(HTML_TABLE_COLGROUP_BEGIN + k)) {
          Bestand.schrijfRegel(output,
              prefix + props.getProperty(HTML_TABLE_COLGROUP_BEGIN + k));
          k++;
        }
      }
      for (int i = 0; i < noSpelers; i++) {
        if (enkel == 1) {
          Bestand.schrijfRegel(output,
              prefix + props.getProperty("table.colgroup.enkel"));
        } else {
          Bestand.schrijfRegel(output,
              prefix + props.getProperty("table.colgroup.dubbel"));
        }
      }
      k = 1;
      if (props.containsKey(HTML_TABLE_COLGROUP_EIND + k)) {
        while (props.containsKey(HTML_TABLE_COLGROUP_EIND + k)) {
          Bestand.schrijfRegel(output,
              prefix + props.getProperty(HTML_TABLE_COLGROUP_EIND + k));
          k++;
        }
      }
      // De thead
      k = 1;
      if (props.containsKey(HTML_TABLE_HEAD_BEGIN + k)) {
        while (props.containsKey(HTML_TABLE_HEAD_BEGIN + k)) {
          Bestand.schrijfRegel(output,
              prefix + props.getProperty(HTML_TABLE_HEAD_BEGIN + k));
          k++;
        }
      }
      for (int i = 0; i < noSpelers; i++) {
        if (enkel == 1) {
          Bestand.schrijfRegel(output, prefix + MessageFormat
              .format(props.getProperty("table.head.enkel"), (i + 1)));
        } else {
          Bestand.schrijfRegel(output, prefix + MessageFormat
              .format(props.getProperty("table.head.dubbel"), (i + 1)));
        }
      }
      Bestand.schrijfRegel(output, prefix
          + MessageFormat.format(props.getProperty(HTML_TABLE_HEAD_PUNTEN),
              resourceBundle.getString("tag.punten")));
      Bestand.schrijfRegel(output,
          prefix + MessageFormat.format(
              props.getProperty(HTML_TABLE_HEAD_PARTIJEN),
              resourceBundle.getString("tag.partijen")));
      Bestand.schrijfRegel(output,
          prefix + MessageFormat.format(props.getProperty(HTML_TABLE_HEAD_SB),
              resourceBundle.getString("tag.sb")));
      if (enkel == 2) {
        Bestand.schrijfRegel(output,
            prefix + props.getProperty(HTML_TABLE_HEAD_EIND + "1"));
        Bestand.schrijfRegel(output,
            prefix + props.getProperty(HTML_TABLE_HEAD_BEGIN + "2"));
        Bestand.schrijfRegel(output,
            prefix + props.getProperty(HTML_TABLE_HEAD_BEGIN + "3"));
        for (int i = 0; i < noSpelers; i++) {
          Bestand.schrijfRegel(output,
              prefix
                  + MessageFormat.format(
                      props.getProperty("table.head.dubbel2"),
                      resourceBundle.getString("tag.wit"),
                      resourceBundle.getString("tag.zwart")));
        }
        Bestand.schrijfRegel(output, prefix
            + MessageFormat.format(props.getProperty(HTML_TABLE_HEAD_PUNTEN),
                                   ""));
        Bestand.schrijfRegel(output, prefix
            + MessageFormat.format(props.getProperty(HTML_TABLE_HEAD_PARTIJEN),
                                   ""));
        Bestand.schrijfRegel(output, prefix
            + MessageFormat.format(props.getProperty(HTML_TABLE_HEAD_SB), ""));
      }
      k = 1;
      if (props.containsKey(HTML_TABLE_HEAD_EIND + k)) {
        while (props.containsKey(HTML_TABLE_HEAD_EIND + k)) {
          Bestand.schrijfRegel(output,
              prefix + props.getProperty(HTML_TABLE_HEAD_EIND + k));
          k++;
        }
      }
      // De tbody
      Bestand.schrijfRegel(output,
          prefix + props.getProperty("table.body.begin"));
      for (int i = 0; i < noSpelers; i++) {
        maakMatrixBody(output, prefix.toString(), props, punten[i], i,
                       enkel, kolommen, matrix);
      }
      // Alles netjes afsluiten
      Bestand.schrijfRegel(output, prefix
                                   + props.getProperty("table.body.eind"));
      Bestand.schrijfRegel(output, prefix + props.getProperty("table.eind"));
    } catch (IOException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (output != null) {
          output.close();
        }
      } catch (IOException ex) {
        DoosUtils.foutNaarScherm(ex.getLocalizedMessage());
      }
    }
  }

  public static void maakMatrixBody(BufferedWriter output, String prefix,
                                    Properties props, Spelerinfo speler,
                                    int i, int enkel, int kolommen,
                                    double[][] matrix) throws IOException {
    Bestand.schrijfRegel(output,
        prefix + props.getProperty("table.body.start"));
    Bestand.schrijfRegel(output, prefix
        + MessageFormat.format(props.getProperty("table.body.nr"), (i + 1)));
    Bestand.schrijfRegel(output,
        prefix + MessageFormat.format(props.getProperty("table.body.naam"),
                                      swapNaam(speler.getNaam())));
    for (int j = 0; j < kolommen; j++) {
      if ((j / enkel) * enkel == j) {
        Bestand.schrijfRegel(output, prefix + "      ", 0);
      }
      if (i == j / enkel) {
        Bestand.schrijfRegel(output, props.getProperty("table.body.zelf"), 0);
      } else {
        // -1 is een niet gespeelde partij.
        if ((j / enkel) * enkel == j) {
          Bestand.schrijfRegel(output,
              MessageFormat.format(props.getProperty("table.body.wit"),
                  (matrix[i][j] < 0.0 ? ""
                      : (matrix[i][j] == 0.5 ? Utilities.kwart(matrix[i][j])
                          : "" + ((Double) matrix[i][j]).intValue()
                              + Utilities.kwart(matrix[i][j])))),
              0);
        } else {
          Bestand.schrijfRegel(output,
              MessageFormat.format(props.getProperty("table.body.zwart"),
                  (matrix[i][j] < 0.0 ? ""
                      : (matrix[i][j] == 0.5 ? Utilities.kwart(matrix[i][j])
                          : "" + ((Double) matrix[i][j]).intValue()
                              + Utilities.kwart(matrix[i][j])))),
              0);
        }
      }
      if ((j / enkel) * enkel != j) {
        Bestand.schrijfRegel(output, "");
      }
    }
    int     pntn  = speler.getPunten().intValue();
    String  decim = Utilities.kwart(speler.getPunten());
    Bestand.schrijfRegel(output,
        prefix + MessageFormat.format(props.getProperty("table.body.punten"),
            ((pntn == 0 && "".equals(decim)) || pntn >= 1 ? pntn : decim),
            (pntn == 0 && "".equals(decim)) || pntn >= 1 ? decim : ""));
    Bestand.schrijfRegel(output, prefix + MessageFormat.format(
        props.getProperty("table.body.partijen"), speler.getPartijen()));
    int     wpntn   = speler.getTieBreakScore().intValue();
    String  wdecim  = Utilities.kwart(speler.getTieBreakScore());
    Bestand.schrijfRegel(output,
        prefix + MessageFormat.format(props.getProperty("table.body.sb"),
            ((wpntn == 0 && "".equals(wdecim)) || wpntn >= 1 ? wpntn : wdecim),
            (wpntn == 0 && "".equals(wdecim)) || wpntn >= 1 ? wdecim : ""));
    Bestand.schrijfRegel(output,
        prefix + props.getProperty("table.body.stop"));
  }

  private static String swapNaam(String naam) {
    String[]  deel  = naam.split(",");
    if (deel.length == 1) {
      return naam;
    }
    
    return deel[1].trim() + " " + deel[0].trim();
  }
}
