package net.mtgto.infrastructure

import java.util.UUID

case class Project(id: UUID, name: String, hostname: String, recipe: String)
