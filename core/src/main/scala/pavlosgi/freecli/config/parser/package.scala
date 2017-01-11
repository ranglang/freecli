package pavlosgi.freecli.config

import pavlosgi.freecli.parser.CliParser

package object parser {
  type ParseResult[A] = CliParser[ConfigParsingError, A]
}
