package pavlosgi.freecli.core.dsl.config

import shapeless._
import shapeless.test.illTyped

import pavlosgi.freecli.testkit.Test

class ConfigDslTest extends Test {
  describe("ConfigDsl tests") {

    it("allow different styles") {
      arg[String] --"one" -'c' -| "1"
      string --"one" -'c' -| "1"
      string("one", 'c', None, Some("1"))
      opt[String] --"one" -'c'
      optString --"one" -'c'
      optString("one", 'c', None)
    }

    it("allow using default in arg") {
      string --"one" -| "1"
      string --"one" -'c' -| "1"
    }

    it("allow convert to tuple") {
//      string --"one" -| "1" :: string --"two" -| "2"
    }

    it("not allow using default in opt") {
      illTyped("""optString --"one" -| "1"""")
      illTyped("""optString --"one" -'c' -| "1"""")
    }

    it("allow using default in flag") {
      flag --"one" -| true
      flag --"one" -'c' -| false
    }

    it("sub compiles") {
      case class A(value: String)
        sub[A]("description") {
          string --"one"
        } : ConfigDsl[A :: HNil]

    }

    it("sub does not compile without subconfiguration") {
      case class A(v: String)
      illTyped("""sub[A] -?"description": ConfigDsl[A :: HNil]""")
    }

    it("not allow using field name in sub") {
      case class A(value: String)
      illTyped(
        """sub[A] --"name" -?"description" -& {
             string --"one"
          } : ConfigDsl[A :: HNil]""")
    }

    it("not allow using field abbreviation in sub") {
      case class A(value: String)
      illTyped(
        """sub[A] -'n' -?"description" -& {
             string --"one"
          } : ConfigDsl[A :: HNil]""")
    }

    it("not allow using default in sub") {
      case class A(value: String)
      illTyped(
        """sub[A] -| A("1") -?"description" -& {
             string --"one"
          } : ConfigDsl[A :: HNil]""")
    }

    it("complex configuration") {
      case class A(aString: String, aFlag: Boolean, aInt: Int, b: B, aString2: String)
      case class B(bString: String, bFlag: Boolean, bOptInt: Option[Int], c: C, d: D)
      case class C(cString: String, cInt: Int)
      case class D(dFlag: Boolean)

      config[A] {
        string --"aValue" -'a' -?"sa" -|"as" ::
        flag   --"aFlag"  ::
        int    --"aInt"   ::
        sub[B]("b") {
          string --"bString" ::
          flag   --"bFlag"   ::
          optInt --"bOptInt" ::
          sub[C]("c") {
            string --"cString" ::
            int    --"cInt" -| 1
          } ::
          sub[D]("d") {
            flag -- "dFlag"
          }
        } ::
        string --"aString2"
      }
    }
  }
}