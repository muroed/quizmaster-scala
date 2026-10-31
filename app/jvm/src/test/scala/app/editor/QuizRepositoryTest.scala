package app.editor

import java.nio.file.{Files, Path}
import java.nio.charset.StandardCharsets
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class QuizRepositoryTest extends Specification {
  sequential
  private def git(cwd: Path, args: String*): Unit = {
    val p = new ProcessBuilder((Seq("git", "-C", cwd.toString) ++ args): _*).redirectErrorStream(true).start()
    val output = new String(p.getInputStream.readAllBytes(), StandardCharsets.UTF_8)
    require(p.waitFor() == 0, output)
  }
  "QuizRepository" should {
    "save, upload and activate via the remote while rejecting stale changes" in {
      val parent = Files.createTempDirectory("quiz-repo-test")
      val remote = parent.resolve("remote.git")
      Files.createDirectory(remote)
      git(remote, "init", "--bare", "--initial-branch=main")
      git(parent, "clone", remote.toString, "writer")
      val writer = parent.resolve("writer")
      git(writer, "config", "user.name", "Test")
      git(writer, "config", "user.email", "test@example.invalid")
      Files.writeString(writer.resolve("quiz-config.yml"), "title: Initial\n")
      git(writer, "add", ".")
      git(writer, "commit", "-m", "initial")
      git(writer, "push", "origin", "main")
      git(parent, "clone", remote.toString, "other")
      val repo = new QuizRepository(writer)
      repo.list() must beEmpty
      val start = repo.revision
      repo.save("new.yml", "title: Test\n", start)
      repo.list() must contain("new.yml")
      repo.read("new.yml") mustEqual "title: Test\n"
      repo.save("new.yml", "title: Stale\n", start) must throwA[IllegalArgumentException]
      val media = repo.upload("photo.png", Array[Byte](1, 2, 3), repo.revision)
      media must startWith("uploads/")
      repo.activate("new.yml", repo.revision)
      git(parent.resolve("other"), "pull", "--ff-only")
      Files.readString(parent.resolve("other/quiz-config.yml")) mustEqual "title: Test\n"
      Files.exists(parent.resolve("other/assets").resolve(media)) must beTrue
      repo.read("../invalid.yml") must throwA[IllegalArgumentException]
    }
  }
}
