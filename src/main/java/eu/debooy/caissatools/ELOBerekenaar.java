/**
 * Copyright (c) 2012 Marco de Booij
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by
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
import eu.debooy.caissa.ELO;
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.Spelerinfo;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.MarcoBanner;
import eu.debooy.doosutils.ParameterBundle;
import eu.debooy.doosutils.access.BestandConstants;
import eu.debooy.doosutils.access.CsvBestand;
import eu.debooy.doosutils.exception.BestandException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * @author Marco de Booij
 */
public final class ELOBerekenaar extends Batchjob {
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());
  private static final  String[]      KOLOMMEN    =
      new String[]{"speler","elo","groei","partijen","eerstePartij",
                   "laatstePartij","eersteEloDatum","minElo","minEloDatum",
                   "maxElo","maxEloDatum"};
  private static final  List<String>  UITSLAGEN   =
          Arrays.asList(CaissaConstants.PARTIJ_ZWART_WINT,
                        CaissaConstants.PARTIJ_REMISE,
                        CaissaConstants.PARTIJ_WIT_WINT,
                        CaissaConstants.PARTIJ_BEZIG);

  private static  String      eindDatum;
  private static  boolean     extraInfo;
  private static  Integer     kFactor;
  private static  Integer     maxVerschil;
  private static  String      startDatum;
  private static  int         startElo;
  private static  int         verwerkt;

  private static  List<Spelerinfo>      spelerinfos;
  private static  Map<String, Integer>  spelers;

  protected ELOBerekenaar() {}

  public static void execute(String[] args) {
    setParameterBundle(
        new ParameterBundle.Builder()
                           .setArgs(args)
                           .setBanner(new MarcoBanner())
                           .setBaseName(CaissaTools.TOOL_ELOBEREKENAAR)
                           .setValidator(new ELOBerekenaarParameters())
                           .build());

    if (!paramBundle.isValid()) {
      return;
    }

    spelerinfos   = new ArrayList<>();
    spelers       = new TreeMap<>();

    if (paramBundle.containsArgument(CaissaTools.PAR_EINDDATUM)) {
      eindDatum   = paramBundle.getString(CaissaTools.PAR_EINDDATUM);
    } else {
      eindDatum   = CaissaConstants.DEF_EINDDATUM;
    }
    extraInfo     = paramBundle.getBoolean(CaissaTools.PAR_EXTRAINFO);
    if (paramBundle.containsArgument(CaissaTools.PAR_VASTEKFACTOR)) {
      kFactor     = paramBundle.getInteger(CaissaTools.PAR_VASTEKFACTOR);
    } else {
      kFactor     = null;
    }
    maxVerschil   = paramBundle.getInteger(CaissaTools.PAR_MAXVERSCHIL);
    startElo      = paramBundle.getInteger(CaissaTools.PAR_STARTELO);
    if (paramBundle.containsArgument(CaissaTools.PAR_STARTDATUM)) {
      startDatum  = paramBundle.getString(CaissaTools.PAR_STARTDATUM);
    } else {
      startDatum  = CaissaConstants.DEF_STARTDATUM;
    }
    verwerkt      = 0;

    leesSpelers(paramBundle.getBestand(CaissaTools.PAR_SPELERBESTAND));
    var aantalPartijen  = verwerkToernooi();

    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString(CaissaTools.LBL_BESTAND),
                             paramBundle
                                 .getBestand(CaissaTools.PAR_SPELERBESTAND)));
    if (!CaissaConstants.DEF_STARTDATUM.equals(startDatum)) {
      DoosUtils.naarScherm(
          MessageFormat.format(resourceBundle.getString("label.startdatum"),
                               startDatum));
    }
    if (!CaissaConstants.DEF_EINDDATUM.equals(eindDatum)) {
      DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.einddatum"),
                             eindDatum));
    }
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString(CaissaTools.LBL_PARTIJEN),
                             aantalPartijen));
    if (verwerkt > 0) {
      schrijfSpelers(spelers, spelerinfos);
      DoosUtils.naarScherm(
          MessageFormat.format(resourceBundle.getString("label.verwerkt"),
                               verwerkt));
    }
    if (null != kFactor) {
      DoosUtils.naarScherm(
          MessageFormat.format(resourceBundle.getString("label.kFactor"),
                               kFactor));
    }
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  private static void leesSpelers(String spelerBestand) {
    try (var invoer  = new CsvBestand.Builder()
                              .setBestand(spelerBestand)
                              .build()) {
      while (invoer.hasNext()) {
        var veld        = invoer.next();
        var spelerId    = spelers.size();
        var spelerinfo  = new Spelerinfo();
        spelerinfo.setNaam(veld[0]);
        spelerinfo.setElo(Integer.valueOf(veld[1]));
        spelerinfo.setElogroei(Integer.valueOf(veld[2]));
        spelerinfo.setPartijen(Integer.valueOf(veld[3]));
        spelerinfo.setEerstePartij(Datum.toDate(veld[4],
                                   PGN.PGN_DATUM_FORMAAT));
        spelerinfo.setLaatstePartij(Datum.toDate(veld[5],
                                    PGN.PGN_DATUM_FORMAAT));
        if (spelerinfo.getPartijen() >= ELO.MIN_PARTIJEN) {
          spelerinfo.setOfficieel(Datum.toDate(veld[6],
                                  PGN.PGN_DATUM_FORMAAT));
          spelerinfo.setMinElo(Integer.valueOf(veld[7]));
          spelerinfo.setMinDatum(Datum.toDate(veld[8],
                                 PGN.PGN_DATUM_FORMAAT));
          spelerinfo.setMaxElo(Integer.valueOf(veld[9]));
          spelerinfo.setMaxDatum(Datum.toDate(veld[10],
                                 PGN.PGN_DATUM_FORMAAT));
        }
        spelerinfo.setSpelerId(spelerId);
        spelerinfos.add(spelerId, spelerinfo);
        spelers.put(veld[0], spelerId);
      }
    } catch (BestandException e) {
      DoosUtils.naarScherm(
          MessageFormat.format(
              resourceBundle.getString("message.nieuwbestand"), spelerBestand));
    } catch (ParseException e) {
      DoosUtils.foutNaarScherm(
          MessageFormat.format(
              resourceBundle.getString(CaissaTools.ERR_FOUTEDATUMIN),
              spelerBestand) + " [" + e.getLocalizedMessage() + "]");
    }
  }

  private static boolean partijAfwijzen(String datum, Date eloDatum,
                                        int uitslag, Date laatsteWit,
                                        Date laatsteZwart) {
    if (startDatum.compareTo(datum) > 0
        || eindDatum.compareTo(datum) < 0) {
      return true;
    }

    if (uitslag > 2) {
      return true;
    }

    if (null == laatsteWit
        && null == laatsteZwart) {
      return false;
    }

    if (null == laatsteWit) {
      return !eloDatum.after(laatsteZwart);
    }

    if (null == laatsteZwart) {
      return !eloDatum.after(laatsteWit);
    }

    return !eloDatum.after(Datum.max(laatsteWit, laatsteZwart));
  }

  private static void pasSpelerAan(int id, List<Spelerinfo> spelerinfos,
                                   Date eloDatum, Integer andereElo,
                                   int uitslag) {
    var     vorigeElo = spelerinfos.get(id).getElo();
    var     aantal    = spelerinfos.get(id).getPartijen();
    Integer elo;
    if (null == kFactor) {
      elo     = ELO.berekenELO(spelerinfos.get(id).getElo(), uitslag, andereElo,
                               aantal);
    } else {
      elo     = ELO.berekenELO(spelerinfos.get(id).getElo(), uitslag, andereElo,
                               kFactor, maxVerschil);
    }
    spelerinfos.get(id).setElo(elo);
    spelerinfos.get(id).setElogroei(elo-vorigeElo);
    spelerinfos.get(id).setLaatstePartij(eloDatum);
    spelerinfos.get(id).addPartij();
    aantal++;
    if (aantal == ELO.MIN_PARTIJEN) {
      spelerinfos.get(id).setMinElo(elo);
      spelerinfos.get(id).setMaxElo(elo);
      spelerinfos.get(id).setMinDatum(eloDatum);
      spelerinfos.get(id).setMaxDatum(eloDatum);
      spelerinfos.get(id).setOfficieel(eloDatum);
    }
    if (aantal > ELO.MIN_PARTIJEN) {
      if (spelerinfos.get(id).getMinElo() >= elo) {
        spelerinfos.get(id).setMinElo(elo);
        spelerinfos.get(id).setMinDatum(eloDatum);
      }
      if (spelerinfos.get(id).getMaxElo() <= elo) {
        spelerinfos.get(id).setMaxElo(elo);
        spelerinfos.get(id).setMaxDatum(eloDatum);
      }
    }
  }

  private static void schrijfSpeler(CsvBestand csvBestand, Integer spelerId)
      throws BestandException {
    var velden  = new Object[KOLOMMEN.length];

    velden[0] = spelerinfos.get(spelerId).getNaam();
    velden[1] = spelerinfos.get(spelerId).getElo();
    velden[2] = spelerinfos.get(spelerId).getElogroei();
    velden[3] = spelerinfos.get(spelerId).getPartijen();
    velden[4] = Datum.fromDate(spelerinfos.get(spelerId).getEerstePartij(),
                               PGN.PGN_DATUM_FORMAAT);
    velden[5] = Datum.fromDate(spelerinfos.get(spelerId).getLaatstePartij(),
                               PGN.PGN_DATUM_FORMAAT);
    if (spelerinfos.get(spelerId).getPartijen() < ELO.MIN_PARTIJEN) {
      velden[6]   = "";
      velden[7]   = "";
      velden[8]   = "";
      velden[9]   = "";
      velden[10]  = "";
    } else {
      velden[6]   = Datum.fromDate(spelerinfos.get(spelerId).getOfficieel(),
                                   PGN.PGN_DATUM_FORMAAT);
      velden[7]   = spelerinfos.get(spelerId).getMinElo();
      velden[8]   = Datum.fromDate(spelerinfos.get(spelerId).getMinDatum(),
                                   PGN.PGN_DATUM_FORMAAT);
      velden[9]   = spelerinfos.get(spelerId).getMaxElo();
      velden[10]  = Datum.fromDate(spelerinfos.get(spelerId).getMaxDatum(),
                                   PGN.PGN_DATUM_FORMAAT);
    }
    csvBestand.write(velden);
  }

  private static void schrijfSpelers(Map<String, Integer> spelers,
                                     List<Spelerinfo> spelerinfos) {
    try (var csvBestand  =
            new CsvBestand.Builder()
                          .setBestand(
                              paramBundle
                                  .getBestand(CaissaTools.PAR_SPELERBESTAND,
                                              BestandConstants.EXT_CSV))
                          .setLezen(false)
                          .setKolomNamen(KOLOMMEN)
                          .build()) {
      for (Integer spelerId  : spelers.values()) {
        if (null != spelerinfos.get(spelerId).getLaatstePartij()) {
          schrijfSpeler(csvBestand, spelerId);
        }
      }
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }
  }

  private static int verwerkPartij(PGN partij, CsvBestand geschiedenis)
      throws BestandException {
    var   datum     = partij.getTag(PGN.PGNTAG_DATE);
    Date  eloDatum;
    try {
      eloDatum  =
          Datum.toDate(datum, PGN.PGN_DATUM_FORMAAT);
    } catch (ParseException e) {
      DoosUtils.foutNaarScherm(
          MessageFormat.format(
              resourceBundle.getString(CaissaTools.ERR_FOUTEDATUM),
              datum) + " [" + e.getLocalizedMessage() + "].");
      return 0;
    }

    var wit         = partij.getTag(PGN.PGNTAG_WHITE);
    var zwart       = partij.getTag(PGN.PGNTAG_BLACK);

    voegSpelerToe(wit, eloDatum);
    voegSpelerToe(zwart, eloDatum);

    var resultaat   = partij.getTag(PGN.PGNTAG_RESULT);
    var uitslag     = UITSLAGEN.indexOf(resultaat);
    var witId       = spelers.get(wit);
    var zwartId     = spelers.get(zwart);

    if (partijAfwijzen(datum, eloDatum, uitslag,
                       spelerinfos.get(witId).getLaatstePartij(),
                       spelerinfos.get(zwartId).getLaatstePartij())) {
      return 0;
    }

    var witElo    = spelerinfos.get(witId).getElo();
    var zwartElo  = spelerinfos.get(zwartId).getElo();
    pasSpelerAan(witId,   spelerinfos, eloDatum, zwartElo, uitslag);
    pasSpelerAan(zwartId, spelerinfos, eloDatum, witElo, 2 - uitslag);
    if (null != geschiedenis) {
      if (extraInfo) {
        geschiedenis.write(wit, datum,
                           spelerinfos.get(witId).getElo(),
                           spelerinfos.get(witId).getPartijen(),
                           spelerinfos.get(witId).getElo() - witElo, zwart,
                           partij.getTag(PGN.PGNTAG_EVENT));
        geschiedenis.write(zwart, datum,
                           spelerinfos.get(zwartId).getElo(),
                           spelerinfos.get(zwartId).getPartijen(),
                           spelerinfos.get(zwartId).getElo() - zwartElo, wit,
                           partij.getTag(PGN.PGNTAG_EVENT));
      } else {
        geschiedenis.write(wit, datum,
                           spelerinfos.get(witId).getElo(),
                           spelerinfos.get(witId).getPartijen(),
                           spelerinfos.get(witId).getElo() - witElo);
        geschiedenis.write(zwart, datum,
                           spelerinfos.get(zwartId).getElo(),
                           spelerinfos.get(zwartId).getPartijen(),
                           spelerinfos.get(zwartId).getElo() - zwartElo);
      }
    }

    return 1;
  }

  private static int verwerkToernooi() {
    var aantalPartijen  = 0;

    try (var geschiedenis  =
            new CsvBestand.Builder().setBestand(
                    paramBundle.getBestand(CaissaTools.PAR_GESCHIEDENISBESTAND,
                                           BestandConstants.EXT_CSV))
                                    .setLezen(false)
                                    .setHeader(false)
                                    .setAppend(true)
                                    .build()) {
      Collection<PGN>
          partijen  = new TreeSet<>();
      partijen.addAll(
          CaissaUtils
              .laadPgnBestand(
                  paramBundle.getBestand(CaissaTools.PAR_TOERNOOIBESTAND,
                                         BestandConstants.EXT_PGN)));
      for (var partij : partijen) {
        if (!partij.isBye()
            && partij.isRated()) {
          verwerkt  += verwerkPartij(partij, geschiedenis);
        }
      }
      aantalPartijen  = partijen.size();
    } catch (BestandException | PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }

    return aantalPartijen;
  }

  private static void voegSpelerToe(String speler, Date eloDatum) {
    if (spelers.containsKey(speler)) {
      return;
    }

    var spelerId    = spelers.size();
    var spelerinfo  = new Spelerinfo();
    spelerinfo.setEerstePartij(eloDatum);
    spelerinfo.setElo(startElo);
    spelerinfo.setNaam(speler);
    spelerinfo.setPartijen(0);
    spelerinfo.setSpelerId(spelerId);
    spelerinfos.add(spelerId, spelerinfo);
    spelers.put(speler, spelerId);
  }
}
