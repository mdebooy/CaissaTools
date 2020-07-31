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

import eu.debooy.doosutils.access.Bestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.test.BatchTest;
import eu.debooy.doosutils.test.DoosUtilsTestConstants;
import eu.debooy.doosutils.test.VangOutEnErr;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Marco de Booij
 */
public class PgnToJsonTest extends BatchTest {
  protected static final  ClassLoader CLASSLOADER =
      PgnToJsonTest.class.getClassLoader();

  private static final  String  BST_COMPETITIE1_JSON  = "competitie1.json";
  private static final  String  BST_JSON_JSON         = "json.json";
  private static final  String  BST_JSON_PGN          = "json.pgn";
  private static final  String  BST_PARTIJ_JSON       = "partij.json";

  private static final  String  PAR_BESTAND_JSON      = "--bestand=json";
  private static final  String  PAR_BESTAND_PARTIJ    = "--bestand=partij";
  private static final  String  PAR_INCL_LEGE_J       = "--includelege=J";
  private static final  String  PAR_JSON_COMPETITIE1  = "--json=competitie1";
  private static final  String  PAR_JSON_JSON         = "--json=json";
  private static final  String  PAR_JSON_PARTIJ       = "--json=partij";

  @AfterClass
  public static void afterClass() {
    verwijderBestanden(TEMP + File.separator,
                       new String[] {BST_COMPETITIE1_JSON,
                                     TestConstants.BST_COMPETITIE2_PGN,
                                     BST_JSON_JSON, BST_JSON_PGN,
                                     BST_PARTIJ_JSON,
                                     TestConstants.BST_PARTIJ_PGN});
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    Locale.setDefault(new Locale(TestConstants.TST_TAAL));
    resourceBundle  = ResourceBundle.getBundle("ApplicatieResources",
                                               Locale.getDefault());

    for (String bestand : new String[] {TestConstants.BST_COMPETITIE2_PGN,
                                        BST_JSON_PGN,
                                        TestConstants.BST_PARTIJ_PGN}) {
      try {
        kopieerBestand(CLASSLOADER, bestand, TEMP + File.separator + bestand);
      } catch (IOException e) {
        throw new BestandException(e);
      }
    }
  }

//  @Test
//  public void testCompetitieMetPgnView() throws BestandException {
//    String[]  args      = new String[] {TestConstants.PAR_BESTAND2,
//                                        PAR_INCL_LEGE_J,
//                                        TestConstants.PAR_INVOERDIR + TEMP,
//                                        PAR_JSON_COMPETITIE,
//                                        "--pgnviewer=J",
//                                        TestConstants.PAR_UITVOERDIR + TEMP};
//
//    try {
//      Bestand.delete(TEMP + File.separator + BST_COMPETITIE_JSON);
//    } catch (BestandException e) {
//    }
//
//    VangOutEnErr.execute(PgnToJson.class, DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);
//
//    assertEquals("PgnToJson - helptekst", 18, out.size());
//    assertEquals("PgnToJson - fouten", 0, 0);
//    assertEquals("PgnToJson - 14",
//                 TEMP + File.separator + TestConstants.BST_COMPETITIE2_PGN,
//                 out.get(13).split(":")[1].trim());
//    assertEquals("PgnToJson - 15", "64",
//                 out.get(14).split(":")[1].trim());
//    assertEquals("PgnToJson - 16", TEMP + File.separator + BST_COMPETITIE_JSON,
//                 out.get(15).split(":")[1].trim());
//    assertEquals("PgnToJson - 17", "64",
//                 out.get(16).split(":")[1].trim());
//    assertTrue("PgnToJson - equals",
//        Bestand.equals(
//            Bestand.openInvoerBestand(TEMP + File.separator
//                                      + BST_COMPETITIE_JSON),
//            Bestand.openInvoerBestand(PgnToJsonTest.class.getClassLoader(),
//                                      "competitie2.json")));
//
//    Bestand.delete(TEMP + File.separator + BST_COMPETITIE_JSON);
//  }

