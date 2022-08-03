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
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.Partij;
import eu.debooy.caissa.Spelerinfo;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.ParameterBundle;
import eu.debooy.doosutils.access.JsonBestand;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;


/**
  @author Marco de Booij
 */
public class TournamentReportFile extends Batchjob {
  public static void execute(String[] args) {
    setParameterBundle(new ParameterBundle.Builder()
                                  .setBaseName(CaissaTools.TOOL_TRF)
                                  .setValidator(new BestandDefaultParameters())
                                  .build());

    Banner.printMarcoBanner(DoosUtils.nullToEmpty(paramBundle.getBanner()));

    if (!paramBundle.isValid()
        || !paramBundle.setArgs(args)) {
      help();
      printFouten();
      return;
    }

    JsonBestand competitie;
    try {
      competitie  =
          new JsonBestand.Builder()
                         .setBestand(paramBundle
                                        .getBestand(CaissaTools.PAR_SCHEMA))
                         .build();
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    // enkel: 0 = Tweekamp, 1 = Enkelrondig, 2 = Dubbelrondig
    int toernooitype;
    if (competitie.containsKey(CaissaConstants.JSON_TAG_TOERNOOITYPE)) {
      toernooitype  =
          ((Long) competitie.get(CaissaConstants.JSON_TAG_TOERNOOITYPE))
              .intValue();
    } else {
      toernooitype  = CaissaConstants.TOERNOOI_MATCH;
    }

    List<Spelerinfo>  spelers   = new ArrayList<>();

    CaissaUtils.vulSpelers(spelers,
                           competitie.getArray(
                                CaissaConstants.JSON_TAG_SPELERS));
    var matrix  = new double[spelers.size()][spelers.size() * toernooitype];

    Collection<PGN> partijen  = new TreeSet<>(new PGN.ByEventComparator());
    try {
      partijen.addAll(
          CaissaUtils.laadPgnBestand(
              paramBundle.getBestand(CaissaTools.PAR_BESTAND)));
    } catch (PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    CaissaUtils.vulToernooiMatrix(partijen, spelers, matrix, 0, false,
                                  CaissaConstants.TIEBREAK_SB);

    spelers.sort(new Spelerinfo.BySpelerSeqComparator());

    var spelerstrf  = vulBasisTrf(spelers);
    var enkelrondig = (toernooitype == CaissaConstants.TOERNOOI_ENKEL);
    var schema      =
        CaissaUtils.genereerSpeelschema(spelers, enkelrondig, partijen);

    var rondes      = 0;
    for (Partij partij : schema) {
      rondes = Math.max(rondes,
                        Integer.valueOf(partij.getRonde()
                                              .getRound().split("\\.")[0]));
      verwerkPartij(partij, spelers, spelerstrf);
    }

    schrijfTrfBestand(competitie, rondes, spelerstrf);

    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  protected static void schrijfTrfBestand(JsonBestand schema, int rondes,
                                          StringBuilder[] spelerstrf) {
    try  (var trf = new TekstBestand.Builder()
                          .setBestand(paramBundle.getBestand(
                                          CaissaTools.PAR_TRFBESTAND))
                          .setLezen(false).build()) {
      trf.write("XXC white1");
      trf.write(String.format("XXR %d", rondes));
      trf.write("XXS W=1 D=0.5 L=0 FL=0 ZPB=0 PAB=1");
      trf.write(String.format("012 %s",
                              schema.get(CaissaTools.PAR_EVENT).toString()));
      trf.write(String.format("023 %s",
                              schema.get(CaissaTools.PAR_SITE).toString()));
      for (StringBuilder spelertrf : spelerstrf) {
        trf.write(spelertrf.toString());
      }
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }
  }

  protected static void verwerkPartij(Partij partij, List<Spelerinfo> spelers,
                                      StringBuilder[] spelerstrf) {
    String  punten;
    int     witseq    = 0;
    int     zwartseq  = 0;

    var uitslag = partij.getUitslag();
    var wit     = vindSpeler(partij.getWitspeler().getNaam(), spelers);
    var zwart   = vindSpeler(partij.getZwartspeler().getNaam(), spelers);

    if (wit != -1) {
      witseq = spelers.get(wit).getSpelerSeq();
      switch (uitslag) {
        case CaissaConstants.PARTIJ_WIT_WINT:
          punten  = "1";
          break;
        case CaissaConstants.PARTIJ_REMISE:
          punten  = "=";
          break;
        case CaissaConstants.PARTIJ_ZWART_WINT:
          punten  = "0";
          break;
        default:
          if (partij.isBye()) {
            punten  = "Z";
          } else {
            punten  = "0";
          }
          break;
      }
      if (zwart != -1) {
        zwartseq = spelers.get(zwart).getSpelerSeq();
      }
      spelerstrf[wit].append(String.format("  %04d %s %s", zwartseq,
                                            partij.isBye() ? "-" : "w",
                                            partij.isRanked() ? punten : "0"));
    }

    if (zwart != -1) {
      switch (uitslag) {
        case CaissaConstants.PARTIJ_WIT_WINT:
          punten  = "0";
          break;
        case CaissaConstants.PARTIJ_REMISE:
          punten  = "=";
          break;
        case CaissaConstants.PARTIJ_ZWART_WINT:
          punten  = "1";
          break;
        default:
          if (partij.isBye()) {
            punten  = "Z";
          } else {
            punten  = "0";
          }
          break;
      }
      spelerstrf[zwart].append(String.format("  %04d %s %s",
                                             witseq,
                                             partij.isBye() ? "-" : "b",
                                             partij.isRanked() ? punten : "0"));
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
    StringBuilder[] spelerstrf  = new StringBuilder[spelers.size()];

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
