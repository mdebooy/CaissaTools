/**
 * Copyright 2009 Marco de Booij
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
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.ParameterBundle;
import eu.debooy.doosutils.access.JsonBestand;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/**
 * @author Marco de Booij
 *
 * Genereer een PGN met behulp van een JSON. Vul de eventuele niet
 *  'Seven Tag Roster' tags en zetten aan vanuit de 'extra' PGN.
 * Schrijf de verkregen PGN weg.
 */
public final class StartPgn extends Batchjob {
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());

  private static  String[]          rondes;
  private static  List<String>      speeldata;
  private static  List<Spelerinfo>  spelers;
  private static  String            eventDate;
  private static  String            event;
  private static  int               noSpelers;
  private static  TekstBestand      output;
  private static  String            site;
  private static  int               toernooitype;

  protected StartPgn() {}

  public static void execute(String[] args) {
    setParameterBundle(new ParameterBundle.Builder()
                           .setBaseName(CaissaTools.TOOL_STARTPGN)
                           .setValidator(new BestandDefaultParameters())
                           .build());

    Banner.printMarcoBanner(DoosUtils.nullToEmpty(paramBundle.getBanner()));

    if (!paramBundle.isValid()
        || !paramBundle.setArgs(args)) {
      help();
      printFouten();
      return;
    }

    JsonBestand schema;
    try {
      schema  =
          new JsonBestand.Builder()
                         .setBestand(paramBundle
                                        .getBestand(CaissaTools.PAR_SCHEMA))
                         .build();
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    eventDate = schema.get(CaissaConstants.JSON_TAG_EVENTDATE).toString();
    if (schema.containsKey(CaissaConstants.JSON_TAG_TOERNOOITYPE)) {
      toernooitype  =
          ((Long) schema.get(CaissaConstants.JSON_TAG_TOERNOOITYPE))
              .intValue();
    } else {
      toernooitype  = CaissaConstants.TOERNOOI_MATCH;
    }
    event     = schema.get(CaissaTools.PAR_EVENT).toString();
    site      = schema.get(CaissaTools.PAR_SITE).toString();
    speeldata = new ArrayList<>();
    spelers   = new ArrayList<>();

    try {
      vulSpeeldata(schema.getArray(CaissaConstants.JSON_TAG_KALENDER));
    } catch (ParseException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }
    CaissaUtils.vulSpelers(spelers,
                           schema.getArray(CaissaConstants.JSON_TAG_SPELERS));

    noSpelers = spelers.size();
    rondes    = CaissaUtils.bergertabel(spelers.size());
    if (speeldata.size() != rondes.length * toernooitype) {
      DoosUtils.foutNaarScherm(
          MessageFormat.format(
              resourceBundle.getString(CaissaTools.ERR_KALENDER),
              speeldata.size(), rondes.length * toernooitype));
      return;
    }

    Collection<PGN> extra;
    if (paramBundle.containsArgument(CaissaTools.PAR_EXTRA)) {
      try {
        extra = CaissaUtils.laadPgnBestand(
                    paramBundle.getBestand(CaissaTools.PAR_EXTRA));
      } catch (PgnException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
        return;
      }
    } else {
      extra = new ArrayList<>();
    }

    try {
      output  =
          new TekstBestand.Builder()
                          .setBestand(paramBundle
                                          .getBestand(CaissaTools.PAR_BESTAND))
                          .setLezen(false).build();
    schrijfToernooi(extra);
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        schema.close();
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
      try {
        if (output != null) {
          output.close();
        }
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }

    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString(
                                CaissaTools.LBL_BESTAND),
                             paramBundle.getBestand(CaissaTools.PAR_BESTAND)));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  private static void schrijfPartij(String date, String ronde,
                                    int wit, int zwart, PGN extra)
      throws BestandException, PgnException {
    if (wit >= noSpelers
        || zwart >= noSpelers) {
      return;
    }

    PGN partij;
    if (null == extra) {
      partij  = new PGN();
      partij.setTag(CaissaConstants.PGNTAG_WHITE, spelers.get(wit).getNaam());
      partij.setTag(CaissaConstants.PGNTAG_BLACK, spelers.get(zwart).getNaam());
      partij.setTag(CaissaConstants.PGNTAG_RESULT,
                    CaissaConstants.PARTIJ_BEZIG);
    } else {
      partij  = new PGN(extra);
    }

    partij.setTag(CaissaConstants.PGNTAG_DATE, date);
    partij.setTag(CaissaConstants.PGNTAG_EVENT, event);
    partij.setTag(CaissaConstants.PGNTAG_EVENTDATE, eventDate);
    partij.setTag(CaissaConstants.PGNTAG_ROUND, ronde);
    partij.setTag(CaissaConstants.PGNTAG_SITE, site);

    output.write(partij.toString());
  }

  private static void schrijfToernooi(Collection<PGN> extra)
      throws BestandException {
    verwerkRondes(0, 0, 1, extra);

    if (toernooitype != CaissaConstants.TOERNOOI_ENKEL) {
      verwerkRondes(rondes.length, 1, 0, extra);
    }
  }

  private static void verwerkRondes(Integer round, int wit, int zwart,
                                    Collection<PGN> partijen)
      throws BestandException {
    for (var ronde : rondes) {
      round++;
      for (var partij : ronde.split(" ")) {
        var paring  = partij.split("-");
        var pwit    = Integer.valueOf(paring[wit]) - 1;
        var pzwart  = Integer.valueOf(paring[zwart]) - 1;
        var extra   = vindPartij(pwit, pzwart, partijen);
        try {
          schrijfPartij(speeldata.get(round - 1), round.toString(),
                        pwit, pzwart, extra);
        } catch (PgnException e) {
          DoosUtils.foutNaarScherm(ronde + " " + e.getLocalizedMessage());
        }
      }
    }
  }

  private static PGN vindPartij(int wit, int zwart, Collection<PGN> partijen) {
    return
        partijen.stream()
                .filter(pgn -> pgn.getWhite()
                                  .equals(spelers.get(wit).getNaam()))
                .filter(pgn -> pgn.getBlack()
                                  .equals(spelers.get(zwart).getNaam()))
                .findFirst().orElse(null);
  }

  private static void vulSpeeldata(JSONArray kalender) throws ParseException {
     for (var i = 0; i < kalender.size(); i++) {
      var item  = (JSONObject) kalender.get(i);
      if (item.containsKey(CaissaConstants.JSON_TAG_KALENDER_RONDE)
          && item.containsKey(CaissaConstants.JSON_TAG_KALENDER_DATUM)) {
        speeldata.add(Datum.fromDate(
                Datum.toDate(item.get(CaissaConstants.JSON_TAG_KALENDER_DATUM)
                                 .toString()),
                      CaissaConstants.PGN_DATUM_FORMAAT));
      }
    }
  }
}
