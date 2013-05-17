package net.mtgto.carpenter.infrastructure.vcs

import java.util.UUID

/**
 * Path for subversion (ex. "branches/feature1", "trunk")
 * @param path path to the directory
 * @param isDirectory true if this path is contains subdirectories
 */
case class SubversionPath(
  path: String,
  isDirectory: Boolean
)
