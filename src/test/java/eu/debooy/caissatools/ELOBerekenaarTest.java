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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import eu.debooy.doosutils.access.Bestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.test.BatchTest;
import eu.debooy.doosutils.test.VangOutEnErr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * @author Marco de Booij
 */
public class ELOBerekenaarTest extends BatchTest {
  @AfterClass
  public static void afterClass() throws BestandException {
    Bestand.delete(TEMP + File.separator + "competitie1.pgn");
    Bestand.delete(TEMP + File.separator + "competitie2.pgn");
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    Locale.setDefault(new Locale("nl"));
    BufferedReader  bron  = null;
    BufferedWriter  doel  = null;
    try {
      bron  = Bestand.openInvoerBestand(PgnToLatexTest.class.getClassLoader(),
                                        "competitie1.pgn");
      doel  = Bestand.openUitvoerBestand(TEMP + File.separator
                                         + "competitie1.pgn");
      kopieerBestand(bron, doel);
      bron.close();
      doel.close();
      bron  = Bestand.openInvoerBestand(PgnToLatexTest.class.getClassLoader(),
                                        "competitie2.pgn");
      doel  = Bestand.openUitvoerBestand(TEMP + File.separator
                                         + "competitie2.pgn");
      kopieerBestand(bron, doel);
    } catch (IOException e) {
      throw new BestandException(e);
    } finally {
      try {
        if (null != bron) {
          bron.close();
        }
      } catch (IOException e) {
        throw new BestandException(e);
      }
      try {
        if (null != doel) {
          doel.close();
        }
      } catch (IOException e) {
        throw new BestandException(e);
      }
    }
  }

  @Test
  public void testLeeg() {
    String[]  args      = new String[] {};

    VangOutEnErr.execute(ELOBerekenaar.class, "execute", args, out, err);

    assertEquals("Zonder parameters - helptekst", 47, out.size());
    assertEquals("Zonder parameters - fouten", 0, 0);
  }

