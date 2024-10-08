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

import eu.debooy.doosutils.DoosConstants;
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
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * @author Marco de Booij
 */
public class PgnToHtmlTest extends BatchTest {
  protected static final  ClassLoader CLASSLOADER =
      PgnToHtmlTest.class.getClassLoader();

  private static final  String  BST_BESTANDE  = "schemaE.pgn";
  private static final  String  BST_INDEXE    = "indexE.html";
  private static final  String  BST_MATRIX1   = "matrix1.html";
  private static final  String  BST_MATRIX2   = "matrix2.html";
  private static final  String  BST_MATRIXE   = "matrixE.html";
  private static final  String  BST_SCHEMAE   = "schemaE.json";

  private static final  String  PAR_BESTANDE  =
      "--bestand=" + getTemp() + File.separator + "schemaE.pgn";

  @AfterClass
  public static void afterClass() {
    verwijderBestanden(getTemp() + File.separator,
                       new String[] {TestConstants.BST_COMPETITIE1_PGN,
                                     TestConstants.BST_INDEX_HTML,
                                     TestConstants.BST_MATRIX_HTML,
                                     BST_MATRIX1, BST_MATRIX2,
                                     TestConstants.BST_COMPETITIE2A_PGN,
                                     TestConstants.BST_SCHEMA1_JSON,
                                     TestConstants.BST_SCHEMA2_JSON,
                                     BST_BESTANDE, BST_SCHEMAE,
                                     TestConstants.BST_UITSLAGEN_HTML,
                                     TestConstants.BST_KALENDER_HTML,
                                     TestConstants.BST_INHALEN_HTML
                       });
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
    resourceBundle  = ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                                               Locale.getDefault());

