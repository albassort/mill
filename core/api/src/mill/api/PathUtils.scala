package mill.api
import os.Path
import upickle.default.ReadWriter as RW
import scala.reflect.ClassTag
import scala.util.matching.Regex
import mill.api.WorkspaceRoot
import mill.constants.EnvVars
import mill.constants.{OutFiles}

/**
 * Defines a trait which handles deerialization of paths, in a way that can be used by both path refs and paths
 */
trait PathUtils {
  // TEMPORARY, A MORE IDEAL SOLUTION NEEDS TO BE FOUND!
  implicit def findOutRoot(): os.Path = {
    val outFolderName = OutFiles.out
    val root = WorkspaceRoot.workspaceRoot / outFolderName
    var currentPath = root
    var faliure = false
    var i = 0
    while (100 > i) {
      if (os.exists(currentPath / "mill-java-home")) {
        return currentPath
      } else {
        println(currentPath.toString)
        if (currentPath.toString == "/") {
          return root
        } else {
          i = i + 1
          currentPath = currentPath / ".."
        }
      }
    }
    return root
  }
  /*
   * Returns a list of paths and their variables to be substituted with.
   */
  implicit def substitutions(): List[(String, String)] = {

    val outRoot = findOutRoot().toString
    var result = List((outRoot, "*$WorkplaceRoot*"))

    val javaHome = System.getProperty("java.home");
    result = result :+ (javaHome, "*$JavaHome*")

    val courseierPath = coursier.paths.CoursierPaths.cacheDirectory().toString
    result = result :+ (courseierPath, "*$CourseirCache*")
    result
  }

  /*
   * Handles the JSON serialization of paths. Normalizes paths based on variables returned by PathUtils.substitutions.
   * Substituting specific paths with variables as they are read from JSON.
   * The inverse function is PathUtils.deserializeEnvVariables.
   */
  implicit def serializeEnvVariables(a: os.Path): String = {
    val subs = substitutions()
    val stringified = a.toString
    var result = a.toString
    var depth = 0
    subs.foreach { case (path, sub) =>
      // Serializes by replacing the path with the substitution
      val pathDepth = path.count(_ == '/')
      if (stringified.startsWith(path) && pathDepth >= depth) {
        depth = pathDepth
        result = stringified.replace(path, sub)
      }
    }
    // println(s"1!! $stringified -> $result")
    result
  }

  /*
   * Handles the JSON deserialization of paths. Normalizes paths based on variables returned by PathUtils.substitutions.
   * Substituting specific strings with variables as they are read from JSON.
   * The inverse function is PathUtils.serializeEnvVariables
   */
  implicit def deserializeEnvVariables(a: String): os.Path = {
    val subs = substitutions()
    var result = new String(a)
    var depth = 0
    subs.foreach { case (path, sub) =>
      val pathDepth = path.count(_ == '/')
      // In the case that a path is in the folder of another path, it picks the path with the most depth
      if (result.startsWith(sub) && pathDepth >= depth) {
        depth = pathDepth
        val clone = new String(a)
        result = clone.replace(sub, path)
      }
    }

    // println(s"2!! $a -> $result")
    os.Path(result)
  }
}
