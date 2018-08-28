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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Marco de Booij
 */
public class PgnToLatexTest extends BatchTest {
  @AfterClass
  public static void afterClass() throws BestandException {
    Bestand.delete(temp + File.separator + "competitie1.pgn");
  }

  @BeforeClass
  public static void beforeClass() throws BestandException {
    Locale.setDefault(new Locale("nl"));
    try {
      BufferedReader  bron  =
          Bestand.openInvoerBestand(PgnToLatexTest.class.getClassLoader(),
                                    "competitie1.pgn");
      BufferedWriter  doel  = Bestand.openUitvoerBestand(temp + File.separator
                                                         + "competitie1.pgn");
      kopieerBestand(bron, doel);
      bron.close();
      doel.close();
    } catch (IOException e) {
      throw new BestandException(e);
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
                                        "--invoerdir=" + temp,
                                        "--uitvoerdir=" + temp};

    try {
      Bestand.delete(temp + File.separator + "competitie1.tex");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(PgnToLatex.class, "execute", args, out, err);

    assertEquals("PgnToLatex - helptekst", 16, out.size());
    assertEquals("PgnToLatex - fouten", 0, 0);
    assertEquals("PgnToLatex - 14",
                 temp + File.separator + "competitie1.tex",
                 out.get(13).split(":")[1].trim());
    assertEquals("PgnToLatex - 15", "150",
                 out.get(14).split(":")[1].trim());
    assertTrue("PgnToLatex - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(temp + File.separator
                                      + "competitie1.tex"),
            Bestand.openInvoerBestand(PgnToLatexTest.class.getClassLoader(),
                                      "competitie1.tex")));

    Bestand.delete(temp + File.separator + "competitie1.tex");
  }

  @Test
  public void testOpStand() throws BestandException {
    String[]  args      = new String[] {"--bestand=competitie1",
                                        "--enkel=N",
                                        "--invoerdir=" + temp,
                                        "--matrixopstand=J",
                                        "--uitvoerdir=" + temp};

    try {
      Bestand.delete(temp + File.separator + "competitie1.tex");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(PgnToLatex.class, "execute", args, out, err);

    assertEquals("Op Stand - helptekst", 16, out.size());
    assertEquals("Op Stand - fouten", 0, 0);
    assertEquals("Op Stand - 14",
                 temp + File.separator + "competitie1.tex",
                 out.get(13).split(":")[1].trim());
    assertEquals("Op Stand - 15", "150",
                 out.get(14).split(":")[1].trim());
    assertTrue("Op Stand - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(temp + File.separator
                                      + "competitie1.tex"),
            Bestand.openInvoerBestand(PgnToLatexTest.class.getClassLoader(),
                                      "competitie2.tex")));

    Bestand.delete(temp + File.separator + "competitie1.tex");
  }

  @Test
  public void testZonderMatrix() throws BestandException {
    String[]  args      = new String[] {"--bestand=competitie1",
                                        "--enkel=N",
                                        "--invoerdir=" + temp,
                                        "--matrix=N",
                                        "--matrixopstand=J",
                                        "--uitvoerdir=" + temp};

    try {
      Bestand.delete(temp + File.separator + "competitie1.tex");
    } catch (BestandException e) {
    }

    VangOutEnErr.execute(PgnToLatex.class, "execute", args, out, err);

    assertEquals("Zonder Matrix - helptekst", 16, out.size());
    assertEquals("Zonder Matrix - fouten", 0, 0);
    assertEquals("Zonder Matrix - 14",
                 temp + File.separator + "competitie1.tex",
                 out.get(13).split(":")[1].trim());
    assertEquals("Zonder Matrix - 15", "150",
                 out.get(14).split(":")[1].trim());
    assertTrue("Zonder Matrix - equals",
        Bestand.equals(
            Bestand.openInvoerBestand(temp + File.separator
                                      + "competitie1.tex"),
            Bestand.openInvoerBestand(PgnToLatexTest.class.getClassLoader(),
                                      "competitie3.tex")));

    Bestand.delete(temp + File.separator + "competitie1.tex");
  }
}
