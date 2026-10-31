package app.controllers

import app.models.quiz.config.QuizConfig
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import play.api.Configuration
import play.api.mvc.RequestHeader

object MasterSession {
  private def token(config: QuizConfig, settings: Configuration): String = {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(new SecretKeySpec(settings.get[String]("play.http.secret.key").getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
    Base64.getUrlEncoder.withoutPadding().encodeToString(mac.doFinal(config.masterSecret.getBytes(StandardCharsets.UTF_8)))
  }
  def valid(request: RequestHeader, config: QuizConfig, settings: Configuration): Boolean = {
    config.masterSecret.length >= 12 && request.session.get("quizmaster").exists { value =>
      MessageDigest.isEqual(value.getBytes(StandardCharsets.UTF_8), token(config, settings).getBytes(StandardCharsets.UTF_8))
    }
  }
  def loginValue(config: QuizConfig, settings: Configuration): String = token(config, settings)
}
