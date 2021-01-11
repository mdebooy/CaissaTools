/*
 * Copyright (c) 2021 Marco de Booij
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
import eu.debooy.caissa.Spelerinfo;
import eu.debooy.doosutils.Arguments;
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.MailData;
import eu.debooy.doosutils.access.CsvBestand;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import java.io.File;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
  @author Marco de Booij
 */
public class StartCorrespondentie extends Batchjob {
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

    public static void execute(String[] args) {
    Banner.printMarcoBanner(
        resourceBundle.getString("banner.startcorrespondentie"));

    if (!setParameters(args)) {
      return;
    }

    String            date          = parameters.get(CaissaTools.PAR_DATE);
    String            event         = parameters.get(CaissaTools.PAR_EVENT);
    String            invoer        = parameters.get(PAR_INVOERDIR)
                                      + parameters.get(CaissaTools.PAR_BESTAND)
                                      + EXT_CSV;
    List<String>      email         = new ArrayList<>();
    List<String>      emailparams   = new ArrayList<>();
    List<String>      nieuwespelers = new ArrayList<>();
    String            site          = parameters.get(CaissaTools.PAR_SITE);
    List<Spelerinfo>  spelers       = new ArrayList<>();
    String            subject       = "Start " + event;
    String datum;
    try {
      datum  = Datum.fromDate(Datum.toDate(date,
                                           CaissaConstants.PGN_DATUM_FORMAAT),
                              DoosConstants.DATUM);
    } catch (ParseException e) {
      datum = date;
    }
    emailparams.add(0, event);
    emailparams.add(1, site);
    emailparams.add(2, datum);
    for (int i = 3; i < 11; i++) {
      emailparams.add(i, "");
    }
    if (parameters.containsKey(CaissaTools.PAR_BERICHT)) {
      String  bericht = parameters.get(CaissaTools.PAR_BERICHT);
      if (!bericht.contains(File.separator)) {
        bericht = parameters.get(PAR_UITVOERDIR) + bericht;
      }
      TekstBestand  message = null;
      try {
        message  = new TekstBestand.Builder()
                                .setBestand(bericht)
                                .setCharset(parameters.get(PAR_CHARSETIN))
                                .build();
        if (message.hasNext()) {
          subject = message.next();
        }
        while (message.hasNext()) {
          email.add(message.next());
        }
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
        subject = "Start " + event;
        email.add(resourceBundle.getString(CaissaTools.MSG_STARTTOERNOOI));
      } finally {
        try {
          if (message != null) {
            message.close();
          }
        } catch (BestandException e) {
          DoosUtils.foutNaarScherm(e.getLocalizedMessage());
        }
      }
    } else {
      email.add(resourceBundle.getString(CaissaTools.MSG_STARTTOERNOOI));
    }
    if (parameters.containsKey(CaissaTools.PAR_NIEUWESPELERS)) {
      nieuwespelers.addAll(
              Arrays.asList(parameters.get(CaissaTools.PAR_NIEUWESPELERS)
                                      .split(";")));
    }
    String            uitvoer = parameters.get(PAR_UITVOERDIR)
                                + parameters.get(CaissaTools.PAR_BESTAND)
                                + EXT_PGN;

    leesSpelers(invoer, spelers);
    int       noSpelers = spelers.size();
    String[]  rondes    = CaissaUtils.bergertabel(noSpelers);
    maakToernooi(spelers, rondes, event, site, date, uitvoer);

    if (parameters.containsKey(CaissaTools.PAR_SMTPSERVER)) {
      if (parameters.get(CaissaTools.PAR_PERPARTIJ)
                    .equals(DoosConstants.WAAR)) {
        for (String ronde : rondes) {
          String[]  partijen  = ronde.split(" ");
          for (String partij : partijen) {
            String[] speler = partij.split("-");
            int wit   = Integer.valueOf(speler[0])-1;
            int zwart = Integer.valueOf(speler[1])-1;
            if (wit < noSpelers
                && zwart < noSpelers) {
              Spelerinfo  witspeler   = spelers.get(wit);
              Spelerinfo  zwartspeler = spelers.get(zwart);
              if ((!parameters.containsKey(CaissaTools.PAR_NIEUWESPELERS)
                  || nieuwespelers.contains(witspeler.getNaam())
                  || nieuwespelers.contains(zwartspeler.getNaam()))) {
                stuurEmail(witspeler, zwartspeler, spelers, rondes, subject,
                           email, emailparams, getSession());
            }
            }
          }
        }
      } else {
        stuurEmail(spelers, rondes, subject, email, emailparams, getSession());
      }
    }

    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString("label.bestand"),
                             uitvoer));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  private static InternetAddress[] fillAddresses(Collection<String> addresses)
      throws AddressException {
    int             index     = 0;
    InternetAddress result[]  = new InternetAddress[addresses.size()];
    for (String address : addresses) {
      result[index]  = new InternetAddress(address);
      index++;
    }

    return result;
  }

  private static String formatLijn(String lijn, List<String> params) {
    return MessageFormat.format(lijn, params.toArray());
  }

  private static Session getSession() {
    Properties properties = System.getProperties();
    properties.setProperty("mail.smtp.host",
                           parameters.get(CaissaTools.PAR_SMTPSERVER));
    if (parameters.containsKey(CaissaTools.PAR_SMTPPOORT)) {
      properties.setProperty("mail.smtp.port",
                             parameters.get(CaissaTools.PAR_SMTPPOORT));
    }

    Session session = Session.getInstance(properties);
    if (null == session) {
      DoosUtils.foutNaarScherm(resourceBundle.getString("warn.session"));
      parameters.remove(CaissaTools.PAR_SMTPSERVER);
    }

    return session;
  }

  public static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar StartCorrespondentie "
                         + " --bestand=<"
                         + resourceBundle.getString("label.csvbestand") + ">");
    DoosUtils.naarScherm("    --date=<"
                         + resourceBundle.getString("label.date")
                         + "> --event=<"
                         + resourceBundle.getString("label.event")
                         + "> --site=<"
                         + resourceBundle.getString("label.site")
                         + ">");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_BERICHT, 14),
                         resourceBundle.getString("help.bericht"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_BESTAND, 14),
                         resourceBundle.getString("help.spelers.csv"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETIN, 14),
        MessageFormat.format(getMelding(HLP_CHARSETIN),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETUIT, 14),
        MessageFormat.format(getMelding(HLP_CHARSETUIT),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_DATE, 14),
                         resourceBundle.getString("help.date"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_ENKEL, 14),
                         resourceBundle.getString("help.enkel.simpel"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_EVENT, 14),
                         resourceBundle.getString("help.event"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_INVOERDIR, 14),
                         getMelding(HLP_INVOERDIR), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_NIEUWESPELERS, 14),
                         resourceBundle.getString("help.nieuwespelers"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_PERPARTIJ, 14),
                         resourceBundle.getString("help.perpartij"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_SITE, 14),
                         resourceBundle.getString("help.site"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_SMTPPOORT, 14),
                         resourceBundle.getString("help.smtppoort"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_SMTPSERVER, 14),
                         resourceBundle.getString("help.smtpserver"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_TSEMAIL, 14),
                         resourceBundle.getString("help.tsemail"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_UITVOERDIR, 14),
                         getMelding(HLP_UITVOERDIR), 80);
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(
        MessageFormat.format(
            getMelding(HLP_PARAMSVERPLICHT),
            CaissaTools.PAR_BESTAND, CaissaTools.PAR_DATE,
            CaissaTools.PAR_EVENT, CaissaTools.PAR_SITE),
            80);
    DoosUtils.naarScherm();
  }

  private static void leesSpelers(String invoer, List<Spelerinfo> spelers) {
    CsvBestand  csv = null;
    try {
      csv = new CsvBestand.Builder()
                          .setBestand(invoer)
                          .setHeader(true)
                          .setCharset(parameters.get(PAR_CHARSETIN))
                          .build();

      int spelerid  = 1;
      while (csv.hasNext()) {
        String[]    velden  = csv.next();
        Spelerinfo  speler  = new Spelerinfo();
        speler.setAlias(velden[2]);
        speler.setEmail(velden[1]);
        speler.setNaam(velden[0]);
        speler.setSpelerId(spelerid);
        spelers.add(speler);
        spelerid++;
      }

    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      if (null != csv) {
        try {
          csv.close();
        } catch (BestandException e) {
          DoosUtils.foutNaarScherm(e.getLocalizedMessage());
        }
      }
    }
  }

  private static String maakMessage(List<String> email,
                                    List<String> emailparams,
                                    List<Spelerinfo> spelers, String[] rondes) {
    String        speler  = emailparams.get(4);
    StringBuilder message = new StringBuilder();

    for (String lijn : email) {
      if (DoosUtils.telTeken(lijn, '@') > 1) {
        StringBuilder _lijn = new StringBuilder();
        while (DoosUtils.telTeken(lijn, '@') > 1) {
          int at  = lijn.indexOf('@');
          _lijn.append(lijn.substring(0, at));
          lijn    = lijn.substring(at+1);
          at      = lijn.indexOf('@');
          String  sublijn = lijn.substring(0, at);
          if (sublijn.contains("_")) {
            String[]  delen = sublijn.split("_");
            switch (delen[0].toLowerCase()) {
              case "metwit":
                _lijn.append(maakMessageMetwit(speler, delen[1], rondes,
                                               spelers, emailparams));
                break;
              case "metzwart":
                _lijn.append(maakMessageMetzwart(speler, delen[1], rondes,
                                                 spelers, emailparams));
                break;
              case "partijen":
                _lijn.append(maakMessagePartijen(delen[1], rondes, spelers,
                                                 emailparams));
                break;
              case "spelers":
                _lijn.append(maakMessageSpelers(delen[1], spelers,
                                                emailparams));
                break;
              default:
                break;
            }
          } else {
            _lijn.append(formatLijn(sublijn, emailparams));
          }
          message.append(formatLijn(_lijn.toString(), emailparams));
          lijn  = lijn.substring(at+1);
        }
        message.append(lijn);
      } else {
        message.append(formatLijn(lijn, emailparams));
      }
    }

    return message.toString();
  }

  private static String maakMessageMetwit(String to, String template,
                                          String[] rondes,
                                          List<Spelerinfo> spelers,
                                          List<String> emailparams) {
    int           noSpelers = spelers.size();
    StringBuilder resultaat = new StringBuilder();

    for (String ronde : rondes) {
      String[]  partijen  = ronde.split(" ");
      for (String partij : partijen) {
        String[] speler = partij.split("-");
        int wit   = Integer.valueOf(speler[0])-1;
        int zwart = Integer.valueOf(speler[1])-1;
        if (wit < noSpelers
            && zwart < noSpelers) {
          Spelerinfo  witspeler   = spelers.get(wit);
          Spelerinfo  zwartspeler = spelers.get(zwart);
          if (witspeler.getNaam().equals(to)) {
            emailparams.set(3,  zwartspeler.getVoornaam());
            emailparams.set(4,  zwartspeler.getNaam());
            emailparams.set(5,  zwartspeler.getAlias());
            emailparams.set(6,  zwartspeler.getEmail());
            emailparams.set(7,  witspeler.getVoornaam());
            emailparams.set(8,  witspeler.getNaam());
            emailparams.set(9,  witspeler.getAlias());
            emailparams.set(10, witspeler.getEmail());
            resultaat.append(formatLijn(template, emailparams)).append(" , ");
          }
        }
      }
    }

    return resultaat.toString()
                    .replaceAll(" , $", "")
                    .replaceFirst("(?s),(?!.*?,)",
                                  resourceBundle.getString("label.en"))
                    .replaceAll(" ,", ",");
  }

  private static String maakMessageMetzwart(String to, String template,
                                            String[] rondes,
                                            List<Spelerinfo> spelers,
                                            List<String> emailparams) {
    int           noSpelers = spelers.size();
    StringBuilder resultaat = new StringBuilder();

    for (String ronde : rondes) {
      String[]  partijen  = ronde.split(" ");
      for (String partij : partijen) {
        String[] speler = partij.split("-");
        int wit   = Integer.valueOf(speler[0])-1;
        int zwart = Integer.valueOf(speler[1])-1;
        if (wit < noSpelers
            && zwart < noSpelers) {
          Spelerinfo  witspeler   = spelers.get(wit);
          Spelerinfo  zwartspeler = spelers.get(zwart);
          if (zwartspeler.getNaam().equals(to)) {
            emailparams.set(3,  zwartspeler.getVoornaam());
            emailparams.set(4,  zwartspeler.getNaam());
            emailparams.set(5,  zwartspeler.getAlias());
            emailparams.set(6,  zwartspeler.getEmail());
            emailparams.set(7,  witspeler.getVoornaam());
            emailparams.set(8,  witspeler.getNaam());
            emailparams.set(9,  witspeler.getAlias());
            emailparams.set(10, witspeler.getEmail());
            resultaat.append(formatLijn(template, emailparams)).append(" , ");
          }
        }
      }
    }

    return resultaat.toString()
                    .replaceAll(" , $", "")
                    .replaceFirst("(?s),(?!.*?,)",
                                  resourceBundle.getString("label.en"))
                    .replaceAll(" ,", ",");
  }

  private static String maakMessagePartijen(String template, String[] rondes,
                                            List<Spelerinfo> spelers,
                                            List<String> emailparams) {
    int           noSpelers = spelers.size();
    StringBuilder resultaat = new StringBuilder();

    for (String ronde : rondes) {
      String[]  partijen  = ronde.split(" ");
      for (String partij : partijen) {
        String[] speler = partij.split("-");
        int wit   = Integer.valueOf(speler[0])-1;
        int zwart = Integer.valueOf(speler[1])-1;
        if (wit < noSpelers
            && zwart < noSpelers) {
          Spelerinfo  witspeler   = spelers.get(wit);
          Spelerinfo  zwartspeler = spelers.get(zwart);
          emailparams.set(3,  zwartspeler.getVoornaam());
          emailparams.set(4,  zwartspeler.getNaam());
          emailparams.set(5,  zwartspeler.getAlias());
          emailparams.set(6,  zwartspeler.getEmail());
          emailparams.set(7,  witspeler.getVoornaam());
          emailparams.set(8,  witspeler.getNaam());
          emailparams.set(9,  witspeler.getAlias());
          emailparams.set(10, witspeler.getEmail());
          resultaat.append(formatLijn(template, emailparams));
        }
      }
    }

    return resultaat.toString();
  }

  private static String maakMessageSpelers(String template,
                                           List<Spelerinfo> spelers,
                                           List<String> emailparams) {
    StringBuilder resultaat = new StringBuilder();

    emailparams.set(7,  "");
    emailparams.set(8,  "");
    emailparams.set(9,  "");
    emailparams.set(10, "");

    for (Spelerinfo speler : spelers) {
      emailparams.set(3,  speler.getVoornaam());
      emailparams.set(4,  speler.getNaam());
      emailparams.set(5,  speler.getAlias());
      emailparams.set(6,  speler.getEmail());
      resultaat.append(formatLijn(template, emailparams));
    }

    return resultaat.toString();
  }

  private static void maakToernooi(List<Spelerinfo> spelers, String[] rondes,
                                   String event, String site, String date,
                                   String uitvoer) {
    int       noSpelers = spelers.size();

    TekstBestand  output  = null;
    try {
      output  = new TekstBestand.Builder()
                                .setBestand(uitvoer)
                                .setCharset(parameters.get(PAR_CHARSETUIT))
                                .setLezen(false).build();
      for (String ronde : rondes) {
        for (String partij : ronde.split(" ")) {
          String[]  paring  = partij.split("-");
          int       wit     = Integer.valueOf(paring[0]) - 1;
          int       zwart   = Integer.valueOf(paring[1]) - 1;
          if (wit != noSpelers
              && zwart != noSpelers) {
            String    witspeler   = spelers.get(wit).getNaam();
            String    zwartspeler = spelers.get(zwart).getNaam();
            schrijfPartij(output, event, site, date, witspeler, zwartspeler);
            if (parameters.get(CaissaTools.PAR_ENKEL)
                          .equals(DoosConstants.ONWAAR)) {
              schrijfPartij(output, event, site, date, zwartspeler, witspeler);
            }
          }
        }
      }
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    } finally {
      try {
        if (output != null) {
          output.close();
        }
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      }
    }
  }

  private static void schrijfPartij(TekstBestand output, String event,
                                    String site, String date, String witspeler,
                                    String zwartspeler)
      throws BestandException {
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_EVENT, event));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_SITE, site));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_DATE, date));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_ROUND, "-"));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_WHITE, witspeler));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_BLACK,
                                      zwartspeler));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_RESULT, "*"));
    output.write(MessageFormat.format(CaissaConstants.FMT_PGNTAG,
                                      CaissaConstants.PGNTAG_EVENTDATE, date));
    output.write("");
    output.write("*");
    output.write("");
  }

  private static void sendEmail(MailData maildata, Session session) {
        try {
      MimeMessage msg = new MimeMessage(session);
      msg.setHeader("Content-Type", maildata.getContentType());

      msg.setFrom(new InternetAddress(maildata.getFrom()));

      if (maildata.getToSize() > 0) {
        msg.setRecipients(MimeMessage.RecipientType.TO,
                          fillAddresses(maildata.getTo().values()));
      }

      if (maildata.getCcSize() > 0) {
        msg.setRecipients(MimeMessage.RecipientType.CC,
                          fillAddresses(maildata.getCc().values()));
      }

      if (maildata.getBccSize() > 0) {
        msg.setRecipients(MimeMessage.RecipientType.BCC,
                          fillAddresses(maildata.getBcc().values()));
      }

      msg.setSubject(maildata.getSubject());
      msg.setSentDate(maildata.getSentDate());
      msg.setContent(maildata.getMessage(), maildata.getContentType());

      if (maildata.getHeaderSize() > 0) {
        Map<String, String> headers  = maildata.getHeader();
        for (String key : maildata.getHeader().keySet()) {
          msg.setHeader(key, headers.get(key));
        }
      }

      Transport.send(msg);
    } catch (MessagingException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }
  }

  private static boolean setParameters(String[] args) {
    Arguments     arguments = new Arguments(args);
    List<String>  fouten    = new ArrayList<>();

    arguments.setParameters(new String[] {CaissaTools.PAR_BERICHT,
                                          CaissaTools.PAR_BESTAND,
                                          PAR_CHARSETIN,
                                          PAR_CHARSETUIT,
                                          CaissaTools.PAR_DATE,
                                          CaissaTools.PAR_ENKEL,
                                          CaissaTools.PAR_EVENT,
                                          PAR_INVOERDIR,
                                          CaissaTools.PAR_NIEUWESPELERS,
                                          CaissaTools.PAR_PERPARTIJ,
                                          CaissaTools.PAR_SITE,
                                          CaissaTools.PAR_SMTPPOORT,
                                          CaissaTools.PAR_SMTPSERVER,
                                          CaissaTools.PAR_SPELERS,
                                          CaissaTools.PAR_TSEMAIL,
                                          PAR_UITVOERDIR});
    arguments.setVerplicht(new String[] {CaissaTools.PAR_BESTAND,
                                         CaissaTools.PAR_DATE,
                                         CaissaTools.PAR_EVENT,
                                         CaissaTools.PAR_SITE});
    if (!arguments.isValid()) {
      fouten.add(getMelding(ERR_INVALIDPARAMS));
    }

    parameters.clear();
    setParameter(arguments, CaissaTools.PAR_BERICHT);
    setBestandParameter(arguments, CaissaTools.PAR_BESTAND, EXT_PGN);
    setParameter(arguments, PAR_CHARSETIN, Charset.defaultCharset().name());
    setParameter(arguments, PAR_CHARSETUIT, Charset.defaultCharset().name());
    setParameter(arguments, CaissaTools.PAR_DATE);
    setParameter(arguments, CaissaTools.PAR_ENKEL, DoosConstants.WAAR);
    setParameter(arguments, CaissaTools.PAR_ENKEL);
    setParameter(arguments, CaissaTools.PAR_EVENT);
    setDirParameter(arguments, PAR_INVOERDIR);
    setParameter(arguments, CaissaTools.PAR_NIEUWESPELERS);
    setParameter(arguments, CaissaTools.PAR_PERPARTIJ, DoosConstants.ONWAAR);
    setParameter(arguments, CaissaTools.PAR_SMTPPOORT);
    setParameter(arguments, CaissaTools.PAR_SMTPSERVER);
    setParameter(arguments, CaissaTools.PAR_SITE);
    setParameter(arguments, CaissaTools.PAR_SPELERS);
    setParameter(arguments, CaissaTools.PAR_TSEMAIL);
    setDirParameter(arguments, PAR_UITVOERDIR, getParameter(PAR_INVOERDIR));

    String enkel  = parameters.get(CaissaTools.PAR_ENKEL);
    if (!enkel.equals(DoosConstants.WAAR)
        && !enkel.equals(DoosConstants.ONWAAR)) {
      setParameter(CaissaTools.PAR_ENKEL, DoosConstants.WAAR);
    }

    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_BESTAND))
                 .contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              getMelding(ERR_BEVATDIRECTORY), CaissaTools.PAR_BESTAND));
    }
    if (parameters.containsKey(CaissaTools.PAR_SMTPSERVER)
        && !parameters.containsKey(CaissaTools.PAR_TSEMAIL)) {
      fouten.add(resourceBundle.getString(CaissaTools.ERR_TSEMAIL));
    }

    if (fouten.isEmpty()) {
      return true;
    }

    help();
    printFouten(fouten);

    return false;
  }

  private static void stuurEmail(List<Spelerinfo> spelers, String[] rondes,
                                 String subject, List<String> email,
                                 List<String> emailparams, Session session ) {
    for (Spelerinfo speler : spelers) {
      MailData  maildata  = new MailData();
      maildata.addTo(speler.getEmail());
      maildata.setFrom(parameters.get(CaissaTools.PAR_TSEMAIL));
      maildata.setSubject(formatLijn(subject, emailparams));
      emailparams.set(3,  speler.getVoornaam());
      emailparams.set(4,  speler.getNaam());
      emailparams.set(5,  speler.getAlias());
      emailparams.set(6,  speler.getEmail());
      emailparams.set(7,  "");
      emailparams.set(8,  "");
      emailparams.set(9,  "");
      emailparams.set(10, "");
      maildata.setMessage(maakMessage(email, emailparams, spelers, rondes));
      System.out.println(
          MessageFormat.format(resourceBundle.getString("label.email"),
                               speler.getNaam(), "-"));
      sendEmail(maildata, session);
    }
  }

  private static void stuurEmail(Spelerinfo wit, Spelerinfo zwart,
                                 List<Spelerinfo> spelers, String[] rondes,
                                 String subject, List<String> email,
                                 List<String> emailparams, Session session ) {
    MailData  maildata  = new MailData();
    maildata.addTo(zwart.getEmail());
    maildata.addCc(wit.getEmail());
    maildata.setFrom(parameters.get(CaissaTools.PAR_TSEMAIL));
    maildata.setSubject(formatLijn(subject, emailparams));
    emailparams.set(3,  zwart.getVoornaam());
    emailparams.set(4,  zwart.getNaam());
    emailparams.set(5,  zwart.getAlias());
    emailparams.set(6,  zwart.getEmail());
    emailparams.set(7,  wit.getVoornaam());
    emailparams.set(8,  wit.getNaam());
    emailparams.set(9,  wit.getAlias());
    emailparams.set(10, wit.getEmail());
    maildata.setMessage(maakMessage(email, emailparams, spelers, rondes));
    System.out.println(
        MessageFormat.format(resourceBundle.getString("label.email"),
                             zwart.getNaam(), wit.getNaam()));
    sendEmail(maildata, session);
  }
}
