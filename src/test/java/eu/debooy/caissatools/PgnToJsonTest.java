/**
 * Copyright 2018 Marco de Booij
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import eu.debooy.doosutils.access.Bestand;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.test.BatchTest;
import eu.debooy.doosutils.test.VangOutEnErr;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Marco de Booij
 */
public class PgnToJsonTest extends BatchTest {
  protected static final  ClassLoader CLASSLOADER =
      PgnToJsonTest.class.getClassLoader();

  @AfterClass
  public static void afterClass() {
    try {
      Bestand.delete(TEMP + File.separator + "competitie.json");
    } catch (BestandException e) {
    }

    try {
      Bestand.delete(TEMP + File.separator + "competitie2.pgn");
    } catch (BestandException e) {
    }

    try {
      Bestand.delete(TEMP + File.separator + "json.json");
    } catch (BestandException e) {
    }

    try {
      Bestand.delete(TEMP + File.separator + "json.pgn");
    } catch (BestandException e) {
    }

    try {
      Bestand.delete(TEMP + File.separator + "partij.json");
    } catch (BestandException e) {
    }

    try {
      Bestand.delete(TEMP + File.separator + "partij.pgn");
    } catch (BestandException e) {
    }
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    Locale.setDefault(new Locale("nl"));
    resourceBundle  = ResourceBundle.getBundle("ApplicatieResources",
                                               Locale.getDefault());

    TekstBestand  bron  = null;
    TekstBestand  doel  = null;
    try {
      bron  = new TekstBestand.Builder().setClassLoader(CLASSLOADER)
                              .setBestand("competitie2.pgn").build();
      doel  = new TekstBestand.Builder().setBestand(TEMP + File.separator
                                + "competitie2.pgn")
                              .setLezen(false).build();
      doel.add(bron);
      bron.close();
      doel.close();
      bron  = new TekstBestand.Builder().setClassLoader(CLASSLOADER)
                              .setBestand("json.pgn").build();
      doel  = new TekstBestand.Builder().setBestand(TEMP + File.separator
                                                    + "json.pgn")
                              .setLezen(false).build();
      doel.add(bron);
      bron.close();
      doel.close();
      bron  = new TekstBestand.Builder().setClassLoader(CLASSLOADER)
                              .setBestand("partij.pgn").build();
      doel  = new TekstBestand.Builder().setBestand(TEMP + File.separator
                                                    + "partij.pgn")
                              .setLezen(false).build();
      doel.add(bron);
    } finally {
      if (null != bron) {
        bron.close();
      }
      if (null != doel) {
        doel.close();
      }
    }
  }

//  @Test
//  public void testCompetitieMetPgnView() throws BestandException {
//    String[]  args      = new String[] {"--bestand=competitie2",
//                                        "--includelege=J",
//                                        "--invoerdir=" + TEMP,
//                                        "--json=competitie",
//                                        "--pgnviewer=J",
//                                        "--uitvoerdir=" + TEMP};
//
//    try {
//      Bestand.delete(TEMP + File.separator + "competitie.json");
//    } catch (BestandException e) {
//    }
//
//    VangOutEnErr.execute(PgnToJson.class, "execute", args, out, err);
//
//    assertEquals("PgnToJson - helptekst", 18, out.size());
//    assertEquals("PgnToJson - fouten", 0, 0);
//    assertEquals("PgnToJson - 14",
//                 TEMP + File.separator + "competitie2.pgn",
//                 out.get(13).split(":")[1].trim());
//    assertEquals("PgnToJson - 15", "64",
//                 out.get(14).split(":")[1].trim());
//    assertEquals("PgnToJson - 16", TEMP + File.separator + "competitie.json",
//                 out.get(15).split(":")[1].trim());
//    assertEquals("PgnToJson - 17", "64",
//                 out.get(16).split(":")[1].trim());
//    assertTrue("PgnToJson - equals",
//        Bestand.equals(
//            Bestand.openInvoerBestand(TEMP + File.separator
//                                      + "competitie.json"),
//            Bestand.openInvoerBestand(PgnToJsonTest.class.getClassLoader(),
//                                      "competitie2.json")));
//
//    Bestand.delete(TEMP + File.separator + "competitie.json");
//  }

