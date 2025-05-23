package mill.integration
import mill.testkit.UtestIntegrationTestSuite
import os.*
import utest._
import scala.collection.immutable.{Map}
import scala.collection.mutable.{Map}
object OutPathTestSuite extends UtestIntegrationTestSuite {
import mill.define.PathUtils

  val referencePath = os.pwd / "6one"
  val modifiedPath = os.pwd / "6two"

  def jsonRecurse(obj: ujson.Obj, path: String): scala.collection.mutable.Map[String, String] = {
    var result = scala.collection.mutable.Map.empty[String, String]
    val map = obj.obj.toMap
    map.foreach { case (k, v) =>

      val kind = v.getClass.getSimpleName
      if (kind == "Str") {
        val stringified = v.str
        if (
          stringified.contains("*/") || stringified.contains("ref:") || stringified.take(1) == "/"
        ) {

          if (stringified.contains("ref:")) {
            // removes the prefix of a ref: string, which contains noise and would throw off our readings
            result += (s"$path.$k" -> stringified.substring(16))
          } else {
            result += (s"$path.$k" -> stringified)
          }
        }
      } else if (kind == "Obj") {
        val recursed = jsonRecurse(v.obj, s"$path.$k")
        result = result ++ recursed
      }
    }

    return result
  }

  implicit def flatDirToMap(rootPath: os.Path): scala.collection.mutable.Map[String, String] = {
    var result = scala.collection.mutable.Map.empty[String, String]
    val jsonPaths = os.walk(rootPath).filter(file => file.last.endsWith(".json"))

    jsonPaths.foreach(path => {
      if (os.exists(path)) {
        try {
          val read = scala.io.Source.fromFile(path.toString).mkString
          val json = ujson.read(read)
          val pathy = path.toString.split(rootPath.toString).last
          val keys = jsonRecurse(json.obj, pathy)
          result = result ++ keys
        } catch {
          case e: Exception => {}
        }
      }
    })

    return result;

  }

  val tests: Tests = Tests {
    test("Create Directories") - integrationTest { tester =>
      // This path is from the perspective of being inside an out/ folder in the mill root, ran by ./mill
      val libPath = os.pwd / ".." / ".." / ".." / ".." / ".." / ".." / ".." /
        ".." / "example" / "scalalib" / "web" / "6-webapp-scalajs-shared"

      if (os.exists(referencePath)) {
        os.remove(referencePath)
      }

      if (os.exists(modifiedPath)) {
        os.remove(modifiedPath)
      }

      os.copy(
        libPath,
        referencePath
      )

      os.copy(
        libPath,
        modifiedPath
      )

      println(PathUtils.getSubstitutions())

      assert(os.exists(referencePath) && os.exists(modifiedPath))
    }

    test("Compile") - integrationTest { tester =>
      val env = scala.collection.immutable.Map("COURSIER_CACHE" -> (os.home.toString))
      val pwd = os.pwd.toString

      os.copy(
        os.home / ".ivy2",
        os.pwd / ".ivy2"
      )

      val resReference1 = tester.eval(("runBackground"), cwd = referencePath)
      val resModified1 =
        tester.eval((s"-Duser.home=$pwd", "runBackground"), cwd = modifiedPath, env = env)
      assert(resModified1.isSuccess && resReference1.isSuccess)

      val resReference2 = tester.eval(("clean", "runBackground"), cwd = referencePath)
      val resModified2 =
        tester.eval((s"-Duser.home=$pwd", "clean", "runBackground"), cwd = modifiedPath, env = env)
      assert(resModified2.isSuccess && resReference2.isSuccess)

      val resReference3 = tester.eval(("jar"), cwd = referencePath)
      val resModified3 = tester.eval((s"-Duser.home=$pwd", "jar"), cwd = modifiedPath, env = env)
      assert(resModified3.isSuccess && resReference3.isSuccess)

      val resReference4 = tester.eval(("assembly"), cwd = referencePath)
      val resModified4 =
        tester.eval((s"-Duser.home=$pwd", "assembly"), cwd = modifiedPath, env = env)
      assert(resModified4.isSuccess && resReference4.isSuccess)

      assert(os.exists(os.home / "https"))
    }

    test("Compare") - integrationTest { tester =>

      val reference = flatDirToMap(referencePath)
      val modified = flatDirToMap(modifiedPath)

      modified.foreach { case (k, v) =>
        assert(reference.contains(k))

        val referenceValue = reference.get(k).get
        if (v.contains("$")) {
          // Normalization fails when the Coursier_Cache is set to a file within the Mill Directory
          val modifiedFirst = v.split("/")(0)
          val referenceFirst = referenceValue.split("/")(0)
          assert(modifiedFirst == referenceFirst)
        }
      }
      // reference.foreach { case (k, v) =>
      // assert(modified.contains(k))
      // }
    }
  }
}
