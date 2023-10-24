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

import eu.debooy.doosutils.DoosConstants;
import eu.debooy.doosutils.access.Bestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.test.BatchTest;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.AfterClass;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Marco de Booij
 */
public class PgnToLatexTest extends BatchTest {
  protected static final  ClassLoader CLASSLOADER =
      PgnToLatexTest.class.getClassLoader();

  private static final  String  BST_COMPETITIE1_TEX = "competitie1.tex";
  private static final  String  BST_COMPETITIE2_TEX = "competitie2.tex";
  private static final  String  BST_COMPETITIE3_TEX = "competitie3.tex";
  private static final  String  BST_COMPETITIE4_TEX = "competitie4.tex";
  private static final  String  BST_COMPETITIE5_TEX = "competitie5.tex";

  private static final  String  PAR_AUTEUR    = "--auteur=Caissa Tools";
  private static final  String  PAR_BESTAND1  =
      "--bestand=/tmp/competitie1;/tmp/competitie2;competitie3";
  private static final  String  PAR_BESTAND2  =
      "--bestand=competitie1;competitie2";
  private static final  String  PAR_MATRIX    = "--matrix";
  private static final  String  PAR_SCHEMA2   =
      "--schema=schema1;schema2";
  private static final  String  PAR_TITEL     = "--titel=Testing 97/98 - 98/99";

  @AfterClass
  public static void afterClass() {
    verwijderBestanden(getTemp() + File.separator,
                       new String[] {TestConstants.BST_COMPETITIE1_PGN,
                                     TestConstants.BST_COMPETITIE2_PGN,
                                     TestConstants.BST_SCHEMA1_JSON,
                                     TestConstants.BST_SCHEMA2_JSON,
                                     BST_COMPETITIE1_TEX});
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    Locale.setDefault(new Locale(TestConstants.TST_TAAL));
    resourceBundle  = ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                                               Locale.getDefault());

