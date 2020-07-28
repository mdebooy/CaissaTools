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
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.test.BatchTest;
import eu.debooy.doosutils.test.VangOutEnErr;
import java.io.File;
import java.text.MessageFormat;
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

  @AfterClass
  public static void afterClass() {
    verwijderBestanden(TEMP + File.separator,
                       new String[] {"competitie1.pgn", "competitie2.pgn",
                                     "competitie1.tex"});
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
                              .setBestand("competitie1.pgn").build();
      doel  = new TekstBestand.Builder().setBestand(TEMP + File.separator
                                                    + "competitie1.pgn")
                              .setLezen(false).build();
      doel.add(bron);
      bron.close();
      doel.close();
      bron  = new TekstBestand.Builder().setClassLoader(CLASSLOADER)
                              .setBestand("competitie2.pgn").build();
      doel  = new TekstBestand.Builder().setBestand(TEMP + File.separator
                                                    + "competitie2.pgn")
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

    VangOutEnErr.execute(PgnToLatex.class, "execute", args, out, err);

    assertEquals("Zonder parameters - helptekst", 39, out.size());
    assertEquals("Zonder parameters - fouten", 1, err.size());
  }

  @Test
  public void testFouten() {
    String[]  args      = new String[] {"--bestand=/tmp/competitie1;/tmp/competitie2;competitie3",
                                        "--halve=Speler, 01;Speler, 02"};
    String[]  verwacht  = new String[] {
        MessageFormat.format("Het bestand {0} bevat een directory.",
                                     "bestand"),
        resourceBundle.getString(CaissaTools.ERR_HALVE),
        resourceBundle.getString(CaissaTools.ERR_BIJBESTAND)};

    VangOutEnErr.execute(PgnToLatex.class, "execute", args, out, err);

    assertEquals("Zonder parameters - helptekst", 39, out.size());
    assertEquals("Zonder parameters - fouten", 3, err.size());
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

    assertEquals("PgnToLatex - helptekst", 18, out.size());
    assertEquals("PgnToLatex - fouten", 0, err.size());
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

    assertEquals("PgnToLatex 2 - helptekst", 19, out.size());
    assertEquals("PgnToLatex 2 - fouten", 0, err.size());
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

    assertEquals("Op Stand - helptekst", 18, out.size());
    assertEquals("Op Stand - fouten", 0, err.size());
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

    assertEquals("Zonder Matrix - helptekst", 18, out.size());
    assertEquals("Zonder Matrix - fouten", 0, err.size());
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

    assertEquals("Zonder Matrix 2 - helptekst", 19, out.size());
    assertEquals("Zonder Matrix 2 - fouten", 0, err.size());
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
