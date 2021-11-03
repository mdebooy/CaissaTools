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
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.access.CsvBestand;
import eu.debooy.doosutils.exception.BestandException;
import java.io.File;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
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
  private static final  int           START_ELO   = 1600;
  private static final  String[]      KOLOMMEN    =
      new String[]{"speler","elo","groei","partijen","eerstePartij",
                   "laatstePartij","eersteEloDatum","minElo","minEloDatum",
                   "maxElo","maxEloDatum"};
  private static final  String        TXT_BANNER  = "banner.eloberekenaar";
  private static final  List<String>  UITSLAGEN   = new ArrayList<>();

  private static  String      eindDatum;
  private static  boolean     extraInfo;
  private static  CsvBestand  geschiedenis;
  private static  Integer     kFactor;
  private static  Integer     maxVerschil;
  private static  String      startDatum;
  private static  int         startElo;
  private static  int         verwerkt;

  private static final  ResourceBundle    resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());
  private static final  List<Spelerinfo>  spelerinfos     = new ArrayList<>();
  private static final  Map<String, Integer>
                                          spelers         = new TreeMap<>();

  private ELOBerekenaar() {}

  public static void execute(String[] args) {
    init();

    Banner.printMarcoBanner(resourceBundle.getString(TXT_BANNER));

    if (!setParameters(args)) {
      return;
    }

    if (parameters.containsKey(CaissaTools.PAR_STARTELO)) {
      startElo    =
          Integer.parseInt(parameters.get(CaissaTools.PAR_STARTELO));
    }
    if (parameters.containsKey(CaissaTools.PAR_VASTEKFACTOR)) {
      kFactor =
          Integer.parseInt(parameters.get(CaissaTools.PAR_VASTEKFACTOR));
    }
    if (parameters.containsKey(CaissaTools.PAR_MAXVERSCHIL)) {
      maxVerschil =
          Integer.parseInt(parameters.get(CaissaTools.PAR_MAXVERSCHIL));
    }

    extraInfo = parameters.get(CaissaTools.PAR_EXTRAINFO)
                          .equals(DoosConstants.WAAR);
    String  geschiedenisbestand =
        parameters.get(PAR_UITVOERDIR)
        + parameters.get(CaissaTools.PAR_GESCHIEDENISBESTAND) + EXT_CSV;
    String  toernooibestand     =
        parameters.get(PAR_INVOERDIR)
        + parameters.get(CaissaTools.PAR_TOERNOOIBESTAND) + EXT_PGN;
    String  spelerbestand       =
        parameters.get(PAR_UITVOERDIR)
        + parameters.get(CaissaTools.PAR_SPELERBESTAND) + EXT_CSV;

    startDatum  = leesSpelers(spelerbestand);
    int aantalPartijen  = verwerkToernooi(toernooibestand, geschiedenisbestand);

    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.bestand"),
                             spelerbestand));
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.startdatum"),
                             startDatum));
    if (!CaissaConstants.DEF_EINDDATUM.equals(eindDatum)) {
      DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.einddatum"),
                             eindDatum));
    }
    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.partijen"),
                             aantalPartijen));
    if (verwerkt > 0) {
      schrijfSpelers(spelers, spelerinfos, spelerbestand,
                     parameters.get(PAR_CHARSETUIT));
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

  public static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar ELOBerekenaar ["
                         + getMelding(LBL_OPTIE) + "] \\");
    DoosUtils.naarScherm("    --spelerBestand=<"
                         + resourceBundle.getString("label.csvbestand")
                         + "> --toernooiBestand=<"
                         + resourceBundle.getString("label.pgnbestand") + ">");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETIN, 20),
        MessageFormat.format(getMelding(HLP_CHARSETIN),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETUIT, 20),
        MessageFormat.format(getMelding(HLP_CHARSETUIT),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_EINDDATUM, 20),
                         resourceBundle.getString("help.einddatum"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_EXTRAINFO, 20),
                         resourceBundle.getString("help.extrainfo"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_GESCHIEDENISBESTAND,
                                           20),
                         resourceBundle.getString("help.geschiedenisbestand"),
                         80);
    DoosUtils.naarScherm(getParameterTekst(PAR_INVOERDIR, 20),
                         getMelding(HLP_INVOERDIR), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_MAXVERSCHIL, 20),
        MessageFormat.format(resourceBundle.getString("help.maxverschil"),
                             ELO.MAX_VERSCHIL), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_SPELERBESTAND, 20),
                         resourceBundle.getString("help.spelerbestand"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_STARTDATUM, 20),
                         resourceBundle.getString("help.startdatum"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_STARTELO, 20),
        MessageFormat.format(resourceBundle.getString("help.startelo"),
                             START_ELO), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_TOERNOOIBESTAND, 20),
                         resourceBundle.getString("help.toernooibestand"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_UITVOERDIR, 20),
                         getMelding(HLP_UITVOERDIR), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_VASTEKFACTOR, 20),
                         resourceBundle.getString("help.vastekfactor"), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(getMelding(HLP_PARAMSVERPLICHT),
                             CaissaTools.PAR_SPELERBESTAND,
                             CaissaTools.PAR_TOERNOOIBESTAND), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(resourceBundle.getString("help.eloberekenaar.extra"),
                         80);
    DoosUtils.naarScherm();
  }

  private static void init() {
    UITSLAGEN.clear();
    UITSLAGEN.add(CaissaConstants.PARTIJ_ZWART_WINT);
    UITSLAGEN.add(CaissaConstants.PARTIJ_REMISE);
    UITSLAGEN.add(CaissaConstants.PARTIJ_WIT_WINT);
    UITSLAGEN.add(CaissaConstants.PARTIJ_BEZIG);

    geschiedenis  = null;
    kFactor       = null;
    maxVerschil   = ELO.MAX_VERSCHIL;
    spelerinfos.clear();
    spelers.clear();
    startElo      = START_ELO;
    verwerkt      = 0;
  }

  private static String leesSpelers(String spelerBestand) {
    String      laatsteDatum  = startDatum;
    CsvBestand  invoer        = null;
    try {
      Calendar  calendar  = Calendar.getInstance();
      // Is eigenlijk een uitvoer.
      invoer  = new CsvBestand.Builder()
                              .setBestand(spelerBestand)
                              .setCharset(parameters.get(PAR_CHARSETUIT))
                              .build();
      while (invoer.hasNext()) {
        String[]    veld        = invoer.next();
        int         spelerId    = spelers.size();
        spelers.put(veld[0], spelerId);
        Spelerinfo  spelerinfo  = new Spelerinfo();
        spelerinfo.setNaam(veld[0]);
        spelerinfo.setElo(Integer.valueOf(veld[1]));
        spelerinfo.setElogroei(Integer.valueOf(veld[2]));
        spelerinfo.setPartijen(Integer.valueOf(veld[3]));
        spelerinfo.setEerstePartij(Datum.toDate(veld[4],
                                   CaissaConstants.PGN_DATUM_FORMAAT));
        spelerinfo.setLaatstePartij(Datum.toDate(veld[5],
                                    CaissaConstants.PGN_DATUM_FORMAAT));
        if (spelerinfo.getPartijen() >= ELO.MIN_PARTIJEN) {
          spelerinfo.setOfficieel(Datum.toDate(veld[6],
                                  CaissaConstants.PGN_DATUM_FORMAAT));
          spelerinfo.setMinElo(Integer.valueOf(veld[7]));
          spelerinfo.setMinDatum(Datum.toDate(veld[8],
                                 CaissaConstants.PGN_DATUM_FORMAAT));
          spelerinfo.setMaxElo(Integer.valueOf(veld[9]));
          spelerinfo.setMaxDatum(Datum.toDate(veld[10],
                                 CaissaConstants.PGN_DATUM_FORMAAT));
        }
        spelerinfo.setSpelerId(spelerId);
        spelerinfos.add(spelerId, spelerinfo);
        if (veld[5].compareTo(laatsteDatum) >= 0) {
          calendar.setTime(spelerinfo.getLaatstePartij());
          calendar.add(Calendar.DATE, 1);
          laatsteDatum  = Datum.fromDate(calendar.getTime(),
                                         CaissaConstants.PGN_DATUM_FORMAAT);
        }
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
    } finally {
      try {
        if (invoer != null) {
          invoer.close();
        }
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }

    return laatsteDatum;
  }

  private static void pasSpelerAan(int id, List<Spelerinfo> spelerinfos,
                                   Date eloDatum, Integer andereElo,
                                   int uitslag) {
    Integer vorigeElo = spelerinfos.get(id).getElo();
    int     aantal    = spelerinfos.get(id).getPartijen();
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

  private static void schrijfSpelers(Map<String, Integer> spelers,
                                     List<Spelerinfo> spelerinfos,
                                     String spelerBestand, String charsetUit) {
    CsvBestand  csvBestand  = null;
    try {
      csvBestand  = new CsvBestand.Builder().setBestand(spelerBestand)
                                            .setCharset(charsetUit)
                                            .setLezen(false)
                                            .setKolomNamen(KOLOMMEN)
                                            .build();

      Object[]  velden  = new Object[KOLOMMEN.length];
      for (Integer spelerId  : spelers.values()) {
        velden[0] = spelerinfos.get(spelerId).getNaam();
        velden[1] = spelerinfos.get(spelerId).getElo();
        velden[2] = spelerinfos.get(spelerId).getElogroei();
        velden[3] = spelerinfos.get(spelerId).getPartijen();
        velden[4] = Datum.fromDate(spelerinfos.get(spelerId).getEerstePartij(),
                                   CaissaConstants.PGN_DATUM_FORMAAT);
        velden[5] = Datum.fromDate(spelerinfos.get(spelerId).getLaatstePartij(),
                                   CaissaConstants.PGN_DATUM_FORMAAT);
        if (spelerinfos.get(spelerId).getPartijen() < ELO.MIN_PARTIJEN) {
          velden[6]   = "";
          velden[7]   = "";
          velden[8]   = "";
          velden[9]   = "";
          velden[10]  = "";
        } else {
          velden[6]   = Datum.fromDate(spelerinfos.get(spelerId).getOfficieel(),
                                       CaissaConstants.PGN_DATUM_FORMAAT);
          velden[7]   = spelerinfos.get(spelerId).getMinElo();
          velden[8]   = Datum.fromDate(spelerinfos.get(spelerId).getMinDatum(),
                                       CaissaConstants.PGN_DATUM_FORMAAT);
          velden[9]   = spelerinfos.get(spelerId).getMaxElo();
          velden[10]  = Datum.fromDate(spelerinfos.get(spelerId).getMaxDatum(),
                                       CaissaConstants.PGN_DATUM_FORMAAT);
        }
        csvBestand.write(velden);
      }
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (null != csvBestand) {
          csvBestand.close();
        }
      } catch (BestandException ex) {
        DoosUtils.foutNaarScherm(ex.getLocalizedMessage());
      }
    }
  }

  private static boolean setParameters(String[] args) {
    Arguments     arguments = new Arguments(args);
    List<String>  fouten    = new ArrayList<>();

    arguments.setParameters(new String[] {PAR_CHARSETIN,
                                          PAR_CHARSETUIT,
                                          CaissaTools.PAR_EINDDATUM,
                                          CaissaTools.PAR_EXTRAINFO,
                                          CaissaTools.PAR_GESCHIEDENISBESTAND,
                                          PAR_INVOERDIR,
                                          CaissaTools.PAR_MAXVERSCHIL,
                                          CaissaTools.PAR_SPELERBESTAND,
                                          CaissaTools.PAR_STARTDATUM,
                                          CaissaTools.PAR_STARTELO,
                                          CaissaTools.PAR_TOERNOOIBESTAND,
                                          PAR_UITVOERDIR,
                                          CaissaTools.PAR_VASTEKFACTOR});
    arguments.setVerplicht(new String[] {CaissaTools.PAR_SPELERBESTAND,
                                         CaissaTools.PAR_TOERNOOIBESTAND});
    if (!arguments.isValid()) {
      fouten.add(getMelding(ERR_INVALIDPARAMS));
    }

    parameters.clear();
    setParameter(arguments, PAR_CHARSETIN, Charset.defaultCharset().name());
    setParameter(arguments, PAR_CHARSETUIT, Charset.defaultCharset().name());
    setParameter(arguments, CaissaTools.PAR_EINDDATUM);
    setParameter(arguments, CaissaTools.PAR_EXTRAINFO, DoosConstants.ONWAAR);
    setDirParameter(arguments, PAR_INVOERDIR);
    setParameter(arguments, CaissaTools.PAR_MAXVERSCHIL);
    setBestandParameter(arguments, CaissaTools.PAR_SPELERBESTAND, EXT_CSV);
    setParameter(arguments, CaissaTools.PAR_STARTDATUM);
    setParameter(arguments, CaissaTools.PAR_STARTELO);
    setBestandParameter(arguments, CaissaTools.PAR_TOERNOOIBESTAND, EXT_PGN);
    setDirParameter(arguments, PAR_UITVOERDIR, getParameter(PAR_INVOERDIR));
    setParameter(arguments, CaissaTools.PAR_VASTEKFACTOR);
    if (arguments.hasArgument(CaissaTools.PAR_GESCHIEDENISBESTAND)) {
      setBestandParameter(arguments, CaissaTools.PAR_GESCHIEDENISBESTAND,
                    EXT_CSV);
    } else {
      setParameter(CaissaTools.PAR_GESCHIEDENISBESTAND,
                   parameters.get(CaissaTools.PAR_SPELERBESTAND) + "H");
    }

    for (String parameter : new String[] {CaissaTools.PAR_GESCHIEDENISBESTAND,
                                          CaissaTools.PAR_SPELERBESTAND,
                                          CaissaTools.PAR_TOERNOOIBESTAND}) {
      if (DoosUtils.nullToEmpty(parameters.get(parameter))
                   .contains(File.separator)) {
        fouten.add(
            MessageFormat.format(getMelding(ERR_BEVATDIRECTORY), parameter));
      }
    }
    if (parameters.containsKey(CaissaTools.PAR_MAXVERSCHIL)
        && !parameters.containsKey(CaissaTools.PAR_VASTEKFACTOR)) {
      fouten.add(resourceBundle.getString(CaissaTools.ERR_MAXVERSCHIL));
    }

    eindDatum   =
        DoosUtils.nullToValue(parameters.get(CaissaTools.PAR_EINDDATUM),
                              CaissaConstants.DEF_EINDDATUM);
    startDatum  =
        DoosUtils.nullToValue(parameters.get(CaissaTools.PAR_STARTDATUM),
                              CaissaConstants.DEF_STARTDATUM);
    if (eindDatum.compareTo(startDatum) < 0) {
      fouten.add(
          MessageFormat.format(
              resourceBundle.getString(CaissaTools.ERR_EINDVOORSTART),
                                       startDatum, eindDatum));
    }

    if (fouten.isEmpty()) {
      return true;
    }

    help();
    printFouten(fouten);

    return false;
  }

  private static int verwerkPartij(PGN partij)
      throws BestandException {
    String  datum     = partij.getTag(CaissaConstants.PGNTAG_DATE);
    Date    eloDatum;

    if (startDatum.compareTo(datum) > 0
        || eindDatum.compareTo(datum) < 0) {
      return 0;
    }

    String  wit       = partij.getTag(CaissaConstants.PGNTAG_WHITE);
    String  zwart     = partij.getTag(CaissaConstants.PGNTAG_BLACK);
    String  resultaat = partij.getTag(CaissaConstants.PGNTAG_RESULT);
    int     uitslag   = UITSLAGEN.indexOf(resultaat);
    try {
      eloDatum  =
          Datum.toDate(datum, CaissaConstants.PGN_DATUM_FORMAAT);
    } catch (ParseException e) {
      DoosUtils.foutNaarScherm(
          MessageFormat.format(
              resourceBundle.getString(CaissaTools.ERR_FOUTEDATUM),
              datum) + " [" + e.getLocalizedMessage() + "].");
      eloDatum  = null;
    }

    if (uitslag > 2) {
      return 0;
    }

    voegSpelerToe(wit, eloDatum);
    voegSpelerToe(zwart, eloDatum);

    int     witId     = spelers.get(wit);
    int     zwartId   = spelers.get(zwart);
    Integer witElo    = spelerinfos.get(witId).getElo();
    Integer zwartElo  = spelerinfos.get(zwartId).getElo();
    pasSpelerAan(witId,   spelerinfos, eloDatum, zwartElo, uitslag);
    pasSpelerAan(zwartId, spelerinfos, eloDatum, witElo, 2 - uitslag);
    if (null != geschiedenis) {
      if (extraInfo) {
        geschiedenis.write(wit, datum,
                           spelerinfos.get(witId).getElo(),
                           spelerinfos.get(witId).getPartijen(),
                           spelerinfos.get(witId).getElo() - witElo, zwart,
                           partij.getTag(CaissaConstants.PGNTAG_EVENT));
        geschiedenis.write(zwart, datum,
                           spelerinfos.get(zwartId).getElo(),
                           spelerinfos.get(zwartId).getPartijen(),
                           spelerinfos.get(zwartId).getElo() - zwartElo, wit,
                           partij.getTag(CaissaConstants.PGNTAG_EVENT));
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

  private static int verwerkToernooi(String toernooibestand,
                                     String geschiedenisbestand) {
    int aantalPartijen  = 0;

    try {
      geschiedenis  = new CsvBestand.Builder().setBestand(geschiedenisbestand)
                                              .setLezen(false)
                                              .setHeader(false)
                                              .setAppend(true)
                                              .build();
      Collection<PGN>
          partijen  = new TreeSet<>(new PGN.DefaultComparator());
      partijen.addAll(
          CaissaUtils.laadPgnBestand(toernooibestand,
                                     parameters.get(PAR_CHARSETIN)));
      for (PGN partij : partijen) {
        if (!partij.isBye()
            && partij.isRated()) {
          verwerkt  += verwerkPartij(partij);
        }
      }
      aantalPartijen  = partijen.size();
    } catch (BestandException | PgnException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (geschiedenis != null) {
          geschiedenis.close();
        }
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }

    return aantalPartijen;
  }

  private static void voegSpelerToe(String speler, Date eloDatum) {
    if (spelers.containsKey(speler)) {
      return;
    }

    int spelerId  = spelers.size();
    spelers.put(speler, spelerId);
    Spelerinfo  spelerinfo  = new Spelerinfo();
    spelerinfo.setEerstePartij(eloDatum);
    spelerinfo.setElo(startElo);
    spelerinfo.setNaam(speler);
    spelerinfo.setPartijen(0);
    spelerinfo.setSpelerId(spelerId);
    spelerinfos.add(spelerId, spelerinfo);
  }
}
