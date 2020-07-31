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
public class StartPgnTest extends BatchTest {
  protected static final  ClassLoader CLASSLOADER =
      StartPgnTest.class.getClassLoader();

  private static final  String  BST_START_PGN = "start.pgn";

  private static final  String  PAR_BESTAND = "--bestand=start";
  private static final  String  PAR_DATE    = "--date=1999.10.01";
  private static final  String  PAR_EVENT   = "--event=\"Testing 97/98\"";
  private static final  String  PAR_SITE    = "--site=\"Caissa Tools\"";
  private static final  String  PAR_SPELERS =
      "--spelers=\"Speler, 01;Speler, 02;Speler, 03;Speler, 04\"";


  @AfterClass
  public static void afterClass() throws BestandException {
    Bestand.delete(TEMP + File.separator + TestConstants.BST_COMPETITIE1_PGN);
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    Locale.setDefault(new Locale(TestConstants.TST_TAAL));
    resourceBundle  = ResourceBundle.getBundle("ApplicatieResources",
                                               Locale.getDefault());

    try {
      kopieerBestand(CLASSLOADER,
                     TestConstants.BST_COMPETITIE1_PGN,
                     TEMP + File.separator + TestConstants.BST_COMPETITIE1_PGN);
    } catch (IOException e) {
      System.out.println(e.getLocalizedMessage());
      throw new BestandException(e);
    }
  }

  @Test
  public void testLeeg() {
    String[]  args      = new String[] {};

    VangOutEnErr.execute(StartPgn.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("Zonder parameters - helptekst", 29, out.size());
    assertEquals("Zonder parameters - fouten", 1, err.size());
  }

  @Test
  public void testStartPgn() throws BestandException {
    String[]  args      = new String[] {PAR_BESTAND, PAR_DATE, PAR_EVENT,
                                        PAR_SITE, PAR_SPELERS,
                                        TestConstants.PAR_UITVOERDIR + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + BST_START_PGN);
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(StartPgn.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("StartPgn - helptekst", 17, out.size());
    assertEquals("StartPgn - fouten", 0, err.size());
    assertEquals("StartPgn - 14",
                 TEMP + File.separator + BST_START_PGN,
                 out.get(13).split(":")[1].trim());
    assertTrue("StartPgn - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(StartPgnTest.class.getClassLoader(),
                                      BST_START_PGN),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + BST_START_PGN)));

    Bestand.delete(TEMP + File.separator + BST_START_PGN);
  }
}
