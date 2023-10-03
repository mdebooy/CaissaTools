/*
 * Copyright (c) 2023 Marco de Booij
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by
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
import eu.debooy.caissa.Competitie;
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.exceptions.CompetitieException;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.MarcoBanner;
import eu.debooy.doosutils.ParameterBundle;
import eu.debooy.doosutils.access.JsonBestand;
import eu.debooy.doosutils.exception.BestandException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;
import org.json.simple.JSONObject;


/**
 * @author Marco de Booij
 */
public class Clubstatistiek extends Batchjob {
  private static  final ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());

  private static  JSONObject  club;
  private static  Competitie  competitie;

  protected Clubstatistiek() {}

  public static void execute(String[] args) {
    setParameterBundle(
        new ParameterBundle.Builder()
                           .setArgs(args)
                           .setBanner(new MarcoBanner())
                           .setBaseName(CaissaTools.TOOL_CLUBSTATISTIEK)
                           .setValidator(new BestandDefaultParameters())
                           .build());

    if (!paramBundle.isValid()) {
      return;
    }

    var invoer  = paramBundle.getBestand(CaissaTools.PAR_BESTAND);
    Collection<PGN> partijen;
    try {
      competitie  =
          new Competitie(paramBundle.getBestand(CaissaTools.PAR_SCHEMA));
      partijen    =
          CaissaUtils.laadPgnBestand(invoer);
    } catch (CompetitieException | PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    leesStatistiek();

    CaissaUtils.vulToernooiMatrix(partijen, competitie, false);

    verwerkToernooi();

    schrijfStatistiek();

    klaar();
  }

  private static void leesStatistiek() {
    var statistiek    = paramBundle.getBestand(CaissaTools.PAR_CLUBSTATISTIEK);
    try (var invoer   = new JsonBestand.Builder()
                              .setBestand(statistiek)
                              .build()) {
      club    = invoer.read();
    } catch (BestandException e) {
      DoosUtils.naarScherm(
          MessageFormat.format(
              resourceBundle.getString(CaissaTools.MSG_NIEUWBESTAND),
              statistiek));
      club  = new JSONObject();
      club.put(PGN.PGNTAG_SITE, competitie.getSite());
    }
  }

  private static void schrijfStatistiek() {
    var statistiek  = paramBundle.getBestand(CaissaTools.PAR_CLUBSTATISTIEK);
    try (var output =
          new JsonBestand.Builder()
                          .setBestand(statistiek)
                          .setLezen(false).build()) {
      output.write(club);
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }
  }

  private static void verwerkToernooi() {
    competitie.sorteerOpStand();

    var clubspelers   = new JSONObject();
    if (club.containsKey(CaissaTools.PAR_SPELERS)) {
      clubspelers     = (JSONObject) club.get(CaissaTools.PAR_SPELERS);
    }

    var plaats        = 1;
    for (var speler : competitie.getSpelers()) {
      var naam        = speler.getNaam();
      if (naam.equals(CaissaConstants.BYE)) {
        continue;
      }

      var clubspeler  = new JSONObject();
      if (clubspelers.containsKey(naam)) {
        clubspeler    = (JSONObject) clubspelers.get(naam);
      }
      var stat        = new JSONObject();
      stat.put("plaats", plaats);
      stat.put("punten", speler.getPunten());
      stat.put("partijen", speler.getPartijen());
      stat.put("SB", speler.getTieBreakScore());
      stat.put("event", competitie.getEvent());
      stat.put("eventdate", competitie.getEventdate());
      clubspeler.put(competitie.getEventdate(), stat);

      clubspelers.put(naam, clubspeler);
      plaats++;
    }
    club.put(CaissaTools.PAR_SPELERS, clubspelers);
  }
}
