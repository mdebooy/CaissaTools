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
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.JsonBestand;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import java.io.File;
import java.nio.charset.Charset;
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
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

  private static  String[]          rondes;
  private static  List<String>      speeldata;
  private static  List<Spelerinfo>  spelers;
  private static  String            eventDate;
  private static  boolean           enkel;
  private static  String            event;
  private static  int               noSpelers;
  private static  TekstBestand      output;
  private static  String            site;

  private StartPgn() {}

  public static void execute(String[] args) {
    Banner.printMarcoBanner(resourceBundle.getString("banner.startpgn"));

    if (!setParameters(args)) {
      return;
    }

    JsonBestand schema;
    try {
      schema  =
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

    eventDate = schema.get(CaissaConstants.PGNTAG_EVENTDATE).toString();
    enkel     = true;
    if (schema.containsKey(CaissaTools.PAR_ENKELRONDIG)) {
      enkel   = (boolean) schema.get(CaissaTools.PAR_ENKELRONDIG);
    }
    event     = schema.get(CaissaConstants.PGNTAG_EVENT).toString();
    site      = schema.get(CaissaConstants.PGNTAG_SITE).toString();

    try {
      vulSpeeldata(schema.getArray("kalender"));
    } catch (ParseException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }
    vulSpelers(schema.getArray("spelers"));

    noSpelers = spelers.size();
    rondes    = CaissaUtils.bergertabel(spelers.size());
    if (speeldata.size() != rondes.length * (enkel ? 1 : 2)) {
      DoosUtils.foutNaarScherm(
          MessageFormat.format(
              resourceBundle.getString(CaissaTools.ERR_KALENDER),
              speeldata.size(), rondes.length * (enkel ? 1 : 2)));
      return;
    }

    String  uitvoer = parameters.get(PAR_UITVOERDIR)
                      + parameters.get(CaissaTools.PAR_BESTAND) + EXT_PGN;

    try {
      output  = new TekstBestand.Builder()
                                .setBestand(uitvoer)
                                .setCharset(parameters.get(PAR_CHARSETUIT))
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
                             uitvoer));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  public static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar StartPgn "
                         +  getMelding(LBL_OPTIE) + " "
                         + MessageFormat.format(
                                getMelding(LBL_PARAM), CaissaTools.PAR_BESTAND,
                                resourceBundle.getString(
                                    CaissaTools.LBL_PGNBESTAND)) + " "
                         + MessageFormat.format(
                                getMelding(LBL_PARAM), CaissaTools.PAR_SCHEMA,
                                resourceBundle.getString(
                                    CaissaTools.LBL_SCHEMA)));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_BESTAND, 14),
                         resourceBundle.getString(CaissaTools.HLP_BESTAND), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETIN, 14),
        MessageFormat.format(getMelding(HLP_CHARSETIN),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETUIT, 14),
        MessageFormat.format(getMelding(HLP_CHARSETUIT),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_INVOERDIR, 14),
                         getMelding(HLP_INVOERDIR), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_SCHEMA, 14),
                         resourceBundle.getString(CaissaTools.HLP_SCHEMA), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_UITVOERDIR, 14),
                         getMelding(HLP_UITVOERDIR), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(getMelding(HLP_PARAMSVERPLICHT),
                             CaissaTools.PAR_BESTAND, CaissaTools.PAR_SCHEMA),
                             80);
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

    String    witspeler   = spelers.get(wit).getNaam();
    String    zwartspeler = spelers.get(zwart).getNaam();

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

  private static boolean setParameters(String[] args) {
    Arguments     arguments = new Arguments(args);
    List<String>  fouten    = new ArrayList<>();

    arguments.setParameters(new String[] {CaissaTools.PAR_BESTAND,
                                          PAR_CHARSETIN,
                                          PAR_CHARSETUIT,
                                          PAR_INVOERDIR,
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

  private static void verwerkRondes(Integer round, int wit, int zwart)
      throws BestandException {
    for (String ronde : rondes) {
      round++;
      for (String partij : ronde.split(" ")) {
        String[]  paring  = partij.split("-");
        schrijfPartij(speeldata.get(round - 1), round.toString(),
                      Integer.valueOf(paring[wit]) - 1,
                      Integer.valueOf(paring[zwart]) - 1);
      }
    }
  }

  private static void vulSpeeldata(JSONArray kalender) throws ParseException {
    speeldata = new ArrayList<>();

    for (int i = 0; i < kalender.size(); i++) {
      JSONObject  item  = (JSONObject) kalender.get(i);
      if (item.containsKey("ronde")
          && item.containsKey("datum")) {
        speeldata.add(Datum.fromDate(Datum.toDate(item.get("datum").toString()),
                      CaissaConstants.PGN_DATUM_FORMAAT));
      }
    }
  }

  private static void vulSpelers(JSONArray jsonArray) {
    int spelerId  = 1;
    spelers       = new ArrayList<>();

    for (Object naam : jsonArray.toArray()) {
      Spelerinfo  speler  = new Spelerinfo();
      speler.setSpelerId(spelerId);
      speler.setNaam(((JSONObject) naam).get("naam").toString());
      spelers.add(speler);
      spelerId++;
    }
  }
}
