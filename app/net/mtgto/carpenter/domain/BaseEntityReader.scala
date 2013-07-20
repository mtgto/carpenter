package net.mtgto.carpenter.domain

import org.sisioh.dddbase.core.lifecycle.EntityReader
import org.sisioh.dddbase.core.model.{Entity, Identity}
import scala.util.Try

trait BaseEntityReader[ID <: Identity[_], T <: Entity[ID]] extends EntityReader[ID, T, Try] {
  /**
   * 指定した識別子のエンティティが存在するかを返す。
   *
   * @param identifier 識別子
   * @return Success:
   *          存在する場合はtrue
   *         Failure:
   *          RepositoryExceptionは、リポジトリにアクセスできなかった場合。
   */
  override def contains(identifier: ID): Try[Boolean] = {
    resolve(identifier).map(_ => true)
  }
}
