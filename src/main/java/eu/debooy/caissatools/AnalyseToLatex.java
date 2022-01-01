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
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import static eu.debooy.doosutils.Batchjob.help;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.ParameterBundle;
import eu.debooy.doosutils.access.BestandConstants;
import eu.debooy.doosutils.access.CsvBestand;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
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
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());
  private static final  ResourceBundle  ecoBundle       =
      ResourceBundle.getBundle("eco", Locale.getDefault());

  private static final  Map<String, String> notaties  = new HashMap<>();
  private static        TekstBestand        texInvoer;
  private static        TekstBestand        uitvoer;

  AnalyseToLatex(){}

  public static void execute(String[] args) {
    var                 charsetIn     = Charset.defaultCharset().name();
    var                 charsetUit    = BestandConstants.UTF8;
    CsvBestand          schaaknotatie = null;

    setParameterBundle(new ParameterBundle.Builder()
                           .setBaseName(CaissaTools.TOOL_ANALYSETEX)
                           .build());

    Banner.printMarcoBanner(DoosUtils.nullToEmpty(paramBundle.getBanner()));

    if (!paramBundle.isValid()
        || !paramBundle.setArgs(args)) {
      help();
      printFouten();
      return;
    }

    try {
      if (paramBundle.containsArgument(CaissaTools.PAR_TEMPLATE)) {
        texInvoer =
            new TekstBestand.Builder()
                            .setBestand(
                                paramBundle
                                    .getBestand(CaissaTools.PAR_TEMPLATE))
                            .setCharset(charsetIn).build();
      } else {
        texInvoer =
            new TekstBestand.Builder()
                            .setClassLoader(AnalyseToLatex.class
                                                          .getClassLoader())
                            .setBestand(PAR_ANALYZEBEGIN).build();
      }

      uitvoer =
          new TekstBestand.Builder()
                          .setBestand(
                              paramBundle.getBestand(CaissaTools.PAR_BESTAND,
                                                     BestandConstants.EXT_TEX))
                          .setCharset(charsetUit)
                          .setLezen(false).build();
      schrijfBegin(
          DoosUtils.nullToEmpty(paramBundle.getString(CaissaTools.PAR_AUTEUR)),
          DoosUtils.nullToEmpty(paramBundle.getString(CaissaTools.PAR_TITEL)));

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
                          paramBundle.getBestand(CaissaTools.PAR_BESTAND),
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
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
      try {
        if (null != texInvoer) {
          texInvoer.close();
        }
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
      try {
        if (null != uitvoer) {
          uitvoer.close();
        }
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }
  }

  private static String getTexmatezetten(char[] data, int start, int lengte) {
    var texmatezetten =
      new StringBuilder(new String(data, start, lengte));

    notaties.forEach((k,v) -> replaceAll(texmatezetten, k, v));

    return texmatezetten.toString()
                        .replaceAll("^[1-9][0-9]*\\.", "")
                        .replace("\\.\\.", "")
                        .replaceAll(" [1-9][0-9]*\\.", " ")
                        .replace("  ", " ").trim();
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
      uitvoer.write(regel.replace("@Auteur@", auteur)
                         .replace("@Titel@",  titel));
    }
  }

  private static void schrijfCommentaar(char[] commentaar,
                                        int start, int lengte)
      throws BestandException {
    uitvoer.write("\\par\\justify "
                    + new String(commentaar, start, lengte));
  }

  private static void schrijfEinde()
      throws BestandException {
    uitvoer.write("\\end{document}");
  }

  private static void schrijfVariant(char[] variant, int start, int lengte)
      throws BestandException {
  }

  private static void schrijfZetten(char[] zetten,
                                    int start, int lengte)
      throws BestandException {
    uitvoer.write("\\par\\centering|"
                    + getTexmatezetten(zetten, start, lengte) + "|");
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
      uitvoer.write("|\\drawn|");
      break;
    case CaissaConstants.PARTIJ_WIT_WINT:
      uitvoer.write("|\\whitewins|");
      break;
    case CaissaConstants.PARTIJ_ZWART_WINT:
      uitvoer.write("|\\blackwins|");
      break;
    default:
      uitvoer.write("|");
      break;
    }
  }

  private static void verwerkPartijHeading(PGN partij) throws BestandException {
    uitvoer.write("");
    uitvoer.write("\\whitename{" + partij.getWhite() + "}");
    uitvoer.write("\\blackname{" + partij.getBlack() + "}");
    uitvoer.write("\\chessevent{"
                    + partij.getTag(CaissaConstants.PGNTAG_EVENT) + "}");
    String  eco = partij.getTag(CaissaConstants.PGNTAG_ECO);
    if (DoosUtils.isNotBlankOrNull(eco)) {
      uitvoer.write("\\chessopening{"
                      + ecoBundle.getString(eco.substring(0, 3)) + "}");
      uitvoer.write("\\ECO{" + eco + "}");
    }
    uitvoer.write("");
    uitvoer.write("\\makegametitle");
    uitvoer.write("");

    if (partij.hasTag("Annotator")) {
      uitvoer.write(resourceBundle.getString("label.annotator")
                      + " \\emph{" + partij.getTag("Annotator") + "}");
      uitvoer.write("");
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
    var aantalbegin = 1;
    var aantaleind  = 0;
    var einde       = 0;

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
