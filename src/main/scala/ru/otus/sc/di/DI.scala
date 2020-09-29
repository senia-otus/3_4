package ru.otus.sc.di

import ru.otus.sc.config.{HttpConfig, RootConfig}
import ru.otus.sc.db.SlickContext
import ru.otus.sc.route.Router.Router
import ru.otus.sc.route.{Router, ZDirectives}
import ru.otus.sc.user.dao.UserDao
import ru.otus.sc.user.route.UserRouter
import ru.otus.sc.user.service.UserService
import zio.blocking.Blocking
import zio.clock.Clock
import zio.logging.Logging
import zio._

object DI {
  val live: URLayer[Blocking with Clock with Logging, Router with Has[HttpConfig]] =
    (RootConfig.allConfigs ++ ZLayer.requires[Blocking with Clock with Logging]) >+>
      SlickContext.live >+>
      UserDao.live >+>
      (UserService.live ++ ZDirectives.live) >>>
      UserRouter.live >>>
      (Router.live ++ RootConfig.allConfigs)
}
