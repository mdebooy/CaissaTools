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
  private static final  String  PAR_EXTRAINFO = "--extraInfo=J";

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
    Locale.setDefault(new Locale(TestConstants.TST_TAAL));
    resourceBundle  = ResourceBundle.getBundle("ApplicatieResources",
                                               Locale.getDefault());

    for (String bestand : new String[] {TestConstants.BST_COMPETITIE1_PGN,
                                        TestConstants.BST_COMPETITIE2_PGN}) {
      try {
        kopieerBestand(CLASSLOADER, bestand, TEMP + File.separator + bestand);
      } catch (IOException e) {
        throw new BestandException(e);
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
                                        PAR_EINDDATUM};

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
    assertEquals("Met Einddatum 1 - 18", TestConstants.TOT_PARTIJEN,
                 out.get(17).split(":")[1].trim());
    assertEquals("Met Einddatum 1 - 19", "52",
                 out.get(18).split(":")[1].trim());
    assertTrue("Met Einddatum 1 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMP_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue("Met Einddatum 1 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPH_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV)));

    after();
    VangOutEnErr.execute(ELOBerekenaar.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("Met Einddatum 2 - helptekst", 20, out.size());
    assertEquals("Met Einddatum 2 - fouten", 0, err.size());
    assertEquals("Met Einddatum 2 - 14",
                 TEMP + File.separator + TestConstants.BST_COMPETITIE_CSV,
                 out.get(13).split(":")[1].trim());
    assertEquals("Met Einddatum 2 - 15", DATUM1,
                 out.get(14).split(":")[1].trim());
    assertEquals("Met Einddatum 2 - 16", DATUM2,
                 out.get(15).split(":")[1].trim());
    assertEquals("Met Einddatum 2 - 17", TestConstants.TOT_PARTIJEN,
                 out.get(16).split(":")[1].trim());
    assertTrue("Met Einddatum 2 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMP_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue("Met Einddatum 2 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPH_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV)));

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
    assertEquals("Met Einddatum 3 - 16", TestConstants.TOT_PARTIJEN,
                 out.get(15).split(":")[1].trim());
    assertEquals("Met Einddatum 3 - 17", "98",
                 out.get(16).split(":")[1].trim());
    assertTrue("Met Einddatum 3 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMP_TOT_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue("Met Einddatum 3 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPH_TOT_CSV),
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
    assertEquals("Volledig 1 - 17", TestConstants.TOT_PARTIJEN,
                 out.get(16).split(":")[1].trim());
    assertEquals("Volledig 1 - 18", TestConstants.TOT_PARTIJEN,
                 out.get(17).split(":")[1].trim());
    assertTrue("Volledig 1 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMP_TOT_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue("Volledig 1 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPH_TOT_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV)));

    after();

    VangOutEnErr.execute(ELOBerekenaar.class,
                         DoosUtilsTestConstants.CMD_EXECUTE, args, out, err);

    assertEquals("Volledig 2 - helptekst", 19, out.size());
    assertEquals("Volledig 2 - fouten", 0, err.size());
    assertEquals("Volledig 2 - 14",
                 TEMP + File.separator + TestConstants.BST_COMPETITIE_CSV,
                 out.get(13).split(":")[1].trim());
    assertEquals("Volledig 2 - 15", DATUM3,
                 out.get(14).split(":")[1].trim());
    assertEquals("Volledig 2 - 16", TestConstants.TOT_PARTIJEN,
                 out.get(15).split(":")[1].trim());
    assertTrue("Volledig 2 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMP_TOT_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue("Volledig 2 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPH_TOT_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV)));
  }

  @Test
  public void testVolledigBestandExtra() throws BestandException {
    String[]  args      = new String[] {PAR_EXTRAINFO,
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
    assertEquals("Volledig Extra - 17", TestConstants.TOT_PARTIJEN,
                 out.get(16).split(":")[1].trim());
    assertEquals("Volledig Extra - 18", TestConstants.TOT_PARTIJEN,
                 out.get(17).split(":")[1].trim());
    assertTrue("Volledig Extra - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMP_TOT_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue("Volledig Extra - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, "competitieH.extra.csv"),
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
    assertEquals("Twee Bestanden 1 - 17", TestConstants.TOT_PARTIJEN,
                 out.get(16).split(":")[1].trim());
    assertEquals("Twee Bestanden 1 - 18", TestConstants.TOT_PARTIJEN,
                 out.get(17).split(":")[1].trim());
    assertTrue("Twee Bestanden 1 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMP_TOT_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue("Twee Bestanden 1 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPH_TOT_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV)));

    after();

    args      = new String[] {TestConstants.PAR_TOERNOOIBESTAND2,
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
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPD_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIE_CSV)));
    assertTrue("Volledig 2 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(CLASSLOADER, BST_COMPHD_CSV),
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + TestConstants.BST_COMPETITIEH_CSV)));
  }
}
