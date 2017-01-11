package pavlosgi.freecli.option

import shapeless._

import pavlosgi.freecli.core.all._
import pavlosgi.freecli.option.all._
import pavlosgi.freecli.testkit.Test
import pavlosgi.freecli.Helpers._
import pavlosgi.freecli.option.parser._

class OptionParserTest extends Test {
  describe("Options parser") {
    it("parse string option with name") {
      val res = parseOptions(Seq("--host", "localhost"))(string --"host")
      res.valid should === (Some("localhost"))
    }

    it("parse option if missing") {
      val res = parseOptions(Seq())(string --"debug" -'d')
      res.valid should === (None)
    }

    it("parse string option with abbreviation") {
      val res = parseOptions(Seq("-h", "localhost"))(string -'h')
      res.valid should === (Some("localhost"))
    }

    it("parse string option with both name and abbreviation using abbreviation") {
      val res = parseOptions(Seq("-h", "localhost"))(string --"host" -'h')
      res.valid should === (Some("localhost"))
    }

    it("parse string option with both name and abbreviation using name") {
      val res = parseOptions(Seq("--host", "localhost"))(string --"host" -'h')
      res.valid should === (Some("localhost"))
    }

    it("fail to parse string option using the wrong field name format") {
      val dsl = string --"host" -'h' -~ req
      val res = parseOptions(Seq("-host", "localhost"))(dsl)

      res.invalid.errors.toList.collect {
        case c: AdditionalArgumentsFound => c.getClass.getName
      }.distinct.size should === (1)

      val res1 = parseOptions(Seq("host", "localhost"))(dsl)

      res1.invalid.errors.toList.collect {
        case c: AdditionalArgumentsFound => c.getClass.getName
        case c: OptionFieldMissing => c.getClass.getName
      }.distinct.size should === (2)
    }

    it("fail to parse string option using the wrong field abbreviation format") {
      val dsl = string --"host" -'h' -~ req
      val res = parseOptions(Seq("--h", "localhost"))(dsl)

      res.invalid.errors.toList.collect {
        case c: AdditionalArgumentsFound => c.getClass.getName
        case c: OptionFieldMissing => c.getClass.getName
      }.distinct.size should === (2)

      val res1 = parseOptions(Seq("h", "localhost"))(dsl)

      res1.invalid.errors.toList.collect {
        case c: AdditionalArgumentsFound => c.getClass.getName
        case c: OptionFieldMissing => c.getClass.getName
      }.distinct.size should === (2)
    }

    it("fail to parse string option if value not provided") {
      val dsl = string --"host" -'h'
      val res = parseOptions(Seq("-h"))(dsl)
      res.invalid.errors.toList.collect {
        case c: AdditionalArgumentsFound => c.getClass.getName
        case c: OptionFieldValueMissing => c.getClass.getName
      }.distinct.size should === (2)
    }

    it("parse string option with default") {
      val res = parseOptions(Seq())(string --"host" -'h'-~ or("myhost"))
      res.valid should === ("myhost")
    }

    it("parse string option with default and override") {
      val res = parseOptions(Seq("--host", "localhost"))(
                  string --"host" -'h' -~ or("myhost"))

      res.valid should === ("localhost")
    }

    it("parse int option with default") {
      val res = parseOptions(Seq("-p", "8080"))(int --"port" -'p' -~ or(5432))
      res.valid should === (8080)
    }

    it("fail to parse int option") {
      val res = parseOptions(Seq("-p", "8080s"))(int --"port" -'p' -~ or(5432))

      res.invalid.errors.toList.collect {
        case c: FailedToDecodeOption => c.getClass.getName
      }.distinct.size should === (1)
    }

    it("parse flag option") {
      val res = parseOptions(Seq("--debug"))(flag --"debug" -'d')
      res.valid should === (true)
    }

    it("parse flag option if flag is missing") {
      val res = parseOptions(Seq())(flag --"debug" -'d')
      res.valid should === (false)
    }

    it("parse required option") {
      val res = parseOptions(Seq("--debug", "true"))(string --"debug" -'d' -~ req)
      res.valid should === ("true")
    }

    it("parse required option missing") {
      val res = parseOptions(Seq())(string --"debug" -'d')
      res.valid should === (None)
    }

    it("fail to parse required option") {
      val res = parseOptions(Seq("--debug", "value"))(int --"debug" -'d')
      res.invalid.errors.toList.collect {
        case c: FailedToDecodeOption => c.getClass.getName
      }.distinct.size should === (1)

      val res2 = parseOptions(Seq("--debug"))(int --"debug" -'d')
      res2.invalid.errors.toList.collect {
        case c: AdditionalArgumentsFound => c.getClass.getName
        case c: OptionFieldValueMissing => c.getClass.getName
      }.distinct.size should === (2)
    }

    it("parse tuple string int with name") {
      val res = parseOptions(Seq("--host", "localhost", "--port", "8080"))(
        groupT(string --"host" :: int --"port"))

      res.valid should === (Some("localhost") -> Some(8080))
    }

    it("parse hlist string int with name") {
      val res = parseOptions(Seq("--host", "localhost", "--port", "8080"))(
        string --"host" :: int --"port")

      res.valid should === (Some("localhost") :: Some(8080) :: HNil)
    }

    it("parse options to build type") {
      case class ServerConfig(host: Option[String], port: Option[Int], debug: Boolean)

      val dsl =
        group[ServerConfig] {
          string --"host"  ::
          int    --"port"  ::
          flag   --"debug1"
        }

      val res = parseOptions(Seq("--host", "localhost", "--port", "8080", "--debug1"))(dsl)
      res.valid should === (ServerConfig(Some("localhost"), Some(8080), true))
    }

    it("parse options to build type with subconfiguration") {
      case class DbConfig(dbHost: String, dbPort: Int)
      case class ServerConfig(host: String, port: Int, debug: Boolean, dbConfig: DbConfig)

      val dsl =
        group[ServerConfig] {
          string --"host" -~ req ::
          int   --"port" -~ req ::
          flag   --"debug" ::
          sub[DbConfig]("Database configuration") {
            string --"dbhost" -~ req ::
            int    --"dbport" -~ req
          }
        }

      val res = parseOptions(
                  Seq(
                    "--host",
                    "localhost",
                    "--port",
                    "8080",
                    "--debug",
                    "--dbhost",
                    "postgresql",
                    "--dbport",
                    "5432"))(dsl)

      res.valid should === (ServerConfig(
                              "localhost",
                              8080,
                              true,
                              DbConfig("postgresql", 5432)))
    }

    it("should allow passing multiple field abbreviations under a single slash for flags") {
      case class Flags(first: Boolean, second: Boolean, third: Boolean, fourth: Boolean)

      val dsl =
        group[Flags] {
          flag   - 'p' ::
          flag   - 'n' ::
          flag   - 's' ::
          flag   - 't'
        }

      parseOptions(Seq("-pnst"))(dsl).valid should === (
        Flags(first = true, second = true, third = true, fourth = true))

      case class Flags2(first: Boolean, second: Boolean, third: Option[String])

      val dsl2 =
        group[Flags2] {
          flag   - 'p' ::
          flag   - 'n' ::
          string - 's'
        }

      parseOptions(Seq("-pn", "-s", "string"))(dsl2).valid should === (
        Flags2(true, true, Some("string")))

    }

    it("should escape multiple field abbreviation split") {
      case class A(first: String)

      val dsl =
        group[A] {
          string -'p' -~ req
        }

      parseOptions(Seq("-p", "-host"))(dsl).valid should === (A("-host"))
    }

    it("should throw a runtime exception for bad field names") {
      assertThrows[IllegalArgumentException](string --"--host")
      assertThrows[IllegalArgumentException](string --"123")
      assertThrows[IllegalArgumentException](string -'0')
    }
  }
}