  @Test
  public void testMetEindDatum() throws BestandException {
    String[]  args      = new String[] {"--toernooiBestand=competitie1",
                                        "--spelerBestand=competitie",
                                        "--invoerdir=" + TEMP,
                                        "--eindDatum=1997.12.31"};

    try {
      Bestand.delete(TEMP + File.separator + "competitie.csv");
      Bestand.delete(TEMP + File.separator + "competitieH.csv");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(ELOBerekenaar.class, "execute", args, out, err);

    assertEquals("Met Einddatum 1 - helptekst", 19, out.size());
    assertEquals("Met Einddatum 1 - fouten", 0, 0);
    assertEquals("Met Einddatum 1 - 14",
                 TEMP + File.separator + "competitie.csv",
                 out.get(13).split(":")[1].trim());
    assertEquals("Met Einddatum 1 - 15", "0000.00.00",
                 out.get(14).split(":")[1].trim());
    assertEquals("Met Einddatum 1 - 16", "1997.12.31",
                 out.get(15).split(":")[1].trim());
    assertEquals("Met Einddatum 1 - 17", "150",
                 out.get(16).split(":")[1].trim());
    assertEquals("Met Einddatum 1 - 18", "52",
                 out.get(17).split(":")[1].trim());
    assertTrue("Met Einddatum 1 - equals",
               Bestand.equals(
        Bestand.openInvoerBestand(TEMP + File.separator + "competitie.csv"),
        Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                  "competitie.1997.12.31.csv")));
    assertTrue("Met Einddatum 1 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + "competitieH.csv"),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitieH.1997.12.31.csv")));

    after();

    VangOutEnErr.execute(ELOBerekenaar.class, "execute", args, out, err);

    assertEquals("Met Einddatum 2 - helptekst", 19, out.size());
    assertEquals("Met Einddatum 2 - fouten", 0, 0);
    assertEquals("Met Einddatum 2 - 14",
                 TEMP + File.separator + "competitie.csv",
                 out.get(13).split(":")[1].trim());
    assertEquals("Met Einddatum 2 - 15", "1997.12.20",
                 out.get(14).split(":")[1].trim());
    assertEquals("Met Einddatum 2 - 16", "1997.12.31",
                 out.get(15).split(":")[1].trim());
    assertEquals("Met Einddatum 2 - 17", "150",
                 out.get(16).split(":")[1].trim());
    assertEquals("Met Einddatum 2 - 18", "0",
                 out.get(17).split(":")[1].trim());
    assertTrue("Met Einddatum 2 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator + "competitie.csv"),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitie.1997.12.31.csv")));
    assertTrue("Met Einddatum 2 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + "competitieH.csv"),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitieH.1997.12.31.csv")));

    after();

    args  = new String[] {"--toernooiBestand=competitie1",
                          "--spelerBestand=competitie",
                          "--invoerdir=" + TEMP};
    VangOutEnErr.execute(ELOBerekenaar.class, "execute", args, out, err);

    assertEquals("Met Einddatum 3 - helptekst", 18, out.size());
    assertEquals("Met Einddatum 3 - fouten", 0, 0);
    assertEquals("Met Einddatum 3 - 14",
                 TEMP + File.separator + "competitie.csv",
                 out.get(13).split(":")[1].trim());
    assertEquals("Met Einddatum 3 - 15", "1997.12.20",
                 out.get(14).split(":")[1].trim());
    assertEquals("Met Einddatum 3 - 16", "150",
                 out.get(15).split(":")[1].trim());
    assertEquals("Met Einddatum 3 - 17", "98",
                 out.get(16).split(":")[1].trim());
    assertTrue("Met Einddatum 3 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator + "competitie.csv"),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitie.totaal.csv")));
    assertTrue("Met Einddatum 3 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + "competitieH.csv"),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitieH.totaal.csv")));

    Bestand.delete(TEMP + File.separator + "competitie.csv");
    Bestand.delete(TEMP + File.separator + "competitieH.csv");
  }

  @Test
  public void testVolledigBestand() throws BestandException {
    String[]  args      = new String[] {"--toernooiBestand=competitie1",
                                        "--spelerBestand=competitie",
                                        "--invoerdir=" + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + "competitie.csv");
      Bestand.delete(TEMP + File.separator + "competitieH.csv");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(ELOBerekenaar.class, "execute", args, out, err);

    assertEquals("Met Volledig 1 - helptekst", 18, out.size());
    assertEquals("Met Volledig 1 - fouten", 0, 0);
    assertEquals("Met Volledig 1 - 14",
                 TEMP + File.separator + "competitie.csv",
                 out.get(13).split(":")[1].trim());
    assertEquals("Met Volledig 1 - 15", "0000.00.00",
                 out.get(14).split(":")[1].trim());
    assertEquals("Volledig 1 - 16", "150",
                 out.get(15).split(":")[1].trim());
    assertEquals("Volledig 1 - 17", "150",
                 out.get(16).split(":")[1].trim());
    assertTrue("Volledig 1 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator + "competitie.csv"),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitie.totaal.csv")));
    assertTrue("Volledig 1 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + "competitieH.csv"),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitieH.totaal.csv")));

    after();

    VangOutEnErr.execute(ELOBerekenaar.class, "execute", args, out, err);
    assertEquals("Volledig 2 - helptekst", 18, out.size());
    assertEquals("Volledig 2 - fouten", 0, 0);
    assertEquals("Volledig 2 - 14",
                 TEMP + File.separator + "competitie.csv",
                 out.get(13).split(":")[1].trim());
    assertEquals("Volledig 2 - 15", "1998.06.13",
                 out.get(14).split(":")[1].trim());
    assertEquals("Volledig 2 - 16", "150",
                 out.get(15).split(":")[1].trim());
    assertEquals("Volledig 2 - 17", "0",
                 out.get(16).split(":")[1].trim());
    assertTrue("Volledig 2 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator + "competitie.csv"),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitie.totaal.csv")));
    assertTrue("Volledig 2 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + "competitieH.csv"),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitieH.totaal.csv")));

    Bestand.delete(TEMP + File.separator + "competitie.csv");
    Bestand.delete(TEMP + File.separator + "competitieH.csv");
  }

  @Test
  public void testVolledigBestandExtra() throws BestandException {
    String[]  args      = new String[] {"--extraInfo=J",
                                        "--toernooiBestand=competitie1",
                                        "--spelerBestand=competitie",
                                        "--invoerdir=" + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + "competitie.csv");
      Bestand.delete(TEMP + File.separator + "competitieH.csv");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(ELOBerekenaar.class, "execute", args, out, err);

    assertEquals("Met Volledig Extra - helptekst", 18, out.size());
    assertEquals("Met Volledig Extra - fouten", 0, 0);
    assertEquals("Met Volledig Extra - 14",
                 TEMP + File.separator + "competitie.csv",
                 out.get(13).split(":")[1].trim());
    assertEquals("Met Volledig Extra - 15", "0000.00.00",
                 out.get(14).split(":")[1].trim());
    assertEquals("Volledig Extra - 16", "150",
                 out.get(15).split(":")[1].trim());
    assertEquals("Volledig Extra - 17", "150",
                 out.get(16).split(":")[1].trim());
    assertTrue("Volledig Extra - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator + "competitie.csv"),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitie.totaal.csv")));
    assertTrue("Volledig Extra - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + "competitieH.csv"),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitieH.extra.csv")));

    Bestand.delete(TEMP + File.separator + "competitie.csv");
    Bestand.delete(TEMP + File.separator + "competitieH.csv");
  }

  @Test
  public void testTweeBestanden() throws BestandException {
    String[]  args      = new String[] {"--toernooiBestand=competitie1",
                                        "--spelerBestand=competitie",
                                        "--invoerdir=" + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + "competitie.csv");
      Bestand.delete(TEMP + File.separator + "competitieH.csv");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(ELOBerekenaar.class, "execute", args, out, err);

    assertEquals("Twee Bestanden 1 - helptekst", 18, out.size());
    assertEquals("Twee Bestanden 1 - fouten", 0, 0);
    assertEquals("Twee Bestanden 1 - 14",
                 TEMP + File.separator + "competitie.csv",
                 out.get(13).split(":")[1].trim());
    assertEquals("Twee Bestanden 1 - 15", "0000.00.00",
                 out.get(14).split(":")[1].trim());
    assertEquals("Twee Bestanden 1 - 16", "150",
                 out.get(15).split(":")[1].trim());
    assertEquals("Twee Bestanden 1 - 17", "150",
                 out.get(16).split(":")[1].trim());
    assertTrue("Twee Bestanden 1 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator + "competitie.csv"),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitie.totaal.csv")));
    assertTrue("Volledig 1 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + "competitieH.csv"),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitieH.totaal.csv")));

    after();

    args      = new String[] {"--toernooiBestand=competitie2",
                              "--spelerBestand=competitie",
                              "--invoerdir=" + TEMP};

    VangOutEnErr.execute(ELOBerekenaar.class, "execute", args, out, err);
    assertEquals("Twee Bestanden 2 - helptekst", 18, out.size());
    assertEquals("Twee Bestanden 2 - fouten", 0, 0);
    assertEquals("Twee Bestanden 2 - 14",
                 TEMP + File.separator + "competitie.csv",
                 out.get(13).split(":")[1].trim());
    assertEquals("Twee Bestanden 2 - 15", "1998.06.13",
                 out.get(14).split(":")[1].trim());
    assertEquals("Twee Bestanden 2 - 16", "64",
                 out.get(15).split(":")[1].trim());
    assertEquals("Twee Bestanden 2 - 17", "62",
                 out.get(16).split(":")[1].trim());
    assertTrue("Twee Bestanden 2 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator + "competitie.csv"),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitie.dubbel.csv")));
    assertTrue("Volledig 2 - H equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + "competitieH.csv"),
            Bestand.openInvoerBestand(VertaalPgnTest.class.getClassLoader(),
                                      "competitieH.dubbel.csv")));

    Bestand.delete(TEMP + File.separator + "competitie.csv");
    Bestand.delete(TEMP + File.separator + "competitieH.csv");
  }
}
