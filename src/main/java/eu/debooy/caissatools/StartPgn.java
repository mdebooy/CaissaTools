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
import eu.debooy.caissa.exceptions.CompetitieException;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.MarcoBanner;
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
  private static  List<Date>        speeldata;

  protected StartPgn() {}

  public static void execute(String[] args) {
    setParameterBundle(
        new ParameterBundle.Builder()
                           .setArgs(args)
                           .setBanner(new MarcoBanner())
                           .setBaseName(CaissaTools.TOOL_STARTPGN)
                           .setValidator(new BestandDefaultParameters())
                           .build());

    if (!paramBundle.isValid()) {
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
                                    String ronde, String wit, String zwart,
                                    PGN extra)
      throws BestandException, PgnException {
    if (wit.equals(CaissaConstants.BYE)
        || zwart.equals(CaissaConstants.BYE)) {
      return;
    }

    PGN partij;

    if (null == extra) {
      partij  = new PGN();
      partij.setTag(PGN.PGNTAG_WHITE, wit);
      partij.setTag(PGN.PGNTAG_BLACK, zwart);
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
                                      Collection<PGN> bestaand)
      throws BestandException {
    var metVolgorde = paramBundle.getBoolean(CaissaTools.PAR_METVOLGORDE);
    var speelschema = CaissaUtils.genereerSpeelschema(competitie);

    for (var partij : speelschema) {
      var ronde   = partij.getRonde().getRonde();
      var round   = partij.getRonde().getRound();
      var wit     = partij.getWitspeler().getNaam();
      var zwart   = partij.getZwartspeler().getNaam();
      var extra   = vindPartij(wit, zwart, bestaand);
      try {
        schrijfPartij(output, Datum.fromDate(speeldata.get(ronde - 1),
                                             PGN.PGN_DATUM_FORMAAT),
                      metVolgorde ? round : ronde.toString(), wit, zwart,
                      extra);
      } catch (PgnException e) {
        DoosUtils.foutNaarScherm(ronde + " " + e.getLocalizedMessage());
      }
    }
  }

  private static PGN vindPartij(String wit, String zwart,
                                Collection<PGN> partijen) {
    return
        partijen.stream()
                .filter(pgn -> pgn.getWhite().equals(wit))
                .filter(pgn -> pgn.getBlack().equals(zwart))
                .findFirst().orElse(null);
  }
}
