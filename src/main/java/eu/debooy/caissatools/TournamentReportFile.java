/*
 * Copyright (c) 2022 Marco de Booij
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
import eu.debooy.caissa.Partij;
import eu.debooy.caissa.Spelerinfo;
import eu.debooy.caissa.exceptions.CompetitieException;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.MarcoBanner;
import eu.debooy.doosutils.ParameterBundle;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;


/**
  @author Marco de Booij
 */
public class TournamentReportFile extends Batchjob {
  private static  Competitie  competitie;

  public static void execute(String[] args) {
    setParameterBundle(new ParameterBundle.Builder()
                                  .setArgs(args)
                                  .setBanner(new MarcoBanner())
                                  .setBaseName(CaissaTools.TOOL_TRF)
                                  .setValidator(new BestandDefaultParameters())
                                  .build());

    if (!paramBundle.isValid()) {
      return;
    }

    try {
      competitie =
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

    var spelers = competitie.getSpelers();

    Collection<PGN> partijen  = new TreeSet<>(new PGN.ByEventComparator());
    try {
      partijen.addAll(
          CaissaUtils.laadPgnBestand(
              paramBundle.getBestand(CaissaTools.PAR_BESTAND)));
    } catch (PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    CaissaUtils.vulToernooiMatrix(partijen, competitie, false);

    spelers.sort(new Spelerinfo.BySpelerSeqComparator());

    var spelerstrf  = vulBasisTrf(spelers);
    var schema      =
        CaissaUtils.genereerSpeelschema(competitie, partijen);

    var rondes      = 0;
    for (Partij partij : schema) {
      rondes = Math.max(rondes,
                        Integer.parseInt(partij.getRonde()
                                               .getRound().split("\\.")[0]));
      verwerkPartij(partij, spelers, spelerstrf);
    }

    schrijfTrfBestand(rondes, spelerstrf);

    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  private static String getPunten(Partij partij, String winst, String remise,
                                  String verlies, String bye) {
    switch (partij.getUitslag()) {
      case CaissaConstants.PARTIJ_WIT_WINT:
        return winst;
      case CaissaConstants.PARTIJ_REMISE:
        return remise;
      case CaissaConstants.PARTIJ_ZWART_WINT:
        return verlies;
      default:
        if (partij.isBye()) {
          return bye;
        } else {
          return verlies;
        }
    }
  }

  protected static void schrijfTrfBestand(int rondes,
                                          StringBuilder[] spelerstrf) {
    try  (var trf = new TekstBestand.Builder()
                          .setBestand(paramBundle.getBestand(
                                          CaissaTools.PAR_TRFBESTAND))
                          .setLezen(false).build()) {
      trf.write("XXC white1");
      trf.write(String.format("XXR %d", rondes));
      trf.write("XXS W=1 D=0.5 L=0 FL=0 ZPB=0 PAB=1");
      trf.write(String.format("012 %s", competitie.getEvent()));
      trf.write(String.format("023 %s", competitie.get(Competitie.JSON_TAG_SITE)
                                                  .toString()));
      for (var spelertrf : spelerstrf) {
        trf.write(spelertrf.toString());
      }
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }
  }

  protected static void verwerkPartij(Partij partij, List<Spelerinfo> spelers,
                                      StringBuilder[] spelerstrf) {
    var wit       = vindSpeler(partij.getWitspeler().getNaam(), spelers);
    var witseq    = 0;
    var zwart     = vindSpeler(partij.getZwartspeler().getNaam(), spelers);
    var zwartseq  = 0;

    if (zwart != -1) {
      zwartseq  = spelers.get(zwart).getSpelerSeq();
    }

    if (wit != -1) {
      witseq    = spelers.get(wit).getSpelerSeq();
      spelerstrf[wit].append(
          String.format("  %04d %s %s",
                        zwartseq,
                        partij.isBye() ? "-" : "w",
                        partij.isRanked() ? getPunten(partij,
                                                      "1", "=", "0", "Z")
                                          : "0"));
    }

    if (zwart != -1) {
      spelerstrf[zwart].append(
          String.format("  %04d %s %s",
                        witseq,
                        partij.isBye() ? "-" : "b",
                        partij.isRanked() ? getPunten(partij,
                                                      "0", "=", "1", "Z")
                                          : "0"));
    }
  }

  protected static int vindSpeler(String speler, List<Spelerinfo> spelers) {
    var seq = -1;

    for (var i = 0; i < spelers.size(); i++) {
      if (speler.equals(spelers.get(i).getNaam())) {
        seq = i;
        break;
      }
    }

    return seq;
  }

  protected static StringBuilder[] vulBasisTrf(List<Spelerinfo> spelers) {
    var spelerstrf  = new StringBuilder[spelers.size()];

    for (var i = 0; i < spelerstrf.length; i++) {
      var speler    = spelers.get(i);
      spelerstrf[i] = new StringBuilder();
      // Moet een decimale punt zijn.
      spelerstrf[i].append(String.format(Locale.ENGLISH,
                                         "%03d %04d%6s%-33.33s%33s%4.1f%5s",
                                         1,
                                         speler.getSpelerSeq(),
                                         "",
                                         speler.getNaam(),
                                         "",
                                         speler.getPunten(),
                                         ""));
    }

    return spelerstrf;
  }
}
