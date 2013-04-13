package net.mtgto.carpenter.domain

import java.net.URI

case class SourceRepository(sourceRepositoryType: SourceRepositoryType.Value, uri: URI)
