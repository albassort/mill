package mill.define
import scala.reflect.ClassTag
import java.io.File;

/**
 * Defines a trait which handles deerialization of paths, in a way that can be used by both path refs and paths
 */
trait PathUtils {
  // TEMPORARY! A better solution needs to be found.
  def findOutRoot(): os.Path = {
    //os.PWD breaks in some parts of the codebase under some situations. The native java methods do not.
    val root = os.Path(new File("").getCanonicalPath().toString)
    var currentPath = root

    for (i <- 1 to 100) {
      if (os.exists(currentPath / "mill-java-home")) {
        currentPath
      } else {
        if (currentPath == os.root) {
          root
        } else {
          currentPath = currentPath / ".."
        }
      }
    }
    root
  }

  lazy val outFolderRoot: os.Path = findOutRoot()

  /*
   * Returns a list of paths and their variables to be substituted with.
   */
  implicit def substitutions(): List[(os.Path, String)] = {
    var result = List((outFolderRoot, "*$WorkplaceRoot*"))
    val javaHome = os.Path(System.getProperty("java.home"))
    result = result :+ (javaHome, "*$JavaHome*")

    val courseierPath = os.Path(coursier.paths.CoursierPaths.cacheDirectory().getAbsolutePath())

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
      //
      val pathDepth = path.segments.length
      val pathString = path.toString
      if (stringified.startsWith(pathString) && pathDepth >= depth) {
        depth = pathDepth
        result = stringified.replace(pathString, sub)
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
    var result = a
    var depth = 0
    subs.foreach { case (path, sub) =>
      val pathDepth = path.segments.length
      // In the case that a path is in the folder of another path, it picks the path with the most depth
      if (result.startsWith(sub) && pathDepth >= depth) {
        depth = pathDepth
        result = a.replace(sub, path.toString)
      }
    }

    // println(s"2!! $a -> $result")
    os.Path(result)
  }
}
