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

import eu.debooy.caissa.CaissaUtils;
import eu.debooy.caissa.PGN;
import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.access.TekstBestand;
import eu.debooy.doosutils.exception.BestandException;
import eu.debooy.doosutils.test.BatchTest;
import eu.debooy.doosutils.test.VangOutEnErr;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.AfterClass;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * @author Marco de Booij
 */
public class VertaalPgnTest extends BatchTest {
  protected static final  ClassLoader CLASSLOADER =
      VertaalPgnTest.class.getClassLoader();

  private final String  pgnZetten   =
      "1.e4 e5 2.d4 exd4 3.Nf3 Nc6 4.Bc4 d6 5.O-O Bg4 6.c3 dxc3 7.Nxc3 Nf6 8.Bg5 Ne5 9.Be2 Nxf3+ 10.Bxf3 Bxf3 11.Qxf3 Be7 12.Rad1 Qc8 13.Rfe1 0-0 14.Qd3 Re8 15.f4 Nh5 16.Bxe7 Rxe7 17.Qf3 Nf6 18.e5 dxe5 19.fxe5 Nd7 20.Qg3 c6 21.Rc1 Qe8 22.Ne4 Kf8 23.Nd6 Qd8 24.Qf4 Nxe5 25.Rxe5 Qxd6 26.Rce1 Rxe5 27.Rxe5 Rd8 28.Qe3 Qd1+ 29.Kf2 Qd2+ 30.Kf3 Qxe3+ 31.Kxe3 Re8 32.Rxe8+ Kxe8";
  private final String  pgnZettenNl =
      "1.e4 e5 2.d4 exd4 3.Pf3 Pc6 4.Lc4 d6 5.O-O Lg4 6.c3 dxc3 7.Pxc3 Pf6 8.Lg5 Pe5 9.Le2 Pxf3+ 10.Lxf3 Lxf3 11.Dxf3 Le7 12.Tad1 Dc8 13.Tfe1 O-O 14.Dd3 Te8 15.f4 Ph5 16.Lxe7 Txe7 17.Df3 Pf6 18.e5 dxe5 19.fxe5 Pd7 20.Dg3 c6 21.Tc1 De8 22.Pe4 Kf8 23.Pd6 Dd8 24.Df4 Pxe5 25.Txe5 Dxd6 26.Tce1 Txe5 27.Txe5 Td8 28.De3 Dd1+ 29.Kf2 Dd2+ 30.Kf3 Dxe3+ 31.Kxe3 Te8 32.Txe8+ Kxe8";

  @AfterClass
  public static void afterClass() {
    verwijderBestanden(TEMP + File.separator,
                       new String[] {"partij.pgn", "partij_nl.pgn"});
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
                              .setBestand("partij.pgn").build();
      doel  = new TekstBestand.Builder().setBestand(TEMP + File.separator
                                                    + "partij.pgn")
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
  public void testLeeg() throws PgnException, IOException {
    String[]  args      = new String[] {};
    String    naarTaal  = Locale.getDefault().getLanguage();
    String    vanTaal   = Locale.getDefault().getLanguage();
    String[]  verwacht  = new String[] {
        resourceBundle.getString(CaissaTools.ERR_GEENINVOER),
        MessageFormat.format(
          resourceBundle.getString(CaissaTools.ERR_TALENGELIJK),
          vanTaal, naarTaal)};

    VangOutEnErr.execute(VertaalPgn.class, "execute", args, out, err);

    assertEquals("Zonder parameters - helptekst", 29, out.size());
    assertEquals("Zonder parameters - fouten", 2, err.size());
    assertArrayEquals("Error mesages", verwacht, err.toArray());
  }

  @Test
  public void testParameters() {
    String[]  args      = new String[] {"--pgn=" + pgnZetten,
                                        "--bestand=partij.pgn",
                                        "--vantaal=en",
                                        "--naartaal=nl"};
    String[]  verwacht  = new String[] {
        resourceBundle.getString(CaissaTools.ERR_BESTANDENPGN)};

    VangOutEnErr.execute(VertaalPgn.class, "execute", args, out, err);

    assertEquals("Met parameters - helptekst", 29, out.size());
    assertEquals("Met parameters - fouten", 1, err.size());
    assertArrayEquals("Error mesages", verwacht, err.toArray());
  }

  @Test
  public void testPgn() throws PgnException {
    String[]  args  = new String[] {"--pgn=" + pgnZetten,
                                    "--vantaal=en",
                                    "--naartaal=nl"};

    VangOutEnErr.execute(VertaalPgn.class, "execute", args, out, err);

    assertEquals("Met pgn - helptekst", 14, out.size());
    assertEquals("Met pgn - fouten", 0, err.size());
    assertEquals("Vertaald", pgnZettenNl, out.get(out.size()-1));
  }

  @Test
  public void testBestand() throws PgnException {
    String[]  args          = new String[] {"--bestand=partij.pgn",
                                            "--invoerdir=" + TEMP,
                                            "--vantaal=en",
                                            "--naartaal=nl"};

    VangOutEnErr.execute(VertaalPgn.class, "execute", args, out, err);

    Collection<PGN> partijen  =
        CaissaUtils.laadPgnBestand(TEMP + File.separator + "partij_nl.pgn",
                                   CHARSET);

    assertEquals("Met bestand - helptekst", 18, out.size());
    assertEquals("Met bestand - fouten", 0, err.size());
    assertEquals("Met bestand - uitvoer",
                 TEMP + File.separator + "partij_nl.pgn",
                 out.get(13).split(":")[1].trim());
    assertEquals("Met bestand - aantal (1)", "1",
                 out.get(14).split(":")[1].trim());
    assertEquals("Met bestand - aantal (2)", 1, partijen.size());
    assertEquals("Vertaald", pgnZettenNl, partijen.iterator().next()
                                                  .getZetten());
  }
}
