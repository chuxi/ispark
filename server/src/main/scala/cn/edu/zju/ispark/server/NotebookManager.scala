package cn.edu.zju.ispark.server

import java.io._
import java.net.{URLDecoder, URLEncoder}
import java.text.SimpleDateFormat
import java.util.{Date, UUID}

import org.json4s.ShortTypeHints
import org.json4s.jackson.Serialization.{read, write}


/**
 * Created by king on 15-6-2.
 * I copied most of the NotebookManager, replaced the JSON tool with jackson
 * removed the NBSerializer
 */

//@JsonTypeInfo(
//  use = JsonTypeInfo.Id.NAME,
//  include = JsonTypeInfo.As.PROPERTY,
//  property = "type")
//@JsonSubTypes(Array(
//  new Type(value = classOf[ScalaOutput], name = "scalaoutput"),
//  new Type(value = classOf[ScalaError], name = "scalaerror"),
//  new Type(value = classOf[ScalaStream], name = "scalastream")
//))
trait Output
case class ScalaOutput(prompt_number: Int, html: Option[String], text: Option[String]) extends Output
case class ScalaError(prompt_number: Int, traceback: String) extends Output
case class ScalaStream(text: String, stream: String) extends Output

/**
 * Jackson Polymorphism, reference to: http://stackoverflow.com/questions/18027233/jackson-scala-json-deserialization-to-case-classes
 */

//@JsonTypeInfo(
//  use = JsonTypeInfo.Id.NAME,
//  include = JsonTypeInfo.As.PROPERTY,
//  property = "type")
//@JsonSubTypes(Array(
//  new Type(value = classOf[CodeCell], name = "codecell"),
//  new Type(value = classOf[MarkdownCell], name = "markdowncell"),
//  new Type(value = classOf[RawCell], name = "rawcell"),
//  new Type(value = classOf[HeadingCell], name = "headingcell")
//))
trait Cell
case class CodeCell(input: String, language: String, collapsed: Boolean,prompt_number:Option[Int], outputs: List[Output]) extends Cell
case class MarkdownCell(source: String) extends Cell
case class RawCell(source: String) extends Cell
case class HeadingCell(source: String, level: Int) extends Cell


case class Metadata(name: String, user_save_timestamp: Date, auto_save_timestamp: Date)  {
  def this(name: String) = this(name, new Date(0), new Date(0))
}
case class Worksheet(cells: List[Cell])
case class Notebook(metadata: Metadata, worksheets: List[Worksheet], autosaved: List[Worksheet], nbformat: Option[Int]) {
  def name = metadata.name
}


class NotebookManager(val name: String, val notebookDir: File) {

  val extension = ".snb"

  val idToName = collection.mutable.Map[String, String]()

  implicit val formats = org.json4s.jackson.Serialization.formats(ShortTypeHints(List(
    classOf[CodeCell], classOf[MarkdownCell], classOf[RawCell], classOf[HeadingCell],
    classOf[ScalaOutput], classOf[ScalaError], classOf[ScalaStream]
  )))


  def listNotebooks = {
    val files = notebookDir.listFiles map {_.getName} filter {_.endsWith(extension)} toIndexedSeq
    val res = files.sorted map { fn => {
      val name = URLDecoder.decode(fn.substring(0, fn.length - extension.length), "UTF-8")
      "name" -> name
    } }
    res.toList
  }

  def notebookFile(name: String) = {
    val basePath = notebookDir.getCanonicalPath
    val nbFile = new File(basePath, URLEncoder.encode(name, "UTF-8") + extension)
    /* This check is probably not strictly necessary due to URL encoding of name (should escape any path traversal components), but let's be safe */
    require(nbFile.getParentFile.getCanonicalPath == basePath, "Unable to access notebook outside of notebooks path.")
    nbFile
  }

  def incrementFileName(base:String) = {
    Stream.from(1) map { i => base + i } filterNot { fn => notebookFile(fn).exists() } head
  }

  def newNotebook() = {
    val name = incrementFileName("Untitled")
    val nb = Notebook(new Metadata(name), List(Worksheet(Nil)), Nil, None)
    val id = notebookId(name)
    save(Some(id), name, nb, false)
    id
  }

  def copyNotebook(nbId: Option[String], nbName: String) = {
    val nbData = getNotebook(nbId, nbName)
    nbData.map { nb =>
      val name = incrementFileName(nb._2)
      val oldNB = nb._3
      val id = notebookId(name)
      save(Some(id), name, Notebook(new Metadata(name), oldNB.worksheets, oldNB.autosaved, None), false)
      id
    } getOrElse newNotebook
  }

  /**
   * Attempts to select a notebook by ID first, if supplied and if the ID
   * is known; falls back to supplied name otherwise.
   */
  def getNotebook(id: Option[String], name: String) = {
    val nameToUse = id flatMap idToName.get getOrElse name
    for (notebook <- load(nameToUse)) yield {
//      val data = FileUtils.readFileToString(notebookFile(notebook.name))
      val data = read[Notebook](new BufferedReader(new FileReader(notebookFile(notebook.name))))
      val df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z'('Z')'")
      val last_mtime = df.format(new Date(notebookFile(notebook.name).lastModified()))
      (last_mtime, notebook.name, data)
    }
  }

  
  def deleteNotebook(id: Option[String], name: String) = {
    val realId = id match {
      case Some(x) => id
      case None => nameToId(name)
    }
    val nameToUse = realId flatMap idToName.get getOrElse name
    val file = notebookFile(nameToUse)
    if (file.exists()) {
      realId foreach removeMapping
      file.delete()
    }
  }


  def save(id: Option[String], name: String, nbI: Notebook, overwrite: Boolean): Unit = {
    val file = notebookFile(name)
    if (!overwrite && file.exists()) throw new NotebookExistsException("Notebook " + name + " already exists.")
    val nb = if (nbI.name != name) nbI.copy(new Metadata(name)) else nbI

    write(nb, new FileOutputStream(file))

    // If there was an old file that's different, then delete it because this is a rename
    id flatMap idToName.get foreach { oldName =>
      if (notebookFile(nb.name).compareTo(notebookFile(oldName)) != 0)
        notebookFile(oldName).delete()
    }

    setMapping(id getOrElse notebookId(name), name)
  }

  def load(name: String): Option[Notebook] = {
    val file = notebookFile(name)
    if (file.exists())
      Some(read[Notebook](new BufferedReader(new FileReader(notebookFile(name)))))
    else None
  }


  private def removeMapping(id: String) {
    idToName.remove(id)
  }
  private def setMapping(id: String, name:String) {
    nameToId(name).foreach(idToName.remove(_))
    idToName.put(id, name)
  }

  def nameToId(name: String) = idToName.find(_._2 == name).map(_._1)

  def notebookId(name: String) = nameToId(name) getOrElse {
    val id = UUID.randomUUID.toString
    setMapping(id, name)
    id
  }


}

class NotebookExistsException(message:String) extends IOException(message)