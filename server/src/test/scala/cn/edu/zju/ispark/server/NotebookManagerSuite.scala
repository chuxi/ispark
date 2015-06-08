package cn.edu.zju.ispark.server

import java.io.File

import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfterAll, FunSuite}

/**
 * Created by king on 15-6-3.
 * test Notebook management
 *
 */
class NotebookManagerSuite extends FunSuite with BeforeAndAfterAll{

  FileUtils.forceMkdir(new File("notebooks"))
  val nbm = new NotebookManager("testnbm", new File("notebooks"))
  val testnb = Notebook(new Metadata("ken1"), List(Worksheet(List(CodeCell("1+2", "python", false, Some(2), List(ScalaOutput(2, None, Some("3"))))))), Nil, None)


  override def beforeAll(): Unit = {

  }

  override def afterAll(): Unit = {

  }

  def assertFileExist(name: String): Unit = {
    assert(nbm.listNotebooks.filter(_._2.contentEquals(name)).nonEmpty)
  }

  test("save and load file") {
    nbm.save(None, "testnb", testnb, true)
    assertFileExist("testnb")
    val nb = nbm.load("testnb")
    assert(nb.get.isInstanceOf[Notebook])
    assert(nb.get.worksheets.head.cells.head.isInstanceOf[CodeCell])
    nbm.deleteNotebook(None, "testnb")
  }

  test("new Notebook") {
    val id1 = nbm.newNotebook()
    val files = nbm.listNotebooks
    println(id1, files.head._2)
    assertFileExist("Untitled1")
    nbm.deleteNotebook(Some(id1), "Untitled1")
  }



}
