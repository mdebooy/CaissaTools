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
import eu.debooy.doosutils.Banner;
import eu.debooy.doosutils.Batchjob;
import eu.debooy.doosutils.Datum;
import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.DoosUtils;
import eu.debooy.doosutils.MailData;
import eu.debooy.doosutils.ParameterBundle;
import eu.debooy.doosutils.access.BestandConstants;
import eu.debooy.doosutils.access.JsonBestand;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import static javax.mail.Message.RecipientType.BCC;
import static javax.mail.Message.RecipientType.CC;
import static javax.mail.Message.RecipientType.TO;
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
      ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                               Locale.getDefault());

  private static  List<String>      email;
  private static  List<String>      emailparams;
  private static  List<String>      nieuwespelers;
  private static  List<Spelerinfo>  spelers;

  private static  String[]  rondes;

  public static void execute(String[] args) {
    setParameterBundle(new ParameterBundle.Builder()
                           .setBaseName(CaissaTools.TOOL_STARTCORRESP)
                           .build());

    Banner.printMarcoBanner(DoosUtils.nullToEmpty(paramBundle.getBanner()));

    if (!paramBundle.isValid()
        || !paramBundle.setArgs(args)) {
      help();
      printFouten();
      return;
    }

    email         = new ArrayList<>();
    emailparams   = new ArrayList<>();
    nieuwespelers = new ArrayList<>();
    spelers       = new ArrayList<>();

    var subject   = leesBericht();

    if (paramBundle.containsParameter(CaissaTools.PAR_NIEUWESPELERS)) {
      nieuwespelers.addAll(
          Arrays.asList(paramBundle.getString(CaissaTools.PAR_NIEUWESPELERS)
                                  .split(";")));
    }

    JsonBestand competitie;
    try {
      competitie  =
          new JsonBestand.Builder()
                         .setBestand(paramBundle
                                        .getBestand(CaissaTools.PAR_SCHEMA))
                         .setCharset(paramBundle.getString(PAR_CHARSETIN))
                         .build();
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    var date    = competitie.get(CaissaConstants.JSON_TAG_EVENTDATE).toString();
    var datum   = getDatum(date);
    var enkel   = true;
    if (competitie.containsKey(CaissaTools.PAR_ENKELRONDIG)) {
      enkel = (boolean) competitie.get(CaissaTools.PAR_ENKELRONDIG);
    }
    var event   = competitie.get(CaissaTools.PAR_EVENT).toString();
    var site    = competitie.get(CaissaTools.PAR_SITE).toString();

    initEmailparams(13);
    emailparams.add(0, event);
    emailparams.add(1, site);
    emailparams.add(2, datum);

    CaissaUtils.vulSpelers(spelers,
                    competitie.getArray(CaissaConstants.JSON_TAG_SPELERS));
    var noSpelers = spelers.size();

    var uitvoer   = paramBundle.getString(PAR_UITVOERDIR) + event
                      + BestandConstants.EXT_PGN;

    rondes  = CaissaUtils.bergertabel(noSpelers);
    maakToernooi(event, site, date, enkel, uitvoer);

    if (paramBundle.containsParameter(CaissaTools.PAR_SMTPSERVER)) {
      if (paramBundle.getBoolean(CaissaTools.PAR_PERPARTIJ)) {
        stuurPerPartij(subject);
      } else {
        stuurPerSpeler(subject);
      }
      DoosUtils.naarScherm();
    }

    DoosUtils.naarScherm(
        MessageFormat.format(resourceBundle.getString(CaissaTools.LBL_BESTAND),
                             uitvoer));
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getMelding(MSG_KLAAR));
    DoosUtils.naarScherm();
  }

  private static InternetAddress[] fillAddresses(Collection<String> addresses)
      throws AddressException {
    var index   = 0;
    var result  = new InternetAddress[addresses.size()];
    for (var address : addresses) {
      result[index]  = new InternetAddress(address);
      index++;
    }

    return result;
  }

  private static String formatSpelerlijst(String lijst) {
    return lijst.replaceAll(" , $", "")
                .replaceFirst("(?s),(?!.*?,)",
                              resourceBundle.getString("label.en"))
                .replace(" ,", ",");
  }

  private static String formatLijn(String lijn) {
    return MessageFormat.format(lijn, emailparams.toArray());
  }

  private static String getDatum(String date) {
    String  datum;
    try {
      datum  = Datum.fromDate(Datum.toDate(date,
                                           CaissaConstants.PGN_DATUM_FORMAAT),
                              DoosConstants.DATUM);
    } catch (ParseException e) {
      datum = date;
    }

    return datum;
  }

  private static Session getSession() {
    var properties  = System.getProperties();
    properties.setProperty("mail.smtp.host",
                           paramBundle.getString(CaissaTools.PAR_SMTPSERVER));
    if (paramBundle.containsParameter(CaissaTools.PAR_SMTPPOORT)) {
      properties.setProperty("mail.smtp.port",
                             paramBundle.getString(CaissaTools.PAR_SMTPPOORT));
    }

    return Session.getInstance(properties);
  }

  private static String getTekst(String lijn) {
    if (!lijn.startsWith("#")) {
      return lijn;
    }

    if (DoosUtils.telTeken(lijn, '#') != 2) {
      return "";
    }

    var parameter = lijn.substring(1).split("#")[0];
    switch (parameter) {
      case "geennieuwespelers":
        if (!paramBundle.containsParameter(CaissaTools.PAR_NIEUWESPELERS)) {
          return lijn.substring(19);
        }
        break;
      case "metzwart":
        if (Integer.valueOf(emailparams.get(12)) > 0) {
          return lijn.substring(10);
        }
        break;
      case "nieuwespelers":
        if (paramBundle.containsParameter(CaissaTools.PAR_NIEUWESPELERS)) {
          return lijn.substring(15);
        }
        break;
      default:
        break;
    }

    return "";
  }

  private static void initEmailparams(int params) {
    for (var i = 0; i < params; i++) {
      emailparams.add(i, "");
    }
  }

  private static boolean isUitdaging(String wit, String zwart) {
    return nieuwespelers.isEmpty()
            || nieuwespelers.contains(wit)
            || nieuwespelers.contains(zwart);
  }

  private static String leesBericht() {
    var subject = "Start " + paramBundle.getString(CaissaTools.PAR_EVENT);
    if (paramBundle.containsParameter(CaissaTools.PAR_BERICHT)) {
      try (var message  =
            new TekstBestand.Builder()
                            .setBestand(paramBundle
                                            .getString(CaissaTools.PAR_BERICHT))
                            .setCharset(paramBundle.getString(PAR_CHARSETIN))
                            .build()) {
        if (message.hasNext()) {
          subject = message.next();
        }
        while (message.hasNext()) {
          email.add(message.next());
        }
      } catch (BestandException e) {
        DoosUtils.foutNaarScherm(e.getLocalizedMessage());
        email.add(resourceBundle.getString(CaissaTools.MSG_STARTTOERNOOI));
      }
    } else {
      email.add(resourceBundle.getString(CaissaTools.MSG_STARTTOERNOOI));
    }

    return subject;
  }

  private static String maakLijstNieuweSpelers() {
    if (nieuwespelers.isEmpty()) {
      return "";
    }

    var     resultaat = new StringBuilder();
    String  lijn;

    if (nieuwespelers.size() == 1) {
      lijn  = resourceBundle.getString(CaissaTools.MSG_NIEUWESPELER);
    } else {
      lijn  = resourceBundle.getString(CaissaTools.MSG_NIEUWESPELERS);
    }
    nieuwespelers.forEach(speler ->
            resultaat.append(speler.split(",")[1].trim()).append(" , "));

    return MessageFormat.format(lijn, formatSpelerlijst(resultaat.toString()));
  }

  private static String maakMessage() {
    var speler  = emailparams.get(4);
    var message = new StringBuilder();

    for (var lijn : email) {
      lijn  = getTekst(lijn);
      if (DoosUtils.isNotBlankOrNull(lijn)) {
        if (DoosUtils.telTeken(lijn, '@') > 1) {
          var hulplijn = new StringBuilder();
          while (DoosUtils.telTeken(lijn, '@') > 1) {
            var at      = lijn.indexOf('@');
            hulplijn.append(lijn.substring(0, at));
            lijn        = lijn.substring(at+1);
            at          = lijn.indexOf('@');
            var sublijn = lijn.substring(0, at);
            if (sublijn.contains("_")) {
              var delen = sublijn.split("_");
              switch (delen[0].toLowerCase()) {
                case "metwit":
                  hulplijn.append(maakMessageMetwit(speler, delen[1]));
                  break;
                case "metzwart":
                  hulplijn.append(maakMessageMetzwart(speler, delen[1]));
                  break;
                case "partijen":
                  hulplijn.append(maakMessagePartijen(delen[1]));
                  break;
                case "spelers":
                  hulplijn.append(maakMessageSpelers(delen[1]));
                  break;
                default:
                  break;
              }
            } else {
              hulplijn.append(formatLijn(sublijn));
            }
            message.append(formatLijn(hulplijn.toString()));
            lijn  = lijn.substring(at+1);
          }
          message.append(lijn);
        } else {
          message.append(formatLijn(lijn));
        }
      }
    }

    return message.toString();
  }

  private static String maakMessageMetwit(String to, String template) {
    var noSpelers = spelers.size();
    var resultaat = new StringBuilder();

    for (var ronde : rondes) {
      var partijen  = ronde.split(" ");
      for (String partij : partijen) {
        String[] speler = partij.split("-");
        var wit   = Integer.valueOf(speler[0])-1;
        var zwart = Integer.valueOf(speler[1])-1;
        if (wit < noSpelers
            && zwart < noSpelers) {
          var witspeler   = spelers.get(wit);
          var zwartspeler = spelers.get(zwart);
          if (witspeler.getNaam().equals(to)
              && isUitdaging(witspeler.getNaam(), zwartspeler.getNaam())) {
            setEmailParams(witspeler, zwartspeler);
            resultaat.append(formatLijn(template)).append(" , ");
          }
        }
      }
    }

    return formatSpelerlijst(resultaat.toString());
  }

  private static String maakMessageMetzwart(String to, String template) {
    var noSpelers = spelers.size();
    var resultaat = new StringBuilder();

    for (var ronde : rondes) {
      var partijen  = ronde.split(" ");
      for (String partij : partijen) {
        var speler  = partij.split("-");
        var wit     = Integer.valueOf(speler[0])-1;
        var zwart   = Integer.valueOf(speler[1])-1;
        if (wit < noSpelers
            && zwart < noSpelers) {
          var witspeler   = spelers.get(wit);
          var zwartspeler = spelers.get(zwart);
          if (zwartspeler.getNaam().equals(to)
              && isUitdaging(witspeler.getNaam(), zwartspeler.getNaam())) {
            setEmailParams(witspeler, zwartspeler);
            resultaat.append(formatLijn(template)).append(" , ");
          }
        }
      }
    }

    return formatSpelerlijst(resultaat.toString());
  }

  private static String maakMessagePartijen(String template) {
    var noSpelers = spelers.size();
    var resultaat = new StringBuilder();

    for (var ronde : rondes) {
      var partijen  = ronde.split(" ");
      for (var partij : partijen) {
        var speler = partij.split("-");
        var wit     = Integer.valueOf(speler[0])-1;
        var zwart   = Integer.valueOf(speler[1])-1;
        if (wit < noSpelers
            && zwart < noSpelers) {
          var witspeler   = spelers.get(wit);
          var zwartspeler = spelers.get(zwart);
          setEmailParams(witspeler, zwartspeler);
          resultaat.append(formatLijn(template));
        }
      }
    }

    return resultaat.toString();
  }

  private static String maakMessageSpelers(String template) {
    var             resultaat   = new StringBuilder();

    Set<Spelerinfo> gesorteerd  =
        new TreeSet<>(new Spelerinfo.ByNaamComparator());
    gesorteerd.addAll(spelers);

    emailparams.set(7,  "");
    emailparams.set(8,  "");
    emailparams.set(9,  "");
    emailparams.set(10, "");
    emailparams.set(11, "");
    emailparams.set(12, "");

    gesorteerd.forEach(speler -> {
      emailparams.set(3,  speler.getVoornaam());
      emailparams.set(4,  speler.getNaam());
      emailparams.set(5,  speler.getAlias());
      emailparams.set(6,  speler.getEmail());
      resultaat.append(formatLijn(template));
    });

    return resultaat.toString();
  }

  private static void maakToernooi(String event, String site, String date,
                                   boolean enkel, String uitvoer) {
    var           noSpelers = spelers.size();
    TekstBestand  output    = null;

    try {
      output  = new TekstBestand.Builder()
                                .setBestand(uitvoer)
                                .setCharset(paramBundle
                                                .getString(PAR_CHARSETUIT))
                                .setLezen(false).build();
      for (var ronde : rondes) {
        for (var partij : ronde.split(" ")) {
          var paring  = partij.split("-");
          var wit     = Integer.valueOf(paring[0]) - 1;
          var zwart   = Integer.valueOf(paring[1]) - 1;
          if (wit != noSpelers
              && zwart != noSpelers) {
            var witspeler   = spelers.get(wit).getNaam();
            var zwartspeler = spelers.get(zwart).getNaam();
            schrijfPartij(output, event, site, date, witspeler, zwartspeler);
            if (!enkel) {
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
      var msg = new MimeMessage(session);
      msg.setHeader("Content-Type", maildata.getContentType());

      msg.setFrom(new InternetAddress(maildata.getFrom()));

      if (maildata.getToSize() > 0) {
        msg.setRecipients(TO, fillAddresses(maildata.getTo().values()));
      }

      if (maildata.getCcSize() > 0) {
        msg.setRecipients(CC, fillAddresses(maildata.getCc().values()));
      }

      if (maildata.getBccSize() > 0) {
        msg.setRecipients(BCC, fillAddresses(maildata.getBcc().values()));
      }

      msg.setSubject(maildata.getSubject());
      msg.setSentDate(maildata.getSentDate());
      msg.setContent(maildata.getMessage(), maildata.getContentType());

      if (maildata.getHeaderSize() > 0) {
        var headers  = maildata.getHeader();
        for (var key : maildata.getHeader().keySet()) {
          msg.setHeader(key, headers.get(key));
        }
      }

      Transport.send(msg);
    } catch (MessagingException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
    }
  }

  private static void setEmailParams(Spelerinfo witspeler,
                                     Spelerinfo zwartspeler) {
    emailparams.set(3,  zwartspeler.getVoornaam());
    emailparams.set(4,  zwartspeler.getNaam());
    emailparams.set(5,  zwartspeler.getAlias());
    emailparams.set(6,  zwartspeler.getEmail());
    emailparams.set(7,  witspeler.getVoornaam());
    emailparams.set(8,  witspeler.getNaam());
    emailparams.set(9,  witspeler.getAlias());
    emailparams.set(10, witspeler.getEmail());
    emailparams.set(11, "");
    emailparams.set(12, "");
  }

  private static void stuurPerPartij(String subject) {
    var noSpelers = spelers.size();
    var session   = getSession();
    for (var ronde : rondes) {
      var partijen  = ronde.split(" ");
      for (var partij : partijen) {
        var speler = partij.split("-");
        var wit     = Integer.valueOf(speler[0])-1;
        var zwart   = Integer.valueOf(speler[1])-1;
        if (wit < noSpelers
            && zwart < noSpelers) {
          var witspeler   = spelers.get(wit);
          var zwartspeler = spelers.get(zwart);
          if ((!paramBundle.containsParameter(CaissaTools.PAR_NIEUWESPELERS)
              || nieuwespelers.contains(witspeler.getNaam())
              || nieuwespelers.contains(zwartspeler.getNaam()))) {
            var maildata  = new MailData();
            maildata.addTo(zwartspeler.getEmail());
            maildata.addCc(witspeler.getEmail());
            maildata.setFrom(paramBundle.getString(CaissaTools.PAR_TSEMAIL));
            maildata.setSubject(formatLijn(subject));
            setEmailParams(witspeler, zwartspeler);
            maildata.setMessage(maakMessage());
            DoosUtils.naarScherm(
                MessageFormat.format(resourceBundle
                                        .getString(CaissaTools.LBL_EMAIL),
                                     zwartspeler.getNaam(),
                                     witspeler.getNaam()));
            sendEmail(maildata, session);
          }
        }
      }
    }
  }

  private static void stuurPerSpeler(String subject) {
    var session = getSession();

    spelers.forEach(speler -> {
      var maildata  = new MailData();
      maildata.addTo(speler.getEmail());
      maildata.setFrom(paramBundle.getString(CaissaTools.PAR_TSEMAIL));
      maildata.setSubject(formatLijn(subject));
      emailparams.set(3,  speler.getVoornaam());
      emailparams.set(4,  speler.getNaam());
      emailparams.set(5,  speler.getAlias());
      emailparams.set(6,  speler.getEmail());
      emailparams.set(7,  "");
      emailparams.set(8,  "");
      emailparams.set(9,  "");
      emailparams.set(10, "");
      if (nieuwespelers.contains(speler.getNaam())) {
        emailparams.set(11, MessageFormat.format(
                resourceBundle.getString(CaissaTools.MSG_NIEUWESPELER),
                resourceBundle.getString(CaissaTools.LBL_JIJ)));
      } else {
        emailparams.set(11, maakLijstNieuweSpelers());
      }
      emailparams.set(12, telPartijenMetZwart(speler).toString());
      maildata.setMessage(maakMessage());
      DoosUtils.naarScherm(
              MessageFormat.format(resourceBundle
                                      .getString(CaissaTools.LBL_EMAIL),
                      speler.getNaam(), "-"));
      sendEmail(maildata, session);
    });
  }

  private static Integer telPartijenMetZwart(Spelerinfo zwartspeler) {
    var aantalPartijen  = 0;
    var noSpelers       = spelers.size();
    var zwartspelerId   = "-" + zwartspeler.getSpelerId().toString();

    for (var ronde : rondes) {
      var partijen  = ronde.split(" ");
      for (var partij : partijen) {
        if (partij.endsWith(zwartspelerId)) {
          var witspelerId = Integer.valueOf(partij.split("-")[0]) - 1;
          if (witspelerId < noSpelers
              && isUitdaging(spelers.get(witspelerId).getNaam(),
                             zwartspeler.getNaam())) {
            aantalPartijen++;
          }
        }
      }
    }

    return aantalPartijen;
  }
}
