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
import java.text.MessageFormat;
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
public class ELOBerekenaarTest extends BatchTest {
  protected static final  ClassLoader CLASSLOADER =
      ELOBerekenaarTest.class.getClassLoader();

  private static final  String  BST_COMP_TOT_CSV  = "competitie.totaal.csv";
  private static final  String  BST_COMPH_TOT_CSV = "competitieH.totaal.csv";
  private static final  String  BST_COMP_CSV      = "competitie.1997.12.31.csv";
  private static final  String  BST_COMPH_CSV     =
      "competitieH.1997.12.31.csv";
  private static final  String  BST_COMPD_CSV     = "competitie.dubbel.csv";
  private static final  String  BST_COMPHD_CSV    = "competitieH.dubbel.csv";
  private static final  String  DATUM1            = "1997.12.20";
  private static final  String  DATUM2            = "1997.12.31";
  private static final  String  DATUM3            = "1998.06.13";

  private static final  String  PAR_EINDDATUM = "--eindDatum=1997.12.31";
  private static final  String  PAR_EXTRAINFO = "--extraInfo";

  @Before
  public void beforeTest() {
    verwijderBestanden(getTemp() + File.separator,
                       new String[] {TestConstants.BST_COMPETITIE_CSV,
                                     TestConstants.BST_COMPETITIEH_CSV,
                                     TestConstants.BST_GESCHIEDENIS_CSV,
                                     TestConstants.BST_INDEX_HTML,
                                     TestConstants.BST_MATRIX_HTML});
  }

  @AfterClass
  public static void afterClass() {
    verwijderBestanden(getTemp() + File.separator,
                       new String[] {TestConstants.BST_COMPETITIE1_PGN,
                                     TestConstants.BST_COMPETITIE2_PGN,
                                     TestConstants.BST_COMPETITIE_CSV,
                                     TestConstants.BST_COMPETITIEH_CSV,
                                     TestConstants.BST_GESCHIEDENIS_CSV,
                                     TestConstants.BST_INDEX_HTML,
                                     TestConstants.BST_MATRIX_HTML});
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    Locale.setDefault(new Locale(TestConstants.TST_TAAL));
    resourceBundle  = ResourceBundle.getBundle(DoosConstants.RESOURCEBUNDLE,
                                               Locale.getDefault());

    for (String bestand : new String[] {TestConstants.BST_COMPETITIE1_PGN,
                                        TestConstants.BST_COMPETITIE2_PGN}) {
      try {
        kopieerBestand(CLASSLOADER, bestand, getTemp()
                        + File.separator + bestand);
      } catch (IOException e) {
        throw new BestandException(e);
      }
    }
  }

  @Test
  public void testEnkelMaxVerschil() {
    String[]  args  = new String[] {TestConstants.PAR_TOERNOOIBESTAND1,
                                    TestConstants.PAR_SPELERBESTAND,
                                    "--" + CaissaTools.PAR_MAXVERSCHIL, "600"};

    before();
    ELOBerekenaar.execute(args);
    after();

    assertEquals(1, err.size());
    assertEquals(resourceBundle.getString(CaissaTools.ERR_MAXVERSCHIL),
                 err.get(0));
  }

  @Test
  public void testFouteDatums() {
    String[]  args  = new String[] {TestConstants.PAR_TOERNOOIBESTAND1,
                                    TestConstants.PAR_SPELERBESTAND,
                                    "--" + CaissaTools.PAR_EINDDATUM,
                                    "0000.00.00",
                                    "--" + CaissaTools.PAR_STARTDATUM,
                                    "9999.99.99"};

    before();
    ELOBerekenaar.execute(args);
    after();

    assertEquals(1, err.size());
    assertEquals(MessageFormat.format(
          resourceBundle.getString(CaissaTools.ERR_EINDVOORSTART),
          "9999.99.99", "0000.00.00"),
                 err.get(0));
  }

  @Test
  public void testLeeg() {
    String[]  args  = new String[] {};

    before();
    ELOBerekenaar.execute(args);
    after();

    assertEquals(1, err.size());
    assertEquals("PAR-0001", err.get(0).split(" ")[0]);
  }

  @Test
  public void testMaxVerschil() {
    String[]  args  = new String[] {TestConstants.PAR_TOERNOOIBESTAND1,
                                    TestConstants.PAR_SPELERBESTAND,
                                    "--" + CaissaTools.PAR_MAXVERSCHIL, "600",
                                    "--" + CaissaTools.PAR_VASTEKFACTOR, "3"};

    before();
    ELOBerekenaar.execute(args);
    after();

    assertTrue(err.isEmpty());
  }

