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
import eu.debooy.caissa.Competitie;
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.Spelerinfo;
import eu.debooy.caissa.exceptions.CompetitieException;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.ParameterBundle;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;


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

  private static  Competitie        competitie;
  private static  String[]          rondes;
  private static  List<Date>        speeldata;
  private static  List<Spelerinfo>  spelers;

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

    try {
      competitie  =
          new Competitie(paramBundle.getBestand(CaissaTools.PAR_SCHEMA));
      if (!competitie.containsKey(Competitie.JSON_TAG_SITE)) {
        DoosUtils.foutNaarScherm(
            competitie.getMissingTag(Competitie.JSON_TAG_SITE));
        return;
      }
    } catch (CompetitieException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    speeldata = competitie.getSpeeldata();
    spelers   = competitie.getSpelers();
    rondes    = CaissaUtils.bergertabel(spelers.size());
    var tespelen  = rondes.length * competitie.getHeenTerug();
    if (speeldata.size() != tespelen) {
      DoosUtils.foutNaarScherm(
          MessageFormat.format(
              resourceBundle.getString(CaissaTools.ERR_KALENDER),
              speeldata.size(), tespelen));
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

    try (var output=
          new TekstBestand.Builder()
                          .setBestand(paramBundle
                                          .getBestand(CaissaTools.PAR_BESTAND))
                          .setLezen(false).build()) {
      schrijfToernooi(output, extra);
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }

    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString(
                                CaissaTools.LBL_BESTAND),
                             paramBundle.getBestand(CaissaTools.PAR_BESTAND)));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  private static void schrijfPartij(TekstBestand output, String date,
                                    String ronde, int wit, int zwart, PGN extra)
      throws BestandException, PgnException {
    if (wit >= spelers.size()
        || zwart >= spelers.size()) {
      return;
    }

    PGN partij;
    if (null == extra) {
      partij  = new PGN();
      partij.setTag(PGN.PGNTAG_WHITE, spelers.get(wit).getNaam());
      partij.setTag(PGN.PGNTAG_BLACK, spelers.get(zwart).getNaam());
      partij.setTag(PGN.PGNTAG_RESULT, CaissaConstants.PARTIJ_BEZIG);
    } else {
      partij  = new PGN(extra);
    }

    partij.setTag(PGN.PGNTAG_DATE, date);
    partij.setTag(PGN.PGNTAG_EVENT, competitie.getEvent());
    partij.setTag(PGN.PGNTAG_EVENTDATE,
                  competitie.get(Competitie.JSON_TAG_EVENTDATE).toString());
    partij.setTag(PGN.PGNTAG_ROUND, ronde);
    partij.setTag(PGN.PGNTAG_SITE,
                  competitie.get(Competitie.JSON_TAG_SITE).toString());

    output.write(partij.toString());
  }

  private static void schrijfToernooi(TekstBestand output,
                                      Collection<PGN> extra)
      throws BestandException {
    verwerkRondes(output, 0, 0, 1, extra);

    if (competitie.isDubbel()) {
      verwerkRondes(output, rondes.length, 1, 0, extra);
    }
  }

  private static void verwerkRondes(TekstBestand output, Integer round,
                                    int wit, int zwart,
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
          schrijfPartij(output, Datum.fromDate(speeldata.get(round - 1),
                                               PGN.PGN_DATUM_FORMAAT),
                        round.toString(), pwit, pzwart, extra);
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
}
