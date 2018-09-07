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

import static org.junit.Assert.assertArrayEquals;
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
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Marco de Booij
 */
public class PgnToLatexTest extends BatchTest {
  @AfterClass
  public static void afterClass() throws BestandException {
    Bestand.delete(TEMP + File.separator + "competitie1.pgn");
    Bestand.delete(TEMP + File.separator + "competitie2.pgn");
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    Locale.setDefault(new Locale("nl"));
    resourceBundle  = ResourceBundle.getBundle("ApplicatieResources",
                                               Locale.getDefault());

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

    VangOutEnErr.execute(PgnToLatex.class, "execute", args, out, err);

    assertEquals("Zonder parameters - helptekst", 38, out.size());
    assertEquals("Zonder parameters - fouten", 0, 0);
  }

  @Test
  public void testFouten() {
    String[]  args      = new String[] {"--bestand=/tmp/competitie1;/tmp/competitie2;competitie3",
                                        "--halve=Speler, 01;Speler, 02"};
    String[]  verwacht  = new String[] {
        MessageFormat.format(
            resourceBundle.getString(CaissaTools.ERR_BEVATDIRECTORY),
                                     "/tmp/competitie1"),
        MessageFormat.format(
            resourceBundle.getString(CaissaTools.ERR_BEVATDIRECTORY),
                                     "/tmp/competitie2"),
        resourceBundle.getString(CaissaTools.ERR_HALVE),
        resourceBundle.getString(CaissaTools.ERR_BIJBESTAND)};

    VangOutEnErr.execute(PgnToLatex.class, "execute", args, out, err);

    assertEquals("Zonder parameters - helptekst", 38, out.size());
    assertEquals("Zonder parameters - fouten", 4, err.size());
    assertArrayEquals("Error mesages", verwacht, err.toArray());
  }

  @Test
  public void testPgnToLatex() throws BestandException {
    String[]  args      = new String[] {"--bestand=competitie1",
                                        "--enkel=N",
                                        "--invoerdir=" + TEMP,
                                        "--uitvoerdir=" + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + "competitie1.tex");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(PgnToLatex.class, "execute", args, out, err);

    assertEquals("PgnToLatex - helptekst", 16, out.size());
    assertEquals("PgnToLatex - fouten", 0, 0);
    assertEquals("PgnToLatex - 14",
                 TEMP + File.separator + "competitie1.tex",
                 out.get(13).split(":")[1].trim());
    assertEquals("PgnToLatex - 15", "150",
                 out.get(14).split(":")[1].trim());
    assertTrue("PgnToLatex - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + "competitie1.tex"),
            Bestand.openInvoerBestand(PgnToLatexTest.class.getClassLoader(),
                                      "competitie1.tex")));

    Bestand.delete(TEMP + File.separator + "competitie1.tex");
  }

  @Test
  public void testPgnToLatex2() throws BestandException {
    String[]  args      = new String[] {"--auteur=Caissa Tools",
                                        "--bestand=competitie1;competitie2",
                                        "--enkel=N",
                                        "--invoerdir=" + TEMP,
                                        "--titel=Testing 97/98 - 98/99",
                                        "--uitvoerdir=" + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + "competitie1.tex");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(PgnToLatex.class, "execute", args, out, err);

    assertEquals("PgnToLatex 2 - helptekst", 17, out.size());
    assertEquals("PgnToLatex 2 - fouten", 0, 0);
    assertEquals("PgnToLatex 2 - 14",
                 TEMP + File.separator + "competitie1.tex",
                 out.get(13).split(":")[1].trim());
    assertEquals("PgnToLatex 2 - 15",
                 TEMP + File.separator + "competitie2.tex",
                 out.get(14).split(":")[1].trim());
    assertEquals("PgnToLatex 2 - 16", "214",
                 out.get(15).split(":")[1].trim());
    assertTrue("PgnToLatex 2 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + "competitie1.tex"),
            Bestand.openInvoerBestand(PgnToLatexTest.class.getClassLoader(),
                                      "competitie4.tex")));

    Bestand.delete(TEMP + File.separator + "competitie1.tex");
  }

  @Test
  public void testOpStand() throws BestandException {
    String[]  args      = new String[] {"--bestand=competitie1",
                                        "--enkel=N",
                                        "--invoerdir=" + TEMP,
                                        "--matrixopstand=J",
                                        "--uitvoerdir=" + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + "competitie1.tex");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(PgnToLatex.class, "execute", args, out, err);

    assertEquals("Op Stand - helptekst", 16, out.size());
    assertEquals("Op Stand - fouten", 0, 0);
    assertEquals("Op Stand - 14",
                 TEMP + File.separator + "competitie1.tex",
                 out.get(13).split(":")[1].trim());
    assertEquals("Op Stand - 15", "150",
                 out.get(14).split(":")[1].trim());
    assertTrue("Op Stand - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + "competitie1.tex"),
            Bestand.openInvoerBestand(PgnToLatexTest.class.getClassLoader(),
                                      "competitie2.tex")));

    Bestand.delete(TEMP + File.separator + "competitie1.tex");
  }

  @Test
  public void testZonderMatrix() throws BestandException {
    String[]  args      = new String[] {"--bestand=competitie1",
                                        "--enkel=N",
                                        "--invoerdir=" + TEMP,
                                        "--matrix=N",
                                        "--uitvoerdir=" + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + "competitie1.tex");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(PgnToLatex.class, "execute", args, out, err);

    assertEquals("Zonder Matrix - helptekst", 16, out.size());
    assertEquals("Zonder Matrix - fouten", 0, 0);
    assertEquals("Zonder Matrix - 14",
                 TEMP + File.separator + "competitie1.tex",
                 out.get(13).split(":")[1].trim());
    assertEquals("Zonder Matrix - 15", "150",
                 out.get(14).split(":")[1].trim());
    assertTrue("Zonder Matrix - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + "competitie1.tex"),
            Bestand.openInvoerBestand(PgnToLatexTest.class.getClassLoader(),
                                      "competitie3.tex")));

    Bestand.delete(TEMP + File.separator + "competitie1.tex");
  }

  @Test
  public void testZonderMatrix2() throws BestandException {
    String[]  args      = new String[] {"--auteur=Caissa Tools",
                                        "--bestand=competitie1;competitie2",
                                        "--enkel=N",
                                        "--invoerdir=" + TEMP,
                                        "--matrix=N",
                                        "--titel=Testing 97/98 - 98/99",
                                        "--uitvoerdir=" + TEMP};

    try {
      Bestand.delete(TEMP + File.separator + "competitie1.tex");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(PgnToLatex.class, "execute", args, out, err);

    assertEquals("Zonder Matrix 2 - helptekst", 17, out.size());
    assertEquals("Zonder Matrix 2 - fouten", 0, 0);
    assertEquals("Zonder Matrix 2 - 14",
                 TEMP + File.separator + "competitie1.tex",
                 out.get(13).split(":")[1].trim());
    assertEquals("Zonder Matrix 2 - 15",
                 TEMP + File.separator + "competitie2.tex",
                 out.get(14).split(":")[1].trim());
    assertEquals("Zonder Matrix 2 - 16", "214",
                 out.get(15).split(":")[1].trim());
    assertTrue("Zonder Matrix 2 - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(TEMP + File.separator
                                      + "competitie1.tex"),
            Bestand.openInvoerBestand(PgnToLatexTest.class.getClassLoader(),
                                      "competitie5.tex")));

    Bestand.delete(TEMP + File.separator + "competitie1.tex");
  }
}
