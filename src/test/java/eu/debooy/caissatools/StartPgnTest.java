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

import eu.debooy.caissa.CaissaConstants;
import eu.debooy.caissa.Competitie;
import eu.debooy.doosutils.access.Bestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.test.BatchTest;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Marco de Booij
 */
public class StartPgnTest extends BatchTest {
  protected static final  ClassLoader CLASSLOADER =
      StartPgnTest.class.getClassLoader();

  private static final  String  BST_START_PGN   = "start.pgn";
  private static final  String  BST_START51_PGN = "start5-1.pgn";
  private static final  String  BST_START5H_PGN = "start5H.pgn";
  private static final  String  BST_START5T_PGN = "start5T.pgn";
  private static final  String  BST_STARTE_PGN  = "startE.pgn";

  private static  String  errTekort;
  private static  String  errTelang;

  private static final  String  PAR_BESTAND     =
      "--bestand=" + getTemp() + File.separator + "start";
  private static final  String  PAR_METVOLGORDE =
      "--metvolgorde";
  private static final  String  PAR_SCHEMA      =
      "--schema=" + getTemp() + File.separator + "competitie";
  private static final  String  PAR_SCHEMA51    =
      "--schema=" + getTemp()+ File.separator + "competitie5-1";
  private static final  String  PAR_SCHEMA5H    =
      "--schema=" + getTemp()+ File.separator + "competitie5H";
  private static final  String  PAR_SCHEMA5T    =
      "--schema=" + getTemp()+ File.separator + "competitie5T";
  private static final  String  PAR_TEKORT      =
      "--schema=" + getTemp() + File.separator + "tekort";
  private static final  String  PAR_TELANG      =
      "--schema=" + getTemp() + File.separator + "telang";

  @AfterClass
  public static void afterClass() throws BestandException {
    verwijderBestanden(getTemp() + File.separator,
                       new String[] {TestConstants.BST_COMPETITIE_JSON,
                                     TestConstants.BST_COMPETITIE51_JSON,
                                     TestConstants.BST_COMPETITIE5H_JSON,
                                     TestConstants.BST_COMPETITIE5T_JSON,
                                     TestConstants.BST_TEKORT_JSON,
                                     TestConstants.BST_TELANG_JSON});
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    Locale.setDefault(new Locale(TestConstants.TST_TAAL));
    resourceBundle  = ResourceBundle.getBundle(CaissaConstants.RESOURCEBUNDLE,
                                               Locale.getDefault());

    errTekort  =
      MessageFormat.format(resourceBundle.getString(Competitie.ERR_KALENDER),
                           9, 10);
    errTelang  =
      MessageFormat.format(resourceBundle.getString(Competitie.ERR_KALENDER),
                           11, 10);
    try {
      kopieerBestand(CLASSLOADER,
                     TestConstants.BST_COMPETITIE_JSON,
                     getTemp() + File.separator
                      + TestConstants.BST_COMPETITIE_JSON);
      kopieerBestand(CLASSLOADER,
                     TestConstants.BST_COMPETITIE51_JSON,
                     getTemp() + File.separator
                      + TestConstants.BST_COMPETITIE51_JSON);
      kopieerBestand(CLASSLOADER,
                     TestConstants.BST_COMPETITIE5H_JSON,
                     getTemp() + File.separator
                      + TestConstants.BST_COMPETITIE5H_JSON);
      kopieerBestand(CLASSLOADER,
                     TestConstants.BST_COMPETITIE5T_JSON,
                     getTemp() + File.separator
                      + TestConstants.BST_COMPETITIE5T_JSON);
      kopieerBestand(CLASSLOADER,
                     TestConstants.BST_TEKORT_JSON,
                     getTemp() + File.separator
                      + TestConstants.BST_TEKORT_JSON);
      kopieerBestand(CLASSLOADER,
                     TestConstants.BST_TELANG_JSON,
                     getTemp() + File.separator
                      + TestConstants.BST_TELANG_JSON);
    } catch (IOException e) {
      System.out.println(e.getLocalizedMessage());
      throw new BestandException(e);
    }
  }

