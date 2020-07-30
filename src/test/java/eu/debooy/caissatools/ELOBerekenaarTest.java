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
import eu.debooy.doosutils.access.Bestand;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.test.BatchTest;
import eu.debooy.doosutils.test.DoosUtilsTestConstants;
import eu.debooy.doosutils.test.VangOutEnErr;
import java.io.File;
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


  private static String AANTAL150 = "150";

  private static String DATUM1  = "1997.12.20";
  private static String DATUM2  = "1997.12.31";
  private static String DATUM3  = "1998.06.13";

  public static final String  BST_COMP_TOT_CSV  = "competitie.totaal.csv";
  public static final String  BST_COMPH_TOT_CSV = "competitieH.totaal.csv";

  @Before
  public void before() {
    verwijderBestanden(TEMP + File.separator,
                       new String[] {TestConstants.BST_COMPETITIE_CSV,
                                     TestConstants.BST_COMPETITIEH_CSV,
                                     TestConstants.BST_INDEX_HTML,
                                     TestConstants.BST_MATRIX_HTML});
  }

  @AfterClass
  public static void afterClass() {
    verwijderBestanden(TEMP + File.separator,
                       new String[] {TestConstants.BST_COMPETITIE1_PGN,
                                     TestConstants.BST_COMPETITIE2_PGN,
                                     TestConstants.BST_COMPETITIE_CSV,
                                     TestConstants.BST_COMPETITIEH_CSV,
                                     TestConstants.BST_INDEX_HTML,
                                     TestConstants.BST_MATRIX_HTML});
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
                              .setBestand(TestConstants.BST_COMPETITIE1_PGN)
                              .build();
      doel  = new TekstBestand.Builder()
                              .setBestand(TEMP + File.separator
                                          + TestConstants.BST_COMPETITIE1_PGN)
                              .setLezen(false).build();
      doel.add(bron);
      bron.close();
      doel.close();
      bron  = new TekstBestand.Builder().setClassLoader(CLASSLOADER)
                              .setBestand(TestConstants.BST_COMPETITIE2_PGN)
                              .build();
      doel  = new TekstBestand.Builder()
                              .setBestand(TEMP + File.separator
                                          + TestConstants.BST_COMPETITIE2_PGN)
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

  @Test
  public void testLeeg() {
    String[]  args      = new String[] {};

    VangOutEnErr.execute(ELOBerekenaar.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("Zonder parameters - helptekst", 49, out.size());
    assertEquals("Zonder parameters - fouten", 1, err.size());
  }

  @Test
  public void testMetEindDatum() throws BestandException {
    String[]  args      = new String[] {TestConstants.PAR_TOERNOOIBESTAND1,
                                        TestConstants.PAR_SPELERBESTAND,
                                        TestConstants.PAR_INVOERDIR + TEMP,
                                        "--eindDatum=1997.12.31"};

    VangOutEnErr.execute(ELOBerekenaar.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("Met Einddatum 1 - helptekst", 22, out.size());
    assertEquals("Met Einddatum 1 - fouten", 0, err.size());
    assertEquals("Met Einddatum 1 - 15",
                 TEMP + File.separator + TestConstants.BST_COMPETITIE_CSV,
                 out.get(14).split(":")[1].trim());
    assertEquals("Met Einddatum 1 - 16", CaissaConstants.DEF_STARTDATUM,
                 out.get(15).split(":")[1].trim());
    assertEquals("Met Einddatum 1 - 17", DATUM2,
                 out.get(16).split(":")[1].trim());
    assertEquals("Met Einddatum 1 - 18", AANTAL150,
                 out.get(17).split(":")[1].trim());
    assertEquals("Met Einddatum 1 - 19", "52",
                 out.get(18).split(":")[1].trim());
    assertTrue("Met Einddatum 1 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitie.1997.12.31.csv"),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue("Met Einddatum 1 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitieH.1997.12.31.csv"),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV)));

    after();
    VangOutEnErr.execute(ELOBerekenaar.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("Met Einddatum 2 - helptekst", 21, out.size());
    assertEquals("Met Einddatum 2 - fouten", 0, err.size());
    assertEquals("Met Einddatum 2 - 14",
                 TEMP + File.separator + TestConstants.BST_COMPETITIE_CSV,
                 out.get(13).split(":")[1].trim());
    assertEquals("Met Einddatum 2 - 15", DATUM1,
                 out.get(14).split(":")[1].trim());
    assertEquals("Met Einddatum 2 - 16", DATUM2,
                 out.get(15).split(":")[1].trim());
    assertEquals("Met Einddatum 2 - 17", AANTAL150,
                 out.get(16).split(":")[1].trim());
    assertEquals("Met Einddatum 2 - 18", "0",
                 out.get(17).split(":")[1].trim());
    assertTrue("Met Einddatum 2 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitie.1997.12.31.csv"),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue("Met Einddatum 2 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitieH.1997.12.31.csv")));

    after();

    args  = new String[] {TestConstants.PAR_TOERNOOIBESTAND1,
                          TestConstants.PAR_SPELERBESTAND,
                          TestConstants.PAR_INVOERDIR + TEMP};
    VangOutEnErr.execute(ELOBerekenaar.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("Met Einddatum 3 - helptekst", 20, out.size());
    assertEquals("Met Einddatum 3 - fouten", 0, err.size());
    assertEquals("Met Einddatum 3 - 14",
                 TEMP + File.separator + TestConstants.BST_COMPETITIE_CSV,
                 out.get(13).split(":")[1].trim());
    assertEquals("Met Einddatum 3 - 15", DATUM1,
                 out.get(14).split(":")[1].trim());
    assertEquals("Met Einddatum 3 - 16", AANTAL150,
                 out.get(15).split(":")[1].trim());
    assertEquals("Met Einddatum 3 - 17", "98",
                 out.get(16).split(":")[1].trim());
    assertTrue("Met Einddatum 3 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      BST_COMP_TOT_CSV)));
    assertTrue("Met Einddatum 3 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      BST_COMPH_TOT_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV)));
  }

  @Test
  public void testVolledigBestand() throws BestandException {
    String[]  args      = new String[] {TestConstants.PAR_TOERNOOIBESTAND1,
                                        TestConstants.PAR_SPELERBESTAND,
                                        TestConstants.PAR_INVOERDIR + TEMP};

    VangOutEnErr.execute(ELOBerekenaar.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("Met Volledig 1 - helptekst", 21, out.size());
    assertEquals("Met Volledig 1 - fouten", 0, err.size());
    assertEquals("Met Volledig 1 - 15",
                 TEMP + File.separator + TestConstants.BST_COMPETITIE_CSV,
                 out.get(14).split(":")[1].trim());
    assertEquals("Met Volledig 1 - 16", CaissaConstants.DEF_STARTDATUM,
                 out.get(15).split(":")[1].trim());
    assertEquals("Volledig 1 - 17", AANTAL150,
                 out.get(16).split(":")[1].trim());
    assertEquals("Volledig 1 - 18", AANTAL150,
                 out.get(17).split(":")[1].trim());
    assertTrue("Volledig 1 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      BST_COMP_TOT_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue("Volledig 1 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      BST_COMPH_TOT_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV)));

    after();

    VangOutEnErr.execute(ELOBerekenaar.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("Volledig 2 - helptekst", 20, out.size());
    assertEquals("Volledig 2 - fouten", 0, err.size());
    assertEquals("Volledig 2 - 14",
                 TEMP + File.separator + TestConstants.BST_COMPETITIE_CSV,
                 out.get(13).split(":")[1].trim());
    assertEquals("Volledig 2 - 15", DATUM3,
                 out.get(14).split(":")[1].trim());
    assertEquals("Volledig 2 - 16", AANTAL150,
                 out.get(15).split(":")[1].trim());
    assertEquals("Volledig 2 - 17", "0",
                 out.get(16).split(":")[1].trim());
    assertTrue("Volledig 2 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      BST_COMP_TOT_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue("Volledig 2 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      BST_COMPH_TOT_CSV)));
  }

  @Test
  public void testVolledigBestandExtra() throws BestandException {
    String[]  args      = new String[] {"--extraInfo=J",
                                        TestConstants.PAR_TOERNOOIBESTAND1,
                                        TestConstants.PAR_SPELERBESTAND,
                                        TestConstants.PAR_INVOERDIR + TEMP};

    VangOutEnErr.execute(ELOBerekenaar.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("Met Volledig Extra - helptekst", 21, out.size());
    assertEquals("Met Volledig Extra - fouten", 0, err.size());
    assertEquals("Met Volledig Extra - 14",
                 "Maak bestand "+ TEMP + File.separator
                 + TestConstants.BST_COMPETITIE_CSV + ".",
                 out.get(13).trim());
    assertEquals("Met Volledig Extra - 15",
                 TEMP + File.separator + TestConstants.BST_COMPETITIE_CSV,
                 out.get(14).split(":")[1].trim());
    assertEquals("Met Volledig Extra - 16", CaissaConstants.DEF_STARTDATUM,
                 out.get(15).split(":")[1].trim());
    assertEquals("Volledig Extra - 17", AANTAL150,
                 out.get(16).split(":")[1].trim());
    assertEquals("Volledig Extra - 18", AANTAL150,
                 out.get(17).split(":")[1].trim());
    assertTrue("Volledig Extra - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(ELOBerekenaar.class.getClassLoader(),
                                      BST_COMP_TOT_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue("Volledig Extra - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(ELOBerekenaar.class.getClassLoader(),
                "competitieH.extra.csv"),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV)));
  }

  @Test
  public void testTweeBestanden() throws BestandException {
    String[]  args      = new String[] {TestConstants.PAR_TOERNOOIBESTAND1,
                                        TestConstants.PAR_SPELERBESTAND,
                                        TestConstants.PAR_INVOERDIR + TEMP};

    VangOutEnErr.execute(ELOBerekenaar.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("Twee Bestanden 1 - helptekst", 21, out.size());
    assertEquals("Twee Bestanden 1 - fouten", 0, err.size());
    assertEquals("Twee Bestanden 1 - 15",
                 TEMP + File.separator + TestConstants.BST_COMPETITIE_CSV,
                 out.get(14).split(":")[1].trim());
    assertEquals("Twee Bestanden 1 - 16", CaissaConstants.DEF_STARTDATUM,
                 out.get(15).split(":")[1].trim());
    assertEquals("Twee Bestanden 1 - 17", AANTAL150,
                 out.get(16).split(":")[1].trim());
    assertEquals("Twee Bestanden 1 - 18", AANTAL150,
                 out.get(17).split(":")[1].trim());
    assertTrue("Twee Bestanden 1 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      BST_COMP_TOT_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue("Twee Bestanden 1 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      BST_COMPH_TOT_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV)));

    after();

    args      = new String[] {"--toernooiBestand=competitie2",
                              TestConstants.PAR_SPELERBESTAND,
                              TestConstants.PAR_INVOERDIR + TEMP};

    VangOutEnErr.execute(ELOBerekenaar.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("Twee Bestanden 2 - helptekst", 20, out.size());
    assertEquals("Twee Bestanden 2 - fouten", 0, err.size());
    assertEquals("Twee Bestanden 2 - 14",
                 TEMP + File.separator + TestConstants.BST_COMPETITIE_CSV,
                 out.get(13).split(":")[1].trim());
    assertEquals("Twee Bestanden 2 - 15", DATUM3,
                 out.get(14).split(":")[1].trim());
    assertEquals("Twee Bestanden 2 - 16", "64",
                 out.get(15).split(":")[1].trim());
    assertEquals("Twee Bestanden 2 - 17", "62",
                 out.get(16).split(":")[1].trim());
    assertTrue("Twee Bestanden 2 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitie.dubbel.csv")));
    assertTrue("Volledig 2 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitieH.dubbel.csv")));
  }
}
