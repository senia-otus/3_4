package ru.otus.sc.user.service

import ru.otus.sc.user.dao.UserDao.UserDao
import ru.otus.sc.user.model._
import zio.clock.Clock
import zio.logging.Logging
import zio._

object UserService {
  type UserService = Has[Service]

  type Env = Logging with Clock

  trait Service {
    def createUser(request: CreateUserRequest): URIO[Env, CreateUserResponse]
    def getUser(request: GetUserRequest): URIO[Env, GetUserResponse]
    def updateUser(request: UpdateUserRequest): URIO[Env, UpdateUserResponse]
    def deleteUser(request: DeleteUserRequest): URIO[Env, DeleteUserResponse]
    def findUsers(request: FindUsersRequest): URIO[Env, FindUsersResponse]
  }

  val live: URLayer[UserDao, UserService] = ZLayer.fromService { dao =>
    new Service {
      def createUser(request: CreateUserRequest): URIO[Env, CreateUserResponse] =
        dao.createUser(request.user).map(CreateUserResponse)

      def getUser(request: GetUserRequest): URIO[Env, GetUserResponse] =
        dao.getUser(request.userId) map {
          case Some(user) => GetUserResponse.Found(user)
          case None       => GetUserResponse.NotFound(request.userId)
        }

      def updateUser(request: UpdateUserRequest): URIO[Env, UpdateUserResponse] =
        request.user.id match {
          case None => ZIO.succeed(UpdateUserResponse.CantUpdateUserWithoutId)
          case Some(userId) =>
            dao.updateUser(request.user) map {
              case Some(user) => UpdateUserResponse.Updated(user)
              case None       => UpdateUserResponse.NotFound(userId)
            }
        }

      def deleteUser(request: DeleteUserRequest): URIO[Env, DeleteUserResponse] =
        dao
          .deleteUser(request.userId)
          .map {
            _.map(DeleteUserResponse.Deleted)
              .getOrElse(DeleteUserResponse.NotFound(request.userId))
          }

      def findUsers(request: FindUsersRequest): URIO[Env, FindUsersResponse] =
        request match {
          case FindUsersRequest.ByLastName(lastName) =>
            dao.findByLastName(lastName).map(FindUsersResponse.Result)
        }
    }
  }
}