  @Test
  public void testMetEindDatum() throws BestandException {
    String[]  args  = new String[] {TestConstants.PAR_TOERNOOIBESTAND1,
                                    TestConstants.PAR_SPELERBESTAND,
                                    PAR_EINDDATUM};

    before();
    ELOBerekenaar.execute(args);
    after();

    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator + TestConstants.BST_COMPETITIE_CSV,
                 out.get(14).split(":")[1].trim());
    assertEquals(DATUM2, out.get(15).split(":")[1].trim());
    assertEquals(TestConstants.TOT_PARTIJEN, out.get(16).split(":")[1].trim());
    assertEquals("52", out.get(17).split(":")[1].trim());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMP_CSV),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPH_CSV),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV)));

    before();
    ELOBerekenaar.execute(args);
    after();

    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator + TestConstants.BST_COMPETITIE_CSV,
                 out.get(13).split(":")[1].trim());
    assertEquals(DATUM2, out.get(14).split(":")[1].trim());
    assertEquals(TestConstants.TOT_PARTIJEN, out.get(15).split(":")[1].trim());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMP_CSV),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPH_CSV),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV)));

    args  = new String[] {TestConstants.PAR_TOERNOOIBESTAND1,
                          TestConstants.PAR_SPELERBESTAND};

    before();
    ELOBerekenaar.execute(args);
    after();

    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator + TestConstants.BST_COMPETITIE_CSV,
                 out.get(13).split(":")[1].trim());
    assertEquals(TestConstants.TOT_PARTIJEN, out.get(14).split(":")[1].trim());
    assertEquals("98", out.get(15).split(":")[1].trim());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMP_TOT_CSV),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPH_TOT_CSV),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV)));
  }

  @Test
  public void testVolledigBestand() throws BestandException {
    String[]  args  = new String[] {TestConstants.PAR_TOERNOOIBESTAND1,
                                    TestConstants.PAR_SPELERBESTAND};

    before();
    ELOBerekenaar.execute(args);
    after();

    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator + TestConstants.BST_COMPETITIE_CSV,
                 out.get(14).split(":")[1].trim());
    assertEquals(TestConstants.TOT_PARTIJEN,
                 out.get(15).split(":")[1].trim());
    assertEquals(TestConstants.TOT_PARTIJEN,
                 out.get(16).split(":")[1].trim());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMP_TOT_CSV),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPH_TOT_CSV),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV)));

    before();
    ELOBerekenaar.execute(args);
    after();

    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator + TestConstants.BST_COMPETITIE_CSV,
                 out.get(13).split(":")[1].trim());
    assertEquals(TestConstants.TOT_PARTIJEN, out.get(14).split(":")[1].trim());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMP_TOT_CSV),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPH_TOT_CSV),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV)));
  }

  @Test
  public void testVolledigBestandExtra() throws BestandException {
    String[]  args  = new String[] {PAR_EXTRAINFO,
                                    TestConstants.PAR_TOERNOOIBESTAND1,
                                    TestConstants.PAR_SPELERBESTAND};

    before();
    ELOBerekenaar.execute(args);
    after();

    assertEquals(0, err.size());
    assertEquals("Maak bestand "+ getTemp() + File.separator
                 + TestConstants.BST_COMPETITIE_CSV + ".",
                 out.get(13).trim());
    assertEquals(getTemp() + File.separator + TestConstants.BST_COMPETITIE_CSV,
                 out.get(14).split(":")[1].trim());
    assertEquals(TestConstants.TOT_PARTIJEN,
                 out.get(15).split(":")[1].trim());
    assertEquals(TestConstants.TOT_PARTIJEN,
                 out.get(16).split(":")[1].trim());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMP_TOT_CSV),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, "competitieH.extra.csv"),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV)));
  }

  @Test
  public void testMetGeschiedenisbestand() throws BestandException {
    String[]  args  = new String[] {TestConstants.PAR_TOERNOOIBESTAND1,
                                    TestConstants.PAR_GESCHIEDENIS,
                                    TestConstants.PAR_SPELERBESTAND};

    before();
    ELOBerekenaar.execute(args);
    after();

    assertEquals(0, err.size());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPH_TOT_CSV),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_GESCHIEDENIS_CSV)));

    args  = new String[] {TestConstants.PAR_TOERNOOIBESTAND2,
                          TestConstants.PAR_GESCHIEDENIS,
                          TestConstants.PAR_SPELERBESTAND};

    before();
    ELOBerekenaar.execute(args);
    after();

    assertEquals(0, err.size());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPHD_CSV),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                        + TestConstants.BST_GESCHIEDENIS_CSV)));
  }

  @Test
  public void testTweeBestanden() throws BestandException {
    String[]  args  = new String[] {TestConstants.PAR_TOERNOOIBESTAND1,
                                    TestConstants.PAR_SPELERBESTAND};

    before();
    ELOBerekenaar.execute(args);
    after();

    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator + TestConstants.BST_COMPETITIE_CSV,
                 out.get(14).split(":")[1].trim());
    assertEquals(TestConstants.TOT_PARTIJEN,
                 out.get(15).split(":")[1].trim());
    assertEquals(TestConstants.TOT_PARTIJEN,
                 out.get(16).split(":")[1].trim());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMP_TOT_CSV),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPH_TOT_CSV),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV)));

    args      = new String[] {TestConstants.PAR_TOERNOOIBESTAND2,
                              TestConstants.PAR_SPELERBESTAND};

    before();
    ELOBerekenaar.execute(args);
    after();

    assertEquals(0, err.size());
    assertEquals(getTemp() + File.separator + TestConstants.BST_COMPETITIE_CSV,
                 out.get(13).split(":")[1].trim());
    assertEquals("64", out.get(14).split(":")[1].trim());
    assertEquals("62", out.get(15).split(":")[1].trim());
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPD_CSV),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue(
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPHD_CSV),
            Bestand.openInvoerBestand(getTemp() + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV)));
  }
}
