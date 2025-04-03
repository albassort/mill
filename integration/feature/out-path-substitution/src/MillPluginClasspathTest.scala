package mill.integration
import mill.testkit.UtestIntegrationTestSuite
import os.*
import utest._

object OutPathTestSuite extends UtestIntegrationTestSuite {

  val referencePath = os.pwd/"6one"
  val modifiedPath = os.pwd/"6two"

  val tests: Tests = Tests {
    test("Create Directories") - integrationTest { tester =>
      import tester._
      //This path is from the perspective of being inside an out/ folder in the mill root, ran by ./mill
      val  libPath = os.pwd/".."/".."/".."/".."/".."/".."/".."/".."/
        ".."/"example"/"scalalib"/"web"/"6-webapp-scalajs-shared"


      if (os.exists(referencePath)){
        os.remove(referencePath)
      }

      if (os.exists(modifiedPath)){
        os.remove(modifiedPath)
      }

      os.copy(
        libPath,
        os.pwd/"6one"
      )

      os.copy(
        libPath,
        os.pwd/"6two"
      ) 

      assert(os.exists(os.pwd/"6one") && os.exists(os.pwd/"6two"))
    }

    test("Compile") - integrationTest { tester => 
      val res1 = tester.eval(("runBackground"), cwd = os.pwd/"6one" )
      val res2 = tester.eval(("runBackground"), cwd = os.pwd/"6two" )
      assert(res1.isSuccess && res2.isSuccess)
    }
  }
}

