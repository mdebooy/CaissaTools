/**
 * Copyright 2008 Marco de Booij
 *
 * Licensed under the EUPL, Version 1.0 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the Licence. You may
 * obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/7330l5
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
import eu.debooy.caissa.Competitie;
import eu.debooy.caissa.FEN;
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.exceptions.CompetitieException;
import eu.debooy.caissa.exceptions.FenException;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.MarcoBanner;
import eu.debooy.doosutils.ParameterBundle;
import eu.debooy.doosutils.access.BestandConstants;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.latex.Utilities;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.TreeSet;


/**
 * Versie 526 is de laatste goede.
 * @author Marco de Booij
 */
public final class PgnToLatex extends Batchjob {
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());

  private static  String            auteur;
  private static  Competitie        competitie;
  private static  String            eindDatum;
  private static  double[][]        matrix;
  private static  boolean           metMatrix;
  private static  TekstBestand      output;
  private static  Collection<PGN>   partijen;
  private static  String            startDatum;
  private static  String            titel;

  private static final String HLINE         = "\\hline";
  private static final String KEYWORDS      = "K";
  private static final String KYW_LOGO      = "L";
  private static final String KYW_MATRIX    = "M";
  private static final String KYW_PARTIJEN  = "P";
  private static final String KYW_PERIODE   = "Q";
  private static final String NORMAAL       = "N";

  PgnToLatex() {}

  private static void bepaalMinMaxDatum(String datum) {
    if (DoosUtils.isNotBlankOrNull(datum)
        && datum.indexOf('?') < 0) {
      if (datum.compareTo(startDatum) < 0 ) {
        startDatum  = datum;
      }
      if (datum.compareTo(eindDatum) > 0 ) {
        eindDatum   = datum;
      }
    }
  }

  protected static String datumInTitel(String startDatum, String eindDatum) {
    var   titelDatum  = new StringBuilder();
    Date  datum;
    try {
      datum = Datum.toDate(startDatum, PGN.PGN_DATUM_FORMAAT);
      titelDatum.append(Datum.fromDate(datum,
                                       CaissaConstants.DEF_DATUMFORMAAT));
    } catch (ParseException e) {
      DoosUtils.foutNaarScherm(resourceBundle.getString("label.startdatum")
                               + " " + e.getLocalizedMessage() + " ["
                               + startDatum + "]");
    }

    if (!startDatum.equals(eindDatum)) {
      try {
        datum = Datum.toDate(eindDatum, PGN.PGN_DATUM_FORMAAT);
        titelDatum.append(" - ")
                  .append(Datum.fromDate(datum,
                                         CaissaConstants.DEF_DATUMFORMAAT));
      } catch (ParseException e) {
        DoosUtils.foutNaarScherm(resourceBundle.getString("label.einddatum")
                                 + " " + e.getLocalizedMessage() + " ["
                                 + eindDatum + "]");
      }
    }

    return titelDatum.toString();
  }

  public static void execute(String[] args) {
    setParameterBundle(
        new ParameterBundle.Builder()
                           .setArgs(args)
                           .setBanner(new MarcoBanner())
                           .setBaseName(CaissaTools.TOOL_PGNTOLATEX)
                           .setClassloader(PgnToLatex.class.getClassLoader())
                           .setValidator(new PgnToLatexParameters())
                           .build());

    if (!paramBundle.isValid()) {
      return;
    }

    var           aantalPartijen  = 0;
    List<String>  template        = new ArrayList<>();

    eindDatum   = CaissaConstants.DEF_STARTDATUM;
    output      = null;
    startDatum  = CaissaConstants.DEF_EINDDATUM;

    var bestand   =
        paramBundle.getString(CaissaTools.PAR_BESTAND)
                   .replace(BestandConstants.EXT_PGN, "").split(";");
    var schema    =
        paramBundle.getString(CaissaTools.PAR_SCHEMA)
                   .replace(BestandConstants.EXT_PGN, "")
                   .replace(BestandConstants.EXT_JSON, "").split(";");

    auteur        = paramBundle.getString(CaissaTools.PAR_AUTEUR);
    metMatrix     = paramBundle.getBoolean(CaissaTools.PAR_MATRIX);
    titel         = paramBundle.getString(CaissaTools.PAR_TITEL);

    var beginBody = -1;
    var eindeBody = -1;
    try (var texInvoer = getTemplate()) {
      String  regel;
      while (texInvoer.hasNext()) {
        regel = texInvoer.next();
        if (regel.startsWith("%@IncludeStart Body")) {
          beginBody = template.size();
        }
        if (regel.startsWith("%@IncludeEind Body")) {
          eindeBody = template.size();
        }
        template.add(regel);
      }

      output  =
          new TekstBestand.Builder()
                          .setBestand(getUitvoerbestand(bestand[0],
                                      BestandConstants.EXT_TEX))
                          .setLezen(false).build();
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }

    for (var i = 0; i < bestand.length; i++) {
      partijen  = new TreeSet<>(new PGN.ByEventComparator());
      Map<String, String> texPartij = new HashMap<>();

      try {
        competitie    = new Competitie(getInvoerbestand(schema[i],
                                       BestandConstants.EXT_JSON));
        partijen.addAll(
            CaissaUtils.laadPgnBestand(getInvoerbestand(bestand[i],
                                       BestandConstants.EXT_PGN)));
      } catch (CompetitieException | PgnException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
        return;
      }

      partijen.forEach(PgnToLatex::verwerkPartij);

      try {
        // Maak de Matrix
        if (Boolean.TRUE.equals(metMatrix)) {
          competitie.sorteerOpNaam();
          // Bepaal de score en weerstandspunten.
          var opStand   = paramBundle.getBoolean(CaissaTools.PAR_MATRIXOPSTAND);
          matrix  = CaissaUtils.vulToernooiMatrix(partijen, competitie,
                                                  opStand);
          if (Boolean.TRUE
                     .equals(paramBundle.getBoolean(CaissaTools.PAR_AKTIEF))) {
            matrix    =
                CaissaUtils.verwijderNietActief(matrix, competitie);
          }
        }

        // Zet de te vervangen waardes.
        Map<String, String> params  = new HashMap<>();
        params.put("Auteur", auteur);
        params.put("Datum",
                   Datum.fromDate(paramBundle.getDate(CaissaTools.PAR_DATUM),
                                  PGN.PGN_DATUM_FORMAAT));
        if (paramBundle.containsArgument(CaissaTools.PAR_KEYWORDS)) {
          params.put(CaissaTools.PAR_KEYWORDS,
                     paramBundle.getString(CaissaTools.PAR_KEYWORDS));
        }
        if (paramBundle.containsArgument(CaissaTools.PAR_LOGO)) {
          params.put(CaissaTools.PAR_LOGO,
                     paramBundle.getString(CaissaTools.PAR_LOGO));
        }
        if (bestand.length == 1) {
          params.put("Periode", datumInTitel(startDatum, eindDatum));
        }
        params.put("Titel", titel);

        var status  = NORMAAL;
        if (i == 0) {
          for (var j = 0; j < beginBody; j++) {
            status  = schrijf(template.get(j), status, competitie, texPartij,
                              params);
          }
        }
        for (var j = beginBody; j < eindeBody; j++) {
          status  = schrijf(template.get(j), status, competitie, texPartij,
                              params);
        }
        if (i == bestand.length - 1) {
          for (var j = eindeBody + 1; j < template.size(); j++) {
            status  = schrijf(template.get(j), status, competitie, texPartij,
                              params);
          }
        }
        aantalPartijen  += partijen.size();
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }

    try {
      if (output != null) {
        output.close();
      }
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }

    for (var tex : bestand) {
      DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString(CaissaTools.LBL_BESTAND),
                             getUitvoerbestand(tex, BestandConstants.EXT_TEX)));
    }
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString(CaissaTools.LBL_PARTIJEN),
                             aantalPartijen));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  private static String getInvoerbestand(String bestand, String extensie) {
    if (bestand.contains(DoosUtils.getFileSep())) {
      return bestand + extensie;
    }
    if (paramBundle.containsArgument(PAR_INVOERDIR)) {
      return paramBundle.getString(PAR_INVOERDIR) + DoosUtils.getFileSep()
              + bestand + extensie;
    }

    return bestand + extensie;
  }

  private static String getPunten(int pntn, String decim) {
    return ((pntn == 0
              && "".equals(decim))
              || pntn >= 1 ? String.valueOf(pntn) : "");
  }

  private static TekstBestand getTemplate()
      throws BestandException {
    TekstBestand  texInvoer;
      if (paramBundle.containsArgument(CaissaTools.PAR_TEMPLATE)) {
        texInvoer =
            new TekstBestand.Builder()
                            .setBestand(
                                paramBundle.getBestand(
                                    CaissaTools.PAR_TEMPLATE))
                            .build();
      } else {
        texInvoer =
            new TekstBestand.Builder()
                            .setBestand("Caissa.tex")
                            .setClassLoader(PgnToLatex.class.getClassLoader())
                            .build();
      }

      return texInvoer;
  }

  private static String getUitvoerbestand(String bestand, String extensie) {
    if (bestand.contains(DoosUtils.getFileSep())) {
      return bestand + extensie;
    }
    if (paramBundle.containsArgument(PAR_INVOERDIR)
        || paramBundle.containsArgument(PAR_UITVOERDIR)) {
      return paramBundle.getString(PAR_UITVOERDIR) + DoosUtils.getFileSep()
              + bestand + extensie;
    }

    return bestand + extensie;
  }

  private static void maakMatrix(int kolommen, int noSpelers, boolean metBye)
      throws BestandException {
    if (Boolean.FALSE.equals(metMatrix)) {
      return;
    }

    maakMatrixHeading(kolommen, noSpelers, metBye);

    var lijn  = new StringBuilder();
    output.write("    \\cline{3-" + (2 + kolommen) + "}");
    if (competitie.isDubbel()) {
      lijn.append("    \\multicolumn{2}{|c|}{} & ");
      for (var i = 0; i < noSpelers; i++) {
        lijn.append(resourceBundle.getString("tag.wit")).append(" & ")
            .append(resourceBundle.getString("tag.zwart")).append(" & ");
      }
      if (metBye) {
        lijn.append("& ");
      }
      lijn.append("& & \\\\");
      output.write(lijn.toString());
      lijn  = new StringBuilder();
    }
    output.write("    " + HLINE);
    for (var i = 0; i < noSpelers; i++) {
      var speler  = competitie.getDeelnemers().get(i);
      if (competitie.isMatch()) {
        lijn.append("\\multicolumn{2}{|l|}{").append(speler.getNaam())
            .append("} & ");
      } else {
        lijn.append((i + 1)).append(" & ").append(speler.getNaam())
            .append(" & ");
      }
      lijn.append(schrijfResultaten(i, kolommen));
      if (metBye) {
        lijn.append(speler.getByeScore().intValue()).append(" & ");
      }
      var pntn  = speler.getPunten().intValue();
      var decim = Utilities.kwart(speler.getPunten());
      lijn.append(getPunten(pntn, decim)).append(decim);
      if (!competitie.isMatch()) {
        var wpntn   = speler.getTieBreakScore().intValue();
        var wdecim  = Utilities.kwart(speler.getTieBreakScore());
        lijn.append(" & ").append(speler.getPartijen()).append(" & ")
            .append(getPunten(wpntn, wdecim)).append(wdecim);
      }
      lijn.append(" \\\\");
      output.write(lijn.toString());
      lijn  = new StringBuilder();
      output.write("    " + HLINE);
    }
    output.write("    \\end{tabular}}");
  }

  private static void maakMatrixHeading(int kolommen, int noSpelers,
                                        boolean metBye)
      throws BestandException {
    var lijn  = new StringBuilder();
    int cols  = competitie.isMatch() ? kolommen : noSpelers;

    lijn.append("    \\resizebox{\\columnwidth}{!}")
        .append("{\\begin{tabular} { | c | l | ");
    for (var i = 0; i < kolommen; i++) {
      lijn.append("c | ");
    }
    if (metBye) {
      lijn.append("c | ");
    }
    lijn.append("r | r | r | }");
    output.write(lijn.toString());
    lijn  = new StringBuilder();
    output.write("    " + HLINE);
    lijn.append("    \\multicolumn{2}{|c|}{} ");
    for (var i = 0; i < cols; i++) {
      if (competitie.isEnkel()) {
        lijn.append(" & ").append((i + 1));
      } else {
        lijn.append(" & \\multicolumn{2}{c|}{").append((i + 1)).append("} ");
      }
    }
    if (metBye) {
      lijn.append("& ").append(resourceBundle.getString("label.bye"));
    }
    lijn.append("& ").append(resourceBundle.getString("tag.punten"));
    if (!competitie.isMatch()) {
      lijn.append(" & ").append(resourceBundle.getString("tag.partijen"))
          .append(" & ").append(resourceBundle.getString("tag.sb"));
    }
    lijn.append(" \\\\");
    output.write(lijn.toString());
  }

  private static String replaceParameters(String regel,
                                          Map<String, String> parameters) {
    var resultaat = regel;
    for (Entry<String, String> parameter : parameters.entrySet()) {
      resultaat =
          resultaat.replace("@"+parameter.getKey()+"@",
                            parameter.getValue());
    }

    return resultaat;
  }

  private static String schrijf(String regel, String status,
                                Competitie competitie,
                                Map<String, String> texPartij,
                                Map<String, String> parameters)
      throws BestandException {
    var kolommen  = null == matrix ? 0 : matrix[0].length;
    var noSpelers = competitie.getDeelnemers().size();
    var metBye    = competitie.metBye();
    var start     = regel.split(" ")[0];

    switch(start) {
      case "%@Include":
        if ("matrix".equalsIgnoreCase(regel.split(" ")[1])
            && null != matrix) {
          maakMatrix(kolommen, noSpelers, metBye);
        }
        break;
      case "%@IncludeStart":
        status  = setStatus(regel.split(" ")[1].toLowerCase());
        break;
      case "%@IncludeEind":
        if ("partij".equalsIgnoreCase(regel.split(" ")[1])) {
          verwerkPartijen(partijen, texPartij, output);
        }
        status  = NORMAAL;
        break;
      case "%@I18N":
        output.write("% " + resourceBundle.getString(regel.split(" ")[1]
                                                          .toLowerCase()));
        break;
      default:
        switch (status) {
          case KEYWORDS:
            schrijfParameter(CaissaTools.PAR_KEYWORDS, regel, parameters);
            break;
          case KYW_LOGO:
            schrijfParameter(CaissaTools.PAR_LOGO, regel, parameters);
            break;
          case KYW_MATRIX:
            if (Boolean.TRUE.equals(metMatrix)) {
              output.write(replaceParameters(regel, parameters));
            }
            break;
          case KYW_PARTIJEN:
            String[]  splits  = regel.substring(1).split("=");
            texPartij.put(splits[0], splits[1]);
            break;
          case KYW_PERIODE:
            schrijfParameter("Periode", regel, parameters);
            break;
          default:
            output.write(replaceParameters(regel, parameters));
            break;
          }
        break;
    }

    return status;
  }

  private static void schrijfParameter(String param, String regel,
                                       Map<String, String> parameters)
      throws BestandException {
    if (parameters.containsKey(param)) {
      output.write(replaceParameters(regel, parameters));
    }
  }

  private static void schrijfResultaat(StringBuilder lijn, int speler,
                                       int resultaat) {
    if (matrix[speler][resultaat] == 0.0) {
      lijn.append("0");
    } else if (matrix[speler][resultaat] == 0.5) {
      lijn.append(Utilities.kwart(0.5));
    } else if (matrix[speler][resultaat] >= 1.0) {
      lijn.append(((Double)matrix[speler][resultaat]).intValue())
          .append(Utilities.kwart(matrix[speler][resultaat]));
    }
  }

  private static String schrijfResultaten(int speler, int kolommen) {
    var lijn  = new StringBuilder();

    for (var j = 0; j < kolommen; j++) {
      if (!competitie.isMatch()) {
        if (speler == j / competitie.getHeenTerug()) {
          lijn.append("\\multicolumn{1}"
                      + "{>{\\columncolor[rgb]{0,0,0}}c|}{} & ");
          continue;
        }
        if ((j / competitie.getHeenTerug())
               * competitie.getHeenTerug() != j ) {
          lijn.append("\\multicolumn{1}"
                      + "{>{\\columncolor[rgb]{0.8,0.8,0.8}}c|}{");
        }
      }

      schrijfResultaat(lijn, speler, j);

      if (!competitie.isMatch()
          && (j / competitie.getHeenTerug())
                * competitie.getHeenTerug() != j ) {
        lijn.append("}");
      }
      lijn.append(" & ");
    }

    return lijn.toString();
  }

  private static String setStatus(String keyword) {
    String  status;
    switch(keyword) {
      case "keywords":
        status  = KEYWORDS;
        break;
      case "logo":
        status  = KYW_LOGO;
        break;
      case "matrix":
        status = KYW_MATRIX;
        break;
      case "partij":
        status  = KYW_PARTIJEN;
        break;
      case "periode":
        status  = KYW_PERIODE;
        break;
      default:
        status  = "";
        break;
    }

    return status;
  }

  private static void verwerkPartij(PGN partij) {
    bepaalMinMaxDatum(partij.getTag(PGN.PGNTAG_EVENTDATE));
    bepaalMinMaxDatum(partij.getTag(PGN.PGNTAG_DATE));

    if (DoosUtils.isBlankOrNull(auteur)) {
      auteur  = partij.getTag(PGN.PGNTAG_SITE);
    }
    if (DoosUtils.isBlankOrNull(titel)) {
      titel   = partij.getTag(PGN.PGNTAG_EVENT);
    }
  }

  private static void verwerkPartijen(Collection<PGN> partijen,
                                      Map<String, String> texPartij,
                                      TekstBestand output) {
    partijen.stream()
            .filter(partij -> !partij.isBye())
            .forEach(partij -> {
      try {
        var fen       = new FEN();
        var regel     = "";
        var resultaat = partij.getTag(PGN.PGNTAG_RESULT)
                .replace("1/2", Utilities.kwart(0.5));
        var zetten    = partij.getZuivereZetten().replace("#", "\\\\#");
        if (partij.hasTag(PGN.PGNTAG_FEN)) {
          fen = new FEN(partij.getTag(PGN.PGNTAG_FEN));
        }
        if (DoosUtils.isNotBlankOrNull(zetten)) {
          if (partij.hasTag(PGN.PGNTAG_FEN)) {
            regel = texPartij.get("fenpartij");
          } else {
            if (partij.getZetten().isEmpty()) {
              regel = texPartij.get("legepartij");
            } else {
              regel = texPartij.get("schaakpartij");
            }
          }
        } else {
          // Partij zonder zetten.
          if (!resultaat.equals("*")) {
            regel = texPartij.get("legepartij");
          }
        }
        int i = regel.indexOf('@');
        while (i >= 0) {
          int j = regel.indexOf('@', i+1);
          if (j > i) {
            var tag = regel.substring(i+1, j);
            if (partij.hasTag(tag)) {
              switch (tag) {
                case PGN.PGNTAG_RESULT:
                  regel = regel.replace("@" + tag + "@",
                          partij.getTag(tag)
                                .replace("1/2", Utilities.kwart(0.5)));
                  break;
                case PGN.PGNTAG_ECO:
                  var extra = "";
                  if (!partij.isRanked()) {
                    extra =
                        " "
                          + resourceBundle.getString("tekst.buitencompetitie");
                  }
                  regel = regel.replace("@" + tag + "@",
                          partij.getTag(tag) + extra);
                  break;
                default:
                  regel = regel.replace("@" + tag + "@", partij.getTag(tag));
                  break;
              }
            } else {
              switch (tag) {
                case PGN.PGNTAG_ECO:
                  var extra = "";
                  if (!partij.isRanked()) {
                    extra =
                        " "
                          + resourceBundle.getString("tekst.buitencompetitie");
                  }
                  regel = regel.replace("@" + tag + "@", extra);
                  break;
                case "_EnkelZetten":
                  regel = regel.replace("@_EnkelZetten@",
                          partij.getZuivereZetten()
                                  .replace("#", "\\mate"));
                  break;
                case "_Start":
                  regel = regel.replace("@_Start@",
                          fen.getAanZet()+" "+ fen.getZetnummer());
                  break;
                case "_Stelling":
                  regel = regel.replace("@_Stelling@", fen.getPositie());
                  break;
                case "_Zetten":
                  regel = regel.replace("@_Zetten@",
                          partij.getZetten()
                                  .replace("#", "\\mate"));
                  break;
                default:
                  regel = regel.replace("@" + tag + "@", tag);
                  break;
              }
            }
            j = i;
          }
          i = regel.indexOf('@', j+1);
        }
        output.write(regel);
      } catch (BestandException | FenException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    });
  }
}
