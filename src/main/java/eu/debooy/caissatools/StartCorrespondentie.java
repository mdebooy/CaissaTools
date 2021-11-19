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
import eu.debooy.doosutils.access.JsonBestand;
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
import java.util.Set;
import java.util.TreeSet;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
  @author Marco de Booij
 */
public class StartCorrespondentie extends Batchjob {
  private static final  ResourceBundle  resourceBundle  =
      ResourceBundle.getBundle("ApplicatieResources", Locale.getDefault());

    private static final  List<String>      email         = new ArrayList<>();
    private static final  List<String>      emailparams   = new ArrayList<>();
    private static final  List<String>      nieuwespelers = new ArrayList<>();
    private static final  List<Spelerinfo>  spelers       = new ArrayList<>();

    private static  String[]  rondes;

    public static void execute(String[] args) {
    Banner.printMarcoBanner(
        resourceBundle.getString("banner.startcorrespondentie"));

    if (!setParameters(args)) {
      return;
    }

    String  subject = leesBericht();

    if (parameters.containsKey(CaissaTools.PAR_NIEUWESPELERS)) {
      nieuwespelers.addAll(
          Arrays.asList(parameters.get(CaissaTools.PAR_NIEUWESPELERS)
                                  .split(";")));
    }

    JsonBestand competitie;
    try {
      competitie  =
          new JsonBestand.Builder()
                         .setBestand(parameters.get(PAR_INVOERDIR)
                                     + parameters.get(CaissaTools.PAR_SCHEMA)
                                     + EXT_JSON)
                         .setCharset(parameters.get(PAR_CHARSETIN))
                         .build();
    } catch (BestandException e) {
      DoosUtils.foutNaarScherm(e.getLocalizedMessage());
      return;
    }

    String            date          = competitie.get("EventDate").toString();
    String            datum         = getDatum(date);
    boolean           enkel         = true;
    if (competitie.containsKey("enkelrondig")) {
      enkel = (boolean) competitie.get("enkelrondig");
    }
    String            event         = competitie.get("Event").toString();
    String            site          = competitie.get("Site").toString();

    initEmailparams(13);
    emailparams.add(0, event);
    emailparams.add(1, site);
    emailparams.add(2, datum);

    JSONArray   jsonArray = competitie.getArray("spelers");
    int         spelerId  = 1;
    for (Object naam : jsonArray.toArray()) {
      Spelerinfo  speler  = new Spelerinfo();
      speler.setSpelerId(spelerId);
      speler.setAlias(((JSONObject) naam).get("alias").toString());
      speler.setEmail(((JSONObject) naam).get("email").toString());
      speler.setNaam(((JSONObject) naam).get("naam").toString());
      speler.setSpelerId(spelerId);
      spelers.add(speler);
      spelerId++;
    }
    int noSpelers = spelers.size();

    String  uitvoer = parameters.get(PAR_UITVOERDIR) + event + EXT_PGN;

    rondes  = CaissaUtils.bergertabel(noSpelers);
    maakToernooi(event, site, date, enkel, uitvoer);

    if (parameters.containsKey(CaissaTools.PAR_SMTPSERVER)) {
      if (parameters.get(CaissaTools.PAR_PERPARTIJ)
                    .equals(DoosConstants.WAAR)) {
        stuurPerPartij(subject);
      } else {
        stuurPerSpeler(subject);
      }
      DoosUtils.naarScherm();
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
    int               index   = 0;
    InternetAddress[] result  = new InternetAddress[addresses.size()];
    for (String address : addresses) {
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

  private static String getTekst(String lijn) {
    if (lijn.startsWith("#")) {
      if (DoosUtils.telTeken(lijn, '#') == 2) {
        String  parameter = lijn.substring(1).split("#")[0];
        switch (parameter) {
          case "geennieuwespelers":
            if (!parameters.containsKey(CaissaTools.PAR_NIEUWESPELERS)) {
              return lijn.substring(19);
            }
            break;
          case "metzwart":
            if (Integer.valueOf(emailparams.get(12)) > 0) {
              return lijn.substring(10);
            }
            break;
          case "nieuwespelers":
            if (parameters.containsKey(CaissaTools.PAR_NIEUWESPELERS)) {
              return lijn.substring(15);
            }
            break;
          default:
            return "";
        }
      }

      return "";
    }

    return lijn;
  }

  public static void help() {
    DoosUtils.naarScherm("java -jar CaissaTools.jar StartCorrespondentie "
                         + " --schema=<"
                         + resourceBundle.getString("label.competitieschema")
                         + ">");
    DoosUtils.naarScherm();
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_BERICHT, 14),
                         resourceBundle.getString("help.bericht"), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETIN, 14),
        MessageFormat.format(getMelding(HLP_CHARSETIN),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_CHARSETUIT, 14),
        MessageFormat.format(getMelding(HLP_CHARSETUIT),
                             Charset.defaultCharset().name()), 80);
    DoosUtils.naarScherm(getParameterTekst(PAR_INVOERDIR, 14),
                         getMelding(HLP_INVOERDIR), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_NIEUWESPELERS, 14),
                         resourceBundle.getString("help.nieuwespelers"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_PERPARTIJ, 14),
                         resourceBundle.getString("help.perpartij"), 80);
    DoosUtils.naarScherm(getParameterTekst(CaissaTools.PAR_SCHEMA, 14),
                         resourceBundle.getString("help.competitieschema"), 80);
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
        MessageFormat.format(getMelding(HLP_PARAMVERPLICHT),
                             CaissaTools.PAR_SCHEMA), 80);
    DoosUtils.naarScherm();
  }

  private static void initEmailparams(int params) {
    for (int i = 0; i < params; i++) {
      emailparams.add(i, "");
    }
  }

  private static boolean isUitdaging(String wit, String zwart) {
    return nieuwespelers.isEmpty()
            || nieuwespelers.contains(wit)
            || nieuwespelers.contains(zwart);
  }

  private static String leesBericht() {
    String  subject = "Start " + parameters.get(CaissaTools.PAR_EVENT);
    if (parameters.containsKey(CaissaTools.PAR_BERICHT)) {
      String  bericht = parameters.get(PAR_INVOERDIR)
                         + parameters.get(CaissaTools.PAR_BERICHT);
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
        subject = "Start " + parameters.get(CaissaTools.PAR_EVENT);
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

    return subject;
  }

  private static String maakLijstNieuweSpelers() {
    if (nieuwespelers.isEmpty()) {
      return "";
    }

    StringBuilder resultaat = new StringBuilder();
    String        lijn;

    if (nieuwespelers.size() == 1) {
      lijn  = resourceBundle.getString("message.nieuwespeler");
    } else {
      lijn  = resourceBundle.getString("message.nieuwespelers");
    }
    nieuwespelers.forEach(speler ->
            resultaat.append(speler.split(",")[1].trim()).append(" , "));

    return MessageFormat.format(lijn, formatSpelerlijst(resultaat.toString()));
  }

  private static String maakMessage() {
    String        speler  = emailparams.get(4);
    StringBuilder message = new StringBuilder();

    for (String lijn : email) {
      lijn  = getTekst(lijn);
      if (DoosUtils.isNotBlankOrNull(lijn)) {
        if (DoosUtils.telTeken(lijn, '@') > 1) {
          StringBuilder hulplijn = new StringBuilder();
          while (DoosUtils.telTeken(lijn, '@') > 1) {
            int at  = lijn.indexOf('@');
            hulplijn.append(lijn.substring(0, at));
            lijn    = lijn.substring(at+1);
            at      = lijn.indexOf('@');
            String  sublijn = lijn.substring(0, at);
            if (sublijn.contains("_")) {
              String[]  delen = sublijn.split("_");
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
          if (witspeler.getNaam().equals(to)
              && isUitdaging(witspeler.getNaam(), zwartspeler.getNaam())) {
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
            resultaat.append(formatLijn(template)).append(" , ");
          }
        }
      }
    }

    return formatSpelerlijst(resultaat.toString());
  }

  private static String maakMessageMetzwart(String to, String template) {
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
          if (zwartspeler.getNaam().equals(to)
              && isUitdaging(witspeler.getNaam(), zwartspeler.getNaam())) {
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
            resultaat.append(formatLijn(template)).append(" , ");
          }
        }
      }
    }

    return formatSpelerlijst(resultaat.toString());
  }

  private static String maakMessagePartijen(String template) {
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
          emailparams.set(11, "");
          emailparams.set(12, "");
          resultaat.append(formatLijn(template));
        }
      }
    }

    return resultaat.toString();
  }

  private static String maakMessageSpelers(String template) {
    StringBuilder   resultaat   = new StringBuilder();

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
    int           noSpelers = spelers.size();
    TekstBestand  output    = null;

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
                                          PAR_CHARSETIN,
                                          PAR_CHARSETUIT,
                                          PAR_INVOERDIR,
                                          CaissaTools.PAR_NIEUWESPELERS,
                                          CaissaTools.PAR_PERPARTIJ,
                                          CaissaTools.PAR_SCHEMA,
                                          CaissaTools.PAR_SMTPPOORT,
                                          CaissaTools.PAR_SMTPSERVER,
                                          CaissaTools.PAR_TSEMAIL,
                                          PAR_UITVOERDIR});
    arguments.setVerplicht(new String[] {CaissaTools.PAR_SCHEMA});
    if (!arguments.isValid()) {
      fouten.add(getMelding(ERR_INVALIDPARAMS));
    }

    parameters.clear();
    setParameter(arguments, CaissaTools.PAR_BERICHT);
    setParameter(arguments, PAR_CHARSETIN, Charset.defaultCharset().name());
    setParameter(arguments, PAR_CHARSETUIT, Charset.defaultCharset().name());
    setDirParameter(arguments, PAR_INVOERDIR);
    setParameter(arguments, CaissaTools.PAR_NIEUWESPELERS);
    setParameter(arguments, CaissaTools.PAR_PERPARTIJ, DoosConstants.ONWAAR);
    setBestandParameter(arguments, CaissaTools.PAR_SCHEMA, EXT_JSON);
    setParameter(arguments, CaissaTools.PAR_SMTPPOORT);
    setParameter(arguments, CaissaTools.PAR_SMTPSERVER);
    setParameter(arguments, CaissaTools.PAR_TSEMAIL);
    setDirParameter(arguments, PAR_UITVOERDIR, getParameter(PAR_INVOERDIR));

    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_BERICHT))
                 .contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              getMelding(ERR_BEVATDIRECTORY), CaissaTools.PAR_BERICHT));
    }
    if (DoosUtils.nullToEmpty(parameters.get(CaissaTools.PAR_SCHEMA))
                 .contains(File.separator)) {
      fouten.add(
          MessageFormat.format(
              getMelding(ERR_BEVATDIRECTORY), CaissaTools.PAR_SCHEMA));
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

  private static void stuurPerPartij(String subject) {
    int     noSpelers = spelers.size();
    Session session   = getSession();
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
            MailData  maildata  = new MailData();
            maildata.addTo(zwartspeler.getEmail());
            maildata.addCc(witspeler.getEmail());
            maildata.setFrom(parameters.get(CaissaTools.PAR_TSEMAIL));
            maildata.setSubject(formatLijn(subject));
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
            maildata.setMessage(maakMessage());
            DoosUtils.naarScherm(
                MessageFormat.format(resourceBundle.getString("label.email"),
                                     zwartspeler.getNaam(),
                                     witspeler.getNaam()));
            sendEmail(maildata, session);
          }
        }
      }
    }
  }

  private static void stuurPerSpeler(String subject) {
    Session session = getSession();

    spelers.forEach(speler -> {
      MailData  maildata  = new MailData();
      maildata.addTo(speler.getEmail());
      maildata.setFrom(parameters.get(CaissaTools.PAR_TSEMAIL));
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
                resourceBundle.getString("message.nieuwespeler"),
                resourceBundle.getString("label.jij")));
      } else {
        emailparams.set(11, maakLijstNieuweSpelers());
      }
      emailparams.set(12, telPartijenMetZwart(speler).toString());
      maildata.setMessage(maakMessage());
      DoosUtils.naarScherm(
              MessageFormat.format(resourceBundle.getString("label.email"),
                      speler.getNaam(), "-"));
      sendEmail(maildata, session);
    });
  }

  private static Integer telPartijenMetZwart(Spelerinfo zwartspeler) {
    Integer aantalPartijen  = 0;
    int     noSpelers       = spelers.size();
    String  zwartspelerId   = "-" + zwartspeler.getSpelerId().toString();

    for (String ronde : rondes) {
      String[]  partijen  = ronde.split(" ");
      for (String partij : partijen) {
        if (partij.endsWith(zwartspelerId)) {
          Integer witspelerId = Integer.valueOf(partij.split("-")[0]) - 1;
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