  @Test
  public void testCompetitiePgnToJson() throws BestandException {
    String[]  args      = new String[] {TestConstants.PAR_BESTAND2,
                                        PAR_INCL_LEGE_J,
                                        TestConstants.PAR_INVOERDIR + TEMP,
                                        PAR_JSON_COMPETITIE1,
                                        TestConstants.PAR_UITVOERDIR + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + BST_COMPETITIE1_JSON);
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(PgnToJson.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("PgnToJson - helptekst", 20, out.size());
    assertEquals("PgnToJson - fouten", 0, err.size());
    assertEquals("PgnToJson - 14",
                 TEMP + File.separator + TestConstants.BST_COMPETITIE2_PGN,
                 out.get(13).split(":")[1].trim());
    assertEquals("PgnToJson - 15", "64",
                 out.get(14).split(":")[1].trim());
    assertEquals("PgnToJson - 16", TEMP + File.separator + BST_COMPETITIE1_JSON,
                 out.get(15).split(":")[1].trim());
    assertEquals("PgnToJson - 17", "64",
                  out.get(16).split(":")[1].trim());
    assertTrue("PgnToJson - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + BST_COMPETITIE1_JSON),
            Bestand.openInvoerBestand(PgnToJsonTest.class.getClassLoader(),
                                      BST_COMPETITIE1_JSON)));

    Bestand.delete(TEMP + File.separator + BST_COMPETITIE1_JSON);
  }

  @Test
  public void testLeeg() {
    String[]  args      = new String[] {};

    VangOutEnErr.execute(PgnToJson.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("Zonder parameters - helptekst", 36, out.size());
    assertEquals("Zonder parameters - fouten", 1, err.size());
  }

  @Test
  public void testMetLegePartijen() throws BestandException {
    String[]  args      = new String[] {PAR_BESTAND_JSON,
                                        PAR_INCL_LEGE_J,
                                        TestConstants.PAR_INVOERDIR + TEMP,
                                        PAR_JSON_JSON,
                                        TestConstants.PAR_UITVOERDIR + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + BST_JSON_JSON);
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(PgnToJson.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("Met lege partijen - helptekst", 20, out.size());
    assertEquals("Met lege partijen - fouten", 0, err.size());
    assertEquals("PgnToJson - 15", "2",
                 out.get(14).split(":")[1].trim());
    assertEquals("PgnToJson - 17", "2",
                 out.get(16).split(":")[1].trim());

    Bestand.delete(TEMP + File.separator + BST_JSON_JSON);
  }

//  @Test
//  public void testPartijMetPgnView() throws BestandException {
//    String[]  args      = new String[] {PAR_BESTAND_PARTIJ,
//                                        TestConstants.PAR_INVOERDIR + TEMP,
//                                        PAR_JSON_PARTIJ,
//                                        "--pgnviewer=J",
//                                        TestConstants.PAR_UITVOERDIR + TEMP};
//
//    try {
//      Bestand.delete(TEMP + File.separator + BST_PARTIJ_JSON);
//    } catch (BestandException e) {
//    }
//
//    VangOutEnErr.execute(PgnToJson.class, DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);
//
//    assertTrue("PartijMetPgnView",
//        Bestand.equals(
//            Bestand.openInvoerBestand(TEMP + File.separator
//                                      + BST_PARTIJ_JSON),
//            Bestand.openInvoerBestand(PgnToJsonTest.class.getClassLoader(),
//                                      "partijPgnviewer.json")));
//
//    Bestand.delete(TEMP + File.separator + BST_PARTIJ_JSON);
//  }

  @Test
  public void testPartijToJson() throws BestandException {
    String[]  args      = new String[] {PAR_BESTAND_PARTIJ,
                                        TestConstants.PAR_INVOERDIR + TEMP,
                                        PAR_JSON_PARTIJ,
                                        TestConstants.PAR_UITVOERDIR + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + BST_PARTIJ_JSON);
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(PgnToJson.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertTrue("PartijToJson",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + BST_PARTIJ_JSON),
            Bestand.openInvoerBestand(PgnToJsonTest.class.getClassLoader(),
                                      BST_PARTIJ_JSON)));

    Bestand.delete(TEMP + File.separator + BST_PARTIJ_JSON);
  }

  @Test
  public void testZonderLegePartijen() throws BestandException {
    String[]  args      = new String[] {PAR_BESTAND_JSON,
                                        TestConstants.PAR_INVOERDIR + TEMP,
                                        PAR_JSON_JSON,
                                        TestConstants.PAR_UITVOERDIR + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + BST_JSON_JSON);
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(PgnToJson.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("Zonder lege partijen - helptekst", 20, out.size());
    assertEquals("Zonder lege partijen - fouten", 0, err.size());
    assertEquals("PgnToJson - 15", "2",
                 out.get(14).split(":")[1].trim());
    assertEquals("PgnToJson - 17", "1",
                 out.get(16).split(":")[1].trim());

    Bestand.delete(TEMP + File.separator + BST_JSON_JSON);
  }
}
