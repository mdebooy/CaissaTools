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
import eu.debooy.caissa.Spelerinfo;
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
import java.util.ArrayList;
import java.util.List;
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

  private static  JSONObject        club;
  private static  Integer           toernooitype;
  private static  List<Spelerinfo>  voorronde;

  protected Clubstatistiek() {}

  public static void execute(String[] args) {
    setParameterBundle(
        new ParameterBundle.Builder()
                           .setArgs(args)
                           .setBanner(new MarcoBanner())
                           .setBaseName(CaissaTools.TOOL_CLUBSTATISTIEK)
                           .setValidator(new ClubstatistiekParameters())
                           .build());

    if (!paramBundle.isValid()) {
      return;
    }

    toernooitype  = paramBundle.getInteger(CaissaTools.PAR_TOERNOOITYPE);
    voorronde     = new ArrayList<>();

    leesStatistiek();

    verwerkVoorronde();
    verwerkFinaleronde();
    schrijfStatistiek();

    klaar();
  }

  private static String getSite() {
    var bestand = paramBundle.getString(CaissaTools.PAR_BESTAND).split(";")[0];

    try {
      var partijen  = CaissaUtils.laadPgnBestand(bestand);
      return partijen.iterator().next().getTag(PGN.PGNTAG_SITE);
    } catch (PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return CaissaTools.TOOL_CLUBSTATISTIEK;
    }
  }

  private static void leesStatistiek() {
    var statistiek  = paramBundle.getBestand(CaissaTools.PAR_STATISTIEK);

    try (var invoer   = new JsonBestand.Builder()
                              .setBestand(statistiek)
                              .build()) {
      club    = invoer.read();
    } catch (BestandException e) {
      DoosUtils.naarScherm(
          MessageFormat.format(
              resourceBundle.getString(CaissaTools.MSG_NIEUWBESTAND),
              statistiek));
      String  site;
      if (paramBundle.containsArgument(CaissaTools.PAR_SITE)) {
        site  = paramBundle.getString(CaissaTools.PAR_SITE);
      } else {
        site  = getSite();
      }
      club    = new JSONObject();
      club.put(PGN.PGNTAG_SITE, site);
    }
  }

  private static void schrijfStatistiek() {
    var statistiek  = paramBundle.getBestand(CaissaTools.PAR_STATISTIEK);

    try (var output =
          new JsonBestand.Builder()
                          .setBestand(statistiek)
                          .setLezen(false).build()) {
      output.write(club);
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }
  }

  private static void verwerkFinaleronde() {
    var start       = 1;
    var doorlopend  = paramBundle.getBoolean(CaissaTools.PAR_DOORLOPEND);

    for (var groep : paramBundle.getString(CaissaTools.PAR_BESTAND)
                                .split(";")) {
      try {
        var partijen    = CaissaUtils.laadPgnBestand(groep);
        var competitie  = new Competitie(partijen, toernooitype);

        CaissaUtils.vulToernooiMatrix(partijen, competitie, false);
        start   = verwerkToernooi(competitie, start);
        if (Boolean.FALSE.equals(doorlopend)) {
          start = 1;
        }

      } catch (CompetitieException | PgnException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }
  }

  private static int verwerkToernooi(Competitie competitie, int start) {
    competitie.sorteerOpStand();

    var clubspelers   = new JSONObject();
    if (club.containsKey(CaissaTools.PAR_SPELERS)) {
      clubspelers     = (JSONObject) club.get(CaissaTools.PAR_SPELERS);
    }

    var plaats        = start;
    for (var speler : competitie.getSpelers()) {
      var naam        = speler.getNaam();
      if (naam.equals(CaissaConstants.BYE)) {
        continue;
      }

      var extra = voorronde.stream()
                           .filter(splr -> speler.getNaam()
                                                 .equals(splr.getNaam()))
                           .findFirst().orElse(null);
      if (null != extra) {
        speler.addPartij(extra.getPartijen());
        speler.addPunt(extra.getPunten());
        speler.addTieBreakScore(extra.getTieBreakScore());
      }
      var clubspeler  = new JSONObject();
      if (clubspelers.containsKey(naam)) {
        clubspeler    = (JSONObject) clubspelers.get(naam);
      }
      if (speler.getPartijen() > 0) {
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
    }
    club.put(CaissaTools.PAR_SPELERS, clubspelers);

    return plaats;
  }

  private static void verwerkVoorronde() {
    if (!paramBundle.containsArgument(CaissaTools.PAR_VOORRONDE)) {
      return;
    }

    for (var groep : paramBundle.getString(CaissaTools.PAR_VOORRONDE)
                                .split(";")) {
      try {
        var partijen    = CaissaUtils.laadPgnBestand(groep);
        var competitie  = new Competitie(partijen, toernooitype);
        CaissaUtils.vulToernooiMatrix(partijen, competitie, false);
        competitie.getDeelnemers().forEach(speler -> voorronde.add(speler));

      } catch (CompetitieException | PgnException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }
  }
}
