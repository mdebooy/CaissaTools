/**
 * Copyright 2010 Marco de Booij
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the Licence. You may
 * obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
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
import java.text.DecimalFormat;
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
import java.util.TreeMap;


/**
 * @author Marco de Booij
 */
public final class SpelerStatistiek extends Batchjob {
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());

  private static final  String    KYW_LOGO        = "L";
  private static final  String    KYW_STATISTIEK  = "P";
  private static final  String    KYW_PERIODE     = "Q";
  private static final  String    LATEX_HLINE     = "\\hline";
  private static final  String    LATEX_NEWLINE   = " \\\\";
  private static final  String    NORMAAL         = "N";
  private static final  String    SPACES6         = "      ";
  private static final  String[]  UITSLAGEN       =
      new String[] {"1-0", "1/2-1/2", "0-1"};

  private static  String              einddatum   = "0000.00.00";
  private static  Map<String, int[]>  items;
  private static  Map<String, String> params;
  private static  String              startdatum  = "9999.99.99";
  private static  int                 verwerkt    = 0;

  protected SpelerStatistiek() {}

  protected static String datumInTitel(String startdatum, String einddatum) {
    var   titelDatum  = new StringBuilder();
    Date  datum;
    try {
      datum = Datum.toDate(startdatum, PGN.PGN_DATUM_FORMAAT);
      titelDatum.append(Datum.fromDate(datum));
    } catch (ParseException e) {
      DoosUtils.foutNaarScherm(resourceBundle.getString("label.startdatum")
                               + " " + e.getLocalizedMessage() + " ["
                               + startdatum + "]");
    }

    if (!startdatum.equals(einddatum)) {
      try {
        datum = Datum.toDate(einddatum, PGN.PGN_DATUM_FORMAAT);
        titelDatum.append(" - ").append(Datum.fromDate(datum));
      } catch (ParseException e) {
        DoosUtils.foutNaarScherm(resourceBundle.getString("label.einddatum")
                                 + " " + e.getLocalizedMessage() + " ["
                                 + einddatum + "]");
      }
    }

    return titelDatum.toString();
  }

  public static void execute(String[] args) {
    setParameterBundle(
        new ParameterBundle.Builder()
                           .setArgs(args)
                           .setBanner(new MarcoBanner())
                           .setBaseName(CaissaTools.TOOL_SPELERSTATISTIEK)
                           .build());

    if (!paramBundle.isValid()) {
      return;
    }

    items             = new TreeMap<>( );
    var speler        = paramBundle.getString(CaissaTools.PAR_SPELER);
    var statistiektag = paramBundle.getString(CaissaTools.PAR_TAG);

    Collection<PGN> partijen;
    try {
      partijen =
          CaissaUtils.laadPgnBestand(paramBundle
                                        .getBestand(CaissaTools.PAR_BESTAND));
    } catch (PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    partijen.forEach(partij -> verwerkPartij(partij, speler, statistiektag));

    vulParams();

    List<String>  template  = new ArrayList<>();
    var           beginBody = -1;
    var           eindeBody = -1;
    try (var texInvoer = CaissaTools.getTemplate(paramBundle,
                                                 "SpelerStatistiek.tex")) {
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
//
//      if (!paramBundle.containsArgument(CaissaTools.PAR_TAG)) {
//        Collections.sort(items, String.CASE_INSENSITIVE_ORDER);
//      }

      schrijfLatex(template, beginBody, eindeBody);
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }


    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.bestand"),
                             paramBundle
                                .getBestand(CaissaTools.PAR_BESTAND,
                                            BestandConstants.EXT_TEX)));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.partijen"),
                             partijen.size()));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.verwerkt"),
                             verwerkt));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  protected static int[] getStatistiek(String sleutel) {
    if (items.containsKey(sleutel)) {
      return items.get(sleutel);
    }

    return new int[] {0,0,0,0,0,0};
  }

  private static String schrijf(String regel, String status,
                                TekstBestand output,
                                Map<String, String> parameters)
      throws BestandException {
    var start = regel.split(" ")[0];

    switch(start) {
      case "%@Include":
        if ("statistiek".equalsIgnoreCase(regel.split(" ")[1])) {
          schrijfStatistieken(output);
        }
        break;
      case "%@IncludeStart":
        status  = setStatus(regel.split(" ")[1].toLowerCase());
        break;
      case "%@IncludeEind":
        status  = NORMAAL;
        break;
      case "%@I18N":
        output.write("% " + resourceBundle.getString(regel.split(" ")[1]
                                                          .toLowerCase()));
        break;
      default:
        switch (status) {
          case KYW_LOGO:
            CaissaTools.schrijfParameter(CaissaTools.PAR_LOGO, regel,
                                         parameters, output);
            break;
          case KYW_STATISTIEK:
            output.write(CaissaTools.replaceParameters(regel, parameters));
            break;
          case KYW_PERIODE:
            CaissaTools.schrijfParameter("Periode", regel, parameters, output);
            break;
          default:
            output.write(CaissaTools.replaceParameters(regel, parameters));
            break;
          }
        break;
    }

    return status;
  }

  private static void schrijfLatex(List<String> template, int beginBody,
                                   int eindeBody) {
    TekstBestand  output  = null;
    try {
      output  =
          new TekstBestand.Builder()
                          .setBestand(paramBundle
                                          .getBestand(CaissaTools.PAR_BESTAND,
                                                      BestandConstants.EXT_TEX))
                          .setLezen(false).build();

      var status  = NORMAAL;
      for (var j = 0; j < beginBody; j++) {
        status  = schrijf(template.get(j), status, output, params);
      }
      for (var j = beginBody; j < eindeBody; j++) {
        status  = schrijf(template.get(j), status, output, params);
      }
      for (var j = eindeBody + 1; j < template.size(); j++) {
        status  = schrijf(template.get(j), status, output, params);
      }

    } catch (BestandException e) {
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

  private static void schrijfStatistiek(String sleutel, int[] statistiek,
                                        TekstBestand output)
      throws BestandException {
    var lijn  = new StringBuilder();
    lijn.append(swapNaam(sleutel));
    // Als witspeler
    lijn  = new StringBuilder(schrijfStatistiekDeel(statistiek[0],
                                                    statistiek[1],
                                                    statistiek[2],
                                                    lijn.toString()));
    // Als zwartspeler
    lijn  = new StringBuilder(schrijfStatistiekDeel(statistiek[3],
                                                    statistiek[4],
                                                    statistiek[5],
                                                    lijn.toString()));
    // Totaal
    lijn  =
        new StringBuilder(schrijfStatistiekDeel(statistiek[0] + statistiek[3],
                                                statistiek[1] + statistiek[4],
                                                statistiek[2] + statistiek[5],
                                                lijn.toString()));
    lijn.append(LATEX_NEWLINE);
    output.write(lijn.toString());
    output.write(SPACES6 + LATEX_HLINE);
  }

  private static String schrijfStatistiekDeel(int winst, int remise,
                                              int verlies, String prefix)
      throws BestandException {
    var format    = new DecimalFormat("0.00");
    Double punten = Double.valueOf(winst) + Double.valueOf(remise) / 2;
    var gespeeld  = winst + remise + verlies;
    var lijn      = new StringBuilder(prefix);

    if (gespeeld == 0) {
      lijn.append(" & & & & &");
    } else {
      lijn.append(" & ").append(winst)
          .append(" & ").append(remise)
          .append(" & ").append(verlies).append(" & ");
      if (punten != 0.5) {
        lijn.append(punten.intValue());
      }
      lijn.append(Utilities.kwart(punten)).append(" & ");
      lijn.append(format.format((punten / gespeeld) * 100)).append("\\%");
    }

    return lijn.toString();
  }

  private static void schrijfStatistieken(TekstBestand output)
      throws BestandException {
    var totaal  = new int[] {0,0,0,0,0,0};
    for (Entry<String, int[]> item : items.entrySet()) {
      var statistiek  = item.getValue();
      for (var i = 0; i < 6; i++) {
        totaal[i] += statistiek[i];
      }
      schrijfStatistiek(item.getKey(), statistiek, output);
    }

    output.write("      \\rowcolor{headingkleur!10}");
    schrijfStatistiek("Totaal", totaal, output);
  }

  private static String setStatus(String keyword) {
    String  status;
    switch(keyword) {
      case "logo":
        status  = KYW_LOGO;
        break;
      case "statistiek":
        status = KYW_STATISTIEK;
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

  private static String swapNaam(String naam) {
    String[]  deel  = naam.split(",");
    if (deel.length == 1) {
      return naam;
    }

    return deel[1].trim() + " " + deel[0].trim();
  }

  private static void verwerkPartij(PGN partij, String speler,
                                    String statistiektag) {
    if (!partij.isBeeindigd()
        || !partij.isRated()) {
      return;
    }

    var     wit       = partij.getTag(PGN.PGNTAG_WHITE);
    var     zwart     = partij.getTag(PGN.PGNTAG_BLACK);

    if (!speler.equals(wit)
        && !speler.equals(zwart)) {
      return;
    }

    String  sleutel;
    var     uitslag   = partij.getTag(PGN.PGNTAG_RESULT);
    var     i         = 0;
    for (var s: UITSLAGEN) {
      if (s.equals(uitslag)) {
        break;
      }
      i++;
    }

    verwerkt++;
    // Verwerk de 'datums'
    var hulpdatum   = partij.getTag(PGN.PGNTAG_EVENTDATE);
    if (DoosUtils.isNotBlankOrNull(hulpdatum)
        && hulpdatum.indexOf('?') < 0) {
      if (hulpdatum.compareTo(startdatum) < 0 ) {
        startdatum  = hulpdatum;
      }
      if (hulpdatum.compareTo(einddatum) > 0 ) {
        einddatum   = hulpdatum;
      }
    }
    hulpdatum       = partij.getTag(PGN.PGNTAG_DATE);
    if (DoosUtils.isNotBlankOrNull(hulpdatum)
        && hulpdatum.indexOf('?') < 0) {
      if (hulpdatum.compareTo(startdatum) < 0 ) {
        startdatum  = hulpdatum;
      }
      if (hulpdatum.compareTo(einddatum) > 0 ) {
        einddatum   = hulpdatum;
      }
    }

    if (DoosUtils.isNotBlankOrNull(statistiektag)) {
      if (PGN.PGNTAG_DATE.equalsIgnoreCase(statistiektag)) {
        var punt  = hulpdatum.indexOf('.');
        if (punt < 1) {
          sleutel = "????";
        } else {
          sleutel = hulpdatum.substring(0, punt);
        }
      } else {
        sleutel = DoosUtils.nullToEmpty(partij.getTag(statistiektag));
      }
    } else {
      if (speler.equals(wit)) {
        sleutel = zwart;
      } else {
        sleutel = wit;
      }
    }

    int[] statistiek  = getStatistiek(sleutel);
    if (speler.equals(wit)) {
      statistiek[i]++;
    } else {
      statistiek[5 - i]++;
    }

    items.put(sleutel, statistiek);
  }

  // Zet de te vervangen waardes.
  private static void vulParams() {
    params  = new HashMap<>();
    params.put("Speler",
               swapNaam(paramBundle.getString(CaissaTools.PAR_SPELER)));
    params.put("Kleur",
               swapNaam(paramBundle.getString(CaissaTools.PAR_KLEUR)));
    if (paramBundle.containsArgument(CaissaTools.PAR_LOGO)) {
      params.put(CaissaTools.PAR_LOGO,
                 paramBundle.getString(CaissaTools.PAR_LOGO));
    }
    if (!CaissaConstants.DEF_STARTDATUM.equals(startdatum)) {
      params.put("Periode", datumInTitel(startdatum, einddatum));
    }
    params.put("Tekstkleur",
               swapNaam(paramBundle.getString(CaissaTools.PAR_TEKSTKLEUR)));
    params.put("Titel", resourceBundle.getString("label.statistiekenvan"));
  }
}