    try {
      kopieerBestand(CLASSLOADER, TestConstants.BST_COMPETITIE2A_PGN,
                     getTemp() + File.separator
                      + TestConstants.BST_COMPETITIE2A_PGN);
      kopieerBestand(CLASSLOADER, BST_BESTANDE,
                     getTemp() + File.separator + BST_BESTANDE);
      kopieerBestand(CLASSLOADER, BST_SCHEMAE,
                     getTemp() + File.separator + BST_SCHEMAE);
      kopieerBestand(CLASSLOADER, TestConstants.BST_COMPETITIE1_PGN,
                     getTemp() + File.separator
                      + TestConstants.BST_COMPETITIE1_PGN);
      kopieerBestand(CLASSLOADER, TestConstants.BST_SCHEMA1_JSON,
                     getTemp() + File.separator
                      + TestConstants.BST_SCHEMA1_JSON);
      kopieerBestand(CLASSLOADER, TestConstants.BST_SCHEMA2_JSON,
                     getTemp() + File.separator
                      + TestConstants.BST_SCHEMA2_JSON);
    } catch (IOException e) {
      System.out.println(e.getLocalizedMessage());
      throw new BestandException(e);
    }
  }

  @Test
  public void testEnkelrondig() {
    String[]  args  = new String[] {PAR_BESTANDE,
                                    TestConstants.PAR_UITVOERDIR + getTemp()};

    before();
    PgnToHtml.execute(args);
    after();

    assertTrue(err.isEmpty());
    try {
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_INDEX_HTML),
              Bestand.openInvoerBestand(CLASSLOADER, BST_INDEXE)));
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_MATRIX_HTML),
              Bestand.openInvoerBestand(CLASSLOADER,  BST_MATRIXE)));
    } catch (BestandException e) {
      fail(e.getLocalizedMessage());
    }
  }

  @Test
  public void testLeeg() {
    String[]  args  = new String[] {};

    before();
    PgnToHtml.execute(args);
    after();

    assertEquals(1, err.size());
  }

  @Test
  public void testOpStand() {
    String[]  args  = new String[] {TestConstants.PAR_BESTAND1,
                                    TestConstants.PAR_MATRIX_OP_STAND,
                                    TestConstants.PAR_SCHEMA1,
                                    TestConstants.PAR_UITVOERDIR + getTemp()};

    before();
    PgnToHtml.execute(args);
    after();

    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator + TestConstants.BST_COMPETITIE1_PGN,
                 out.get(13).split(":")[1].trim());
    assertEquals(TestConstants.TOT_PARTIJEN,
                 out.get(14).split(":")[1].trim());
    assertEquals(getTemp(), out.get(15).split(":")[1].trim());
    try {
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_INDEX_HTML),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_INDEX_HTML)));
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_MATRIX_HTML),
              Bestand.openInvoerBestand(CLASSLOADER, BST_MATRIX2)));
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_UITSLAGEN_HTML),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_UITSLAGEN_HTML)));
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_KALENDER_HTML),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_KALENDER_HTML)));
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_INHALEN_HTML),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_INHALEN_HTML)));
    } catch (BestandException e) {
      fail(e.getLocalizedMessage());
    }
  }

  @Test
  public void testPgnToHtml1() {
    String[]  args  = new String[] {TestConstants.PAR_BESTAND1,
                                    TestConstants.PAR_SCHEMA1,
                                    TestConstants.PAR_UITVOERDIR + getTemp()};

    before();
    PgnToHtml.execute(args);
    after();

    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator + TestConstants.BST_COMPETITIE1_PGN,
                 out.get(13).split(":")[1].trim());
    assertEquals(TestConstants.TOT_PARTIJEN,
                 out.get(14).split(":")[1].trim());
    assertEquals(getTemp(), out.get(15).split(":")[1].trim());
    try {
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_INDEX_HTML),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_INDEX_HTML)));
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_MATRIX_HTML),
              Bestand.openInvoerBestand(CLASSLOADER, BST_MATRIX1)));
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_UITSLAGEN_HTML),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_UITSLAGEN_HTML)));
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_KALENDER_HTML),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_KALENDER_HTML)));
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_INHALEN_HTML),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_INHALEN_HTML)));
    } catch (BestandException e) {
      fail(e.getLocalizedMessage());
    }
  }

  @Test
  public void testPgnToHtml2() {
    String[]  args  = new String[] {TestConstants.PAR_BESTAND2A,
                                    TestConstants.PAR_SCHEMA2,
                                    TestConstants.PAR_UITVOERDIR + getTemp()};

    before();
    PgnToHtml.execute(args);
    after();

    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator
                  + TestConstants.BST_COMPETITIE2A_PGN,
                 out.get(13).split(":")[1].trim());
    assertEquals(TestConstants.TOT_PARTIJEN2,
                 out.get(14).split(":")[1].trim());
    assertEquals(getTemp(), out.get(15).split(":")[1].trim());

    try {
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_INDEX_HTML),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_INDEX2_HTML)));
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_MATRIX_HTML),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_MATRIX2A_HTML)));
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_UITSLAGEN_HTML),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_UITSLAGEN2_HTML)));
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_KALENDER_HTML),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_KALENDER2_HTML)));
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_INHALEN_HTML),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_INHALEN2_HTML)));
    } catch (BestandException e) {
      fail(e.getLocalizedMessage());
    }
  }

  @Test
  public void testPgnToHtml3() {
    String[]  args  = new String[] {TestConstants.PAR_BESTAND2A,
                                    TestConstants.PAR_SCHEMA2,
                                    "--metInhaaldatum",
                                    TestConstants.PAR_UITVOERDIR + getTemp()};

    before();
    PgnToHtml.execute(args);
    after();

    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator
                  + TestConstants.BST_COMPETITIE2A_PGN,
                 out.get(13).split(":")[1].trim());
    assertEquals(TestConstants.TOT_PARTIJEN2,
                 out.get(14).split(":")[1].trim());
    assertEquals(getTemp(), out.get(15).split(":")[1].trim());

    try {
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_INDEX_HTML),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_INDEX2_HTML)));
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_MATRIX_HTML),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_MATRIX2A_HTML)));
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_UITSLAGEN_HTML),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_UITSLAGEN2A_HTML)));
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_KALENDER_HTML),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_KALENDER2_HTML)));
      assertTrue(
          Bestand.equals(
              Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_INHALEN_HTML),
              Bestand.openInvoerBestand(CLASSLOADER,
                                        TestConstants.BST_INHALEN2_HTML)));
    } catch (BestandException e) {
      fail(e.getLocalizedMessage());
    }
  }
}
