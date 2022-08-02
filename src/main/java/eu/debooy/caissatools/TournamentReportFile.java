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

  public static void main(String[] args) throws PgnException {
    String[]  test  = new String[]
        {"--bestand", "/homes/booymar/Schaken/Clubs en Organisaties/De Brug/Seizoen 2021-2022/partijen.pgn",
         "--schema", "/homes/booymar/Schaken/Clubs en Organisaties/De Brug/Seizoen 2021-2022/Competitie.json",
         "--trfbestand", "/homes/booymar/Schaken/Clubs en Organisaties/De Brug/Seizoen 2021-2022/Competitie.trf"};
    execute(test);
  }

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

    // enkel: 0 = Tweekamp, 1 = Enkelrondig, 2 = Dubbelrondig
    int toernooitype;
    if (schema.containsKey(CaissaConstants.JSON_TAG_TOERNOOITYPE)) {
      toernooitype  =
          ((Long) schema.get(CaissaConstants.JSON_TAG_TOERNOOITYPE))
              .intValue();
    } else {
      toernooitype  = CaissaConstants.TOERNOOI_MATCH;
    }
//    var eventDate = schema.get(CaissaConstants.JSON_TAG_EVENTDATE).toString();
    List<Spelerinfo>  spelers   = new ArrayList<>();

    CaissaUtils.vulSpelers(spelers,
                           schema.getArray(CaissaConstants.JSON_TAG_SPELERS));
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

    var spelerstrf  = new StringBuilder[spelers.size()];
    vulBasisTrf(spelers, spelerstrf);

    for (PGN partij : partijen) {
      verwerkPartij(partij, spelers, spelerstrf);
    }

    schrijfTrfBestand(schema, spelerstrf);

    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  protected static void schrijfTrfBestand(JsonBestand schema,
                                          StringBuilder[] spelerstrf) {
    try  (var trf = new TekstBestand.Builder()
                          .setBestand(paramBundle.getBestand(
                                          CaissaTools.PAR_TRFBESTAND))
                          .setLezen(false).build()) {
      trf.write("XXC white1");
      trf.write(String.format("XXR %d", (spelerstrf.length+1)/2));
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

  protected static void verwerkPartij(PGN partij, List<Spelerinfo> spelers,
                                      StringBuilder[] spelerstrf) {
    String  punten;
    int     witseq;
    int     zwartseq;

    var uitslag = partij.getTag(CaissaConstants.PGNTAG_RESULT);
    var wit     = vindSpeler(partij.getWhite(), spelers);
    var zwart   = vindSpeler(partij.getBlack(), spelers);

    if (wit == -1) {
      uitslag = CaissaConstants.PARTIJ_UNRANKED;
      witseq    = 0;
    } else {
      witseq    = spelers.get(wit).getSpelerSeq();
    }
    if (zwart == -1) {
      uitslag   = CaissaConstants.PARTIJ_UNRANKED;
      zwartseq  = 0;
    } else {
      zwartseq  = spelers.get(zwart).getSpelerSeq();
    }

    if (partij.isBye() || !partij.isRanked()) {
      uitslag = CaissaConstants.PARTIJ_UNRANKED;
    }

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
        punten  = "Z";
        break;
    }
    spelerstrf[wit].append(String.format("  %04d %s %s",
                                         zwartseq, "w", punten));

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
        punten  = "Z";
        break;
    }
    spelerstrf[zwart].append(String.format("  %04d %s %s",
                                           witseq, "b", punten));
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

  protected static void vulBasisTrf(List<Spelerinfo> spelers,
                                    StringBuilder[] spelerstrf) {
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
  }
}