    for (String bestand : new String[] {TestConstants.BST_COMPETITIE1_PGN,
                                        TestConstants.BST_COMPETITIE2_PGN,
                                        TestConstants.BST_SCHEMA1_JSON,
                                        TestConstants.BST_SCHEMA2_JSON}) {
      try {
        kopieerBestand(CLASSLOADER, bestand,
                       getTemp() + File.separator + bestand);
      } catch (IOException e) {
        throw new BestandException(e);
      }
    }
  }

  @Test
  public void testLeeg() {
    String[]  args  = new String[] {};

    before();
    PgnToLatex.execute(args);
    after();

    assertEquals(1, err.size());
  }

  @Test
  public void testOngelijkAantalBestanden() {
    String[]  args  = new String[] {PAR_AUTEUR, PAR_BESTAND2,
                                    TestConstants.PAR_SCHEMA1, PAR_TITEL};
    String[]  verwacht  = new String[] {
        resourceBundle.getString(CaissaTools.ERR_BEST_ONGELIJK)};

    before();
    PgnToLatex.execute(args);
    after();

    assertEquals(1, err.size());
    assertArrayEquals(verwacht, err.toArray());
  }

  @Test
  public void testFouten() {
    String[]  args  = new String[] {PAR_BESTAND1,
                                    TestConstants.PAR_SCHEMA1};
    String[]  verwacht  = new String[] {
        resourceBundle.getString(CaissaTools.ERR_BEST_ONGELIJK),
        resourceBundle.getString(CaissaTools.ERR_BIJBESTAND)};

    before();
    PgnToLatex.execute(args);
    after();

    assertEquals(2, err.size());
    assertArrayEquals(verwacht, err.toArray());
  }

  @Test
  public void testPgnToLatex() throws BestandException {
    String[]  args  = new String[] {TestConstants.PAR_BESTAND1,
                                    TestConstants.PAR_INVOERDIR + getTemp(),
                                    PAR_MATRIX,
                                    TestConstants.PAR_SCHEMA1,
                                    TestConstants.PAR_UITVOERDIR + getTemp()};

    try {
      Bestand.delete(getTemp() + File.separator + BST_COMPETITIE1_TEX);
    } catch (BestandException e) {
    }

    before();
    PgnToLatex.execute(args);
    after();

    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator + BST_COMPETITIE1_TEX,
                 out.get(13).split(":")[1].trim());
    assertEquals("150", out.get(14).split(":")[1].trim());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + BST_COMPETITIE1_TEX),
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPETITIE1_TEX)));

    Bestand.delete(getTemp() + File.separator + BST_COMPETITIE1_TEX);
  }

  @Test
  public void testPgnToLatex2() throws BestandException {
    String[]  args  = new String[] {PAR_AUTEUR, PAR_BESTAND2,
                                    TestConstants.PAR_INVOERDIR + getTemp(),
                                    PAR_SCHEMA2, PAR_TITEL,
                                    TestConstants.PAR_UITVOERDIR + getTemp(),
                                    PAR_MATRIX};

    try {
      Bestand.delete(getTemp() + File.separator + BST_COMPETITIE1_TEX);
    } catch (BestandException e) {
    }

    before();
    PgnToLatex.execute(args);
    after();

    if (!err.isEmpty()) {
      debug();
    }
    assertTrue(err.isEmpty());
    assertEquals(getTemp() + File.separator + BST_COMPETITIE1_TEX,
                 out.get(13).split(":")[1].trim());
    assertEquals(getTemp() + File.separator + BST_COMPETITIE2_TEX,
                 out.get(14).split(":")[1].trim());
    assertEquals("214", out.get(15).split(":")[1].trim());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + BST_COMPETITIE1_TEX),
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPETITIE4_TEX)));
    Bestand.delete(getTemp() + File.separator + BST_COMPETITIE1_TEX);
  }

  @Test
  public void testOpStand() throws BestandException {
    String[]  args  = new String[] {TestConstants.PAR_BESTAND1,
                                    TestConstants.PAR_INVOERDIR + getTemp(),
                                    PAR_MATRIX,
                                    TestConstants.PAR_MATRIX_OP_STAND,
                                    TestConstants.PAR_SCHEMA1,
                                    TestConstants.PAR_UITVOERDIR + getTemp()};

    try {
      Bestand.delete(getTemp() + File.separator + BST_COMPETITIE1_TEX);
    } catch (BestandException e) {
    }

    before();
    PgnToLatex.execute(args);
    after();

    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator + BST_COMPETITIE1_TEX,
                 out.get(13).split(":")[1].trim());
    assertEquals("150", out.get(14).split(":")[1].trim());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + BST_COMPETITIE1_TEX),
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPETITIE2_TEX)));

    Bestand.delete(getTemp() + File.separator + BST_COMPETITIE1_TEX);
  }

  @Test
  public void testZonderMatrix() throws BestandException {
    String[]  args  = new String[] {TestConstants.PAR_BESTAND1,
                                    TestConstants.PAR_INVOERDIR + getTemp(),
                                    TestConstants.PAR_SCHEMA1,
                                    TestConstants.PAR_UITVOERDIR + getTemp()};

    try {
      Bestand.delete(getTemp() + File.separator + BST_COMPETITIE1_TEX);
    } catch (BestandException e) {
    }

    before();
    PgnToLatex.execute(args);
    after();

    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator + BST_COMPETITIE1_TEX,
                 out.get(13).split(":")[1].trim());
    assertEquals("150", out.get(14).split(":")[1].trim());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + BST_COMPETITIE1_TEX),
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPETITIE3_TEX)));

    Bestand.delete(getTemp() + File.separator + BST_COMPETITIE1_TEX);
  }

  @Test
  public void testZonderMatrix2() throws BestandException {
    String[]  args  = new String[] {PAR_AUTEUR, PAR_BESTAND2,
                                    TestConstants.PAR_INVOERDIR + getTemp(),
                                    PAR_SCHEMA2, PAR_TITEL,
                                    TestConstants.PAR_UITVOERDIR + getTemp()};

    try {
      Bestand.delete(getTemp() + File.separator + BST_COMPETITIE1_TEX);
    } catch (BestandException e) {
    }

    before();
    PgnToLatex.execute(args);
    after();

    assertEquals( 0, err.size());
    assertEquals(getTemp() + File.separator + BST_COMPETITIE1_TEX,
                 out.get(13).split(":")[1].trim());
    assertEquals(getTemp() + File.separator + BST_COMPETITIE2_TEX,
                 out.get(14).split(":")[1].trim());
    assertEquals("214", out.get(15).split(":")[1].trim());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + BST_COMPETITIE1_TEX),
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPETITIE5_TEX)));

    Bestand.delete(getTemp() + File.separator + BST_COMPETITIE1_TEX);
  }
}
