package mill.api
import os.Path
import upickle.default.ReadWriter as RW
import scala.reflect.ClassTag
import scala.util.matching.Regex
import mill.api.WorkspaceRoot
/**
 * Defines a trait which handles deerialization of paths, in a way that can be used by both path refs and paths
 */
trait PathUtils {
  implicit def substitutions() : List[(String, String)] = {
    val workspaceRootPath : String = WorkspaceRoot.workspaceRoot.toString

    var result = List((workspaceRootPath, "*$WorkplaceRoot*"))

    val javaHome = System.getProperty("java.home");
    result = result :+ (javaHome, "*$JavaHome*")

    val courseierPath = coursier.paths.CoursierPaths.cacheDirectory().toString
    result = result :+ (courseierPath, "*$CourseirCache*")

    result

  }

  implicit def serializeEnvVariables(a : os.Path) : String = {
    // TODO: Make parsing this a little bit more complex. The 
    val subs = substitutions()
    var result = a.toString
    subs.foreach{ case (value,sub) => 
      //Serializes by replacing the path with the substitution
      result = result.replace(value, sub)
    }
    result
  }

  implicit def deserializeEnvVariables(a : String) : os.Path = {
    val subs = substitutions()
    var result = a
    subs.foreach{ case (value,sub) => 
      if (result.startsWith(sub)){
        result = result.replace(sub, value)
      }
    }
    os.Path(result)
  }
}
