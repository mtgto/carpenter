package net.mtgto.infrastructure

import java.util.UUID

case class User(id: UUID, name: String, authority: Authority)
