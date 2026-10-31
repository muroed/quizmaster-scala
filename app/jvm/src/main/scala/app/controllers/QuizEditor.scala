package app.controllers

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.inject.{Inject, Singleton}
import app.editor.QuizRepository
import app.models.quiz.config.{QuizConfigParsableValue, ValidatingYamlParser}
import play.api.libs.json._
import play.api.mvc._
import scala.io.Source
import scala.util.{Try, Using}

@Singleton
final class QuizEditor @Inject()(cc: ControllerComponents, parser: QuizConfigParsableValue) extends AbstractController(cc) {
  private val repo = QuizRepository.configured
  private def authorized[A](request: Request[A]): Boolean = {
    val expected = sys.env.get("QUIZ_EDITOR_PASSWORD").filter(_.length >= 20)
    val presented = request.headers.get("Authorization").filter(_.startsWith("Basic ")).flatMap { h =>
      Try(new String(Base64.getDecoder.decode(h.drop(6)), StandardCharsets.UTF_8)).toOption
    }.flatMap { s => val pos = s.indexOf(':'); if (pos >= 0 && s.take(pos) == "editor") Some(s.drop(pos + 1)) else None }
    (for (a <- expected; b <- presented) yield MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8))).getOrElse(false)
  }
  private def sameOrigin[A](request: Request[A]): Boolean =
    request.headers.get("X-Requested-With").contains("QuizmasterEditor") &&
      sys.env.get("QUIZ_EDITOR_ORIGIN").exists(origin => request.headers.get("Origin").contains(origin))
  private def denied = Unauthorized("Authentication required").withHeaders("WWW-Authenticate" -> "Basic realm=\"Quizmaster editor\", charset=\"UTF-8\"", "Cache-Control" -> "no-store")
  private def guarded[A](request: Request[A], write: Boolean = false)(f: => Result): Result = {
    if (!authorized(request)) denied
    else if (write && !sameOrigin(request)) Forbidden("Invalid request origin")
    else try f.withHeaders("Cache-Control" -> "no-store", "X-Content-Type-Options" -> "nosniff")
    catch { case e: IllegalArgumentException => BadRequest(Json.obj("error" -> JsString(Option(e.getMessage).getOrElse("Invalid input"))))
            case _: Exception => InternalServerError(Json.obj("error" -> "Repository operation failed; check server configuration and connectivity")) }
  }
  def index = Action { request => guarded(request) {
    val html = Using.resource(Source.fromResource("quiz-editor.html"))(_.mkString)
    Ok(html).as(HTML).withHeaders("Content-Security-Policy" -> "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; frame-ancestors 'none'")
  }}
  def list = Action { request => guarded(request) { Ok(Json.obj("quizzes" -> repo.list(), "revision" -> repo.revision)) } }
  def read(name: String) = Action { request => guarded(request) { val yaml = repo.read(name); Ok(Json.obj("yaml" -> yaml, "revision" -> repo.revision)) } }
  def save(name: String) = Action(parse.json) { request => guarded(request, write = true) {
    val yaml = (request.body \ "yaml").as[String]
    val revision = (request.body \ "revision").as[String]
    ValidatingYamlParser.parse(yaml, parser)
    repo.save(name, yaml, revision)
    Ok(Json.obj("revision" -> repo.revision))
  }}
  def upload(name: String) = Action(parse.raw(8 * 1024 * 1024)) { request => guarded(request, write = true) {
    val bytes = request.body.asBytes(8 * 1024 * 1024).map(_.toArray).getOrElse(throw new IllegalArgumentException("Missing file"))
    val path = repo.upload(name, bytes, request.headers.get("X-Quiz-Revision").getOrElse(""))
    Ok(Json.obj("path" -> path, "revision" -> repo.revision))
  }}
  def activate(name: String) = Action { request => guarded(request, write = true) {
    val yaml = repo.read(name)
    ValidatingYamlParser.parse(yaml, parser)
    repo.activate(name, request.headers.get("X-Quiz-Revision").getOrElse(""))
    val t = new Thread(() => { Thread.sleep(1000); System.exit(0) }, "quiz-activation")
    t.setDaemon(true); t.start()
    Accepted(Json.obj("message" -> "Quiz activated; application is restarting"))
  }}
}