  @Test
  public void testExtra() throws BestandException {
    var args  = new String[] {PAR_BESTAND, PAR_METVOLGORDE, PAR_SCHEMA};

    try {
      kopieerBestand(CLASSLOADER, BST_START_PGN,
                     getTemp() + File.separator + BST_START_PGN);
    } catch (IOException e) {
      fail("Er had geen IOException moeten wezen. "
            + e.getLocalizedMessage());
    }

    before();
    StartPgn.execute(args);
    after();

    assertEquals(17, out.size());
    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator + BST_START_PGN,
                 out.get(13).split(":")[1].trim());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(StartPgnTest.class.getClassLoader(),
                                      BST_STARTE_PGN),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + BST_START_PGN)));

    Bestand.delete(getTemp() + File.separator + BST_START_PGN);
  }

  @Test
  public void testLeeg() {
    var args      = new String[] {};

    before();
    StartPgn.execute(args);
    after();

    assertEquals(1, err.size());
  }

  @Test
  public void testSchema51() throws BestandException {
    var args  = new String[] {PAR_BESTAND, PAR_METVOLGORDE, PAR_SCHEMA51};

    try {
      Bestand.delete(getTemp() + File.separator + BST_START_PGN);
    } catch (BestandException e) {
      // No problem.
    }

    before();
    StartPgn.execute(args);
    after();

    assertEquals(17, out.size());
    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator + BST_START_PGN,
                 out.get(13).split(":")[1].trim());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(StartPgnTest.class.getClassLoader(),
                                      BST_START51_PGN),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + BST_START_PGN)));

    Bestand.delete(getTemp() + File.separator + BST_START_PGN);
  }

  @Test
  public void testSchema5H() throws BestandException {
    var args  = new String[] {PAR_BESTAND, PAR_SCHEMA5H};

    try {
      Bestand.delete(getTemp() + File.separator + BST_START_PGN);
    } catch (BestandException e) {
      // No problem.
    }

    before();
    StartPgn.execute(args);
    after();

    assertEquals(17, out.size());
    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator + BST_START_PGN,
                 out.get(13).split(":")[1].trim());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(StartPgnTest.class.getClassLoader(),
                                      BST_START5H_PGN),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + BST_START_PGN)));

    Bestand.delete(getTemp() + File.separator + BST_START_PGN);
  }

  @Test
  public void testSchema5T() throws BestandException {
    var args  = new String[] {PAR_BESTAND, PAR_SCHEMA5T};

    try {
      Bestand.delete(getTemp() + File.separator + BST_START_PGN);
    } catch (BestandException e) {
      // No problem.
    }

    before();
    StartPgn.execute(args);
    after();

    assertEquals(17, out.size());
    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator + BST_START_PGN,
                 out.get(13).split(":")[1].trim());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(StartPgnTest.class.getClassLoader(),
                                      BST_START5T_PGN),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + BST_START_PGN)));

    Bestand.delete(getTemp() + File.separator + BST_START_PGN);
  }

  @Test
  public void testStartPgn() throws BestandException {
    var args  = new String[] {PAR_BESTAND, PAR_SCHEMA};

    try {
      Bestand.delete(getTemp() + File.separator + BST_START_PGN);
    } catch (BestandException e) {
      // No problem.
    }

    before();
    StartPgn.execute(args);
    after();

    assertEquals(17, out.size());
    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator + BST_START_PGN,
                 out.get(13).split(":")[1].trim());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(StartPgnTest.class.getClassLoader(),
                                      BST_START_PGN),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + BST_START_PGN)));

    Bestand.delete(getTemp() + File.separator + BST_START_PGN);
  }

  @Test
  public void testTekort() throws BestandException {
    var args  = new String[] {PAR_BESTAND, PAR_TEKORT};

    before();
    StartPgn.execute(args);
    after();

    assertEquals(13, out.size());
    assertEquals(1, err.size());
    assertEquals(errTekort, err.get(0));
  }

  @Test
  public void testTelang() throws BestandException {
    var args  = new String[] {PAR_BESTAND, PAR_TELANG};

    before();
    StartPgn.execute(args);
    after();

    assertEquals(13, out.size());
    assertEquals(1, err.size());
    assertEquals(errTelang, err.get(0));
  }
}
