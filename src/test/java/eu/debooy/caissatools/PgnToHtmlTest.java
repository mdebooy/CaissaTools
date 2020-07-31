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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * @author Marco de Booij
 */
public class PgnToHtmlTest extends BatchTest {
  protected static final  ClassLoader CLASSLOADER =
      PgnToHtmlTest.class.getClassLoader();

  private static final  String  BST_MATRIX1 = "matrix1.html";
  private static final  String  BST_MATRIX2 = "matrix2.html";

  @AfterClass
  public static void afterClass() {
    verwijderBestanden(TEMP + File.separator,
                       new String[] {TestConstants.BST_COMPETITIE1_PGN,
                                     TestConstants.BST_INDEX_HTML,
                                     TestConstants.BST_MATRIX_HTML,
                                     BST_MATRIX1, BST_MATRIX2});
  }

  @Before
  public void before() {
    verwijderBestanden(TEMP + File.separator,
                       new String[] {TestConstants.BST_INDEX_HTML,
                                     TestConstants.BST_MATRIX_HTML,
                                     BST_MATRIX1, BST_MATRIX2});
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

    VangOutEnErr.execute(PgnToHtml.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("Zonder parameters - helptekst", 30, out.size());
    assertEquals("Zonder parameters - fouten", 1, err.size());
  }

  @Test
  public void testPgnToHtml() throws BestandException {
    String[]  args      = new String[] {TestConstants.PAR_BESTAND1,
                                        TestConstants.PAR_ENKEL,
                                        TestConstants.PAR_INVOERDIR + TEMP,
                                        TestConstants.PAR_UITVOERDIR + TEMP};

    VangOutEnErr.execute(PgnToHtml.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("PgnToHtml - helptekst", 19, out.size());
    assertEquals("PgnToHtml - fouten", 0, err.size());
    assertEquals("PgnToHtml - 14",
                 TEMP + File.separator + TestConstants.BST_COMPETITIE1_PGN,
                 out.get(13).split(":")[1].trim());
    assertEquals("PgnToHtml - 15", TestConstants.TOT_PARTIJEN,
                 out.get(14).split(":")[1].trim());
    assertEquals("PgnToHtml - 16", TEMP + File.separator,
                 out.get(15).split(":")[1].trim());
    assertTrue("PgnToHtml - equals I",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_INDEX_HTML),
            Bestand.openInvoerBestand(PgnToHtmlTest.class.getClassLoader(),
                                      TestConstants.BST_INDEX_HTML)));
    assertTrue("PgnToHtml - equals M",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_MATRIX_HTML),
            Bestand.openInvoerBestand(PgnToHtmlTest.class.getClassLoader(),
                                      BST_MATRIX1)));
  }

  @Test
  public void testOpStand() throws BestandException {
    String[]  args      = new String[] {TestConstants.PAR_BESTAND1,
                                        TestConstants.PAR_ENKEL,
                                        TestConstants.PAR_INVOERDIR + TEMP,
                                        TestConstants.PAR_MATRIX_OP_STAND,
                                        TestConstants.PAR_UITVOERDIR + TEMP};

    VangOutEnErr.execute(PgnToHtml.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("Op Stand - helptekst", 19, out.size());
    assertEquals("Op Stand - fouten", 0, err.size());
    assertEquals("PgnToHtml - 14",
                 TEMP + File.separator + TestConstants.BST_COMPETITIE1_PGN,
                 out.get(13).split(":")[1].trim());
    assertEquals("PgnToHtml - 15", TestConstants.TOT_PARTIJEN,
                 out.get(14).split(":")[1].trim());
    assertEquals("PgnToHtml - 16", TEMP + File.separator,
                 out.get(15).split(":")[1].trim());
    assertTrue("Op Stand - equals I",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_INDEX_HTML),
            Bestand.openInvoerBestand(PgnToHtmlTest.class.getClassLoader(),
                                      TestConstants.BST_INDEX_HTML)));
    assertTrue("PgnToHtml - equals M",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_MATRIX_HTML),
            Bestand.openInvoerBestand(PgnToHtmlTest.class.getClassLoader(),
                                      BST_MATRIX2)));
  }
}
