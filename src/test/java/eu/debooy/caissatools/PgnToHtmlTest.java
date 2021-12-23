/**
 * Copyright (c) 2018 Marco de Booij
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
    verwijderBestanden(getTemp() + File.separator,
                       new String[] {TestConstants.BST_COMPETITIE1_PGN,
                                     TestConstants.BST_INDEX_HTML,
                                     TestConstants.BST_MATRIX_HTML,
                                     BST_MATRIX1, BST_MATRIX2,
                                     TestConstants.BST_COMPETITIE1_PGN,
                                     TestConstants.BST_SCHEMA1_JSON,
                                     TestConstants.BST_UITSLAGEN_HTML});
  }

  @Before
  public void beforeTest() {
    verwijderBestanden(getTemp() + File.separator,
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
      kopieerBestand(CLASSLOADER, TestConstants.BST_COMPETITIE1_PGN,
                     getTemp() + File.separator + TestConstants.BST_COMPETITIE1_PGN);
      kopieerBestand(CLASSLOADER, TestConstants.BST_SCHEMA1_JSON,
                     getTemp() + File.separator + TestConstants.BST_SCHEMA1_JSON);
    } catch (IOException e) {
      System.out.println(e.getLocalizedMessage());
      throw new BestandException(e);
    }
  }

  @Test
  public void testLeeg() {
    String[]  args  = new String[] {};

    before();
    PgnToHtml.execute(args);
    after();

    assertEquals("Zonder parameters - helptekst", 27, out.size());
    assertEquals("Zonder parameters - fouten", 1, err.size());
  }

  @Test
  public void testPgnToHtml() throws BestandException {
    String[]  args  = new String[] {TestConstants.PAR_BESTAND1,
                                    TestConstants.PAR_INVOERDIR + getTemp(),
                                    TestConstants.PAR_SCHEMA1,
                                    TestConstants.PAR_UITVOERDIR + getTemp()};

    before();
    PgnToHtml.execute(args);
    after();

    assertEquals("PgnToHtml - helptekst", 19, out.size());
    assertEquals("PgnToHtml - fouten", 0, err.size());
    assertEquals("PgnToHtml - 14",
                 getTemp() + File.separator + TestConstants.BST_COMPETITIE1_PGN,
                 out.get(13).split(":")[1].trim());
    assertEquals("PgnToHtml - 15", TestConstants.TOT_PARTIJEN,
                 out.get(14).split(":")[1].trim());
    assertEquals("PgnToHtml - 16", getTemp() + File.separator,
                 out.get(15).split(":")[1].trim());
    assertTrue("PgnToHtml - equals I",
        Bestand.equals(
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_INDEX_HTML),
            Bestand.openInvoerBestand(CLASSLOADER,
                                      TestConstants.BST_INDEX_HTML)));
    assertTrue("PgnToHtml - equals M",
        Bestand.equals(
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_MATRIX_HTML),
            Bestand.openInvoerBestand(CLASSLOADER, BST_MATRIX1)));
  }

  @Test
  public void testOpStand() throws BestandException {
    String[]  args  = new String[] {TestConstants.PAR_BESTAND1,
                                    TestConstants.PAR_INVOERDIR + getTemp(),
                                    TestConstants.PAR_MATRIX_OP_STAND,
                                    TestConstants.PAR_SCHEMA1,
                                    TestConstants.PAR_UITVOERDIR + getTemp()};

    before();
    PgnToHtml.execute(args);
    after();

    assertEquals("Op Stand - helptekst", 19, out.size());
    assertEquals("Op Stand - fouten", 0, err.size());
    assertEquals("PgnToHtml - 14",
                 getTemp() + File.separator + TestConstants.BST_COMPETITIE1_PGN,
                 out.get(13).split(":")[1].trim());
    assertEquals("PgnToHtml - 15", TestConstants.TOT_PARTIJEN,
                 out.get(14).split(":")[1].trim());
    assertEquals("PgnToHtml - 16", getTemp() + File.separator,
                 out.get(15).split(":")[1].trim());
    assertTrue("Op Stand - equals I",
        Bestand.equals(
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_INDEX_HTML),
            Bestand.openInvoerBestand(CLASSLOADER,
                                      TestConstants.BST_INDEX_HTML)));
    assertTrue("PgnToHtml - equals M",
        Bestand.equals(
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_MATRIX_HTML),
            Bestand.openInvoerBestand(CLASSLOADER, BST_MATRIX2)));
    assertTrue("PgnToHtml - equals U",
        Bestand.equals(
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_UITSLAGEN_HTML),
            Bestand.openInvoerBestand(CLASSLOADER,
                                      TestConstants.BST_UITSLAGEN_HTML)));
  }
}