  @Test
  public void testCompetitiePgnToJson() throws BestandException {
    String[]  args      = new String[] {"--bestand=competitie2",
                                        "--includelege=J",
                                        "--invoerdir=" + TEMP,
                                        "--json=competitie",
                                        "--uitvoerdir=" + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + "competitie.json");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(PgnToJson.class, "execute", args, out, err);

    assertEquals("PgnToJson - helptekst", 18, out.size());
    assertEquals("PgnToJson - fouten", 0, 0);
    assertEquals("PgnToJson - 14",
                 TEMP + File.separator + "competitie2.pgn",
                 out.get(13).split(":")[1].trim());
    assertEquals("PgnToJson - 15", "64",
                 out.get(14).split(":")[1].trim());
    assertEquals("PgnToJson - 16", TEMP + File.separator + "competitie.json",
                 out.get(15).split(":")[1].trim());
    assertEquals("PgnToJson - 17", "64",
                  out.get(16).split(":")[1].trim());
    assertTrue("PgnToJson - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + "competitie.json"),
            Bestand.openInvoerBestand(PgnToJsonTest.class.getClassLoader(),
                                      "competitie1.json")));

    Bestand.delete(TEMP + File.separator + "competitie.json");
  }

  @Test
  public void testLeeg() {
    String[]  args      = new String[] {};

    VangOutEnErr.execute(PgnToJson.class, "execute", args, out, err);

    assertEquals("Zonder parameters - helptekst", 33, out.size());
    assertEquals("Zonder parameters - fouten", 0, 0);
  }

  @Test
  public void testMetLegePartijen() throws BestandException {
    String[]  args      = new String[] {"--bestand=json",
                                        "--includelege=J",
                                        "--invoerdir=" + TEMP,
                                        "--json=json",
                                        "--uitvoerdir=" + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + "json.json");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(PgnToJson.class, "execute", args, out, err);

    assertEquals("Zonder parameters - helptekst", 18, out.size());
    assertEquals("Zonder parameters - fouten", 0, 0);
    assertEquals("PgnToJson - 17", "2",
                 out.get(16).split(":")[1].trim());

    Bestand.delete(TEMP + File.separator + "json.json");
  }

//  @Test
//  public void testPartijMetPgnView() throws BestandException {
//    String[]  args      = new String[] {"--bestand=partij",
//                                        "--invoerdir=" + TEMP,
//                                        "--json=partij",
//                                        "--pgnviewer=J",
//                                        "--uitvoerdir=" + TEMP};
//
//    try {
//      Bestand.delete(TEMP + File.separator + "partij.json");
//    } catch (BestandException e) {
//    }
//
//    VangOutEnErr.execute(PgnToJson.class, "execute", args, out, err);
//
//    assertTrue("PartijMetPgnView",
//        Bestand.equals(
//            Bestand.openInvoerBestand(TEMP + File.separator
//                                      + "partij.json"),
//            Bestand.openInvoerBestand(PgnToJsonTest.class.getClassLoader(),
//                                      "partijPgnviewer.json")));
//
//    Bestand.delete(TEMP + File.separator + "partij.json");
//  }

  @Test
  public void testPartijToJson() throws BestandException {
    String[]  args      = new String[] {"--bestand=partij",
                                        "--invoerdir=" + TEMP,
                                        "--json=partij", 
                                        "--uitvoerdir=" + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + "partij.json");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(PgnToJson.class, "execute", args, out, err);

    assertTrue("PartijToJson",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + "partij.json"),
            Bestand.openInvoerBestand(PgnToJsonTest.class.getClassLoader(),
                                      "partij.json")));

    Bestand.delete(TEMP + File.separator + "partij.json");
  }

  @Test
  public void testZonderLegePartijen() throws BestandException {
    String[]  args      = new String[] {"--bestand=json",
                                        "--invoerdir=" + TEMP,
                                        "--json=json",
                                        "--uitvoerdir=" + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + "json.json");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(PgnToJson.class, "execute", args, out, err);

    assertEquals("Zonder parameters - helptekst", 18, out.size());
    assertEquals("Zonder parameters - fouten", 0, 0);
    assertEquals("PgnToJson - 17", "1",
                 out.get(16).split(":")[1].trim());

    Bestand.delete(TEMP + File.separator + "json.json");
  }
}
