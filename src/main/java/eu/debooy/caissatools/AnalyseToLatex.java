/**
 * Copyright 2018 Marco de Booij
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the Licence. You may
 * obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
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
import eu.debooy.caissa.exceptions.PgnException;
import static eu.debooy.caissatools.CaissaTools.PAR_SCHAAKNOTATIE;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import static eu.debooy.doosutils.Batchjob.setBestandParameter;
import static eu.debooy.doosutils.Batchjob.setDirParameter;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.CsvBestand;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import java.io.File;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeSet;
import java.util.regex.Pattern;


/**
 * @author Marco de Booij
 */
public class AnalyseToLatex extends Batchjob {
  private static final  String  PAR_ANALYZEBEGIN    = "AnalyseBegin.tex";

  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());
  private static final  ResourceBundle  ecoBundle       =
      ResourceBundle.getBundle("eco", Locale.getDefault());

  private static final  Map<String, String> notaties  = new HashMap<>();
  private static        TekstBestand        texInvoer;
  private static        TekstBestand        uitvoer;

  AnalyseToLatex(){}

  public static void execute(String[] args) {
    var                 charsetIn     = Charset.defaultCharset().name();
    var                 charsetUit    = "UTF-8";
    CsvBestand          schaaknotatie = null;

    Banner.printMarcoBanner(resourceBundle.getString("banner.analysetolatex"));

    if (!setParameters(args)) {
      return;
    }

    try {
      if (parameters.containsKey(CaissaTools.PAR_TEMPLATE)) {
        texInvoer =
            new TekstBestand.Builder()
                            .setBestand(
                                parameters.get(CaissaTools.PAR_TEMPLATE))
                            .setCharset(charsetIn).build();
      } else {
        texInvoer =
            new TekstBestand.Builder()
                            .setClassLoader(AnalyseToLatex.class
                                                          .getClassLoader())
                            .setBestand(PAR_ANALYZEBEGIN).build();
      }

      uitvoer = new TekstBestand.Builder()
                                .setBestand(parameters.get(PAR_UITVOERDIR)
                                    + parameters.get(CaissaTools.PAR_BESTAND)
                                    + EXT_TEX)
                                .setCharset(charsetUit)
                                .setLezen(false).build();
      schrijfBegin(DoosUtils
                      .nullToEmpty(parameters.get(CaissaTools.PAR_AUTEUR)),
                   DoosUtils
                      .nullToEmpty(parameters.get(CaissaTools.PAR_TITEL)));

      schaaknotatie =
          new CsvBestand.Builder()
                        .setBestand(PAR_SCHAAKNOTATIE)
                        .setClassLoader(AnalyseToLatex.class.getClassLoader())
                        .build();
      while (schaaknotatie.hasNext()) {
        var notatie = schaaknotatie.next();
        if (notatie.length == 2) {
          notaties.put(notatie[0], notatie[1]);
        }
      }

      Collection<PGN>
          partijen    = new TreeSet<>(new PGN.ByEventComparator());
      partijen.addAll(CaissaUtils.laadPgnBestand(
                          parameters.get(PAR_INVOERDIR)
                          + parameters.get(CaissaTools.PAR_BESTAND),
                      charsetIn));
      for (var partij: partijen) {
        verwerkPartij(partij);
      }

      schrijfEinde();
    } catch (BestandException | PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (null != schaaknotatie) {
          schaaknotatie.close();
        }
      } catch (BestandException ex) {
        DoosUtils.foutNaarScherm(ex.getLocalizedMessage());
      }
      try {
        if (null != texInvoer) {
          texInvoer.close();
        }
      } catch (BestandException ex) {
        DoosUtils.foutNaarScherm(ex.getLocalizedMessage());
      }
      try {
        if (null != uitvoer) {
          uitvoer.close();
        }
      } catch (BestandException ex) {
        DoosUtils.foutNaarScherm(ex.getLocalizedMessage());
      }
    }
  }

  private static String getTexmatezetten(char[] data, int start, int lengte)
      throws BestandException {
    var texmatezetten =
      new StringBuilder(new String(data, start, lengte));

    notaties.forEach((k,v) -> replaceAll(texmatezetten, k, v));

    return texmatezetten.toString()
                        .replaceAll("^[1-9][0-9]*\\.", "")
                        .replace("\\.\\.", "")
                        .replaceAll(" [1-9][0-9]*\\.", " ")
                        .replace("  ", " ").trim();
  }

  public static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar AnalyseToLatex ["
                         + getMelding(LBL_OPTIE)
                         + "] --bestand=<"
                         + resourceBundle.getString("label.pgnbestand") + ">");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_AUTEUR, 12),
                         resourceBundle.getString("help.auteur"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_BESTAND, 12),
                         resourceBundle.getString("help.bestand"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETIN, 12),
        MessageFormat.format(getMelding(HLP_CHARSETIN),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_INVOERDIR, 12),
                         getMelding(HLP_INVOERDIR), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_TEMPLATE, 12),
                         resourceBundle.getString("help.template"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_TITEL, 12),
                         resourceBundle.getString("help.documenttitel"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_UITVOERDIR, 12),
                         getMelding(HLP_UITVOERDIR), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(getMelding(HLP_PARAMVERPLICHT),
                             "bestand"), 80);
    DoosUtils.naarScherm();
  }

  private static void replaceAll(StringBuilder sb,
                                 String find, String replace) {
    var pattern = Pattern.compile(find);
    var matcher = pattern.matcher(sb);

    var startIndex  = 0;
    while( matcher.find(startIndex) ){
      sb.replace(matcher.start(), matcher.end(), replace);
      startIndex  = matcher.start() + replace.length();
    }
  }

  private static void schrijfBegin(String auteur,
                                   String titel) throws BestandException {
    while (texInvoer.hasNext()) {
      String  regel = texInvoer.next();
      System.out.println(regel.replace("@Auteur@", auteur)
                         .replace("@Titel@",  titel));
    }
  }

  private static void schrijfCommentaar(char[] commentaar,
                                        int start, int lengte)
      throws BestandException {
    System.out.println("\\par\\justify "
                    + new String(commentaar, start, lengte));
  }

  private static void schrijfEinde()
      throws BestandException {
    System.out.println("\\end{document}");
  }

  private static void schrijfVariant(char[] variant, int start, int lengte)
      throws BestandException {
  }

  private static void schrijfZetten(char[] zetten,
                                    int start, int lengte)
      throws BestandException {
    System.out.println("\\par\\centering|"
                    + getTexmatezetten(zetten, start, lengte) + "|");
  }

  private static boolean setParameters(String[] args) {
    Arguments     arguments = new Arguments(args);
    List<String>  fouten    = new ArrayList<>();

    arguments.setParameters(new String[] {CaissaTools.PAR_AUTEUR,
                                          CaissaTools.PAR_BESTAND,
                                          PAR_CHARSETIN,
                                          PAR_INVOERDIR,
                                          CaissaTools.PAR_TEMPLATE,
                                          CaissaTools.PAR_TITEL,
                                          PAR_UITVOERDIR});
    arguments.setVerplicht(new String[] {CaissaTools.PAR_BESTAND});
    if (!arguments.isValid()) {
      fouten.add(getMelding(ERR_INVALIDPARAMS));
    }

    parameters.clear();
    setParameter(arguments, CaissaTools.PAR_AUTEUR);
    setBestandParameter(arguments, CaissaTools.PAR_BESTAND);
    setParameter(arguments, PAR_CHARSETIN, Charset.defaultCharset().name());
    setDirParameter(arguments, PAR_INVOERDIR);
    setParameter(arguments, CaissaTools.PAR_TEMPLATE);
    setParameter(arguments, CaissaTools.PAR_TITEL);
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

  private static void verwerkCommentaar(char[] commentaar, int start, int eind)
      throws BestandException {
    if (start == eind) {
      return;
    }

    schrijfCommentaar(commentaar, start, eind-start);
  }

  private static void verwerkDeel(char[] data, int van, int tot)
      throws BestandException {
    var volgendCommentaar = zoekStartCommentaar(data, van, tot);
    var volgendeVariant   = zoekStartVariant(data, van, tot);

    if (volgendCommentaar < 0
        && volgendeVariant < 0) {
      verwerkZetten(data, van, tot);
      return;
    }

    if (volgendCommentaar == van) {
      var einde = zoekEindeCommentaar(data, van+1, tot);
      verwerkCommentaar(data, van+1, einde);
      verwerkDeel(data, einde+1, tot);
    }
    if (volgendeVariant == van) {
      var einde = zoekEindeVariant(data, van+1, tot);
      verwerkVariant(data, van, einde);
//      verwerkDeel(data, einde, tot);
    }

    var eind  = Math.min(volgendCommentaar, volgendeVariant);
    if (volgendCommentaar < 0) {
      eind  = volgendeVariant;
    }
    if (volgendeVariant < 0) {
      eind  = volgendCommentaar;
    }
    schrijfZetten(data, van, eind-van);
//    verwerkDeel(data, eind, tot);
  }

  private static void verwerkPartij(PGN partij)
      throws BestandException, PgnException {
    verwerkPartijHeading(partij);

    char[]  data  = partij.getZetten().toCharArray();
    verwerkDeel(data, 0, data.length);

    verwerkPartijEind(partij.getTag(CaissaConstants.PGNTAG_RESULT));
  }

  private static void verwerkPartijEind(String resultaat)
      throws BestandException {
    switch (resultaat) {
    case CaissaConstants.PARTIJ_REMISE:
      System.out.println("|\\drawn|");
      break;
    case CaissaConstants.PARTIJ_WIT_WINT:
      System.out.println("|\\whitewins|");
      break;
    case CaissaConstants.PARTIJ_ZWART_WINT:
      System.out.println("|\\blackwins|");
      break;
    default:
      System.out.println("|");
      break;
    }
  }

  private static void verwerkPartijHeading(PGN partij) throws BestandException {
    System.out.println("");
    System.out.println("\\whitename{" + partij.getWhite() + "}");
    System.out.println("\\blackname{" + partij.getBlack() + "}");
    System.out.println("\\chessevent{"
                    + partij.getTag(CaissaConstants.PGNTAG_EVENT) + "}");
    String  eco = partij.getTag(CaissaConstants.PGNTAG_ECO);
    if (DoosUtils.isNotBlankOrNull(eco)) {
      System.out.println("\\chessopening{"
                      + ecoBundle.getString(eco.substring(0, 3)) + "}");
      System.out.println("\\ECO{" + eco + "}");
    }
    System.out.println("");
    System.out.println("\\makegametitle");
    System.out.println("");

    if (partij.hasTag("Annotator")) {
      System.out.println(resourceBundle.getString("label.annotator")
                      + " \\emph{" + partij.getTag("Annotator") + "}");
      System.out.println("");
    }
  }

  private static void verwerkVariant(char[] variant, int start, int eind)
      throws BestandException {
    if (start == eind) {
      return;
    }

    schrijfVariant(variant, start, eind-start);
  }

  private static void verwerkZetten(char[] zetten, int start, int eind)
      throws BestandException {
    if (start == eind) {
      return;
    }

    schrijfZetten(zetten, start, eind-start);
  }

  private static int zoekEinde(char[] zetten, int van, int tot,
                               char begin, char eind) {
    int aantalbegin = 1;
    int aantaleind  = 0;
    int einde       = 0;

    while (aantalbegin != aantaleind
        && van < tot) {
      if (zetten[van] == begin) {
        aantalbegin++;
      }
      if (zetten[van] == eind) {
        aantaleind++;
        einde = van;
      }
      van++;
    }

    return einde;
  }

  private static int zoekEindeCommentaar(char[]  zetten, int van, int tot) {
    return zoekEinde(zetten, van, tot, '{', '}');
  }

  private static int zoekEindeVariant(char[]  zetten, int van, int tot) {
    return zoekEinde(zetten, van, tot, '(', ')');
  }

  private static int zoekStart(char[] zetten, char teken, int van, int tot) {
    int start = -1;

    while (start == -1
        && van < tot) {
      if (zetten[van] == teken) {
        start = van;
      }
      van++;
    }

    return start;
  }

  private static int zoekStartCommentaar(char[] zetten, int van, int tot) {
    return zoekStart(zetten, '{', van, tot);
  }

  private static int zoekStartVariant(char[] zetten, int van, int tot) {
    return zoekStart(zetten, '(', van, tot);
  }
//
//  private static void zoekEind(Map<String, Integer> parameter, char[] data)
//      throws PgnException {
//    int commentaar  = parameter.get(PARAM_COMMENTAAR);
//    int eind        = parameter.get(PARAM_EIND);
//    int i           = parameter.get(PARAM_BEGIN);
//    int variant     = parameter.get(PARAM_VARIANT);
//    while (commentaar > 0 || variant > 0) {
//      i++;
//      if (i > eind) {
//        throw new PgnException(PGN.ERR_BESTAND_EXCEPTION);
//      }
//      switch (data[i]) {
//      case '{':
//        commentaar++;
//        break;
//      case '(':
//        variant++;
//        break;
//      case '}':
//        commentaar--;
//        break;
//      case ')':
//        variant--;
//        break;
//      default:
//        break;
//      }
//    }
//    parameter.put(PARAM_COMMENTAAR, 0);
//    parameter.put(PARAM_EIND, i);
//    parameter.put(PARAM_VARIANT, 0);
//  }
}
