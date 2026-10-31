package app.controllers

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject
import app.models.quiz.config.QuizConfig
import play.api.Configuration
import play.api.mvc._

final class MasterAuth @Inject()(cc: ControllerComponents, config: QuizConfig, settings: Configuration) extends AbstractController(cc) {
  def login = Action(parse.text(1024)) { request =>
    val supplied = request.body.getBytes(StandardCharsets.UTF_8)
    val expected = config.masterSecret.getBytes(StandardCharsets.UTF_8)
    if (!request.headers.get("X-Requested-With").contains("Quizmaster")) Forbidden
    else if (config.masterSecret.length < 12 || !MessageDigest.isEqual(supplied, expected)) Unauthorized
    else Ok.withSession(request.session + ("quizmaster" -> MasterSession.loginValue(config, settings))).withHeaders("Cache-Control" -> "no-store")
  }
  def logout = Action { request =>
    if (!request.headers.get("X-Requested-With").contains("Quizmaster")) Forbidden
    else Ok.withSession(request.session - "quizmaster").withHeaders("Cache-Control" -> "no-store")
  }
}
