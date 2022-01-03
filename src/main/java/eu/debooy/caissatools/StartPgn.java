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
import eu.debooy.caissa.Spelerinfo;
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
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


/**
 * @author Marco de Booij
 */
public final class StartPgn extends Batchjob {
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());

  private static  String[]          rondes;
  private static  List<String>      speeldata;
  private static  List<Spelerinfo>  spelers;
  private static  String            eventDate;
  private static  boolean           enkel;
  private static  String            event;
  private static  int               noSpelers;
  private static  TekstBestand      output;
  private static  String            site;

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

    eventDate = schema.get(CaissaConstants.PGNTAG_EVENTDATE).toString();
    enkel     = true;
    if (schema.containsKey(CaissaTools.PAR_ENKELRONDIG)) {
      enkel   = (boolean) schema.get(CaissaTools.PAR_ENKELRONDIG);
    }
    event     = schema.get(CaissaConstants.PGNTAG_EVENT).toString();
    site      = schema.get(CaissaConstants.PGNTAG_SITE).toString();
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
    if (speeldata.size() != rondes.length * (enkel ? 1 : 2)) {
      DoosUtils.foutNaarScherm(
          MessageFormat.format(
              resourceBundle.getString(CaissaTools.ERR_KALENDER),
              speeldata.size(), rondes.length * (enkel ? 1 : 2)));
      return;
    }

    try {
      output  =
          new TekstBestand.Builder()
                          .setBestand(paramBundle
                                          .getBestand(CaissaTools.PAR_BESTAND))
                          .setLezen(false).build();
    schrijfToernooi();
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

  private static void schrijfToernooi() throws BestandException {
    verwerkRondes(0, 0, 1);

    if (!enkel) {
      verwerkRondes(rondes.length, 1, 0);
    }
  }

  private static void schrijfPartij(String date, String ronde,
                                    int wit, int zwart)
      throws BestandException {
      if (wit >= noSpelers
          || zwart >= noSpelers) {
        return;
      }

    var witspeler   = spelers.get(wit).getNaam();
    var zwartspeler = spelers.get(zwart).getNaam();

    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_EVENT, event));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_SITE, site));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_DATE, date));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_ROUND, ronde));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_WHITE, witspeler));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_BLACK,
                                      zwartspeler));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_RESULT,
                                      CaissaConstants.PARTIJ_BEZIG));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_EVENTDATE,
                                      eventDate));
    output.write("");
    output.write(CaissaConstants.PARTIJ_BEZIG);
    output.write("");
  }

  private static void verwerkRondes(Integer round, int wit, int zwart)
      throws BestandException {
    for (var ronde : rondes) {
      round++;
      for (var partij : ronde.split(" ")) {
        var paring  = partij.split("-");
        schrijfPartij(speeldata.get(round - 1), round.toString(),
                      Integer.valueOf(paring[wit]) - 1,
                      Integer.valueOf(paring[zwart]) - 1);
      }
    }
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
