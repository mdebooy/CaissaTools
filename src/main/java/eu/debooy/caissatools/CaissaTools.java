/**
 * Copyright 2009 Marco de Booij
 *
 * Licensed under the EUPL, Version 1.0 or � as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the Licence. You may
 * obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/7330l5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package eu.debooy.caissatools;

import eu.debooy.caissa.exceptions.PgnException;
import eu.debooy.doosutils.Banner;


/**
 * @author Marco de Booij
 */
public class CaissaTools {
  private CaissaTools() {}

  public static void main(String[] args) {
    if (args.length == 0) {
      Banner.printBanner("Caissa Tools");
      help();
      return;
    }

    String    commando      = args[0];

    String[]  commandoArgs  = new String[args.length-1];
    System.arraycopy(args, 1, commandoArgs, 0, args.length-1);

    if ("pgntohtml".equalsIgnoreCase(commando)) {
      try {
        PgnToHtml.execute(commandoArgs);
      } catch (PgnException e) {
        System.out.println(e.getMessage());
      }
      return;
    }
    if ("pgntolatex".equalsIgnoreCase(commando)) {
      try {
        PgnToLatex.execute(commandoArgs);
      } catch (PgnException e) {
        System.out.println(e.getMessage());
      }
      return;
    }
    if ("startpgn".equalsIgnoreCase(commando)) {
      StartPgn.execute(commandoArgs);
      return;
    }
    if ("spelerstatistiek".equalsIgnoreCase(commando)) {
      SpelerStatistiek.execute(commandoArgs);
      return;
    }

    Banner.printBanner("Caissa Tools");
    help();
  }

  /**
   * Geeft de 'help' pagina.
   */
  private static void help() {
    System.out.println("  PgnToHtml         Zet een bestand met PGN partijen uit een toernooi");
    System.out.println("                    om in 2 HTML Bestanden.");
    System.out.println("  PgnToLatex        Zet een bestand met PGN partijen uit een toernooi");
    System.out.println("                    om in een .tex bestand.");
    System.out.println("  StartPgn          Maakt een PGN bestand aan met alle partijen die");
    System.out.println("                    tussen de opgegeven spelers gespeeld (moeten) worden.");
    System.out.println("  SpelerStatistiek  Maakt een statistiek van alle partijen van de");
    System.out.println("                    opgegeven speler.");
    System.out.println();
    PgnToHtml.help();
    System.out.println();
    PgnToLatex.help();
    System.out.println();
    StartPgn.help();
    System.out.println();
    SpelerStatistiek.help();
  }
}
