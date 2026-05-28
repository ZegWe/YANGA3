package com.yanga.client.data

class LoginRequiredException : IllegalStateException("Login is required for this read operation")

interface NgaReadOnlyRepository {
  suspend fun loadHome(): Result<HomeReadData>

  suspend fun loadBoards(session: LoginSessionData?): Result<BoardsReadData>

  suspend fun loadMessages(session: LoginSessionData?): Result<MessagesReadData>

  suspend fun loadProfile(session: LoginSessionData?): Result<ProfileReadData>
}
